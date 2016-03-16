package remix.myplayer.ui.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.utils.DensityUtil;

/**
 * Created by Remix on 2016/3/7.
 */
public class CustomSeekBar extends View {
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private static final String TAG = "CustomSeekBar";
    private Context mContext;
    //控件高度与宽度
    private int mViewWidth;
    private int mViewHeight;
    //提示字体
    private Paint mTextPaint;
    //轨道垂直中心点
    private int mTrackCenterY;
    //整个轨道颜色
    private int mTrackColor;
    //画整个轨道
    private Paint mTrackPaint;
    //已完成轨道颜色
    private int  mProgressColor;
    //画已完成轨道
    private Paint mProgressPaint;
    //画小圆点
    private Paint mDotPaint;
    //轨道高度与长度
    private int mTrackHeigh;
    private int mTrackWidth;
    //总共几个小圆点
    private int mDotNum;
    //小圆点宽度
    private int mDotWidth;
    //两个小圆点之间的距离
    private int mDotBetween;
    //所有小圆点的坐标
    private ArrayList<Integer> mDotPosition  = new ArrayList<>();
    private int mThumbWidth = 0;
    private int mThumbHeight = 0;
    private Drawable mThumbDrawable = null;
    private int[] mThumbNormal = null;
    private int[] mThumbPressed = null;
    private int mThumbCenterX;
    private int mThumbCenterY;
    //中心高度
    private int mCenterY;
    //大小字符串
    private String[] mTexts = new String[]{"0","300k","500K","800k","1MB","2MB"};
    private int mPositon;
    //是否初始化完成
    private boolean mInit = false;
    public CustomSeekBar(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    public CustomSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //整个轨道
        canvas.drawLine(mThumbWidth,mTrackCenterY,mTrackWidth + mThumbWidth,mTrackCenterY,mTrackPaint);
        //已完成轨道
        canvas.drawLine(mThumbWidth,mTrackCenterY,mThumbCenterX,mTrackCenterY,mProgressPaint);
        //小圆点与底部文字
        for(int i = 0 ; i < mDotNum ;i++){
            canvas.drawCircle(mDotPosition.get(i),mTrackCenterY,mDotWidth,mDotPaint);
            canvas.drawText(mTexts[i],mDotPosition.get(i),mThumbHeight * 2,mTextPaint);
        }
        //thumb
        mThumbDrawable.setBounds(mThumbCenterX - mThumbWidth / 2,0,mThumbCenterX + mThumbWidth / 2,mThumbHeight);
        mThumbDrawable.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG,"EventX:" + event.getX());
        Log.d(TAG,"EventY:" + event.getY());
        int eventX = (int)event.getX();
        if(eventX > mDotPosition.get(mDotPosition.size() - 1) || eventX < mThumbWidth)
            return true;
        boolean isUp = event.getAction() == MotionEvent.ACTION_UP ;
        if(isUp){
            int temp = Integer.MAX_VALUE;
            for(int i = 0 ; i < mDotPosition.size() ;i++){
                if(Math.abs(mDotPosition.get(i) - eventX) < temp){
                    mPositon = i;
                    temp = Math.abs(mDotPosition.get(i) - eventX);
                }
            }
            mThumbCenterX = mDotPosition.get(mPositon);
            if(mOnSeekBarChangeListener != null)
                mOnSeekBarChangeListener.onProgressChanged(this,mPositon,true);
        } else {
            mThumbCenterX = eventX;
        }
        mThumbDrawable.setState(isUp ? mThumbNormal : mThumbPressed);
        invalidate();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if((mViewWidth = getMeasuredWidth()) > 0 && (mViewHeight = getMeasuredHeight()) > 0){
            mCenterY = mViewHeight / 2;
            mTrackWidth = mViewWidth - mThumbWidth * 2;
            mDotBetween = mTrackWidth / (mDotNum - 1);
            mDotPosition.clear();
            for(int i = 0 ; i < mDotNum ; i++){
                mDotPosition.add(mThumbWidth + mDotBetween * i);
            }
            mThumbCenterX = mDotPosition.get(mPositon);
            mInit = true;
        }
    }

    private void init(AttributeSet attrs){
        mInit = false;
        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.CustomSeekBar);
        mThumbDrawable = typedArray.getDrawable(R.styleable.CustomSeekBar_thumb);
        if(mThumbDrawable == null)
            mThumbDrawable = getResources().getDrawable(R.drawable.bg_thumb);
        mThumbNormal = new int[]{-android.R.attr.state_focused, -android.R.attr.state_pressed,
                -android.R.attr.state_selected, -android.R.attr.state_checked};
        mThumbPressed = new int[]{android.R.attr.state_focused, android.R.attr.state_pressed,
                android.R.attr.state_selected, android.R.attr.state_checked};
        mThumbHeight = mThumbDrawable.getIntrinsicHeight();
        mThumbWidth = mThumbDrawable.getIntrinsicWidth();
        mThumbCenterY = mThumbHeight / 2;
        mTrackCenterY = mThumbHeight / 2;

        mTrackColor = typedArray.getColor(R.styleable.CustomSeekBar_trackcolor,Color.parseColor("#6c6a6c"));
        mProgressColor = typedArray.getColor(R.styleable.CustomSeekBar_progresscolor,Color.parseColor("#782899"));

        mDotNum = typedArray.getInteger(R.styleable.CustomSeekBar_dotnum,6);
        mDotWidth = (int)typedArray.getDimension(R.styleable.CustomSeekBar_dotwidth,4);

        mTrackHeigh = (int)typedArray.getDimension(R.styleable.CustomSeekBar_trackheight,4);

        mDotPaint = new Paint();
        mDotPaint.setAntiAlias(true);
        mDotPaint.setColor(Color.parseColor("#ffffffff"));
        mDotPaint.setStyle(Paint.Style.FILL);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.parseColor("#ffffffff"));
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setTextSize(DensityUtil.dip2px(getContext(),13));
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mTrackPaint = new Paint();
        mTrackPaint.setAntiAlias(true);
        mTrackPaint.setColor(mTrackColor);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(mTrackHeigh);

        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mTrackHeigh);

        typedArray.recycle();
    }

    public long getPosition()
    {
        return mPositon;
    }
    public void setPosition(int position)
    {
        if(position > mDotPosition.size())
            position = mDotPosition.size();
        if(position < 0)
            position = 0;
        mPositon = position;
        mThumbCenterX = mDotPosition.get(mPositon);
        invalidate();
    }
    public interface OnSeekBarChangeListener {
        void onProgressChanged(CustomSeekBar seekBar, int position, boolean fromUser);
        void onStartTrackingTouch(CustomSeekBar seekBar);
        void onStopTrackingTouch(CustomSeekBar seekBar);
    }
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l)
    {
        mOnSeekBarChangeListener = l;
    }
    //是否初始化完成
    public boolean isInit(){
        return mInit;
    }
}
