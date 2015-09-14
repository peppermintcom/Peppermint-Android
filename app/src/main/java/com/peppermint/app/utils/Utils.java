package com.peppermint.app.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import org.apache.http.conn.util.InetAddressUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.Normalizer;
import java.util.Collections;
import java.util.List;

/**
 * Created by Nuno Luz (nluz@mobaton.com) on 18-06-2015.
 * <p>
 *     General utility class.
 * </p>
 */
public class Utils {

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
        return Math.round((float)dp * density);
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
     * Checks if internet connection is available.
     * Requires the permission "android.permission.ACCESS_NETWORK_STATE".
     * @param context the app or activity context
     * @return true if the internet connection is available; false otherwise
     */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(conMan != null) {
            NetworkInfo conMobile = conMan.getNetworkInfo(0);
            NetworkInfo conWifi = conMan.getNetworkInfo(1);

            if(conMobile != null && conMan.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }

            if(conWifi != null && conMan.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }

        return false;
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
        return Base64.encodeToString(decodedBytes, Base64.DEFAULT);
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
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
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

    public static String normalizeAndCleanString(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFC).replace(' ', '-').replaceAll("[^a-zA-Z0-9\\.]", "");
    }

    public static void printToFile(Context context, String filePath, String text, Throwable t) {
        try {
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filePath), true));
            printWriter.print("### " + text + "\n");
            t.printStackTrace(printWriter);
            printWriter.close();
        }
        catch (IOException e) {
            Log.e(Utils.class.getSimpleName(), "File write failed: " + e.toString());
        }
    }
}
