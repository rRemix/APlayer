package remix.myplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.utils.DensityUtil;

/**
 * Created by taeja on 16-2-15.
 */
public class CircleSeekBar extends View {
    private Paint mCirclePaint;
    private Paint mArcPaint;
    private Paint mTextPaint;
    private int mCenterX = 0;
    private int mCenterY = 0;
    private int mRadius = 0;
    private boolean mRadiusFlag = true;
    private int mThumbWidth = 0;
    private int mThumbHeight = 0;
    private double mThumbSize = 0;
    private Bitmap mThumbBitmap = null;
    private Drawable mThumbDrawable = null;
    private double mRad = 0;
    private float mOffsetX = 0;
    private float mOffsetY = 0;
    private long mProgressMax = 100;
    private long mProgress = 0;
    private RectF mRectF = new RectF();
    private Rect mTextRect = new Rect();
    private int mProgressWidth = 0;
    private int mProgressCorlor;
    private int[] mThumbNormal = null;
    private int[] mThumbPressed = null;
    private Context mContext;
    private AttributeSet mAttrs = null;
    private float mBaseLine = 0;
    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    //是否开始计时
    private boolean mStart = false;
    public CircleSeekBar(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CircleSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
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
        canvas.drawCircle(mCenterX,mCenterY,mRadius, mCirclePaint);
        canvas.drawArc(mRectF,-90,(float) Math.toDegrees(mRad),false, mArcPaint);
        canvas.drawBitmap(mThumbBitmap,
                mCenterX + mOffsetX - mThumbWidth / 2,
                mCenterY + mOffsetY - mThumbHeight / 2 ,null);
//        canvas.drawText("60:00min",mCenterX,mBaseLine,mTextPaint);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isPointOnThumb(event.getX(),event.getY())
                && event.getAction() != MotionEvent.ACTION_UP
                && !mStart) {
            seekTo(event.getX(),event.getY());
        }
        return true;
    }

    private void seekTo(float eventX,float eventY) {
        mRad = Math.atan2(eventY - mCenterY, eventX - mCenterX);
        //转换角度，以12点方向为0度
        if(mRad > -0.5 * Math.PI && mRad < Math.PI)
            mRad += 0.5 * Math.PI;
        else
            mRad = 2.5 * Math.PI + mRad;
        setThumbPostion(mRad);
        invalidate();
        //设置当前进度
        mProgress = (int)(Math.toDegrees(mRad) / 360.0 * mProgressMax);
        mOnSeekBarChangeListener.onProgressChanged(this,mProgress,true);

    }


    //设置thumb坐标
    private void setThumbPostion(double radian) {
//        mOffsetX = (float) Math.cos(radian) * mRadius;
//        mOffsetY = (float) Math.sin(radian) * mRadius;
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
        TypedArray typedArray = mContext.obtainStyledAttributes(mAttrs, R.styleable.CircleSeekBar);
//        mThumbDrawable = typedArray.getDrawable(android.R.attr.thumb);
//        mThumbDrawable = typedArray.getDrawable(R.styleable.th);
        mProgressCorlor = typedArray.getColor(R.styleable.CircleSeekBar_progress_color, Color.parseColor("#8E24AA"));
        mProgressWidth = typedArray.getInteger(R.styleable.CircleSeekBar_progress_width,15);
        mProgressMax = typedArray.getInteger(R.styleable.CircleSeekBar_progress_max,-1);
        typedArray.recycle();

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(Color.parseColor("#EDEDED"));
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setStrokeWidth(mProgressWidth);

        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setColor(mProgressCorlor);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mProgressWidth);

//        mTextPaint = new Paint();
//        mTextPaint.setAntiAlias(true);
//        mTextPaint.setColor(Color.parseColor("#1b1c19"));
//        mTextPaint.setTextSize(DensityUtil.dip2px(getContext(),22));
//        mTextPaint.setTextAlign(Paint.Align.CENTER);
//        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();
//        mBaseLine = (mRectF.bottom + mRectF.top - fontMetrics.bottom - fontMetrics.top) / 2;


        mThumbBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.progress_indicator);
        mThumbWidth = mThumbBitmap.getWidth();
        mThumbHeight = mThumbBitmap.getHeight();
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
//        if(mOffsetX == 0 && mOffsetY == 0 && mRadius > 0) {
//            //初始0度
//            mOffsetX = (float) Math.sin(0) * mRadius;
//            mOffsetY = -(float) Math.cos(0) * mRadius;
//        }
    }

    public void setMax(long max)
    {
        mProgressMax = max;
    }
    public long getProgress()
    {
        return mProgress;
    }
    public void setStart(boolean start)
    {
        mStart = start;
    }
    public void setProgress(long progress)
    {
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
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l)
    {
        mOnSeekBarChangeListener = l;
    }
}