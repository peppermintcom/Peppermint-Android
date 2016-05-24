package com.peppermint.app.utils;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.peppermint.app.BuildConfig;
import com.peppermint.app.R;
import com.peppermint.app.tracking.TrackerManager;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 18-06-2015.
 * <p>
 *     General utility class.
 * </p>
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    /**
     * Get a presentation friendly string with the supplied duration in the format MM:SS
     * @param millis the duration in milliseconds
     * @return the friendly string
     */
    public static String getFriendlyDuration(final long millis) {
        long totalSecs = millis / 1000;

        long mins = totalSecs / 60;
        long secs = totalSecs % 60;

        return String.format("%02d:%02d", mins, secs);
    }

    /**
     * Parse the supplied full name and return an array with two strings.<br />
     * The full name is split by spaces. The very last word becomes the last name.
     * The remaining words are the first name.<br/>
     *
     * At position 0 is the first name. At position 1 is the last name.
     *
     * @param fullName the full name
     * @return the two strings containing the first and last names
     */
    public static String[] getFirstAndLastNames(String fullName) {
        String[] names = new String[]{"", ""};

        if(fullName == null || fullName.length() <= 0) {
            return names;
        }

        fullName = Utils.capitalizeFully(fullName);
        String[] tmpNames = fullName.split("\\s+");

        if(tmpNames.length > 1) {
            names[1] = tmpNames[tmpNames.length - 1];
            names[0] = fullName.substring(0, fullName.length() - tmpNames[tmpNames.length - 1].length()).trim();
        } else {
            names[0] = fullName;
        }

        return names;
    }

    /**
     * Obtains the screen size in pixels.
     * @param context application context
     * @return Point structure withe the screen size
     */
    public static Point getScreenSize(final Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    /**
     * Converts a value in dp to pixels according to screen density and resolution.
     * @param context application context
     * @param dp value in dps
     * @return value in pixels
     */
    public static int dpToPx(final Context context, final int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * Converts device specific pixels to density independent pixels.
     *
     * @param px      A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float pxToDp(final Context context, final float px) {
        float densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        return px / (densityDpi / 160f);
    }

    /**
     * Obtains the amount of pixels that correspond to the given percentage of the screen width.
     * @param context application context
     * @param percent width percentage value
     * @return value in pixels
     */
    public static float percentScreenWidthToPx(final Context context, final int percent) {
        Point size = getScreenSize(context);
        float width = size.x;
        return Math.round(width * ((float) percent) / 100);
    }

    /**
     * Obtains the amount of pixels that correspond to the given percentage of the screen height.
     * @param context application context
     * @param percent height percentage value
     * @return value in pixels
     */
    public static float percentScreenHeightToPx(final Context context, final int percent) {
        Point size = getScreenSize(context);
        float height = size.y;
        return Math.round(height * ((float) percent) / 100);
    }

    /**
     * Checks if the provided char sequence contains a valid email address.
     * @param email the email address
     * @return true if valid; false if not
     */
    public static boolean isValidEmail(final CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Checks if the provided char sequence contains a valid phone number.
     * @param phoneNumber the phone number
     * @return true if valid; false if not
     */
    public static boolean isValidPhoneNumber(final CharSequence phoneNumber) {
        return !TextUtils.isEmpty(phoneNumber) && android.util.Patterns.PHONE.matcher(phoneNumber).matches();
    }

    /**
     * Checks if the provided char sequence contains a valid name.
     * @param text the name
     * @return true if valid; false if not
     */
    public static boolean isValidName(final CharSequence text) {
        return text != null && !TextUtils.isEmpty(text);
    }

    public static boolean isValidNameMaybeEmpty(final CharSequence text) {
        return true;
    }

    /**
     * Checks if internet connection is available.<br />
     * Requires the permission "android.permission.ACCESS_NETWORK_STATE".<br />
     * Doesn't check if the connection works! Just if the device is connected to some network.
     * @param context the app or activity context
     * @return true if the internet connection is available; false otherwise
     */
    public static boolean isInternetAvailable(final Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        //should check null because in air plan mode it will be null
        return (netInfo != null && netInfo.isConnected());
    }

    /**
     * Actually checks if there's internet connectivity by connecting to the Peppermint web server.<br />
     * Times out after 2500 ms. <strong>Must be invoked from a secondary thread.</strong>
     * @param context the app or activity context
     * @return true if there's connectivity; false otherwise
     */
    public static boolean isInternetActive(final Context context) {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://peppermint.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(2500);
            urlc.connect();
            int code = urlc.getResponseCode() / 100;
            return (code >= 1 && code <= 5);
        } catch (IOException e) {
            Log.e(Utils.class.getSimpleName(), "Error checking internet connection", e);
            return false;
        }
    }

    /**
     * Obtains a Basic Authentication token that can be transmitted through the "Authorization" HTTP header parameter.
     * @param username the username
     * @param password the password
     * @return the basic authentication token in Base64
     */
    public static String getBasicAuthenticationToken(final String username, final String password) {
        String decoded = username + ":" + password;
        byte[] decodedBytes;
        try {
            decodedBytes = decoded.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            throw new RuntimeException(e1);
        }
        return Base64.encodeToString(decodedBytes, Base64.URL_SAFE | Base64.NO_WRAP);
    }

    /**
     * Get a friendly string containing the Android version and release name.
     * @return the string
     */
    public static String getAndroidVersion() {
        String release = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        return "Android SDK: " + sdkVersion + " (" + release + ")";
    }

    /**
     * Get a friendly string containing the device manufacturer and model.
     * @return the string
     */
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return model;
        }
        return manufacturer + " " + model;
    }

    /**
     * Normalizes the string and replaces/removes all special characters.<br />
     * The resulting string can be safely used as an ID and/or file name.
     * @param str the clean string
     * @return the original string
     */
    public static String normalizeAndCleanString(final String str) {
        if(str == null) {
            return null;
        }
        return Normalizer.normalize(str, Normalizer.Form.NFC).replace(' ', '-').replaceAll("[^a-zA-Z0-9\\.]", "");
    }

    /**
     * Get the status bar height according to the current API and theme.
     * @param context the context
     * @return the height in pixels
     */
    public static int getStatusBarHeight(final Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Tries to get user information from the contacts content provider (requires permission READ_CONTACTS/WRITE_CONTACTS).<br />
     *
     * @param context the context
     * @return an array of strings containing: 0) the display name, 1) the photo URI; null if no data is found or doesn't have permission
     */
    public static String[] getUserData(final Context context) {
        String[] data = new String[2];
        try {
            Cursor cursor = context.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                    data[0] = cursor.getString(cursor.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME));
                    long photoId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Profile.PHOTO_URI));
                    if (photoId > 0) {
                        data[1] = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                photoId).toString();
                    }
                }
                cursor.close();
            }
        } catch(SecurityException e) {
            Log.e(TAG, "Permission READ_CONTACTS/WRITE_CONTACTS not granted!", e);
        }

        return data;
    }

    /**
     * Check if the device supports telephony and if a SIM card is available.
     * @param context the context
     * @return true if supported; false if not
     */
    public static boolean isSimAvailable(final Context context) {
        if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return false;
        }
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    /**
     * Fully capitalize the words in the supplied string.<br />
     * The string is also trimmed.<br />
     * E.g. "fOO bAR" or "foo bar" become "Foo Bar".
     *
     * @param s the string
     * @return the fully capitalized string
     */
    public static String capitalizeFully(final String s) {
        if(s == null || s.length() <= 0) {
            return s;
        }

        StringBuilder builder = new StringBuilder();
        String[] words = s.split("\\s+");
        for(int i=0; i<words.length; i++) {
            if(words[i].length() > 0) {
                if (i > 0) {
                    builder.append(" ");
                }
                builder.append(words[i].substring(0, 1).toUpperCase());
                builder.append(words[i].substring(1).toLowerCase());
            }
        }

        return builder.toString();
    }

    /**
     * Hide the keyboard
     * @param context the activity
     */
    public static void hideKeyboard(final Activity context, final Integer additionalModes) {
        int modes = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
        if(additionalModes != null) {
            modes |= additionalModes;
        }

        context.getWindow().setSoftInputMode(modes);

        View view = context.getWindow().getCurrentFocus();

        if(view == null) {
            Log.d(TAG, "Null Focused View - Getting Root View...");
            view = context.findViewById(android.R.id.content);
        }

        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void hideKeyboard(Activity context) {
        hideKeyboard(context, null);
    }

    /**
     * Show the keyboard
     * @param context the activity
     */
    public static void showKeyboard(final Activity context, View view, final Integer additionalModes) {
        int modes = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
        if(additionalModes != null) {
            modes |= additionalModes;
        }

        context.getWindow().setSoftInputMode(modes);

        if(view == null) {
            view = context.getWindow().getCurrentFocus();
        }
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
        }
    }

    public static void showKeyboard(final Activity context, final Integer additionalModes) {
        showKeyboard(context, null, additionalModes);
    }

    /**
     * Converts an array of shorts into an array of bytes (double the length).
     * @param sData the short array
     * @return the byte array
     */
    public static byte[] short2Byte(final short[] sData, byte[] bytes) {
        int shortArrsize = sData.length;
        if(bytes == null) {
            bytes = new byte[shortArrsize * 2];
        }

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }

        return bytes;
    }

    /**
     * Joins the supplied strings into a single string, adding the separator string in-between.
     * @param separator the separator/divider
     * @param strs the set of strings to join
     * @return the joined string
     */
    public static String joinString(final String separator, final String... strs) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String str : strs) {
            if(str != null && str.length() > 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append(separator);
                }
                sb.append(str);
            }
        }
        String result = sb.toString();
        if(result.length() <= 0) {
            return "(1=1)";
        }
        return "(" + sb.toString() + ")";
    }

    /**
     * Builds a string with a set of conditions in SQL syntax (e.g. the where part).
     * @param field the restricted field
     * @param allowed the set of allowed values
     * @param args the set of allowed values in string format
     * @param isAnd the separator between conditions (either AND or OR)
     * @param <T> the allowed values type
     * @return the conditions string
     */
    public static <T> String getSQLConditions(final String field, final List<T> allowed, final List<String> args, final boolean isAnd) {
        if(allowed == null || allowed.size() <= 0) {
            return "1";
        }

        StringBuilder b = new StringBuilder();
        for(int i=0; i<allowed.size(); i++) {
            if(i != 0) {
                b.append(isAnd ? " AND " : " OR ");
            }

            if(args != null) {
                args.add(allowed.get(i).toString());
                b.append(field);
                b.append("=?");
            } else {
                b.append(field);
                b.append("=");
                b.append(allowed.get(i).toString());
            }
        }
        return b.toString();
    }

    /**
     * Deletes all app files in app data directory.
     * @param context the app context
     */
    public static void clearApplicationData(final Context context) {
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()) {
            String[] children = appDir.list();
            for (String s : children) {
                File f = new File(appDir, s);
                if(deleteDir(f)) {
                    TrackerManager.getInstance(context).log("clearApplicationData() - Deleted " + f.getAbsolutePath());
                }
            }
        }
    }

    private static boolean deleteDir(final File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir != null && dir.delete();
    }

    /**
     * Gets the navigation bar height. Tries to find out if the device has a soft navigation bar.
     * If not, returns 0.
     *
     * @param context the app context
     * @return the height in pixels
     */
    public static int getNavigationBarHeight(final Context context) {
        int result = 0;
        boolean hasMenuKey = ViewConfiguration.get(context).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);

        if(!hasMenuKey && !hasBackKey) {
            //The device has a navigation bar
            Resources resources = context.getResources();

            int orientation = resources.getConfiguration().orientation;
            int resourceId;
            if (isTablet(context)){
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_height_landscape", "dimen", "android");
            }  else {
                resourceId = resources.getIdentifier(orientation == Configuration.ORIENTATION_PORTRAIT ? "navigation_bar_height" : "navigation_bar_width", "dimen", "android");
            }

            if (resourceId > 0) {
                return resources.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * Checks if the device is a tablet, according to layout configuration and parameters.
     * @param context the app context
     * @return true if it's a tablet; false otherwise
     */
    public static boolean isTablet(final Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }


    /**
     * Returns the first clickable view found at the specified XY coordinates on screen.
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param root the root view to search
     * @return the found View or null if no clickable view is found
     */
    public static View getClickableViewAtLocation(final float x, final float y, final View root) {
        if(root instanceof ViewGroup) {
            ViewGroup v = (ViewGroup) root;
            int count = v.getChildCount();
            for(int i=0; i<count; i++) {
                View childFocusable = getClickableViewAtLocation(x, y, v.getChildAt(i));
                if(childFocusable != null) {
                    return childFocusable;
                }
            }
        }
        return root.isClickable() && isPointInsideView(x, y, root) ? root : null;
    }

    /**
     * Determines if given points are inside view
     * @param x - x coordinate of point
     * @param y - y coordinate of point
     * @param view - view object to compare
     * @return true if the points are within view bounds, false otherwise
     */
    private static boolean isPointInsideView(final float x, final float y, final View view) {
        int location[] = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        // point is inside view bounds
        return ((x > viewX && x < (viewX + view.getWidth())) &&
                (y > viewY && y < (viewY + view.getHeight())));
    }

    /**
     * Checks if the screen is on (not sleeping) and unlocked.
     * @param context the app context
     * @return true if on and unlocked; false otherwise
     */
    public static boolean isScreenOnAndUnlocked(final Context context) {
        boolean screenOn;
        final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = powerManager.isInteractive();
        } else {
            //noinspection deprecation
            screenOn = powerManager.isScreenOn();
        }

        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return screenOn && !keyguardManager.inKeyguardRestrictedInputMode();
    }

    /**
     * Launches the native email app, pre-filled with Peppermint's support email template.
     * @param context the app context
     */
    public static void triggerSupportEmail(final Context context) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + context.getString(R.string.support_email)));
        i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_text_subject));
        i.putExtra(Intent.EXTRA_TEXT, String.format(context.getString(R.string.support_text_body), Utils.getDeviceName(), Utils.getAndroidVersion(), BuildConfig.VERSION_NAME));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(i, context.getString(R.string.send_email)));
    }

    public static Bundle getParamsFromUri(final Uri uri) {
        Bundle bundle = null;
        if(uri != null) {
            bundle = new Bundle();
            for(String paramName : uri.getQueryParameterNames()) {
                String paramValue = uri.getQueryParameter(paramName);
                if (paramValue != null) {
                    bundle.putString(paramName, paramValue);
                }
            }
        }
        return bundle;
    }

    /**
     * Logs all key-value pairs in a {@link Bundle}.
     * @param bundle the bundle
     * @param parent the parent bundle name (or null if none)
     */
    public static void logBundle(final Bundle bundle, final String parent) {
        for (String key : bundle.keySet()) {
            key = (parent == null ? key : parent + "." + key);
            Object obj = bundle.get(key);
            if(obj instanceof Bundle) {
                logBundle((Bundle) obj, key);
            } else {
                Log.d(TAG, key + " = " + obj);
            }
        }
    }

    /**
     * Modified from:
     * https://github.com/apache/cordova-plugin-globalization/blob/master/src/android/Globalization.java
     *
     * Returns a well-formed ITEF BCP 47 language tag representing this locale string
     * identifier for the client's current locale
     *
     * @return String: The BCP 47 language tag for the current locale
     */
    public static String toBcp47LanguageTag(Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locale.toLanguageTag();
        }

        // we will use a dash as per BCP 47
        final char SEP = '-';
        String language = locale.getLanguage();
        String region = locale.getCountry();
        String variant = locale.getVariant();

        // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
        // this goes before the string matching since "NY" wont pass the variant checks
        if (language.equals("no") && region.equals("NO") && variant.equals("NY")) {
            language = "nn";
            region = "NO";
            variant = "";
        }

        if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
            language = "und";       // Follow the Locale#toLanguageTag() implementation
            // which says to return "und" for Undetermined
        } else if (language.equals("iw")) {
            language = "he";        // correct deprecated "Hebrew"
        } else if (language.equals("in")) {
            language = "id";        // correct deprecated "Indonesian"
        } else if (language.equals("ji")) {
            language = "yi";        // correct deprecated "Yiddish"
        }

        // ensure valid country code, if not well formed, it's omitted
        if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
            region = "";
        }

        // variant subtags that begin with a letter must be at least 5 characters long
        if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
            variant = "";
        }

        StringBuilder bcp47Tag = new StringBuilder(language);
        if (!region.isEmpty()) {
            bcp47Tag.append(SEP).append(region);
        }
        if (!variant.isEmpty()) {
            bcp47Tag.append(SEP).append(variant);
        }

        return bcp47Tag.toString();
    }

}
