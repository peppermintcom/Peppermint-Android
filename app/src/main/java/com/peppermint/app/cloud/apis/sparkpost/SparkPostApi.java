package com.peppermint.app.cloud.apis.sparkpost;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.preference.PreferenceManager;
import android.util.Log;

import com.peppermint.app.R;
import com.peppermint.app.cloud.apis.BaseApi;
import com.peppermint.app.cloud.rest.HttpRequest;
import com.peppermint.app.cloud.rest.HttpResponse;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.utils.DateContainer;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nuno Luz on 22-04-2016.
 * <p>
 *      SparkPost API operations wrapper.
 * </p>
 */
public class SparkPostApi extends BaseApi {

    private static final String TAG = BaseApi.class.getSimpleName();

    public static final String PREF_LAST_TEMPLATE_UPDATE_TIMESTAMP = TAG + "_lastTemplateUpdateTimestamp";

    private static final Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\{{2,3}([^\\}]+)\\}{2,3}");
    private static final int HOURS_BEFORE_TEMPLATE_UPDATE = 24;

    public static final int TYPE_HTML = 1;
    public static final int TYPE_TEXT = 2;

    private static final String EMAIL_TEMPLATE_LOCALFILE_HTML = ".email_template.html";
    private static final String EMAIL_TEMPLATE_LOCALFILE_TEXT = ".email_template.txt";

    private static final String EMAIL_TEMPLATE_ENDPOINT = "https://api.sparkpost.com/api/v1/templates/audio-mail-template";
    private static final String EMAIL_TEMPLATE_API_KEY = "2e3edee129b485f914cb1f2ed5c29fd5df6d3dcf";

    // HttpResponse that parses SparkPost JSON and returns the email template
    public class EmailTemplateResponse extends HttpResponse {
        private String mHtmlTemplate, mTextTemplate, mLastUpdateTime;
        public EmailTemplateResponse() {
            super();
        }
        @Override
        public void readBody(InputStream inStream, HttpRequest request) throws Throwable {
            super.readBody(inStream, request);

            final JSONObject json = new JSONObject(String.valueOf(getBody()));
            final JSONObject jsonResults = json.getJSONObject("results");
            mLastUpdateTime = jsonResults.getString("last_update_time");

            final JSONObject jsonContent = jsonResults.getJSONObject("content");
            mHtmlTemplate = jsonContent.getString("html");
            mTextTemplate = jsonContent.getString("text");
        }
        public String getHtmlTemplate() { return mHtmlTemplate; }
        public String getTextTemplate() { return mTextTemplate; }
        public String getLastUpdateTime() { return mLastUpdateTime; }
        public final Creator<EmailTemplateResponse> CREATOR = new Creator<EmailTemplateResponse>() {
            public EmailTemplateResponse createFromParcel(Parcel in) {
                return new EmailTemplateResponse(in);
            }
            public EmailTemplateResponse[] newArray(int size) {
                return new EmailTemplateResponse[size];
            }
        };
        protected EmailTemplateResponse(Parcel in) {
            super(in);
        }
    }

    public SparkPostApi(final Context mContext) {
        super(mContext);
    }

    @Override
    protected void processGenericExceptions(final HttpRequest request, final HttpResponse response) throws Exception {
        super.processGenericExceptions(request, response);

        if(response.getCode() / 100 != 2) {
            throw new SparkPostApiResponseException(response.getCode(), request.toString());
        }
    }

    public synchronized EmailTemplateResponse getEmailTemplate(final String requesterId) throws Exception {
        final HttpRequest request = new HttpRequest(EMAIL_TEMPLATE_ENDPOINT, HttpRequest.METHOD_GET, false);
        request.setHeaderParam("Authorization", EMAIL_TEMPLATE_API_KEY);

        final EmailTemplateResponse response = executeRequest(requesterId, request, new EmailTemplateResponse(), false);
        processGenericExceptions(request, response);

        final FileOutputStream outHtml = mContext.openFileOutput(EMAIL_TEMPLATE_LOCALFILE_HTML, Context.MODE_PRIVATE);
        outHtml.write(response.getHtmlTemplate().getBytes(Charset.forName("UTF-8")));
        outHtml.close();

        final FileOutputStream outText = mContext.openFileOutput(EMAIL_TEMPLATE_LOCALFILE_TEXT, Context.MODE_PRIVATE);
        outText.write(response.getTextTemplate().getBytes(Charset.forName("UTF-8")));
        outText.close();

        return response;
    }

    public String buildEmailFromTemplate(Context context, String shortUrl, String canonicalUrl, String replyName, String replyEmail, int type, boolean tryToRefreshTemplate, String requesterId, String transcription)
            throws IOException, ParseException {

        if(tryToRefreshTemplate) {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            final String lastTemplateUpdateTimestamp = sharedPreferences.getString(PREF_LAST_TEMPLATE_UPDATE_TIMESTAMP, null);
            final DateContainer currentDate = new DateContainer();
            boolean exceededHoursBeforeTemplateUpdate = false;

            if(lastTemplateUpdateTimestamp != null) {
                final DateContainer lastTemplateUpdateDate = new DateContainer(DateContainer.TYPE_DATETIME, lastTemplateUpdateTimestamp);
                exceededHoursBeforeTemplateUpdate = (currentDate.getCalendar().getTimeInMillis() - lastTemplateUpdateDate.getCalendar().getTimeInMillis()) / 3600000 > HOURS_BEFORE_TEMPLATE_UPDATE;
                Log.d(TAG, "Time since template update " + ((currentDate.getCalendar().getTimeInMillis() - lastTemplateUpdateDate.getCalendar().getTimeInMillis()) / 3600000));
            }

            if(lastTemplateUpdateTimestamp == null || exceededHoursBeforeTemplateUpdate) {
                try {
                    getEmailTemplate(requesterId);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(PREF_LAST_TEMPLATE_UPDATE_TIMESTAMP, currentDate.toString());
                    editor.commit();
                } catch (Exception e) {
                    TrackerManager.getInstance(context).logException(e);
                }
            }
        }

        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("transcription", transcription);
        parameterMap.put("canonical_url", canonicalUrl);
        parameterMap.put("url", shortUrl);
        parameterMap.put("replyLink", "https://peppermint.com/reply?name=" + (replyName == null ? "" : URLEncoder.encode(replyName, "UTF-8")) + "&mail=" + URLEncoder.encode(replyEmail, "UTF-8"));

        final StringBuffer bodyBuilder = new StringBuffer();

        InputStream inputStream;
        try {
            inputStream = context.openFileInput(type == TYPE_HTML ? EMAIL_TEMPLATE_LOCALFILE_HTML : EMAIL_TEMPLATE_LOCALFILE_TEXT);
        } catch(FileNotFoundException e) {
            Log.d(TAG, "Remote template not found. Using local template...");
            inputStream = context.getResources().openRawResource(type == TYPE_HTML ? R.raw.email_template_html : R.raw.email_template_plain);
        }

        final Stack<Boolean> blockStack = new Stack<>();
        blockStack.push(true);  // if block stack; starts with true to include all content
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                final Matcher matcher = SUBSTITUTION_PATTERN.matcher(line);
                int lastIndex = 0;
                while (matcher.find()) {
                    final String before = line.substring(lastIndex, matcher.start());
                    lastIndex = matcher.end();
                    String entry = matcher.group(1).trim();

                    if(blockStack.peek()) {
                        bodyBuilder.append(before);
                    }

                    if (entry.startsWith("if ")) {
                        // start of if block
                        final String key = entry.substring(3);
                        blockStack.push(parameterMap.containsKey(key) && parameterMap.get(key) != null);
                    } else if (entry.compareToIgnoreCase("end") == 0) {
                        // end of if block
                        blockStack.pop();
                    } else {
                        if(blockStack.peek()) {
                            Iterator<String> keyIt = parameterMap.keySet().iterator();
                            boolean matched = false;
                            while (keyIt.hasNext() && !matched) {
                                String parameterKey = keyIt.next();
                                if (entry.compareTo(parameterKey) == 0) {
                                    entry = parameterMap.get(parameterKey);
                                    if (entry == null) {
                                        entry = "";
                                    }
                                    matched = true;
                                }
                            }
                            bodyBuilder.append(matched ? entry : "");
                        }
                    }
                }
                if (blockStack.peek()) {
                    bodyBuilder.append(line.substring(lastIndex));
                    bodyBuilder.append("\n");
                }
            }
        } finally {
            reader.close();
            blockStack.clear();
        }

        return bodyBuilder.toString();
    }

}
