package com.peppermint.app.ui.contacts;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.peppermint.app.R;
import com.peppermint.app.dal.DataObjectEvent;
import com.peppermint.app.dal.DatabaseHelper;
import com.peppermint.app.dal.chat.Chat;
import com.peppermint.app.dal.chat.ChatManager;
import com.peppermint.app.dal.contact.ContactRaw;
import com.peppermint.app.dal.message.Message;
import com.peppermint.app.dal.recipient.Recipient;
import com.peppermint.app.dal.recipient.RecipientManager;
import com.peppermint.app.dal.recording.Recording;
import com.peppermint.app.services.sync.SyncEvent;
import com.peppermint.app.trackers.TrackerManager;
import com.peppermint.app.ui.canvas.avatar.AnimatedAvatarView;
import com.peppermint.app.ui.chat.ChatActivity;
import com.peppermint.app.ui.chat.recorder.ChatRecordOverlayController;
import com.peppermint.app.ui.contacts.add.NewContactActivity;
import com.peppermint.app.ui.contacts.listrecents.ChatView;
import com.peppermint.app.utils.ResourceUtils;
import com.peppermint.app.utils.SameAsyncTaskExecutor;
import com.peppermint.app.utils.Utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base implementation that shows lists of contacts or chats with search feature.
 */
