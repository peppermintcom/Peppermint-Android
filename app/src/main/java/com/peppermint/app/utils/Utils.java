package com.peppermint.app.utils;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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
import java.util.regex.Pattern;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 18-06-2015.
 * <p>
 *     General utility class.
 * </p>
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();
    private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd' 'HH-mm-ss");
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\(\\) \\u0041-\\u005A\\u0061-\\u007A\\u00AA\\u00B5\\u00BA\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02C1\\u02C6-\\u02D1\\u02E0-\\u02E4\\u02EC\\u02EE\\u0370-\\u0374\\u0376\\u0377\\u037A-\\u037D\\u0386\\u0388-\\u038A\\u038C\\u038E-\\u03A1\\u03A3-\\u03F5\\u03F7-\\u0481\\u048A-\\u0527\\u0531-\\u0556\\u0559\\u0561-\\u0587\\u05D0-\\u05EA\\u05F0-\\u05F2\\u0620-\\u064A\\u066E\\u066F\\u0671-\\u06D3\\u06D5\\u06E5\\u06E6\\u06EE\\u06EF\\u06FA-\\u06FC\\u06FF\\u0710\\u0712-\\u072F\\u074D-\\u07A5\\u07B1\\u07CA-\\u07EA\\u07F4\\u07F5\\u07FA\\u0800-\\u0815\\u081A\\u0824\\u0828\\u0840-\\u0858\\u08A0\\u08A2-\\u08AC\\u0904-\\u0939\\u093D\\u0950\\u0958-\\u0961\\u0971-\\u0977\\u0979-\\u097F\\u0985-\\u098C\\u098F\\u0990\\u0993-\\u09A8\\u09AA-\\u09B0\\u09B2\\u09B6-\\u09B9\\u09BD\\u09CE\\u09DC\\u09DD\\u09DF-\\u09E1\\u09F0\\u09F1\\u0A05-\\u0A0A\\u0A0F\\u0A10\\u0A13-\\u0A28\\u0A2A-\\u0A30\\u0A32\\u0A33\\u0A35\\u0A36\\u0A38\\u0A39\\u0A59-\\u0A5C\\u0A5E\\u0A72-\\u0A74\\u0A85-\\u0A8D\\u0A8F-\\u0A91\\u0A93-\\u0AA8\\u0AAA-\\u0AB0\\u0AB2\\u0AB3\\u0AB5-\\u0AB9\\u0ABD\\u0AD0\\u0AE0\\u0AE1\\u0B05-\\u0B0C\\u0B0F\\u0B10\\u0B13-\\u0B28\\u0B2A-\\u0B30\\u0B32\\u0B33\\u0B35-\\u0B39\\u0B3D\\u0B5C\\u0B5D\\u0B5F-\\u0B61\\u0B71\\u0B83\\u0B85-\\u0B8A\\u0B8E-\\u0B90\\u0B92-\\u0B95\\u0B99\\u0B9A\\u0B9C\\u0B9E\\u0B9F\\u0BA3\\u0BA4\\u0BA8-\\u0BAA\\u0BAE-\\u0BB9\\u0BD0\\u0C05-\\u0C0C\\u0C0E-\\u0C10\\u0C12-\\u0C28\\u0C2A-\\u0C33\\u0C35-\\u0C39\\u0C3D\\u0C58\\u0C59\\u0C60\\u0C61\\u0C85-\\u0C8C\\u0C8E-\\u0C90\\u0C92-\\u0CA8\\u0CAA-\\u0CB3\\u0CB5-\\u0CB9\\u0CBD\\u0CDE\\u0CE0\\u0CE1\\u0CF1\\u0CF2\\u0D05-\\u0D0C\\u0D0E-\\u0D10\\u0D12-\\u0D3A\\u0D3D\\u0D4E\\u0D60\\u0D61\\u0D7A-\\u0D7F\\u0D85-\\u0D96\\u0D9A-\\u0DB1\\u0DB3-\\u0DBB\\u0DBD\\u0DC0-\\u0DC6\\u0E01-\\u0E30\\u0E32\\u0E33\\u0E40-\\u0E46\\u0E81\\u0E82\\u0E84\\u0E87\\u0E88\\u0E8A\\u0E8D\\u0E94-\\u0E97\\u0E99-\\u0E9F\\u0EA1-\\u0EA3\\u0EA5\\u0EA7\\u0EAA\\u0EAB\\u0EAD-\\u0EB0\\u0EB2\\u0EB3\\u0EBD\\u0EC0-\\u0EC4\\u0EC6\\u0EDC-\\u0EDF\\u0F00\\u0F40-\\u0F47\\u0F49-\\u0F6C\\u0F88-\\u0F8C\\u1000-\\u102A\\u103F\\u1050-\\u1055\\u105A-\\u105D\\u1061\\u1065\\u1066\\u106E-\\u1070\\u1075-\\u1081\\u108E\\u10A0-\\u10C5\\u10C7\\u10CD\\u10D0-\\u10FA\\u10FC-\\u1248\\u124A-\\u124D\\u1250-\\u1256\\u1258\\u125A-\\u125D\\u1260-\\u1288\\u128A-\\u128D\\u1290-\\u12B0\\u12B2-\\u12B5\\u12B8-\\u12BE\\u12C0\\u12C2-\\u12C5\\u12C8-\\u12D6\\u12D8-\\u1310\\u1312-\\u1315\\u1318-\\u135A\\u1380-\\u138F\\u13A0-\\u13F4\\u1401-\\u166C\\u166F-\\u167F\\u1681-\\u169A\\u16A0-\\u16EA\\u1700-\\u170C\\u170E-\\u1711\\u1720-\\u1731\\u1740-\\u1751\\u1760-\\u176C\\u176E-\\u1770\\u1780-\\u17B3\\u17D7\\u17DC\\u1820-\\u1877\\u1880-\\u18A8\\u18AA\\u18B0-\\u18F5\\u1900-\\u191C\\u1950-\\u196D\\u1970-\\u1974\\u1980-\\u19AB\\u19C1-\\u19C7\\u1A00-\\u1A16\\u1A20-\\u1A54\\u1AA7\\u1B05-\\u1B33\\u1B45-\\u1B4B\\u1B83-\\u1BA0\\u1BAE\\u1BAF\\u1BBA-\\u1BE5\\u1C00-\\u1C23\\u1C4D-\\u1C4F\\u1C5A-\\u1C7D\\u1CE9-\\u1CEC\\u1CEE-\\u1CF1\\u1CF5\\u1CF6\\u1D00-\\u1DBF\\u1E00-\\u1F15\\u1F18-\\u1F1D\\u1F20-\\u1F45\\u1F48-\\u1F4D\\u1F50-\\u1F57\\u1F59\\u1F5B\\u1F5D\\u1F5F-\\u1F7D\\u1F80-\\u1FB4\\u1FB6-\\u1FBC\\u1FBE\\u1FC2-\\u1FC4\\u1FC6-\\u1FCC\\u1FD0-\\u1FD3\\u1FD6-\\u1FDB\\u1FE0-\\u1FEC\\u1FF2-\\u1FF4\\u1FF6-\\u1FFC\\u2071\\u207F\\u2090-\\u209C\\u2102\\u2107\\u210A-\\u2113\\u2115\\u2119-\\u211D\\u2124\\u2126\\u2128\\u212A-\\u212D\\u212F-\\u2139\\u213C-\\u213F\\u2145-\\u2149\\u214E\\u2183\\u2184\\u2C00-\\u2C2E\\u2C30-\\u2C5E\\u2C60-\\u2CE4\\u2CEB-\\u2CEE\\u2CF2\\u2CF3\\u2D00-\\u2D25\\u2D27\\u2D2D\\u2D30-\\u2D67\\u2D6F\\u2D80-\\u2D96\\u2DA0-\\u2DA6\\u2DA8-\\u2DAE\\u2DB0-\\u2DB6\\u2DB8-\\u2DBE\\u2DC0-\\u2DC6\\u2DC8-\\u2DCE\\u2DD0-\\u2DD6\\u2DD8-\\u2DDE\\u2E2F\\u3005\\u3006\\u3031-\\u3035\\u303B\\u303C\\u3041-\\u3096\\u309D-\\u309F\\u30A1-\\u30FA\\u30FC-\\u30FF\\u3105-\\u312D\\u3131-\\u318E\\u31A0-\\u31BA\\u31F0-\\u31FF\\u3400-\\u4DB5\\u4E00-\\u9FCC\\uA000-\\uA48C\\uA4D0-\\uA4FD\\uA500-\\uA60C\\uA610-\\uA61F\\uA62A\\uA62B\\uA640-\\uA66E\\uA67F-\\uA697\\uA6A0-\\uA6E5\\uA717-\\uA71F\\uA722-\\uA788\\uA78B-\\uA78E\\uA790-\\uA793\\uA7A0-\\uA7AA\\uA7F8-\\uA801\\uA803-\\uA805\\uA807-\\uA80A\\uA80C-\\uA822\\uA840-\\uA873\\uA882-\\uA8B3\\uA8F2-\\uA8F7\\uA8FB\\uA90A-\\uA925\\uA930-\\uA946\\uA960-\\uA97C\\uA984-\\uA9B2\\uA9CF\\uAA00-\\uAA28\\uAA40-\\uAA42\\uAA44-\\uAA4B\\uAA60-\\uAA76\\uAA7A\\uAA80-\\uAAAF\\uAAB1\\uAAB5\\uAAB6\\uAAB9-\\uAABD\\uAAC0\\uAAC2\\uAADB-\\uAADD\\uAAE0-\\uAAEA\\uAAF2-\\uAAF4\\uAB01-\\uAB06\\uAB09-\\uAB0E\\uAB11-\\uAB16\\uAB20-\\uAB26\\uAB28-\\uAB2E\\uABC0-\\uABE2\\uAC00-\\uD7A3\\uD7B0-\\uD7C6\\uD7CB-\\uD7FB\\uF900-\\uFA6D\\uFA70-\\uFAD9\\uFB00-\\uFB06\\uFB13-\\uFB17\\uFB1D\\uFB1F-\\uFB28\\uFB2A-\\uFB36\\uFB38-\\uFB3C\\uFB3E\\uFB40\\uFB41\\uFB43\\uFB44\\uFB46-\\uFBB1\\uFBD3-\\uFD3D\\uFD50-\\uFD8F\\uFD92-\\uFDC7\\uFDF0-\\uFDFB\\uFE70-\\uFE74\\uFE76-\\uFEFC\\uFF21-\\uFF3A\\uFF41-\\uFF5A\\uFF66-\\uFFBE\\uFFC2-\\uFFC7\\uFFCA-\\uFFCF\\uFFD2-\\uFFD7\\uFFDA-\\uFFDC]*");

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
     * Checks if the provided char sequence contains a valid name.
     * @param text the name
     * @return true if valid; false if not
     */
    public static boolean isValidName(CharSequence text) {
        return !TextUtils.isEmpty(text) && NAME_PATTERN.matcher(text).matches();
    }

    public static boolean isValidNameMaybeEmpty(CharSequence text) {
        return TextUtils.isEmpty(text) || NAME_PATTERN.matcher(text).matches();
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
     * Get a color state list from resources according to the current API.
     * @param context the context
     * @param colorRes the color resource id
     * @return the color
     */
    public static ColorStateList getColorStateList(Context context, int colorRes) {
        return ContextCompat.getColorStateList(context, colorRes);
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
    public static void hideKeyboard(Activity context, Integer additionalModes) {
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
    public static void showKeyboard(Activity context, View view, Integer additionalModes) {
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

    public static void showKeyboard(Activity context) {
        showKeyboard(context, null, null);
    }

    public static void showKeyboard(Activity context, View view) {
        showKeyboard(context, view, null);
    }

    public static void showKeyboard(Activity context, Integer additionalModes) {
        showKeyboard(context, null, additionalModes);
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
