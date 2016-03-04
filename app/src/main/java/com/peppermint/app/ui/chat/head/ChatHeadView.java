package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.ui.views.RoundImageView;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by Nuno Luz on 01-03-2016.
 *
 * UI components (View) that represents a single chat head.
 */
public class ChatHeadView extends RoundImageView {

    private static WeakHashMap<String, Typeface> mTypefaceCache = new WeakHashMap<>();

    private static Typeface getTypeface(Context context, String fontPath) {
        if(mTypefaceCache.containsKey(fontPath)) {
            return mTypefaceCache.get(fontPath);
        }

        Typeface tf = Typeface.createFromAsset(context.getAssets(), fontPath);
        mTypefaceCache.put(fontPath, tf);

        return tf;
    }

    private static final String DEF_BUTTON_BACKGROUND_COLOR = "#4D000000"; // black 30%

    private static final int DEF_TEXT_MAX_LINES = 2;
    private static final int DEF_TEXT_PADDING_DP = 5;
    private static final int DEF_TEXT_LINE_SPACING_DP = 1;

    private Chat mChat;
    private boolean mNameVisible = true;

    // button-related
    private Bitmap mButtonBitmap;                               // bitmap of button
    private int mButtonBitmapHeight, mButtonBitmapWidth;
    private Paint mButtonBackgroundPaint;
    private Paint mBitmapPaint;
    private RectF mBounds = new RectF();

    // display name-related
    private Paint mTextPaint;
    private int mTextPadding, mTextLineSpacing;
    private Rect mTextBounds = new Rect();
    private List<String> mDisplayNameList = new ArrayList<>();

    public ChatHeadView(Context context) {
        super(context);
        init();
    }

    public ChatHeadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChatHeadView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mTextPadding = Utils.dpToPx(getContext(), DEF_TEXT_PADDING_DP);
        mTextLineSpacing = Utils.dpToPx(getContext(), DEF_TEXT_LINE_SPACING_DP);

        mButtonBackgroundPaint = new Paint();
        mButtonBackgroundPaint.setAntiAlias(true);
        mButtonBackgroundPaint.setColor(Color.parseColor(DEF_BUTTON_BACKGROUND_COLOR));

        mBitmapPaint = new Paint();
        mBitmapPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(Utils.dpToPx(getContext(), 10));
        String tfPath = getContext().getString(R.string.font_regular);
        if(tfPath != null) {
            mTextPaint.setTypeface(getTypeface(getContext(), tfPath));
        }
        mTextPaint.setColor(Utils.getColor(getContext(), R.color.white));
        final int dp1 = Utils.dpToPx(getContext(), 1);
        mTextPaint.setShadowLayer(dp1, dp1, dp1, Utils.getColor(getContext(), R.color.black));

