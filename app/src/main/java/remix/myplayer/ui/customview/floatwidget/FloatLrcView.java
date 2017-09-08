package remix.myplayer.ui.customview.floatwidget;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.FloatColorAdapter;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.lyric.LrcRow;
import remix.myplayer.model.mp3.FloatLrcContent;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
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
    private boolean mCanMove = true;
    private FloatLrcContent mLrcContent;
    private UIHandler mUIHandler;
    @BindView(R.id.widget_line1)
    FloatTextView mLine1;
    @BindView(R.id.widget_line2)
    TextView mLine2;
    @BindView(R.id.widget_pannel)
    View mPanel;
    @BindView(R.id.widget_lock)
    ImageView mClock;
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
    private FloatColorAdapter mColorAdapter;
    //当前Y坐标
    private int mCurrentY;
    //当前字体大小
    private static final int SMALL = 1;
    private static final int MEDIUM = 2;
    private static final int BIG = 3;
    private int mTextSizeType = MEDIUM;
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mPanel.setVisibility(GONE);
            mLrcSettingContainer.setVisibility(GONE);
        }
    };
    private Runnable mLongClickRunnable = new Runnable() {
        @Override
        public void run() {
            saveCanMove(true);
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
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mUIHandler = new UIHandler(this);

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
                mColorAdapter = new FloatColorAdapter(mContext,mColorRecyclerView.getMeasuredWidth());
                mColorAdapter.setOnItemClickLitener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        final int themeColor = ThemeStore.getAllThemeColor().get(position);
                        mLine1.setTextColor(ThemeStore.getThemeColorInt(themeColor));
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
        mLine1.setTextColor(ThemeStore.getThemeColorInt(SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_COLOR,ThemeStore.getThemeColor())));
        mLine1.setTextSize(mTextSizeType == SMALL ? 17 : mTextSizeType == BIG ? 19 : 18);
        mLine2.setTextSize(mTextSizeType == SMALL ? 15 : mTextSizeType == BIG ? 17 : 16);
        mCanMove = SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.CAN_MOVE,true);
        mTextSizeType = SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_SIZE,MEDIUM);
        setPlayIcon(MusicService.isPlay());
    }

    public void setText(LrcRow lrc1, LrcRow lrc2) {
        if(lrc1 != null) {
            if(TextUtils.isEmpty(lrc1.getContent()))
                lrc1.setContent("...");
            mLine1.setLrcRow(lrc1);
        }
        if(lrc2 != null) {
            if(TextUtils.isEmpty(lrc2.getContent()))
                lrc2.setContent("...");
            mLine2.setText(lrc2.getContent());
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
                if(mCanMove){
                    mIsDragging = false;
                    mLastPoint.set(event.getRawX(), event.getRawY());
                    mUIHandler.removeCallbacks(mHideRunnable);
                } else {
                    mUIHandler.postDelayed(mLongClickRunnable,LONGCLICK_THRESHOLD);
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if(mCanMove){
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
                    if (Math.abs(event.getRawY() - mLastPoint.y) > DISTANCE_THRESHOLD ) {
                        params.y += (int) (event.getRawY() - mLastPoint.y);
                        mWindowManager.updateViewLayout(this, params);
                        mIsDragging = true;
                    }
                    mLastPoint.set(event.getRawX(), event.getRawY());
                    //保存y坐标
//                    params = (WindowManager.LayoutParams) getLayoutParams();
//                    if (params != null){
//                        mCurrentY = params.y;
//                        mSaveHandler.obtainMessage(SAVE_Y).sendToTarget();
//                    }
//                        SPUtil.putValue(mContext, "Setting", SPUtil.SPKEY.FLOAT_Y, params.y);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(mCanMove){
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
                    mUIHandler.removeCallbacks(mLongClickRunnable);
                }
                break;
        }
        return true;
    }

    public void setPlayIcon(boolean play){
        mPlay.setImageResource(play ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
    }

    @OnClick({R.id.widget_close, R.id.widget_lock,R.id.widget_next,R.id.widget_play,R.id.widget_prev,
                R.id.widget_lrc_bigger,R.id.widget_lrc_smaller,R.id.widget_setting})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //关闭桌面歌词
            case R.id.widget_close:
                SPUtil.putValue(mContext,"Setting","FloatLrc",false);
                Intent closeIntent = new Intent(Constants.CTL_ACTION);
                closeIntent.putExtra("FloatLrc",false);
                closeIntent.putExtra("Control",Constants.TOGGLE_FLOAT_LRC);
                mContext.sendBroadcast(closeIntent);
                break;
            //是否锁定
            case R.id.widget_lock:
                saveCanMove(false);
                mUIHandler.postDelayed(mHideRunnable,0);
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
                Intent ctlIntent = new Intent(Constants.CTL_ACTION);
                ctlIntent.putExtra("Control",view.getId() == R.id.widget_next ? Constants.NEXT : view.getId() == R.id.widget_prev ? Constants.PREV : Constants.TOGGLE);
                mContext.sendBroadcast(ctlIntent);
                mUIHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPlay.setImageResource(MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
                    }
                },100);
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
                    mLine1.setTextSize(mTextSizeType == SMALL ? 17 : mTextSizeType == BIG ? 19 : 18);
                    mLine2.setTextSize(mTextSizeType == SMALL ? 15 : mTextSizeType == BIG ? 17 : 16);
                    SPUtil.putValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_TEXT_SIZE,mTextSizeType);
                    //操作后重置消息的时间
                    resetHide();
                }
                break;
        }
    }

    private void saveCanMove(boolean canMove){
        mCanMove = canMove;
        ToastUtil.show(mContext,mCanMove ? R.string.float_unlock : R.string.float_lock);
        SPUtil.putValue(mContext,"Setting", SPUtil.SPKEY.CAN_MOVE,mCanMove);
    }

    /**
     * 操作后重置消失的时间
     */
    private void resetHide() {
        mUIHandler.removeCallbacks(mHideRunnable);
        mUIHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
    }

    private static final int SAVE_Y = 10;
    private static final class UIHandler extends Handler{
        private final WeakReference<FloatLrcView> mLrcView;
        UIHandler(final FloatLrcView lrcView){
            super();
            mLrcView = new WeakReference<>(lrcView);
        }

        @Override
        public void handleMessage(Message msg) {
            final FloatLrcView floatLrcView = mLrcView.get();
            if(floatLrcView == null)
                return;
            switch (msg.what){
                default:break;
            }
        }
    }

}
