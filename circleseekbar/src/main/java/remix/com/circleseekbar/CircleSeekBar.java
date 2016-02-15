package remix.com.circleseekbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by taeja on 16-2-15.
 */
public class CircleSeekBar extends View {
    private Paint mCirclePaint;
    private Paint mArcPaint;
    private int mCenterX = 0;
    private int mCenterY = 0;
    private int mRadius = 0;
    private int mThumbWidth = 0;
    private int mThumbHeight = 0;
    private double mThumbSize = 0;
    private Bitmap mThumbBitmap = null;
    private Drawable mThumbDrawable = null;
    private double mRad = 0;
    private float mOffsetX = 0;
    private float mOffsetY = 0;
    private long mMax = 100;
    private long mProgress = 0;
    private RectF mRectF = null;
    private int mProgressWidth = 0;
    private int mProgressCorlor;
    private int[] mThumbNormal = null;
    private int[] mThumbPressed = null;
    private Context mContext;
    private AttributeSet mAttrs = null;
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
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(isPointOnThumb(event.getX(),event.getY()) && event.getAction() != MotionEvent.ACTION_UP) {
            seekTo(event.getX(),event.getY());
        }
        return true;
    }


    private void seekTo(float eventX,float eventY) {
        mRad = Math.atan2(eventY - mCenterY, eventX - mCenterX);
        double degree = Math.toDegrees(mRad);
        //转换角度，以12点方向为0度
        if(mRad > -0.5 * Math.PI && mRad < Math.PI)
            mRad += 0.5 * Math.PI;
        else
            mRad = 2.5 * Math.PI + mRad;
        setThumbPostion(mRad);
        invalidate();
        //设置当前进度

        mProgress = (int)(Math.toDegrees(mRad) / 360.0 * mMax);

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
        mRadius = mCenterX - mThumbWidth / 2;

        if(mRadius > 0)
            mRectF = new RectF(mThumbWidth / 2,mThumbHeight / 2,getWidth() - mThumbWidth / 2,getHeight() - mThumbHeight / 2);
        if(mOffsetX == 0 && mOffsetY == 0 && mRadius > 0) {
            //初始0度
            mOffsetX = (float) Math.sin(0) * mRadius;
            mOffsetY = -(float) Math.cos(0) * mRadius;
        }
    }

    public void setMax(long max)
    {
        mMax = max;
    }
    public long getProgress()
    {
        return mProgress;
    }
    public void setProgress(long progress)
    {
        if(progress >= mMax)
            progress = mMax;
        if(progress <= 0)
            progress = 0;
        mProgress = progress;
        mRad = Math.toRadians((double)progress / mMax * 360);
        setThumbPostion(mRad);
        invalidate();
    }

}