public abstract class ContactListFragment extends ListFragment implements
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
        SearchListBarView.OnSearchListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    protected static final int REQUEST_NEWCONTACT = 224;

    private static final Pattern VIA_PATTERN = Pattern.compile("<([^\\s]*)>");

    // avatar animation frequency
    private static final int FIXED_AVATAR_ANIMATION_INTERVAL_MS = 7500;
    private static final int VARIABLE_AVATAR_ANIMATION_INTERVAL_MS = 7500;

    protected ContactListActivity mActivity;
    protected ChatRecordOverlayController mChatRecordOverlayController;

    // ui
    private SwipeRefreshLayout mLytSwipeRefresh;
    private SearchListBarView mSearchListBarView;
    private ViewGroup mListFooterView;
    private View mEmptyLayout;

    private ChatView mChatShareBoth, mChatShareAudio, mChatShareTranscription;
    private ShareListDialog mShareListDialog;

    // hold popup
    private PopupWindow mHoldPopup;
    private final Point mLastTouchPoint = new Point();
    private final Runnable mDismissPopupRunnable = new Runnable() {
        @Override
        public void run() {
            dismissHoldPopup();
        }
    };

    // search
    private class RefreshTask extends SameAsyncTaskExecutor<String, Void, Object> {
        private String _name, _via;

        public RefreshTask(Context mContext) {
            super(mContext);
            mExecutor = Executors.newSingleThreadExecutor();
        }

        @Override
        protected void onPreExecute() {
            if(mActivity != null && mLytSwipeRefresh != null) {
                mLytSwipeRefresh.setRefreshing(true);
            }

            onAsyncRefreshStarted(mContext);
        }

        @Override
        protected Object doInBackground(SameAsyncTask sameAsyncTask, String... params) {
            final String searchText = params[0];

            if(searchText != null) {
                String[] viaName = getFilterData(searchText);
                _via = viaName[0]; _name = viaName[1];
            }

            mRefreshing = true;

            try {
                return onAsyncRefresh(mContext, _name, _via);
            } catch(Throwable e) {
                TrackerManager.getInstance(mContext).logException(e);
            }

            if(!sameAsyncTask.isNextPending()) {
                mRefreshing = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            onAsyncRefreshFinished(mContext, o);

            // check if data is valid and activity has not been destroyed by the main thread
            if(mActivity != null && mLytSwipeRefresh != null) {
                mLytSwipeRefresh.setRefreshing(false);
                checkEmpty();
            }
        }

        @Override
        protected void onCancelled(Object o) {
            onAsyncRefreshCancelled(mContext, o);

            if(mActivity != null && mLytSwipeRefresh != null) {
                mLytSwipeRefresh.setRefreshing(false);
                checkEmpty();
            }
        }

        private void checkEmpty() {
            final ListAdapter listAdapter = getListView().getAdapter();
            if(listAdapter == null || ((HeaderViewListAdapter) listAdapter).getWrappedAdapter().getCount() <= 0) {
                mEmptyLayout.setVisibility(View.VISIBLE);
            } else {
                mEmptyLayout.setVisibility(View.GONE);
            }
        }

        protected void shutdownExecutor() {
            if(mExecutor != null) {
                mExecutor.shutdown();
            }
        }
    }

    protected boolean mSyncing = false;
    protected boolean mRefreshing = false;
    protected RefreshTask mRefreshTask;

    protected abstract void onAsyncRefreshStarted(Context context);
    protected abstract Object onAsyncRefresh(Context context, String searchName, String searchVia);
    protected abstract void onAsyncRefreshCancelled(Context context, Object data);
    protected abstract void onAsyncRefreshFinished(Context context, Object data);

    // smiley face (avatar) random animations
    private final Random mRandom = new Random();
    private final Handler mHandler = new Handler();
    private final Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            final List<AnimatedAvatarView> possibleAnimationsList = new ArrayList<>();

            // get all anonymous avatar instances
            for (int i = 0; i < getListView().getChildCount() - 1; i++) {
                AnimatedAvatarView v = (AnimatedAvatarView) getListView().getChildAt(i).findViewById(R.id.imgPhoto);
                if (!v.isShowStaticAvatar()) {
                    possibleAnimationsList.add(v);
                }
            }

            // randomly pick one
            final int index = possibleAnimationsList.size() > 0 ? mRandom.nextInt(possibleAnimationsList.size()) : 0;

            // start the animation for the picked avatar and stop all others (avoids unnecessary drawing threads)
            for (int i = 0; i < possibleAnimationsList.size(); i++) {
                AnimatedAvatarView v = possibleAnimationsList.get(i);
                if (i == index) {
                    v.startDrawingThread();
                    v.resetAnimations();
                    v.startAnimations();
                } else {
                    v.stopDrawingThread();
                }
            }

            mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));
        }
    };

    public ContactListFragment() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (ContactListActivity) activity;
        mSearchListBarView = mActivity.getSearchListBarView();
        mRefreshTask = new RefreshTask(mActivity);

        refresh();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // hold popup
        mHoldPopup = new PopupWindow(mActivity);
        mHoldPopup.setContentView(inflater.inflate(R.layout.v_recipients_popup, null));
        // although this is deprecated, it is required for versions  < 22/23, otherwise the popup doesn't show up
        //noinspection deprecation
        mHoldPopup.setWindowLayoutMode(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mHoldPopup.setBackgroundDrawable(ResourceUtils.getDrawable(mActivity, R.drawable.img_popup));
        mHoldPopup.setAnimationStyle(R.style.Peppermint_PopupAnimation);
        // do not let the popup get in the way of user interaction
        mHoldPopup.setFocusable(false);
        mHoldPopup.setTouchable(false);

        // inflate the view
        final View v = inflater.inflate(R.layout.f_recipients_layout, container, false);

        mLytSwipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeContainer);
        mLytSwipeRefresh.setOnRefreshListener(this);

        if(mRefreshing) {
            mLytSwipeRefresh.setRefreshing(true);
        }

        // init no recipients view
        mListFooterView = (ViewGroup) inflater.inflate(R.layout.v_recipients_footer_layout, null);
        final AbsListView.LayoutParams layoutParams = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT);
        mListFooterView.setLayoutParams(layoutParams);

        mEmptyLayout = mListFooterView.findViewById(R.id.lytEmpty);

        final Button btnAddContact = (Button) mListFooterView.findViewById(R.id.btnAddContact);
        btnAddContact.setOnClickListener(this);

        mChatShareBoth = (ChatView) mListFooterView.findViewById(R.id.chatShareBoth);
        mChatShareAudio = (ChatView) mListFooterView.findViewById(R.id.chatShareAudio);
        mChatShareTranscription = (ChatView) mListFooterView.findViewById(R.id.chatShareTranscription);

        // dummy chats for sharing
        final Recipient recipientTranscription = new Recipient(0, 0, 0, 0, Recipient.NAME_SHARE_TRANSCRIPTION, Recipient.SHARE_MIMETYPE, "", Recipient.PHOTO_SHARE, null, false);
        final Recipient recipientTranscriptionAudio = new Recipient(0, 0, 0, 0, Recipient.NAME_SHARE_TRANSCRIPTION_AUDIO, Recipient.SHARE_MIMETYPE, "", Recipient.PHOTO_SHARE, null, false);
        final Recipient recipientAudio = new Recipient(0, 0, 0, 0, Recipient.NAME_SHARE_AUDIO, Recipient.SHARE_MIMETYPE, "", Recipient.PHOTO_SHARE, null, false);

        final Chat chatTranscription = new Chat(0, null, null, recipientTranscription);
        chatTranscription.setTitle(chatTranscription.getRecipientListDisplayNames());
        chatTranscription.setSendMode(Chat.SEND_MODE_TRANSCRIPTION);

        final Chat chatTranscriptionAudio = new Chat(0, null, null, recipientTranscriptionAudio);
        chatTranscriptionAudio.setTitle(chatTranscriptionAudio.getRecipientListDisplayNames());
        chatTranscriptionAudio.setSendMode(Chat.SEND_MODE_BOTH);

        final Chat chatAudio = new Chat(0, null, null, recipientAudio);
        chatAudio.setTitle(chatAudio.getRecipientListDisplayNames());
        chatAudio.setSendMode(Chat.SEND_MODE_AUDIO);

        mChatShareBoth.setChat(chatTranscriptionAudio);
        mChatShareAudio.setChat(chatAudio);
        mChatShareTranscription.setChat(chatTranscription);

        mChatShareBoth.setOnClickListener(mChatShareClickListener);
        mChatShareBoth.setOnLongClickListener(mChatShareLongClickListener);

        mChatShareAudio.setOnClickListener(mChatShareClickListener);
        mChatShareAudio.setOnLongClickListener(mChatShareLongClickListener);

        mChatShareTranscription.setOnClickListener(mChatShareClickListener);
        mChatShareTranscription.setOnLongClickListener(mChatShareLongClickListener);

        mShareListDialog = new ShareListDialog(mActivity);
        mShareListDialog.setOnShareListener(new ShareListDialog.OnShareListener() {
            @Override
            public void onShare(ResolveInfo appInfo) {
                final String packageName = appInfo.activityInfo.packageName;
                final String componentName = appInfo.activityInfo.name;

                final String recipientName = mShareListDialog.getChat().getRecipientListDisplayNames();
                final String appName = appInfo.loadLabel(mActivity.getPackageManager()).toString();

                Chat chat = new Chat(mShareListDialog.getChat());
                final Recipient recipient = new Recipient(0, 0, 0, 0, recipientName, Recipient.SHARE_MIMETYPE,
                        appName, "{{" + packageName + "/" + componentName + "}}", null, false);
                recipient.setViaShare(packageName);
                chat.setRecipientList(Arrays.asList(recipient));

                final List<Chat> possibleChats = ChatManager.getInstance(mActivity).
                        getChatsByRecipientsAndSendMode(DatabaseHelper.getInstance(mActivity).getReadableDatabase(),
                                Arrays.asList(recipient), chat.getSendMode());
                if(possibleChats.size() > 0) {
                    chat = possibleChats.get(0);
                }

                if(chat.getId() <= 0) {
                    final DatabaseHelper databaseHelper = DatabaseHelper.getInstance(mActivity);
                    databaseHelper.lock();
                    try {
                        final SQLiteDatabase db = databaseHelper.getWritableDatabase();
                        RecipientManager.getInstance(mActivity).insertOrUpdate(db, recipient);
                        ChatManager.getInstance(mActivity).insert(db, chat);
                    } catch(SQLException e) {
                        TrackerManager.getInstance(mActivity).logException(e);
                        Toast.makeText(mActivity, R.string.msg_database_error, Toast.LENGTH_LONG).show();
                    } finally {
                        databaseHelper.unlock();
                    }
                }

                mChatRecordOverlayController.getMessagesServiceManager().send(chat, mShareListDialog.getRecording());

                final Intent chatIntent = new Intent(mActivity, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chat.getId());
                startActivity(chatIntent);

                mShareListDialog.dismiss();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(this);

        getListView().addFooterView(mListFooterView);

        mSearchListBarView = mActivity.getSearchListBarView();
        mSearchListBarView.addOnSearchListener(this, ContactListActivity.SEARCH_LISTENER_PRIORITY_FRAGMENT);

        // chat record overlay controller
        mChatRecordOverlayController = new ChatRecordOverlayController(mActivity) {
            @Override
            public void onEventMainThread(SyncEvent event) {
                super.onEventMainThread(event);
                if(event.getType() == SyncEvent.EVENT_FINISHED || event.getType() == SyncEvent.EVENT_PROGRESS) {
                    refresh();
                }

                switch(event.getType()) {
                    case SyncEvent.EVENT_STARTED:
                    case SyncEvent.EVENT_PROGRESS:
                        if(!mSyncing) {
                            mSyncing = true;
                            onSyncOngoing();
                        }
                        break;
                    default:
                        if(mSyncing) {
                            mSyncing = false;
                            onSyncFinished();
                        }
                }
            }

            @Override
            public void onEventMainThread(DataObjectEvent<Message> event) {
                super.onEventMainThread(event);
                if(event.getType() != DataObjectEvent.TYPE_UPDATE || event.getUpdates().get(Message.FIELD_PLAYED) != null) {
                    refresh();
                }
            }

            @Override
            public Message sendMessage(Chat chat, Recording recording) {
                if(chat.getId() <= 0 && chat.getRecipientList().get(0).getMimeType().equals(Recipient.SHARE_MIMETYPE)) {
                    mShareListDialog.setChat(chat);
                    mShareListDialog.setRecording(recording);
                    mShareListDialog.show();
                    return null;
                }

                final Message message = super.sendMessage(chat, recording);

                // launch chat activity
                final long chatId = message.getChatParameter().getPeppermintChatId() > 0 ? message.getChatParameter().getPeppermintChatId() : message.getChatParameter().getId();

                final Intent chatIntent = new Intent(mActivity, ChatActivity.class);
                chatIntent.putExtra(ChatActivity.PARAM_CHAT_ID, chatId);
                startActivity(chatIntent);

                // if the user has gone through the sending process without
                // discarding the recording, then clear the search filter
                // delay so that there's some time to show the new chat activity
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSearchListBarView.clearSearch(true);
                    }
                }, 100);

                return message;
            }
        };

        mChatRecordOverlayController.init(getListView(), mActivity.getOverlayManager(),
                mActivity, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        mChatRecordOverlayController.start();

        mHandler.postDelayed(mAnimationRunnable, FIXED_AVATAR_ANIMATION_INTERVAL_MS + mRandom.nextInt(VARIABLE_AVATAR_ANIMATION_INTERVAL_MS));

        // global touch interceptor to hide keyboard
        mActivity.addTouchEventInterceptor(mTouchInterceptor);
    }

    @Override
    public void onStop() {
        if(mSyncing) {
            mSyncing = false;
            onSyncFinished();
        }

        mActivity.removeTouchEventInterceptor(mTouchInterceptor);

        mHandler.removeCallbacks(mAnimationRunnable);

        dismissHoldPopup();

        mChatRecordOverlayController.stop();

        super.onStop();
    }

    @Override
    public void onDestroy() {
        if(mShareListDialog.isShowing()) {
            mShareListDialog.dismiss();
        }

        if(mRefreshTask != null) {
            mRefreshTask.cancel(true);
            mRefreshTask.shutdownExecutor();
            mRefreshTask = null;
        }

        mChatRecordOverlayController.deinit();
        mChatRecordOverlayController.setContext(null);

        super.onDestroy();
    }

    @Override
    public void onDetach() {
        if(mActivity != null) {
            mSearchListBarView.removeOnSearchListener(this);
            mLytSwipeRefresh.setRefreshing(false);
            mActivity = null;
        }

        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_NEWCONTACT) {
            if(resultCode == Activity.RESULT_OK) {
                final ContactRaw contact = (ContactRaw) data.getSerializableExtra(NewContactActivity.KEY_RECIPIENT);
                if(contact != null) {
                    mSearchListBarView.setSearchText(contact.getDisplayName());
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mChatRecordOverlayController.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    protected void onSyncOngoing() {
        /* nothing to do here; override in children */
    }

    protected void onSyncFinished() {
        /* nothing to do here; override in children */
    }

    @Override
    public boolean onSearch(String searchText, boolean wasClear) {
        if(mActivity == null) {
            return false;
        }

        mRefreshing = true;

        mRefreshTask.execute(searchText);

        return false;
    }

    protected void refresh() {
        if(mActivity != null) {
            onSearch(mSearchListBarView != null ? mSearchListBarView.getSearchText() : null, false);
        }
    }

    protected void dismissHoldPopup() {
        if(mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            mHoldPopup.dismiss();
            mHandler.removeCallbacks(mDismissPopupRunnable);
        }
    }

    // the method that displays the img_popup.
    protected void showHoldPopup(View parent) {
        if(!mHoldPopup.isShowing() && !isDetached() && mActivity != null) {
            dismissHoldPopup();
            mHoldPopup.showAtLocation(parent, Gravity.NO_GRAVITY, mLastTouchPoint.x - Utils.dpToPx(mActivity, 120), mLastTouchPoint.y + Utils.dpToPx(mActivity, 10));
            mHandler.postDelayed(mDismissPopupRunnable, 6000);
        }
    }

    private View.OnTouchListener mTouchInterceptor = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismissHoldPopup();
                mLastTouchPoint.set((int) event.getX(), (int) event.getY());
            }
            return false;
        }
    };

    @Override
    public void onClick(View v) {
        final Intent intent = new Intent(mActivity, NewContactActivity.class);

        final String filter = mSearchListBarView.getSearchText();
        if (filter != null) {
            final String[] viaName = getFilterData(filter);
            intent.putExtra(NewContactActivity.KEY_VIA, viaName[0]);

            if (viaName[0] == null && (Utils.isValidPhoneNumber(viaName[1]) || Utils.isValidEmail(viaName[1]))) {
                intent.putExtra(NewContactActivity.KEY_VIA, viaName[1]);
            } else {
                intent.putExtra(NewContactActivity.KEY_NAME, viaName[1]);
            }
        }

        startActivityForResult(intent, REQUEST_NEWCONTACT);
    }

    private static String[] getFilterData(String filter) {
        final String[] viaName = new String[2];
        final Matcher matcher = VIA_PATTERN.matcher(filter);
        if (matcher.find()) {
            viaName[0] = matcher.group(1);
            viaName[1] = filter.replaceAll(VIA_PATTERN.pattern(), "").trim();

            if(viaName[0].length() <= 0) {
                viaName[0] = null; // adjust filter to via so that only one (or no) result is shown
            }
        } else {
            viaName[1] = filter;
        }
        return viaName;
    }

    @Override
    public void onRefresh() {
        refresh();
    }

    private final View.OnClickListener mChatShareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showHoldPopup(v);
        }
    };

    private final View.OnLongClickListener mChatShareLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            mChatRecordOverlayController.triggerRecording(v, ((ChatView) v).getChat());
            getListView().requestDisallowInterceptTouchEvent(true);
            return false;
        }
    };
}
