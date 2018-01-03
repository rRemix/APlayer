package remix.myplayer.ui.customview.floatwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.adapter.FloatColorAdapter;
import remix.myplayer.bean.FloatLrcContent;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/3/22 15:47
 */

public class FloatLrcView extends RelativeLayout {
    private WindowManager mWindowManager;
    private Context mContext;
    private PointF mLastPoint = new PointF();
    private boolean mIsLock = false;
    private FloatLrcContent mLrcContent;
    private Handler mUIHandler = new Handler();
    @BindView(R.id.widget_line1)
    FloatTextView mText1;
    @BindView(R.id.widget_line2)
    TextView mText2;
    @BindView(R.id.widget_pannel)
    ViewGroup mPanel;
    @BindView(R.id.widget_lock)
    ImageView mLock;
    @BindView(R.id.widget_close)
    ImageView mClose;
    @BindView(R.id.widget_next)
    ImageView mNext;
    @BindView(R.id.widget_play)
    ImageView mPlay;
    @BindView(R.id.widget_prev)
    ImageView mPrev;
    @BindView(R.id.widget_color_recyclerview)
    RecyclerView mColorRecyclerView;
    @BindView(R.id.widget_control_container)
    View mControlContainer;
    @BindView(R.id.widget_lrc_container)
    View mLrcSettingContainer;
    @BindView(R.id.widget_unlock)
    ImageView mUnLock;
    @BindView(R.id.widget_root)
    ViewGroup mRoot;

    private FloatColorAdapter mColorAdapter;

    private UnLockNotify mNotify;

    //当前字体大小
    private static final int SMALL = 1;
    private static final int MEDIUM = 2;
    private static final int BIG = 3;

    //第一行歌词字体大小
    private static final int FIRST_LINE_BIG = 19;
    private static final int FIRST_LINE_MEDIUM = 18;
    private static final int FIRST_LINE_SMALL = 17;
    //第二行歌词字体大小
    private static final int SECOND_LINE_BIG = 17;
    private static final int SECOND_LINE_MEDIUM = 16;
    private static final int SECOND_LINE_SMALL = 15;

    private int mTextSizeType = MEDIUM;
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mPanel.setVisibility(GONE);
            mLrcSettingContainer.setVisibility(GONE);
        }
    };

    public FloatLrcView(Context context) {
        super(context);
        init(context);
    }

    public FloatLrcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatLrcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        mContext = context;
        mNotify = new UnLockNotify();
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        View root = LayoutInflater.from(mContext).inflate(R.layout.layout_floatwidget, null);
        ButterKnife.bind(this, root);
        addView(root);
        setUpView();
    }

    private void setUpColor() {
        mColorRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mColorRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                mColorAdapter = new FloatColorAdapter(mContext,R.layout.item_float_lrc_color,mColorRecyclerView.getMeasuredWidth());
                mColorAdapter.setData(ThemeStore.getAllThemeColor());
                mColorAdapter.setOnItemClickLitener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        final int themeColor = ThemeStore.getAllThemeColor().get(position);
                        mText1.setTextColor(ThemeStore.getThemeColorInt(themeColor));
                        mColorAdapter.setCurrentColor(themeColor);
                        mColorAdapter.notifyDataSetChanged();
                        resetHide();
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {

                    }
                });
                mColorRecyclerView.setLayoutManager(new LinearLayoutManager(mContext,LinearLayoutManager.HORIZONTAL,false));
                mColorRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                mColorRecyclerView.setAdapter(mColorAdapter);
                return true;
            }
        });
    }

    private void setUpView() {
        mText1.setTextColor(ThemeStore.getThemeColorInt(SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_COLOR,ThemeStore.getThemeColor())));
        mText1.setTextSize(mTextSizeType == SMALL ? FIRST_LINE_SMALL : mTextSizeType == BIG ? FIRST_LINE_BIG : FIRST_LINE_MEDIUM);
        mText2.setTextSize(mTextSizeType == SMALL ? SECOND_LINE_SMALL : mTextSizeType == BIG ? SECOND_LINE_BIG : SECOND_LINE_MEDIUM);
        mIsLock = SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_LRC_LOCK,false);

        mTextSizeType = SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_SIZE,MEDIUM);
        setPlayIcon(MusicService.isPlay());

        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
