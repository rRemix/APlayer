package remix.myplayer.ui.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AbsSeekBar;

import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-2-15.
 */

/**
 * 圆形seekar
 * 用于定时关闭界面
 */
public class CircleSeekBar extends AbsSeekBar {
    /**
     * 阴影圆圈画笔
     */
    private Paint mShadowCirclePaint;
    /**
     * 圆圈画笔
     */
    private Paint mCirclePaint;
    /**
     * 圆弧画笔
     */
    private Paint mArcPaint;

    /**
     * 圆的中心点
     */
    private int mCenterX = 0;
    private int mCenterY = 0;

    /**
     * 圆半径
     */
    private int mRadius = 0;

    /**
     * Thumb的高度与宽度
     */
    private int mThumbWidth = 0;
    private int mThumbHeight = 0;
    private double mThumbSize = 0;

    /**
     * 滑过的弧度
     */
    private double mRad = 0;

    /**
     * 距离圆中心的距离
     */
    private float mOffsetX = 0;
    private float mOffsetY = 0;

    /**
     * 最大进度值
     */
    private int mProgressMax = 100;

    /**
     * 当前进度
     */
    private int mProgress = 0;

    /**
     * 整个圆所在的长方形
     */
    private RectF mRectF = new RectF();

    /**
     * 轨道宽度
     */
    private int mProgressWidth = 0;

    /**
     * 轨道颜色
     */
    private int mProgressCorlor;
    private Context mContext;

    /**
     * 属性
     */
    private AttributeSet mAttrs = null;

    /**
     * ThumbDrawable
     */
    private GradientDrawable mThumbDrawable;

    /**
     * Thumb的两种状态： 按下与普通
     */
    private int[] mThumbNormal = null;
    private int[] mThumbPressed = null;

    /**
     *
     */
    private OnSeekBarChangeListener mOnSeekBarChangeListener;

    /**
     * 是否开始计时
     */
    private boolean mStart = false;

    public CircleSeekBar(Context context) {
        this(context,null,0);
        mContext = context;
        init();
    }

