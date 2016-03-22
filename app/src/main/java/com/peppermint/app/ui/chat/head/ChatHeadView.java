package com.peppermint.app.ui.chat.head;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

import com.peppermint.app.R;
import com.peppermint.app.data.Chat;
import com.peppermint.app.data.ChatRecipient;
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

    protected static final int DEF_SEL_LENGTH_DP = 15;

    private static final int DEF_TEXT_MAX_LINES = 2;
    private static final int DEF_TEXT_PADDING_DP = 5;
    private static final int DEF_TEXT_LINE_SPACING_DP = 1;

    private Chat mChat;
    private boolean mNameVisible = true;
    private boolean mNameOnTop = false;
    private boolean mSelected = false;

    // button-related
    private Bitmap mButtonBitmap;                               // bitmap of button
    private int mButtonBitmapHeight, mButtonBitmapWidth;
    private Paint mButtonBackgroundPaint;
    private Paint mBitmapPaint, mSelectionTrianglePaint;
    private RectF mBounds = new RectF();

    // display name-related
    private Paint mTextPaint;
    private int mTextPadding, mTextLineSpacing, mSelectionTriangleLength;
    private Rect mTextBounds = new Rect();
    private List<String> mDisplayNameList = new ArrayList<>();

    private Path mSelectionTrianglePath;

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
        mSelectionTriangleLength = Utils.dpToPx(getContext(), DEF_SEL_LENGTH_DP);

        mButtonBackgroundPaint = new Paint();
        mButtonBackgroundPaint.setAntiAlias(true);
        mButtonBackgroundPaint.setColor(Color.parseColor(DEF_BUTTON_BACKGROUND_COLOR));

        mSelectionTrianglePaint = new Paint();
        mSelectionTrianglePaint.setAntiAlias(true);
        mSelectionTrianglePaint.setStyle(Paint.Style.FILL);
        mSelectionTrianglePaint.setColor(Color.WHITE);

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

        int halfTriangleLength = mSelectionTriangleLength / 2;
        mSelectionTrianglePath = new Path();
        mSelectionTrianglePath.moveTo(0, halfTriangleLength);
        mSelectionTrianglePath.lineTo(halfTriangleLength, 0);
        mSelectionTrianglePath.lineTo(mSelectionTriangleLength, halfTriangleLength);
        mSelectionTrianglePath.close();

        setImageResource(R.drawable.ic_anonymous_green_48dp);
    }

    @Override
    protected void onSizeChanged() {
        super.onSizeChanged();
        refreshDisplayName();
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        if(getBitmap() == null) {
            return;
        }

        canvas.save();
        if(mNameOnTop) {
            canvas.translate(0, (getLocalHeight() - getBitmap().getHeight()) - mSelectionTriangleLength - getBorderWidth());
        }

        super.onDraw(canvas);

        if(mChat == null || getLocalWidth() <= 0) {
            canvas.restore();
            return;
        }

        // draw play button if there are unopened messages
        if(mChat.getLastReceivedUnplayedId() != 0 && mButtonBitmap != null) {
            mBounds.set(getBorderWidth(), getBorderWidth(), getBitmap().getWidth() - getBorderWidth(), getBitmap().getHeight() - getBorderWidth());
            canvas.drawRoundRect(mBounds, getCornerRadius() - getBorderWidth(), getCornerRadius() - getBorderWidth(), mButtonBackgroundPaint);

            int left = (getBitmap().getWidth() / 2) - (mButtonBitmap.getWidth() / 2);
            int top = (getBitmap().getHeight() / 2) - (mButtonBitmap.getHeight() / 2);
            canvas.drawBitmap(mButtonBitmap, left, top, mBitmapPaint);
        }

        canvas.restore();

        if(mNameVisible && mChat.getRecipientList().get(0).getPhotoUri() == null) {
            canvas.save();
            if(mNameOnTop) {
                canvas.translate(0, -getBitmap().getHeight());
            }

            int lines = mDisplayNameList.size();
            for (int i = 0; i < lines; i++) {
                String line = mDisplayNameList.get(i);
                canvas.drawText(line, 0, line.length(),
                        (float) getLocalWidth() / 2f,
                        getBitmap().getHeight() + (mTextPaint.getTextSize() / 2) + mTextPadding + ((mTextPaint.getTextSize() + mTextLineSpacing) * i),
                        mTextPaint);
            }

            canvas.restore();
        }

        if(mNameOnTop && mSelected) {
            canvas.save();
            canvas.translate(getWidth() / 2 - (mSelectionTriangleLength / 2), getHeight() - mSelectionTriangleLength);
            canvas.drawPath(mSelectionTrianglePath, mSelectionTrianglePaint);
            canvas.restore();
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

        if(mChat == null || mChat.getRecipientList().size() <= 0) {
            return;
        }

        ChatRecipient recipient = mChat.getRecipientList().get(0);

        // setup avatar
        if(recipient.getPhotoUri() != null) {
            setImageURI(Uri.parse(recipient.getPhotoUri()));
            if(getDrawable() == null) {
                setImageResource(R.drawable.ic_anonymous_green_48dp);
            }
        } else {
            setImageResource(R.drawable.ic_anonymous_green_48dp);
        }

        // setup display name
        refreshDisplayName();

        invalidate();
    }

    private void refreshDisplayName() {
        mDisplayNameList.clear();

        if(mChat == null || mChat.getRecipientList().size() <= 0) {
            return;
        }

        ChatRecipient recipient = mChat.getRecipientList().get(0);

        // the following code splits the display name into several strings
        // each corresponding to one line
        // this allows the name to fit into the width of the view
        String displayName = recipient.getDisplayName();
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

    public boolean isNameOnTop() {
        return mNameOnTop;
    }

    public void setNameOnTop(boolean mNameOnTop) {
        this.mNameOnTop = mNameOnTop;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean mSelected) {
        this.mSelected = mSelected;
    }
}