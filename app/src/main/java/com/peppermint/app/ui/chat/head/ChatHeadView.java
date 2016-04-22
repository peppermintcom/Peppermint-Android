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
import com.peppermint.app.ui.base.views.RoundImageView;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Created by Nuno Luz on 01-03-2016.
 *
 * View that represents a single chat head.
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

    public static final int BADGE_ORIENTATION_TOP_LEFT = 1;
    public static final int BADGE_ORIENTATION_TOP_RIGHT = 2;

    // avatar and button
    private static final String DEF_BUTTON_BACKGROUND_COLOR = "#4D000000"; // black 30%

    protected static final int DEF_AVATAR_SIZE_DP = 42;
    protected static final int DEF_AVATAR_BORDER_WIDTH_DP = 3;

    // badge
    private static final String DEF_BADGE_BACKGROUND_COLOR = "#f1693f";

    protected static final int DEF_BADGE_DISPLACEMENT_DP = 5;
    private static final int DEF_BADGE_RADIUS_DP = 10;
    private static final int DEF_BADGE_BORDER_DP = 2;

    // selector
    protected static final int DEF_SELECTOR_LENGTH_DP = 15;
    protected static final int DEF_SELECTOR_MARGIN_DP = 5;

    // text
    private static final int DEF_TEXT_MAX_LINES = 2;
    private static final int DEF_TEXT_PADDING_DP = 5;
    private static final int DEF_TEXT_LINE_SPACING_DP = 5;

    // -------------
    private Chat mChat;
    private boolean mSelectMode = false;
    private boolean mSelected = false;
    private boolean mShowBadge = true;

    // global
    private int mWidth, mHeight;
    private Paint mBitmapPaint;

    // button (play)
    private Bitmap mButtonBitmap;
    private int mButtonBitmapHeight, mButtonBitmapWidth;
    private Paint mButtonBackgroundPaint;
    private RectF mButtonBounds = new RectF();

    // text (display name)
    private Paint mTextPaint;
    private int mTextPadding, mTextLineSpacing, mTextHeight;
    private Rect mTextBounds = new Rect();
    private List<String> mTextList = new ArrayList<>();

    // badge
    private Paint mBadgePaint, mBadgeBorderPaint, mBadgeTextPaint;
    private int mBadgeRadius, mBadgeBorderWidth, mBadgeDisplacement, mBadgeTextHeight;
    private int mBadgeOrientation = BADGE_ORIENTATION_TOP_RIGHT;

    // avatar
    private int mAvatarSize, mAvatarBorderWidth;

    // selector (triangle)
    private Paint mSelectionTrianglePaint;
    private Path mSelectionTrianglePath;
    private int mSelectionTriangleLength, mSelectionTriangleMargin;

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
        Context context = getContext();

        // global
        setKeepAspectRatio(false);
        setFallbackImageDrawable(ResourceUtils.getDrawable(context, R.drawable.ic_anonymous_green_48dp));
        setButtonImageResource(R.drawable.ic_play_15dp);

        String tfPath = context.getString(R.string.font_regular);

        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // button (play)
        mButtonBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonBackgroundPaint.setColor(Color.parseColor(DEF_BUTTON_BACKGROUND_COLOR));

        // text (display name)
        mTextPadding = Utils.dpToPx(context, DEF_TEXT_PADDING_DP);
        mTextLineSpacing = Utils.dpToPx(context, DEF_TEXT_LINE_SPACING_DP);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(Utils.dpToPx(context, 10));
        mTextPaint.setTypeface(getTypeface(context, tfPath));
        mTextPaint.setColor(ResourceUtils.getColor(context, R.color.white));
        final int dp1 = Utils.dpToPx(context, 1);
        mTextPaint.setShadowLayer(dp1, dp1, dp1, ResourceUtils.getColor(context, R.color.black));

        mTextPaint.getTextBounds("W", 0, 1, mTextBounds);
        mTextHeight = mTextBounds.height();

        // badge
        mBadgeDisplacement = Utils.dpToPx(context, DEF_BADGE_DISPLACEMENT_DP);
        mBadgeRadius = Utils.dpToPx(context, DEF_BADGE_RADIUS_DP);
        mBadgeBorderWidth = Utils.dpToPx(context, DEF_BADGE_BORDER_DP);

        mBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBadgePaint.setColor(Color.parseColor(DEF_BADGE_BACKGROUND_COLOR));
        mBadgePaint.setStyle(Paint.Style.FILL);

        mBadgeBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBadgeBorderPaint.setColor(Color.WHITE);
        mBadgeBorderPaint.setStyle(Paint.Style.FILL);

        mBadgeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBadgeTextPaint.setTextAlign(Paint.Align.CENTER);
        mBadgeTextPaint.setTextSize(Utils.dpToPx(context, 10));
        mBadgeTextPaint.setTypeface(getTypeface(context, tfPath));
        mBadgeTextPaint.setColor(Color.WHITE);

        mBadgeTextPaint.getTextBounds("9", 0, 1, mTextBounds);
        mBadgeTextHeight = mTextBounds.height();

        // avatar
        mAvatarSize = Utils.dpToPx(context, DEF_AVATAR_SIZE_DP);
        mAvatarBorderWidth = Utils.dpToPx(context, DEF_AVATAR_BORDER_WIDTH_DP);

        // selector (triangle)
        mSelectionTriangleMargin = Utils.dpToPx(context, DEF_SELECTOR_MARGIN_DP);
        mSelectionTriangleLength = Utils.dpToPx(context, DEF_SELECTOR_LENGTH_DP);

        int halfTriangleLength = mSelectionTriangleLength / 2;
        mSelectionTrianglePath = new Path();
        mSelectionTrianglePath.moveTo(0, halfTriangleLength);
        mSelectionTrianglePath.lineTo(halfTriangleLength, 0);
        mSelectionTrianglePath.lineTo(mSelectionTriangleLength, halfTriangleLength);
        mSelectionTrianglePath.close();

        mSelectionTrianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectionTrianglePaint.setStyle(Paint.Style.FILL);
        mSelectionTrianglePaint.setColor(Color.WHITE);

        setLocalHeight(mAvatarSize + (mAvatarBorderWidth * 2));
        setLocalWidth(mAvatarSize + (mAvatarBorderWidth * 2));
        setBorderWidth(mAvatarBorderWidth);

        initShader();
    }

    /**
     * Returns the expected height of the view when in select mode.<br />
     * See {@link #setSelectMode(boolean)} for more information.
     * @return the expected height of the view when in select mode
     */
    public int getSelectModeHeight() {
        return mAvatarSize + (mAvatarBorderWidth * 2) + mBadgeDisplacement +
                (mTextHeight * 2) + mTextLineSpacing + mTextPadding +
                mSelectionTriangleMargin + (mSelectionTriangleLength / 2);
    }

    private boolean hasAvatar() {
        return mChat != null && mChat.getRecipientList().size() > 0;
    }

    private boolean hasBadge() {
        return mShowBadge && hasAvatar() && (mSelected || mSelectMode) && mChat.getAmountUnopened() > 0;
    }

    private boolean hasName() {
        return hasAvatar() && (mSelected || mSelectMode) && mChat.getRecipientList().get(0).getPhotoUri() == null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tmpHeight = mAvatarSize + (mAvatarBorderWidth * 2) + (mShowBadge ? mBadgeDisplacement : 0);
        int tmpWidth = tmpHeight + (mShowBadge ? mBadgeDisplacement : 0);      // fixed width

        if(isSelectMode()) {
            tmpHeight += (mTextHeight * 2) + mTextLineSpacing + mTextPadding;
            tmpHeight += mSelectionTriangleMargin + (mSelectionTriangleLength / 2);
        } else {
            if(hasName()) {
                tmpHeight += (mTextHeight * 2) + mTextLineSpacing + (mTextPadding * 2);
            }
        }

        mWidth = tmpWidth;
        mHeight = tmpHeight;

        setMeasuredDimension(tmpWidth, tmpHeight);
    }

    @Override
    public synchronized void onDraw(Canvas canvas) {
        if(mBitmap == null || mBitmap.isRecycled()) {
            return;
        }

        boolean validChatAndAvatar = hasAvatar();
        boolean hasBadge = hasBadge();
        boolean hasName = hasName();
        boolean hasButton = validChatAndAvatar && mChat.getLastReceivedUnplayedId() != 0 && mButtonBitmap != null;

        if(mShowBadge && !mSelectMode && mBadgeOrientation == BADGE_ORIENTATION_TOP_LEFT) {
            canvas.translate(mBadgeDisplacement, 0);
        }

        int textTotalHeight = (mTextHeight * 2) + mTextLineSpacing + mTextPadding;

        // draw avatar
        canvas.save();
        canvas.translate(0, (mShowBadge ? mBadgeDisplacement : 0) + (mSelectMode ? textTotalHeight : 0));

        super.onDraw(canvas);

        if(!validChatAndAvatar) {
            canvas.restore();
            return;
        }

        // draw play button if there are unopened messages
        if(hasButton) {
            mButtonBounds.set(getBorderWidth(), getBorderWidth(), mBitmap.getWidth() - getBorderWidth(), mBitmap.getHeight() - getBorderWidth());
            canvas.drawRoundRect(mButtonBounds, getCornerRadius() - getBorderWidth(), getCornerRadius() - getBorderWidth(), mButtonBackgroundPaint);

            int left = (mBitmap.getWidth() / 2) - (mButtonBitmap.getWidth() / 2);
            int top = (mBitmap.getHeight() / 2) - (mButtonBitmap.getHeight() / 2);
            canvas.drawBitmap(mButtonBitmap, left, top, mBitmapPaint);
        }

        // draw badge
        if(hasBadge) {
            String amountText = String.valueOf(mChat.getAmountUnopened());
            int centerX = mWidth - mBadgeBorderWidth - mBadgeRadius;
            int centerY = mBadgeRadius + mBadgeBorderWidth - mBadgeDisplacement;
            if(!mSelectMode && mBadgeOrientation == BADGE_ORIENTATION_TOP_LEFT) {
                centerX = mBadgeRadius + mBadgeBorderWidth - mBadgeDisplacement;
            }
            canvas.drawCircle(centerX, centerY, mBadgeRadius + mBadgeBorderWidth, mBadgeBorderPaint);
            canvas.drawCircle(centerX, centerY, mBadgeRadius, mBadgePaint);
            canvas.drawText(amountText, 0, amountText.length(), centerX, centerY + (mBadgeTextHeight / 2), mBadgeTextPaint);
        }

        canvas.restore();

        // draw text
        if(hasName) {
            canvas.save();
            int lines = mTextList.size();

            if(!mSelectMode) {
                canvas.translate(0, mAvatarSize + (mAvatarBorderWidth * 2) + (mShowBadge ? mBadgeDisplacement : 0) + mTextPadding);
            } else {
                if(lines < 2) {
                    canvas.translate(0, mTextHeight + mTextLineSpacing);
                }
            }

            for (int i = 0; i < lines; i++) {
                String line = mTextList.get(i);
                canvas.drawText(line, 0, line.length(),
                        (float) getLocalWidth() / 2f,
                        mTextHeight + ((mTextHeight + mTextLineSpacing) * i),
                        mTextPaint);
            }

            canvas.restore();
        }

        // draw selector triangle
        if(mSelectMode && mSelected) {
            canvas.save();
            canvas.translate((mAvatarSize + (mAvatarBorderWidth * 2)) / 2 - (mSelectionTriangleLength / 2), mHeight - (mSelectionTriangleLength / 2));
            canvas.drawPath(mSelectionTrianglePath, mSelectionTrianglePaint);
            canvas.restore();
        }
    }

    public void setButtonImageResource(int resId) {
        setButtonDrawable(ResourceUtils.getDrawable(getContext(), resId));
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
            canvas.setBitmap(null);
        }
    }

    public Chat getChat() {
        return mChat;
    }

    public void setChat(Chat mChat) {
        if(mChat != null && mChat.getRecipientList().size() > 0) {
            String oldRecipientPhotoUri = this.mChat == null ? null : this.mChat.getRecipientList().get(0).getPhotoUri();
            String newRecipientPhotoUri = mChat.getRecipientList().get(0).getPhotoUri();
            boolean samePhotoUri = (oldRecipientPhotoUri == newRecipientPhotoUri) ||
                    (oldRecipientPhotoUri != null && newRecipientPhotoUri != null && oldRecipientPhotoUri.compareTo(newRecipientPhotoUri) == 0);

            if (!samePhotoUri) {
                // setup avatar
                if (newRecipientPhotoUri != null) {
                    setImageDrawable(ResourceUtils.getDrawableFromUri(getContext(), Uri.parse(newRecipientPhotoUri)));
                } else {
                    setImageDrawable(null);
                }
            }
        } else {
            setImageDrawable(null);
        }

        /*initShader();*/

        this.mChat = mChat;

        // setup display name
        refreshDisplayName();
    }

    private synchronized void refreshDisplayName() {
        mTextList.clear();

        if(mChat == null) {
            return;
        }

        // the following code splits the display name into several strings
        // each corresponding to one line
        // this allows the name to fit into the width of the view
        String displayName = mChat.getTitle();
        mTextPaint.getTextBounds(displayName, 0, displayName.length(), mTextBounds);

        if(mTextBounds.width() > getLocalWidth()) {
            // try to cut by space
            String[] splitRes = displayName.split("\\s+");
            String displayNameLine = "";
            for(int i=0; i<splitRes.length && mTextList.size() < DEF_TEXT_MAX_LINES; i++) {
                String tmpLine = displayNameLine + (displayNameLine.length() > 0 ? " " : "") + splitRes[i];
                mTextPaint.getTextBounds(tmpLine, 0, tmpLine.length(), mTextBounds);
                if(mTextBounds.width() > getLocalWidth()) {
                    if(displayNameLine.trim().length() > 0) {
                        mTextList.add(displayNameLine);
                        displayNameLine = splitRes[i];
                    } else {
                        // just cut the string, even if it has no spaces
                        int cutoffIndex = getCutIndex(splitRes[i]);
                        mTextList.add(splitRes[i].substring(0, cutoffIndex));
                        splitRes[i] = splitRes[i].substring(cutoffIndex);
                        i--;
                    }
                } else {
                    displayNameLine = tmpLine;
                }
            }
            if(displayNameLine.length() > 0) {
                mTextList.add(displayNameLine);
            }

            /*mTextTotalHeight = 0;
            if(mTextList.size() > 0) {
                mTextTotalHeight = mTextLineSpacing * (mTextList.size() - 1);
                for (String nameLine : mTextList) {
                    mTextPaint.getTextBounds(nameLine, 0, nameLine.length(), mTextBounds);
                    mTextTotalHeight += mTextBounds.height();
                }
            }*/
        } else {
            // whole name fits
            mTextList.add(displayName);
            /*mTextTotalHeight = mTextBounds.height();*/
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

    public boolean isSelectMode() {
        return mSelectMode;
    }

    /**
     * Select mode draws the chat head display name on top of the avatar.
     * @param mSelectMode
     */
    public void setSelectMode(boolean mSelectMode) {
        this.mSelectMode = mSelectMode;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean mSelected) {
        this.mSelected = mSelected;
    }

    public int getBadgeOrientation() {
        return mBadgeOrientation;
    }

    public void setBadgeOrientation(int mBadgeOrientation) {
        this.mBadgeOrientation = mBadgeOrientation;
    }

    public boolean isShowBadge() {
        return mShowBadge;
    }

    public void setShowBadge(boolean mShowBadge) {
        this.mShowBadge = mShowBadge;
    }
}