    public CircleSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        mContext = context;
        mAttrs = attrs;
        init();
    }

    public CircleSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAttrs = attrs;
        init();
        
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //阴影
        canvas.drawCircle(mCenterX,mCenterY,mRadius,mShadowCirclePaint);
        //背景圆圈
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint);
        //圆弧
        canvas.drawArc(mRectF, -90, (float) Math.toDegrees(mRad), false, mArcPaint);

        mThumbDrawable.setBounds((int)(mCenterX + mOffsetX - mThumbWidth / 2),
                (int)(mCenterY + mOffsetY - mThumbHeight / 2),
                (int)(mCenterX + mOffsetX + mThumbWidth / 2),
                (int)(mCenterY + mOffsetY + mThumbHeight / 2));

        mThumbDrawable.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //没有开始计时，更新界面
        if(!mStart)
            seekTo(event.getX(),event.getY(),event.getAction() == MotionEvent.ACTION_UP);
        return true;
    }

    //根据触摸位置更新界面

    /**
     * 根据触摸位置计算偏移大小并更新界面
     * @param eventX
     * @param eventY
     * @param isUp 是否抬起，用于更新thumb状态
     */
    private void seekTo(float eventX,float eventY,boolean isUp) {
        if(isPointOnThumb(eventX,eventY) && !isUp){
            mRad = Math.atan2(eventY - mCenterY, eventX - mCenterX);
            //转换角度，以12点方向为0度
            if(mRad > -0.5 * Math.PI && mRad < Math.PI)
                mRad += 0.5 * Math.PI;
            else
                mRad = 2.5 * Math.PI + mRad;
            setThumbPostion(mRad);
            //设置当前进度
            mProgress = (int)(Math.toDegrees(mRad) / 360.0 * mProgressMax);
            //设置thumb状态
            if(mOnSeekBarChangeListener != null)
                mOnSeekBarChangeListener.onProgressChanged(this,mProgress,true);
        }
        mThumbDrawable.setState(isUp ? mThumbNormal : mThumbPressed);
        invalidate();
    }


    /**
     * 根据弧度计算偏移量
     * @param radian 滑过的弧度
     */
    private void setThumbPostion(double radian) {
        mOffsetX = (float) Math.sin(radian) * mRadius;
        mOffsetY = -(float) Math.cos(radian) * mRadius;
    }

    //判断是否点击是否有效
    private boolean isPointOnThumb(float eventX, float eventY) {
        boolean result = false;
        double distance = Math.sqrt(Math.pow(eventX - mCenterX, 2)
                + Math.pow(eventY - mCenterY, 2));
        if (distance > mRadius - mThumbSize / 2 ){
            result = true;
        }
        return result;
    }

    //初始化
    private void init() {
        final TypedArray typedArray = mContext.obtainStyledAttributes(mAttrs, R.styleable.CircleSeekBar);

        mThumbDrawable = Theme.getTinThumb(mContext);

        //轨道颜色 宽度 最大值
        mProgressCorlor = typedArray.getColor(R.styleable.CircleSeekBar_progress_color,
                ThemeStore.getAccentColor());
        mProgressWidth = (int)typedArray.getDimension(R.styleable.CircleSeekBar_progress_width,14);
        mProgressMax = typedArray.getInteger(R.styleable.CircleSeekBar_progress_max,60);
        typedArray.recycle();

        //圆圈画笔
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(ColorUtil.getColor(R.color.gray_b7b7b7));
        mCirclePaint.setStrokeWidth(mProgressWidth);

        //圆弧画笔
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setColor(mProgressCorlor);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mProgressWidth);

        //阴影圆圈画笔
        mShadowCirclePaint = new Paint();
        mShadowCirclePaint.setAntiAlias(true);
        mShadowCirclePaint.setStyle(Paint.Style.STROKE);
        mShadowCirclePaint.setColor(ColorUtil.getColor(R.color.gray_b7b7b7));
        mShadowCirclePaint.setStrokeWidth(mProgressWidth);
        mShadowCirclePaint.setShadowLayer(DensityUtil.dip2px(mContext,2.5f),0,0,ColorUtil.getColor(R.color.gray_b7b7b7));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, mShadowCirclePaint);
        }

        mThumbWidth = mThumbDrawable.getIntrinsicWidth();
        mThumbHeight = mThumbDrawable.getIntrinsicHeight();
        mThumbSize =  Math.sqrt(mThumbHeight * mThumbHeight + mThumbWidth * mThumbWidth);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mCenterX = getWidth() /  2;
        mCenterY = getHeight() / 2;
        mRadius = mCenterX > mCenterY ? mCenterY : mCenterX - mThumbWidth / 2;

        if(mRadius > 0) {
            mRectF = new RectF( mCenterX - mRadius,
                                mCenterY - mRadius,
                                mCenterX + mRadius,
                                mCenterY + mRadius);
            mOffsetX = (float) Math.sin(mRad) * mRadius;
            mOffsetY = -(float) Math.cos(mRad) * mRadius;

            invalidate();
        }
    }

    public void setMax(int max) {
        mProgressMax = max;
    }

    @Override
    public synchronized int getProgress() {
        return mProgress;
    }

    public void setStart(boolean start) {
        mStart = start;
    }

    /**
     * 设置进度，并根据进度值，计算划过角度，再计算偏移距离
     * @param progress
     */
    public void setProgress(int progress) {
        if(progress >= mProgressMax)
            progress = mProgressMax;
        if(progress <= 0)
            progress = 0;
        mProgress = progress;
        mRad = Math.toRadians(progress * 360.0 / mProgressMax );
        setThumbPostion(mRad);
        invalidate();
    }


    public interface OnSeekBarChangeListener {
        void onProgressChanged(CircleSeekBar seekBar, long progress, boolean fromUser);
        void onStartTrackingTouch(CircleSeekBar seekBar);
        void onStopTrackingTouch(CircleSeekBar seekBar);
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }
}