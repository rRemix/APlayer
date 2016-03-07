package remix.myplayer.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;

/**
 * Created by taeja on 16-3-2.
 */
public class ColumnView extends View {
    private static final String TAG = "ColumnVIew";
    //记录动画是否已经启动
    private boolean mIsRun = false;
    //View高度
    private int mRawHeight;
    //View宽度
    private int mRawWdith;
    //多个柱状图
    private ArrayList<Integer> mHeightList = new ArrayList<>();
    private int mHeight1;
    private int mHeight2;
    private int mHeight3;
    private int mHeight4;
    //每个柱状图的宽度
    private int mColWidth = 80;
    //两个柱状图之间的间隙
    private int mSpaceWidth = 20;
    private Paint mPaint;
    //总共几个柱状图
    private int mColNum = 4;
    //动画列表
    private ArrayList<ObjectAnimator> mObjectAnimList = new ArrayList<>();
    //更新动画的定时器
    private Timer mTimer;
    //柱状图背景色
    private int mColumnColor;
    //整个View背景色
    private Color mBackgroundColor;
    private final static int STARTANIM = 0;
    private final static int STOPANIM = 1;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            Log.d(TAG,"action:" + msg.what);
//            Log.d(TAG,"info:" + msg.obj.toString());
//            Log.d(TAG,"isplay:" + MusicService.getIsplay());
            for(int i = 0 ; i < mObjectAnimList.size(); i++){
                int from = mHeightList.get(i);
                int to = msg.what == STARTANIM ? new Random().nextInt(mRawHeight) : (int)(mRawHeight * 0.1);
                mObjectAnimList.get(i).setInterpolator(new AccelerateDecelerateInterpolator());
                mObjectAnimList.get(i).
                        setDuration(300).
                        ofInt(ColumnView.this,"Height" + (i + 1),from,to).
                        start();

            }
        }
    };
    public ColumnView(Context context, AttributeSet attrs) {
        super(context,attrs);
        init(attrs);
    }

    public ColumnView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if(mColWidth > 0 && mSpaceWidth > 0) {
            for(int i  = 0 ; i < mColNum ; i++){
                int left = i * (mSpaceWidth + mColWidth);
                int right = (i + 1) * mColWidth + i * mSpaceWidth;
                canvas.drawRect(new Rect(left,mRawHeight - mHeightList.get(i),right,mRawHeight),mPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if(getMeasuredHeight() > 0 && getMeasuredWidth() > 0){
            mRawHeight = getMeasuredHeight();
            mRawWdith = getMeasuredWidth();
            mSpaceWidth = (mRawWdith - mColWidth * mColNum) / (mColNum - 1);
            if(mSpaceWidth < 0 || mColWidth < 0){
                mColWidth = (int)(mRawWdith * 0.6 / mColNum);
                mSpaceWidth = (int)(mRawWdith * 0.4 / (mColNum - 1));
            }
            int temp = (int)(mRawHeight * 0.02);
            mHeight1 = temp;
            mHeight2 = temp;
            mHeight3 = temp;
            mHeight4 = temp;
            for(int i = 0 ; i < mColNum ; i++){
                mHeightList.add(temp);
                mObjectAnimList.add(new ObjectAnimator());
            }
        }

    }

    private void init(AttributeSet attrs){
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.ColumnView);
        mColumnColor = t.getColor(R.styleable.ColumnView_columncolor,0xffCD0000);
        mColWidth = (int)t.getDimension(R.styleable.ColumnView_columnwidth,80);
        mColNum = t.getInteger(R.styleable.ColumnView_columnnum,4);
        mHeightList = new ArrayList<>(mColNum);
        if(mPaint == null){
            mPaint = new Paint();
            mPaint.setColor(mColumnColor);
        }
        t.recycle();
    }

    public int getHeight1() {
        return mHeight1;
    }

    public void setHeight1(int mHeight1) {
        this.mHeight1 = mHeight1;
        mHeightList.set(0,mHeight1);
        invalidate();
    }

    public int getHeight2() {
        return mHeight2;
    }

    public void setHeight2(int mHeight2) {
        this.mHeight2 = mHeight2;
        mHeightList.set(1,mHeight2);
        invalidate();
    }

    public int getHeight3() {
        return mHeight3;
    }

    public void setHeight3(int mHeight3) {
        this.mHeight3 = mHeight3;
        mHeightList.set(2,mHeight3);
        invalidate();
    }

    public int getHeight4() {
        return mHeight4;
    }

    public void setHeight4(int mHeight4) {
        this.mHeight4 = mHeight4;
        mHeightList.set(3,mHeight4);
        invalidate();
    }

    public int getRawHeight(){
        return mRawHeight;
    }

    public void startAnim(){
        mIsRun = true;
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = STARTANIM;
//                msg.obj = (MP3Info) MusicService.getCurrentMP3();
                mHandler.sendMessage(msg);
            }
        },50,300);
    }

    public void stopAnim(){
        mIsRun = false;
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
        Message msg = new Message();
        msg.what = STOPANIM;
//        msg.obj = (MP3Info) MusicService.getCurrentMP3();
        mHandler.sendMessage(msg);
    }

    //当前动画是否正在播放
    public boolean getStatus(){
        return mIsRun;
    }

}
