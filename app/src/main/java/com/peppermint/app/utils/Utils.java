package com.peppermint.app.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.tracking.TrackerManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 18-06-2015.
 * <p>
 *     General utility class.
 * </p>
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH-mm-ss");

    /**
     * Get a presentation friendly string with the supplied duration in the format MM:SS
     * @param millis the duration in milliseconds
     * @return the friendly string
     */
    public static String getFriendlyDuration(long millis) {
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
    public static Point getScreenSize(Context context) {
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
    public static int dpToPx(Context context, int dp) {
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
    public static float pxToDp(Context context, float px) {
        float densityDpi = context.getResources().getDisplayMetrics().densityDpi;
        return px / (densityDpi / 160f);
    }

    /**
     * Obtains the amount of pixels that correspond to the given percentage of the screen width.
     * @param context application context
     * @param percent width percentage value
     * @return value in pixels
     */
    public static float percentScreenWidthToPx(Context context, int percent) {
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
    public static float percentScreenHeightToPx(Context context, int percent) {
        Point size = getScreenSize(context);
        float height = size.y;
        return Math.round(height * ((float) percent) / 100);
    }

    /**
     * Checks if the provided char sequence contains a valid email address.
     * @param email the email address
     * @return true if valid; false if not
     */
    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Checks if the provided char sequence contains a valid phone number.
     * @param phoneNumber the phone number
     * @return true if valid; false if not
     */
    public static boolean isValidPhoneNumber(CharSequence phoneNumber) {
        return !TextUtils.isEmpty(phoneNumber) && android.util.Patterns.PHONE.matcher(phoneNumber).matches();
    }

    /**
     * Scales an image (to avoid OutOfMemory exceptions) and shows it on the specified ImageView.
     * As seen in http://developer.android.com/training/camera/photobasics.html
     *
     * @param v the ImageView
     * @param fullFilePath the file path of the image
     * @param targetW the scaled image desired width
     * @param targetH the scaled image desired height
     */
    public static void setScaledImage(ImageView v, String fullFilePath, int targetW, int targetH) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(fullFilePath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        bmOptions.inDither = true;

        Bitmap bitmap = BitmapFactory.decodeFile(fullFilePath, bmOptions);
        v.setImageBitmap(bitmap);
    }

    /**
     * Checks if internet connection is available.<br />
     * Requires the permission "android.permission.ACCESS_NETWORK_STATE".<br />
     * Doesn't check if the connection works! Just if the device is connected to some network.
     * @param context the app or activity context
     * @return true if the internet connection is available; false otherwise
     */
    public static boolean isInternetAvailable(Context context) {
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
    public static boolean isInternetActive(Context context) {
        try {
            HttpURLConnection urlc = (HttpURLConnection) (new URL("http://peppermint.com").openConnection());
            urlc.setRequestProperty("User-Agent", "Test");
            urlc.setRequestProperty("Connection", "close");
            urlc.setConnectTimeout(2500);
            urlc.connect();
            return (urlc.getResponseCode() == 200);
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
    public static String getBasicAuthenticationToken(String username, String password) {
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
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        //noinspection deprecation
                        boolean isIPv4 = (addr instanceof Inet4Address);
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                                return delim<0 ? sAddr : sAddr.substring(0, delim);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    /**
     * Creates a {@link Bitmap} instance from the supplied {@link Drawable}.
     * @param drawable the drawable
     * @return the bitmap
     */
    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Normalizes the string and replaces/removes all special characters.<br />
     * The resulting string can be safely used as an ID and/or file name.
     * @param str the clean string
     * @return the original string
     */
    public static String normalizeAndCleanString(String str) {
        if(str == null) {
            return null;
        }
        return Normalizer.normalize(str, Normalizer.Form.NFC).replace(' ', '-').replaceAll("[^a-zA-Z0-9\\.]", "");
    }

    /**
     * Use {@link ExifInterface} to extract image file attributes in order to assess the required
     * rotation in degrees (depends on orientation mode when taking the picture). <br/>
     * Rotate the bitmap accordingly.
     *
     * @param realImage the bitmap to rotate
     * @param filePath the image file path of the bitmap
     * @return the rotated bitmap
     */
    public static Bitmap getRotatedBitmapFromFileAttributes(Bitmap realImage, String filePath) {
        Bitmap rotatedImage = realImage;

        try {
            ExifInterface exif = new ExifInterface(filePath);

            String tagOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (tagOrientation.equalsIgnoreCase("6")) {
                rotatedImage = Utils.getRotatedBitmap(realImage, 90);
                realImage.recycle();
            } else if (tagOrientation.equalsIgnoreCase("8")) {
                rotatedImage = Utils.getRotatedBitmap(realImage, 270);
                realImage.recycle();
            } else if (tagOrientation.equalsIgnoreCase("3")) {
                rotatedImage = Utils.getRotatedBitmap(realImage, 180);
                realImage.recycle();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to adjust bitmap rotation", e);
            Crashlytics.logException(e);
        }

        return rotatedImage;
    }

    /**
     * Rotate the supplied bitmap by the supplied degrees.
     * @param bitmap the original bitmap
     * @param degrees the rotation degress
     * @return the new rotated bitmap
     */
    public static Bitmap getRotatedBitmap(Bitmap bitmap, int degrees) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degrees);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    private static float getBitmapRequiredScale(InputStream fis, int width, int height) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(fis, null, o);

        return getBitmapRequiredScale(o, width, height);
    }

    /**
     * Returns the multiplier necessary to adjust the scale of the bitmap image to
     * the supplied width and height. The multiplier is obtained from the smallest height/width length value.
     * This keeps aspect ratio.
     * @param o the bitmap image options
     * @param width the desired width
     * @param height the desired height
     * @return the scale/multiplier
     */
    private static float getBitmapRequiredScale(BitmapFactory.Options o, int width, int height) {
        float scale = 1;
        // if image height is greater than width
        if (o.outHeight > o.outWidth) {
            scale = (float) o.outWidth / (float) width;
        }
        // if image width is greater than height
        else {
            scale = (float) o.outHeight / (float) height;
        }

        return scale;
    }

    private static Bitmap getScaledBitmap(InputStream fis, int scale) {
        // decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(fis, null, o2);
    }

    /**
     * Read and load the bitmap in the provided {@link Uri} and scale it
     * using the {@link android.graphics.BitmapFactory.Options#inSampleSize} option.<br />
     * The scale multiplier is obtained using {@link #getBitmapRequiredScale(InputStream, int, int)}.<br />
     * <b>Aspect ratio is kept. See {@link #getScaledResizedBitmap(String, int, int, boolean)} for a strict resize.</b>
     *
     * @param context the app or activity context
     * @param contentUri the location of the bitmap image
     * @param width the new desired width
     * @param height the new desired height
     * @return the loaded and scaled bitmap
     */
    public static Bitmap getScaledBitmap(Context context, Uri contentUri, int width, int height) {
        Bitmap bitmap = null;
        try {
            InputStream fis = context.getContentResolver().openInputStream(contentUri);
            int scale = Math.round(getBitmapRequiredScale(fis, width, height));
            fis.close();

            fis = context.getContentResolver().openInputStream(contentUri);
            bitmap = getScaledBitmap(fis, scale);
            fis.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to scale bitmap", e);
            TrackerManager.getInstance(context.getApplicationContext()).logException(e);
        }
        return bitmap;
    }

    /**
     * Read and load the bitmap in the provided image file and scale it
     * using the {@link android.graphics.BitmapFactory.Options#inSampleSize} option.<br />
     * The scale multiplier is obtained using {@link #getBitmapRequiredScale(InputStream, int, int)}.<br />
     * <b>Aspect ratio is kept. See {@link #getScaledResizedBitmap(String, int, int, boolean)} for a strict resize.</b>
     *
     * @param filePath the image file path
     * @param width the new desired width
     * @param height the new desired height
     * @return the loaded and scaled bitmap
     */
    public static Bitmap getScaledBitmap(String filePath, int width, int height) {
        Bitmap bitmap = null;
        try {
            InputStream fis = new FileInputStream(filePath);
            int scale = Math.round(getBitmapRequiredScale(fis, width, height));
            fis.close();

            fis = new FileInputStream(filePath);
            bitmap = getScaledBitmap(fis, scale);
            fis.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to scale bitmap", e);
            Crashlytics.logException(e);
        }
        return bitmap;
    }

    /**
     * Read and load the bitmap in the provided image resource and scale it
     * using the {@link android.graphics.BitmapFactory.Options#inSampleSize} option.<br />
     * The scale multiplier is obtained using {@link #getBitmapRequiredScale(InputStream, int, int)}.<br />
     * <b>Aspect ratio is kept. See {@link #getScaledResizedBitmap(Context, int, int, int, boolean)} for a strict resize.</b>
     *
     * @param context the context
     * @param resId the resource id (image/drawable id)
     * @param width the scaled image width
     * @param height the scaled image height
     * @return the scaled image bitmap
     */
    public static Bitmap getScaledBitmap(Context context, int resId, int width, int height) {
        Bitmap bitmap = null;
        try {
            InputStream fis = context.getResources().openRawResource(resId);
            int scale = Math.round(getBitmapRequiredScale(fis, width, height));
            fis.close();

            fis = context.getResources().openRawResource(resId);
            bitmap = getScaledBitmap(fis, scale);
            fis.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to scale bitmap", e);
            TrackerManager.getInstance(context.getApplicationContext()).logException(e);
        }
        return bitmap;
    }

    /**
     * Read and load the bitmap in the provided image file and scale it to the provided width and height values.<br />
     * If the aspect ratio is to be kept, it is the same as using {@link #getScaledBitmap(String, int, int)}.
     *
     * @param filePath the image file path
     * @param width the new desired width
     * @param height the new desired height
     * @param keepAspectRatio true to keep aspect ratio; false otherwise
     * @return the new scaled/resized bitmap
     */
    public static Bitmap getScaledResizedBitmap(String filePath, int width, int height, boolean keepAspectRatio) {
        Bitmap bitmap = getScaledBitmap(filePath, width, height);

        if(bitmap != null && !keepAspectRatio) {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
            bitmap.recycle();
            bitmap = resized;
        }

        return bitmap;
    }

    /**
     * Read and load the bitmap in the provided image resource and scale it to the provided width and height values.<br />
     * If the aspect ratio is to be kept, it is the same as using {@link #getScaledBitmap(Context, Uri, int, int)}.
     *
     * @param context the context
     * @param resId the resource id (image/drawable id)
     * @param width the new desired width
     * @param height the new desired height
     * @param keepAspectRatio true to keep aspect ratio; false otherwise
     * @return the new scaled/resized bitmap
     */
    public static Bitmap getScaledResizedBitmap(Context context, int resId, int width, int height, boolean keepAspectRatio) {
        Bitmap bitmap = getScaledBitmap(context, resId, width, height);

        if(bitmap != null && !keepAspectRatio) {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
            bitmap.recycle();
            bitmap = resized;
        }

        return bitmap;
    }

    /**
     * Get a drawable from resources according to the current API.
     * @param context the context
     * @param drawableRes the drawable resource id
     * @return the drawable instance
     */
    public static Drawable getDrawable(Context context, int drawableRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return context.getResources().getDrawable(drawableRes, context.getTheme());
        } else {
            //noinspection deprecation
            return context.getResources().getDrawable(drawableRes);
        }
    }

    /**
     * Get a color from resources according to the current API.
     * @param context the context
     * @param colorRes the color resource id
     * @return the color
     */
    public static int getColor(Context context, int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * Get the status bar height according to the current API and theme.
     * @param context the context
     * @return the height in pixels
     */
    public static int getStatusBarHeight(Context context) {
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
    public static String[] getUserData(Context context) {
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
    public static boolean isSimAvailable(Context context) {
        if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return false;
        }
        TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    /**
     * Get current timestamp in the "yyyy-MM-dd HH-mm-ss" format.
     * @return the current timestamp
     */
    public static String getCurrentTimestamp() {
        return DATETIME_FORMAT.format(Calendar.getInstance().getTime());
    }

    /**
     * Parse timestamps into a date object. The timestamp format must be "yyyy-MM-dd HH-mm-ss".
     * @param ts the timestamp string
     * @return the timestamp date instance
     * @throws ParseException
     */
    public static Date parseTimestamp(String ts) throws ParseException {
        return DATETIME_FORMAT.parse(ts);
    }

    /**
     * Fully capitalize the words in the supplied string.<br />
     * The string is also trimmed.<br />
     * E.g. "fOO bAR" or "foo bar" become "Foo Bar".
     *
     * @param s the string
     * @return the fully capitalized string
     */
    public static String capitalizeFully(String s) {
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
    public static void hideKeyboard(Activity context) {
        context.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

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

    /**
     * Show the keyboard
     * @param context the activity
     */
    public static void showKeyboard(Activity context) {
        context.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        );
        View view = context.getWindow().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Converts an array of shorts into an array of bytes (double the length).
     * @param sData the short array
     * @return the byte array
     */
    public static byte[] short2Byte(short[] sData, byte[] bytes) {
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
}