        setKeepAspectRatio(false);
    }

    @Override
    protected void onSizeChanged() {
        super.onSizeChanged();
        refreshDisplayName();
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(mChat == null && getLocalWidth() <= 0) {
            return;
        }

        // draw play button if there are unopened messages
        if(mChat.getAmountUnopened() > 0 && mButtonBitmap != null) {
            mBounds.set(getBorderWidth(), getBorderWidth(), getBitmap().getWidth() - getBorderWidth(), getBitmap().getHeight() - getBorderWidth());
            canvas.drawRoundRect(mBounds, getCornerRadius() - getBorderWidth(), getCornerRadius() - getBorderWidth(), mButtonBackgroundPaint);

            int left = (getLocalWidth() / 2) - (mButtonBitmap.getWidth() / 2);
            int top = (getLocalHeight() / 2) - (mButtonBitmap.getHeight() / 2);
            canvas.drawBitmap(mButtonBitmap, left, top, mBitmapPaint);
        }

        if(mNameVisible) {
            int lines = mDisplayNameList.size();
            for (int i = 0; i < lines; i++) {
                String line = mDisplayNameList.get(i);
                canvas.drawText(line, 0, line.length(),
                        (float) getLocalWidth() / 2f,
                        (float) (getBitmap().getHeight() + (mTextPaint.getTextSize() / 2) + mTextPadding + ((mTextPaint.getTextSize() + mTextLineSpacing) * i)),
                        mTextPaint);
            }
        }
    }

    public void setButtonImageResource(int resId) {
        setButtonDrawable(Utils.getDrawable(getContext(), resId));
    }

    public synchronized void setButtonDrawable(Drawable drawable) {
        if (drawable == null) {
            mButtonBitmap = null;
            return;
        }

        mButtonBitmapHeight = drawable.getIntrinsicHeight();
        mButtonBitmapWidth = drawable.getIntrinsicWidth();

        // draw to bitmap
        if(drawable instanceof BitmapDrawable) {
            mButtonBitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            mButtonBitmap = Bitmap.createBitmap(mButtonBitmapWidth,
                    mButtonBitmapHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mButtonBitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        this.mChat = mChat;

        if(mChat == null) {
            return;
        }

        // setup avatar
        if(mChat.getMainRecipientParameter().getPhotoUri() != null) {
            setImageURI(Uri.parse(mChat.getMainRecipientParameter().getPhotoUri()));
        } else {
            setImageResource(R.drawable.ic_anonymous_green_48dp);
        }

        // setup display name
        refreshDisplayName();

        invalidate();
    }

    private void refreshDisplayName() {
        mDisplayNameList.clear();

        if(mChat == null) {
            return;
        }

        // the following code splits the display name into several strings
        // each corresponding to one line
        // this allows the name to fit into the width of the view
        String displayName = mChat.getMainRecipientParameter().getDisplayName();
        mTextPaint.getTextBounds(displayName, 0, displayName.length(), mTextBounds);

        if(mTextBounds.width() > getLocalWidth()) {
            // try to cut by space
            String[] splitRes = displayName.split("\\s+");
            String displayNameLine = "";
            for(int i=0; i<splitRes.length && mDisplayNameList.size() < DEF_TEXT_MAX_LINES; i++) {
                String tmpLine = displayNameLine + (displayNameLine.length() > 0 ? " " : "") + splitRes[i];
                mTextPaint.getTextBounds(tmpLine, 0, tmpLine.length(), mTextBounds);
                if(mTextBounds.width() > getLocalWidth()) {
                    if(displayNameLine.trim().length() > 0) {
                        mDisplayNameList.add(displayNameLine);
                        displayNameLine = splitRes[i];
                    } else {
                        // just cut the string, even if it has no spaces
                        int cutoffIndex = getCutIndex(splitRes[i]);
                        mDisplayNameList.add(splitRes[i].substring(0, cutoffIndex));
                        splitRes[i] = splitRes[i].substring(cutoffIndex);
                        i--;
                    }
                } else {
                    displayNameLine = tmpLine;
                }
            }
            if(displayNameLine.length() > 0) {
                mDisplayNameList.add(displayNameLine);
            }
        } else {
            // whole name fits
            mDisplayNameList.add(displayName);
        }
    }

    private int getCutIndex(String str) {
        // rough estimation of the cutoff index for performance
        mTextPaint.getTextBounds("W", 0, 1, mTextBounds);
        int estimationAmount = (int) ((float) getLocalWidth() / (float) mTextBounds.width());
        boolean startZero = str.length() / 2 > estimationAmount;

        // taking the estimation into account, we start from the beginning or from the end of the string
        for(int i=(startZero ? 1 : str.length()); (startZero ? i<=str.length() : i>0); i+=(startZero ? 1 : -1)) {
            mTextPaint.getTextBounds(str.substring(0, i), 0, i, mTextBounds);
            if(startZero && mTextBounds.width() > getLocalWidth()) {
                return i-1;
            }
            if(!startZero && mTextBounds.width() <= getLocalWidth()) {
                return i;
            }
        }

        return 0;
    }

    public boolean isNameVisible() {
        return mNameVisible;
    }

    public void setNameVisible(boolean mNameVisible) {
        this.mNameVisible = mNameVisible;
    }
}