//                Song song = MusicService.getCurrentMP3();
//                if(song != null){
//                    setText(new LrcRow("",0,song.getTitle()),new LrcRow("",0,song.getArtist() + " - " + song.getAlbum()));
//                }
                saveLock(mIsLock,false);
                return true;
            }
        });
    }


    private int DEFAULT_COLOR = APlayerApplication.getContext().getResources().getColor(R.color.float_text_color);
    public void setText(LrcRow lrc1, LrcRow lrc2) {
        if(lrc1 != null) {
            if(TextUtils.isEmpty(lrc1.getContent()))
                lrc1.setContent("......");
            mText1.setLrcRow(lrc1);
            mText2.setTextColor(lrc1.hasTranslate() ? ThemeStore.getThemeColorInt(ThemeStore.getThemeColor()) : DEFAULT_COLOR);
        }
        if(lrc2 != null) {
            if(TextUtils.isEmpty(lrc2.getContent()))
                lrc2.setContent(".....");
            mText2.setText(lrc2.getContent());
        }
    }

    private static final int DISTANCE_THRESHOLD = 10;
    private static final int DISMISS_THRESHOLD = 4500;
    private static final int LONGCLICK_THRESHOLD = 1000;
    /** 当前是否正在拖动*/
    private boolean mIsDragging = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(!mIsLock){
                    mIsDragging = false;
                    mLastPoint.set(event.getRawX(), event.getRawY());
                    mUIHandler.removeCallbacks(mHideRunnable);
                } else {
//                    mUIHandler.postDelayed(mLongClickRunnable,LONGCLICK_THRESHOLD);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(!mIsLock){
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();

                    if (Math.abs(event.getRawY() - mLastPoint.y) > DISTANCE_THRESHOLD ) {
                        params.y += (int) (event.getRawY() - mLastPoint.y);
                        mWindowManager.updateViewLayout(this, params);
                        mIsDragging = true;
                    }
                    mLastPoint.set(event.getRawX(), event.getRawY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!mIsLock){
                    if(!mIsDragging ){
                        //点击后隐藏或者显示操作栏
                        if (mPanel.isShown()) {
                            mPanel.setVisibility(INVISIBLE);
                        } else {
                            mPanel.setVisibility(VISIBLE);
                            mUIHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
                        }
                    } else {
                        //滑动
                        if(mPanel.isShown()){
                            mUIHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
                        }
                        mIsDragging = false;
                    }
                    //保存y坐标
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
                    SPUtil.putValue(mContext, "Setting", SPUtil.SPKEY.FLOAT_Y, params.y);
                } else {
//                    mUIHandler.removeCallbacks(mLongClickRunnable);
                }
                break;
        }
        return true;
    }

    public void setPlayIcon(boolean play){
        mPlay.setImageResource(play ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
    }

    @OnClick({R.id.widget_close, R.id.widget_lock,R.id.widget_next,R.id.widget_play,R.id.widget_prev,
                R.id.widget_lrc_bigger,R.id.widget_lrc_smaller,R.id.widget_setting,R.id.widget_unlock})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //关闭桌面歌词
            case R.id.widget_close:
                SPUtil.putValue(mContext,"Setting","FloatLrc",false);
                Intent closeIntent = new Intent(MusicService.ACTION_CMD);
                closeIntent.putExtra("FloatLrc",false);
                closeIntent.putExtra("Control",Constants.TOGGLE_FLOAT_LRC);
                mContext.sendBroadcast(closeIntent);
                break;
            //锁定
            case R.id.widget_lock:
                saveLock(true, true);
                mUIHandler.postDelayed(mHideRunnable, 0);
//                mUnLock.setVisibility(VISIBLE);
                break;
            //解除锁定
            case R.id.widget_unlock:
                saveLock(false, false);
                mUnLock.setVisibility(GONE);
                mPanel.setVisibility(VISIBLE);
                break;
            //歌词字体、大小设置
            case R.id.widget_setting:
                mLrcSettingContainer.setVisibility(mLrcSettingContainer.isShown() ? GONE : VISIBLE);
                setUpColor();
                //操作后重置消息的时间
                resetHide();
                break;
            case R.id.widget_next:
            case R.id.widget_play:
            case R.id.widget_prev:
                Intent ctlIntent = new Intent(MusicService.ACTION_CMD);
                ctlIntent.putExtra("Control",view.getId() == R.id.widget_next ? Constants.NEXT : view.getId() == R.id.widget_prev ? Constants.PREV : Constants.TOGGLE);
                mContext.sendBroadcast(ctlIntent);
                mUIHandler.postDelayed(() -> mPlay.setImageResource(MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal),100);
                //操作后重置消息的时间
                resetHide();
                break;
            //字体放大、缩小
            case R.id.widget_lrc_bigger:
            case R.id.widget_lrc_smaller:
                boolean needRefresh = false;
                if(view.getId() == R.id.widget_lrc_bigger ){
                    //当前已经是最大字体
                    if(mTextSizeType == BIG)
                        break;
                    mTextSizeType++;
                    needRefresh = true;
                }
                if(view.getId() == R.id.widget_lrc_smaller){
                    //当前已经是最小字体
                    if(mTextSizeType == SMALL)
                        break;
                    mTextSizeType--;
                    needRefresh = true;
                }
                if(needRefresh){
                    mText1.setTextSize(mTextSizeType == SMALL ? FIRST_LINE_SMALL : mTextSizeType == BIG ? FIRST_LINE_BIG : FIRST_LINE_MEDIUM);
                    mText2.setTextSize(mTextSizeType == SMALL ? SECOND_LINE_SMALL : mTextSizeType == BIG ? SECOND_LINE_BIG : SECOND_LINE_MEDIUM);
                    SPUtil.putValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_SIZE,mTextSizeType);
                    //操作后重置消息的时间
                    resetHide();
                }
                break;
        }
    }

    public void saveLock(boolean lock, boolean toast){
        mIsLock = lock;
        SPUtil.putValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_LRC_LOCK, mIsLock);
        if(toast){
            ToastUtil.show(mContext, !mIsLock ? R.string.float_unlock : R.string.float_lock);
        }
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
        if(params != null){
            if(lock){
                //锁定后点击通知栏解锁
                mNotify.notifyToUnlock();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            } else {
                mNotify.cancel();
                params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
            }
            mWindowManager.updateViewLayout(this,params);
        }
    }

    /**
     * 应用退出后清除通知
     */
    public void cancelNotify(){
        LogUtil.d("DesktopLrc","取消解锁通知");
        if(mNotify != null)
            mNotify.cancel();
    }

    /**
     * 操作后重置消失的时间
     */
    private void resetHide() {
        mUIHandler.removeCallbacks(mHideRunnable);
        mUIHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mUIHandler.removeCallbacksAndMessages(null);
    }

    private class UnLockNotify {
        private static final String UNLOCK_NOTIFICATION_CHANNEL_ID = "unlock_notification";
        private static final int UNLOCK_NOTIFICATION_ID = 2;

        private NotificationManager mNotificationManager;
        UnLockNotify(){
            mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initNotificationChanel();
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void initNotificationChanel() {
            NotificationChannel notificationChannel = new NotificationChannel(UNLOCK_NOTIFICATION_CHANNEL_ID,mContext.getString(R.string.unlock_notification), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setShowBadge(false);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setDescription(mContext.getString(R.string.unlock_notification_description));
            mNotificationManager.createNotificationChannel(notificationChannel);
        }

        void notifyToUnlock(){
            Notification notification = new NotificationCompat.Builder(mContext, UNLOCK_NOTIFICATION_CHANNEL_ID)
                    .setContentText(mContext.getString(R.string.float_lock))
                    .setContentTitle(mContext.getString(R.string.click_to_unlock))
                    .setShowWhen(false)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setTicker(mContext.getString(R.string.float_lock_ticker))
                    .setContentIntent(PendingIntent.getBroadcast(mContext,
                            10,
                            new Intent(MusicService.ACTION_CMD).putExtra("Control",Constants.UNLOCK_DESTOP_LYRIC),
                            PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSmallIcon(R.drawable.notifbar_icon)
                    .build();
            mNotificationManager.notify(UNLOCK_NOTIFICATION_ID,notification);
        }

        void cancel(){
            mNotificationManager.cancel(UNLOCK_NOTIFICATION_ID);
        }
    }
}
