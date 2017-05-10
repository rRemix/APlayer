package remix.myplayer.ui.customview;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.model.mp3.FloatLrcContent;
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
    private FloatHandler mHandler;
    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mControlContainer.setVisibility(INVISIBLE);
        }
    };
    @BindView(R.id.widget_line1)
    TextView mLine1;
    @BindView(R.id.widget_line2)
    TextView mLine2;
    @BindView(R.id.widget_control)
    View mControlContainer;
    @BindView(R.id.widget_lock)
    ImageView mClock;
    @BindView(R.id.widget_close)
    ImageView mClose;

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
        mHandler = new FloatHandler(this);
        View root = LayoutInflater.from(mContext).inflate(R.layout.layout_floatwidget, null);
        ButterKnife.bind(this, root);
        addView(root);
        setUpTextView();

    }

    public void setText(String lrc1, String lrc2) {
        mLine1.setText(lrc1);
        mLine2.setText(lrc2);
    }

    private void setUpTextView() {
        mLine1.setTextColor(ThemeStore.getAccentColor());
        mLine2.setTextColor(ThemeStore.getAccentColor());
    }

    private static final int DISTANCE_THRESHOLD = 10;
    private static final int DISMISS_THRESHOLD = 2000;
    /** 当前是否正在拖动*/
    private boolean mIsDragging = false;
    /** 锁定了但是有移动的动作*/
    private boolean mHasDragAction = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDragging = false;
                mLastPoint.set(event.getRawX(), event.getRawY());
                mHandler.removeCallbacks(mHideRunnable);
                break;
            case MotionEvent.ACTION_MOVE:
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
                if (Math.abs(event.getRawY() - mLastPoint.y) > DISTANCE_THRESHOLD ) {
                    if(mCanMove){
                        params.y += (int) (event.getRawY() - mLastPoint.y);
                        mWindowManager.updateViewLayout(this, params);
                        mIsDragging = true;
                    } else {
                        mHasDragAction = true;
                    }
                }
                mLastPoint.set(event.getRawX(), event.getRawY());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(!mIsDragging ){
                    //点击后隐藏或者显示操作栏
                    if (mControlContainer.isShown()) {
                        mControlContainer.setVisibility(INVISIBLE);
                    } else {
                        mControlContainer.setVisibility(VISIBLE);
                        mHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
                    }
                } else {
                    //滑动
                    if(mControlContainer.isShown()){
                        mHandler.postDelayed(mHideRunnable,DISMISS_THRESHOLD);
                    }
                    //保存y坐标
                    params = (WindowManager.LayoutParams) getLayoutParams();
                    if (params != null)
                        SPUtil.putValue(mContext, "Setting", "FloatY", params.y);
                }
                mHasDragAction = false;
                mIsDragging = false;
                break;
        }
        return true;
    }

    @OnClick({R.id.widget_close, R.id.widget_lock})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //关闭桌面歌词
            case R.id.widget_close:
                SPUtil.putValue(mContext,"Setting","FloatLrc",false);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("FloatLrc",false);
                intent.putExtra("Control",Constants.TOGGLE_FLOAT_LRC);
                mContext.sendBroadcast(intent);
                break;
            //是否锁定
            case R.id.widget_lock:
                mCanMove = !mCanMove;
                ToastUtil.show(mContext,mCanMove ? R.string.float_unlock : R.string.float_lock);
                break;
        }
    }

    private static final class FloatHandler extends Handler{
        private final WeakReference<FloatLrcView> mLrcView;
        FloatHandler(final FloatLrcView lrcView){
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
