package remix.myplayer.ui.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.utils.DensityUtil;

/**
 * Created by taeja on 16-4-13.
 */
public class EQSeekBar extends View {
    public interface OnSeekBarChangeListener{
        void onProgressChanged(CustomSeekBar seekBar, int position, boolean fromUser);
        void onStartTrackingTouch(CustomSeekBar seekBar);
        void onStopTrackingTouch(CustomSeekBar seekBar);
    }

    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    private static final String TAG = "CustomSeekBar";
    private Context mContext;
    /**
     * 控件高度与宽度
     */
    private int mViewWidth;
    private int mViewHeight;



    /**
     * 底部提示字体画笔
     */
    private Paint mTipTextPaint;

    /**
     * 底部提示字体大小
     */
    private int mTipTextSize = 12;

    /**
     * 顶部提示字体大小
     */
    private int mFreTextSize = 12;

    /**
     * 顶部提示字体画笔
     */
    private Paint mFreTextPaint;

    /**
     * 轨道垂直中心点
     */
    private int mTrackCenterY;

    /**
     * 轨道的颜色
     */
    private int mTrackColor;

    /**
     * 轨道画笔
     */
    private Paint mTrackPaint;

    /**
     * 已完成轨道的颜色
     */
    private int  mProgressColor;

    /**
     * 已完成轨道的画笔
     */
    private Paint mProgressPaint;

    /**
     * 轨道的高度与长度
     */
    private int mTrackHeigh;
    private int mTrackWidth;

    /**
     * 总共几个小圆点
     */
    private int mDotNum;

    /**
     * 小圆点宽度
     */
    private int mDotWidth;

    /**
     * 两个小圆点之间的距离
     */
    private int mDotBetween;

    /**
     * 所有小圆点的坐标
     */
    private ArrayList<Integer> mDotPosition  = new ArrayList<>();

    /**
     * Thumb的高度与宽度
     */
    private int mThumbWidth = 50;
    private int mThumbHeight = 50;

    /**
     * ThumbDrawable 以及两个状态
     */
    private Drawable mThumbDrawable = null;
    private int[] mThumbNormal = null;
    private int[] mThumbPressed = null;

    /**
     * Thumb所在位置
     */
    private int mThumbCenterX;
    private int mThumbCenterY;

    /**
     * 是否初始化完成
     */
    private boolean mInit = false;

    /**
     * 当前索引
     */
    private int mPositon;

    /**
     * 底部提示文字
     *
     */
    private String mFreText = "";


    public EQSeekBar(Context context) {
        super(context);
        init(context,null);

    }

    public EQSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context,attrs);
    }

    public EQSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context,AttributeSet attributeSet){
        mInit = false;
        mContext = context;
        TypedArray typedArray = mContext.obtainStyledAttributes(attributeSet,R.styleable.EQSeekBar);
        //初始化thumbdrawable及其状态
        mThumbDrawable = typedArray.getDrawable(R.styleable.EQSeekBar_eqthumb);
        if(mThumbDrawable == null)
            mThumbDrawable = getResources().getDrawable(R.drawable.bg_thumb);
        mThumbNormal = new int[]{-android.R.attr.state_focused, -android.R.attr.state_pressed,
                -android.R.attr.state_selected, -android.R.attr.state_checked};
        mThumbPressed = new int[]{android.R.attr.state_focused, android.R.attr.state_pressed,
                android.R.attr.state_selected, android.R.attr.state_checked};

        //计算thumb的大小
        mThumbHeight = mThumbDrawable.getIntrinsicHeight();
        mThumbWidth = mThumbDrawable.getIntrinsicWidth();
        mThumbCenterX = mThumbHeight / 2;
        mTrackCenterY = mThumbHeight / 2;


        //轨道颜色与已完成轨道的颜色
        mTrackColor = typedArray.getColor(R.styleable.EQSeekBar_eqtrackcolor, Color.parseColor("#6c6a6c"));
        mProgressColor = typedArray.getColor(R.styleable.EQSeekBar_eqprogresscolor, Color.parseColor("#782899"));

        //间隔点数量
        mDotNum = typedArray.getInteger(R.styleable.EQSeekBar_eqdotnum,29);

        //轨道宽度
        mTrackWidth = (int)typedArray.getDimension(R.styleable.EQSeekBar_eqtrackwidth, DensityUtil.dip2px(mContext,3));

        //顶部提示文字画笔
        mFreTextSize = DensityUtil.dip2px(getContext(),13);
        mFreTextPaint = new Paint();
        mFreTextPaint.setAntiAlias(true);
        mFreTextPaint.setColor(Color.parseColor("#ffffffff"));
        mFreTextPaint.setStyle(Paint.Style.STROKE);
        mFreTextPaint.setTextSize(mFreTextSize);
        mFreTextPaint.setTextAlign(Paint.Align.CENTER);

        //底部提示文字画笔
        mTipTextSize = DensityUtil.dip2px(getContext(),14);
        mTipTextPaint = new Paint();
        mTipTextPaint.setAntiAlias(true);
        mTipTextPaint.setColor(Color.parseColor("#ffffffff"));
        mTipTextPaint.setStyle(Paint.Style.STROKE);
        mTipTextPaint.setTextSize(DensityUtil.dip2px(getContext(),mTipTextSize));
        mTipTextPaint.setTextAlign(Paint.Align.CENTER);

        //整个轨道的画笔
        mTrackPaint = new Paint();
        mTrackPaint.setAntiAlias(true);
        mTrackPaint.setColor(mTrackColor);
        mTrackPaint.setStyle(Paint.Style.STROKE);
        mTrackPaint.setStrokeWidth(mTrackWidth);

        //已完成轨道的画笔
        mProgressPaint = new Paint();
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setColor(mProgressColor);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mTrackWidth);

        typedArray.recycle();

    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        //整个轨道
        canvas.drawLine((mViewWidth - mTrackWidth) / 2, mThumbHeight, (mViewWidth - mTrackWidth) / 2, mThumbHeight + mTrackHeigh, mTrackPaint);
        //已完成轨道
        canvas.drawLine((mViewWidth - mTrackWidth) / 2, mThumbHeight, (mViewWidth - mTrackWidth) / 2, mThumbHeight + mThumbCenterY, mProgressPaint);
//        //顶部与底部文字

        canvas.drawText("+15",mViewWidth / 2, mTipTextSize ,mFreTextPaint);
        canvas.drawText("16K", mViewWidth / 2, mFreTextSize +  mThumbHeight + mTrackHeigh, mFreTextPaint);

        //thumb
        mThumbDrawable.setBounds(mThumbCenterX - mThumbWidth / 2, 0, mThumbCenterX + mThumbWidth / 2, mThumbHeight);
        mThumbDrawable.draw(canvas);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);

        if((mViewWidth = getMeasuredWidth()) > 0 && (mViewHeight = getMeasuredHeight()) > 0){
            int paddingtop = getPaddingTop();
            int paddingbottom = getPaddingBottom();
            mTrackHeigh = mViewHeight - paddingtop - paddingbottom - mThumbHeight * 2;
            //计算轨道宽度 两个小圆点之间的距离
            mDotBetween = mTrackHeigh / (mDotNum - 1);
            mDotPosition.clear();
            //设置所有小圆点的坐标
            for(int i = 0 ; i < mDotNum ; i++){
                mDotPosition.add(mThumbWidth + mDotBetween * i);
            }
            mThumbCenterY = mDotPosition.get(mPositon);
            mInit = true;
        }
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l){
        mOnSeekBarChangeListener = l;
    }
}
