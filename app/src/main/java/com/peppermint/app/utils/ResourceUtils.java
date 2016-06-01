package com.peppermint.app.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;

import com.crashlytics.android.Crashlytics;
import com.peppermint.app.tracking.TrackerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * Created by Nuno Luz on 21-04-2016.
 *
 * Utility routines to handle UI and non-UI resources.
 */
public class ResourceUtils {

    private static final String TAG = ResourceUtils.class.getSimpleName();

    public static void disableChangeAnimations(final Context context, final WindowManager.LayoutParams layoutParams) {
        layoutParams.windowAnimations = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            layoutParams.rotationAnimation = 0;
        }
        layoutParams.layoutAnimationParameters = null;

        String className = "android.view.WindowManager$LayoutParams";
        try {
            Class layoutParamsClass = Class.forName(className);

            Field privateFlags = layoutParamsClass.getField("privateFlags");
            Field noAnim = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION");
            Field forceHardware = layoutParamsClass.getField("PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED");

            int privateFlagsValue = privateFlags.getInt(layoutParams);
            int noAnimFlag = noAnim.getInt(layoutParams);
            int forceHardwareFlag = forceHardware.getInt(layoutParams);
            privateFlagsValue |= noAnimFlag | forceHardwareFlag;

            privateFlags.setInt(layoutParams, privateFlagsValue);
        } catch (Exception e) {
            TrackerManager.getInstance(context).log("Not supported!", e);
        }
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
    public static Bitmap getRotatedBitmapFromFileAttributes(final Bitmap realImage, final String filePath) {
        Bitmap rotatedImage = realImage;

        try {
            ExifInterface exif = new ExifInterface(filePath);

            String tagOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (tagOrientation.equalsIgnoreCase("6")) {
                rotatedImage = getRotatedBitmap(realImage, 90);
                realImage.recycle();
            } else if (tagOrientation.equalsIgnoreCase("8")) {
                rotatedImage = getRotatedBitmap(realImage, 270);
                realImage.recycle();
            } else if (tagOrientation.equalsIgnoreCase("3")) {
                rotatedImage = getRotatedBitmap(realImage, 180);
                realImage.recycle();
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to adjust bitmap rotation for " + filePath, e);
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
    public static Bitmap getRotatedBitmap(final Bitmap bitmap, final int degrees) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degrees);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    private static float getBitmapRequiredScale(final InputStream fis, final int width, final int height) {
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
    private static float getBitmapRequiredScale(final BitmapFactory.Options o, final int width, final int height) {
        float scale;
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

    private static Bitmap getScaledBitmap(final InputStream fis, final int scale) {
        // decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(fis, null, o2);
    }

    private static final String FILE_SCHEME = "file";
    private static InputStream openInputStream(final Context context, final Uri uri) throws FileNotFoundException {
        if(FILE_SCHEME.equals(uri.getScheme())) {
            return new FileInputStream(uri.toString().substring(6));
        }
        return context.getContentResolver().openInputStream(uri);
    }

    /**
     * Read and load the bitmap in the provided {@link Uri} and scale it
     * using the {@link android.graphics.BitmapFactory.Options#inSampleSize} option.<br />
     * The scale multiplier is obtained using {@link #getBitmapRequiredScale(InputStream, int, int)}.<br />
     * <b>Aspect ratio is kept. See {@link #getScaledResizedBitmap(Context, int, int, int, boolean)} for a strict resize.</b>
     *
     * @param context the app or activity context
     * @param contentUri the location of the bitmap image
     * @param width the new desired width
     * @param height the new desired height
     * @return the loaded and scaled bitmap
     */
    public static Bitmap getScaledBitmap(final Context context, final Uri contentUri, final int width, final int height) {
        Bitmap bitmap = null;
        try {
            InputStream fis = openInputStream(context, contentUri);
            int scale = Math.round(getBitmapRequiredScale(fis, width, height));
            fis.close();

            fis = openInputStream(context, contentUri);
            bitmap = getScaledBitmap(fis, scale);
            fis.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to scale bitmap", e);
            TrackerManager.getInstance(context).logException(e);
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
    public static Bitmap getScaledBitmap(final Context context, final int resId, final int width, final int height) {
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
            TrackerManager.getInstance(context).logException(e);
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
    public static Bitmap getScaledResizedBitmap(final Context context, final int resId, final int width, final int height, final boolean keepAspectRatio) {
        Bitmap bitmap = getScaledBitmap(context, resId, width, height);

        if(bitmap != null && !keepAspectRatio) {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, width, height, true);
            if(resized != bitmap) {
                bitmap.recycle();
            }
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
    public static Drawable getDrawable(final Context context, final int drawableRes) {
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
    public static int getColor(final Context context, final int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * Get a color state list from resources according to the current API.
     * @param context the context
     * @param colorRes the color resource id
     * @return the color
     */
    public static ColorStateList getColorStateList(final Context context, final int colorRes) {
        return ContextCompat.getColorStateList(context, colorRes);
    }

    /**
     * Copies the image in the specified Uri to the local app file directory, with the specified file name.
     * @param context the app context
     * @param imageUri the origin image Uri
     * @param newFileName the destination file name
     * @return the Uri of the newly created file (null if unable to copy)
     */
    public static Uri copyImageToLocalDir(final Context context, final Uri imageUri, final String newFileName) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            if(bitmap != null) {
                FileOutputStream outStream = context.openFileOutput(newFileName, Context.MODE_PRIVATE);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                outStream.close();
                return Uri.fromFile(new File(context.getFilesDir(), newFileName));
            }
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return null;
    }

    /**
     * Loads a {@link Drawable} from the specified location in the {@link Uri}
     * @param context the app context
     * @param uri the {@link Uri}
     * @return the {@link Drawable} or null if unable to load
     */
    public static Drawable getDrawableFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            return null;
        }

        Drawable drawable = null;

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if(inputStream != null) {
                drawable = Drawable.createFromStream(inputStream, uri.toString());
                inputStream.close();
            }
        } catch (IOException e) {
            TrackerManager.getInstance(context).log(uri.toString(), e);
        }

        return drawable;
    }
}
