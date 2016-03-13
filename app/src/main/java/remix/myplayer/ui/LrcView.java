package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Scroller;

import java.util.LinkedList;

import remix.myplayer.infos.LrcInfo;

/**
 * Created by Remix on 2015/12/8.
 */
public class LrcView extends View {
    public static LrcView mInstance;
    private static final String TAG = LrcView.class.getSimpleName();
    private LinkedList<LrcInfo> mlrcList;
    //普通歌词画笔
    private Paint mPaint;
    //高亮歌词的索引
    private Paint mHPaint;
    //当前歌词索引
    private int mCurRow;
    //总共多少行歌词
    private int mTotalRow;
    //上下了两行文字间隔
    private int mInterval = 50;
    //控件宽度中心
    private int mViewWidth;
    //控件宽度
    private int mViewCenterX = 0;
    //控件高度
    private int mViewHeight = 0;
    //控件高度中心点
    private int mViewCenterY;
    //辅助滑动类
    private Scroller mScroller;
    private boolean mFinish = false;
    //高亮与非高亮歌词字体大小
    private final int mHTextSize = 45;
    private final int mLTextSize = 30;
    //绘制歌词的垂直中心高度
    private int mCenterY;
    //当前绘制的最小和最大行数
    private int mMinRow;
    private int mMaxRow;
    public LrcView(Context context) {
        super(context);
        mInstance = this;
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInstance = this;
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(30);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
        mHPaint = new Paint();
        mHPaint.setColor(Color.WHITE);
        mHPaint.setTextSize(45);
        mHPaint.setTextAlign(Paint.Align.CENTER);
        mHPaint.setAntiAlias(true);

        mScroller = new Scroller(getContext());
//        new Timer().schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if (AudioHolderActivity.mIsRunning)
//                    postInvalidate();
//            }
//        }, 0, 100);
    }

    public void UpdateLrc(LinkedList<LrcInfo> list){
        mlrcList = list;
        if(mlrcList != null)
            mTotalRow = mlrcList.size();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mlrcList == null)
            return;
        if(mCurRow == -1)
            return;
        if(mTotalRow == 0){
            //初始化将要绘制的歌词行数
            mTotalRow = (getHeight()/(mLTextSize + mInterval)) + 4;
        }

        mMinRow = mCurRow - (mTotalRow-1)/2;
        mMaxRow = mCurRow + (mTotalRow-1)/2;
        mMinRow = Math.max(mMinRow, 0); //处理上边界
        mMaxRow = Math.min(mMaxRow, mlrcList.size() - 1); //处理下边界
        Log.d(TAG,"totalrow:" + mTotalRow);
        Log.d(TAG,"minrow:" + mMinRow);
        Log.d(TAG,"maxrow:" + mMaxRow);
        try {
            for(int i = mMinRow ;i < mMaxRow ;i++){
                mCenterY = mViewCenterY + i * (mInterval + mLTextSize);
                if(i == mCurRow) {
                    //高亮歌词
                    canvas.drawText(mlrcList.get(i).getSentence(), mViewCenterX, mCenterY, mHPaint);
                } else {
                    //非高亮
                    canvas.drawText(mlrcList.get(i).getSentence(), mViewCenterX, mCenterY, mPaint);
                }
            }


//

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void seekTo(int progress,boolean fromuser){
        mCurRow = selectIndex(progress);
        if(!fromuser) {
            smoothScrollTo((mInterval + mLTextSize) * mCurRow, 800);
        } else {
            scrollTo(getScrollX(), (mInterval + mLTextSize) * mCurRow);
        }
        invalidate();
    }

    private void smoothScrollTo(int dstY,int duration){
        int oldScrollY = getScrollY();
        int offset = dstY - oldScrollY;
        mScroller.startScroll(getScrollX(), oldScrollY, getScrollX(), offset, duration);
    }

    @Override
    public void computeScroll() {
       if(mScroller.computeScrollOffset()){
           int oldY = getScrollY();
           int y = mScroller.getCurrY();
           if (oldY != y) {
               scrollTo(getScrollX(), y);
           }
           invalidate();
       }
    }

    public int selectIndex(int time) {
        if(mlrcList == null)
            return -1;
        int index=0;
        for(int i = 0;i < mlrcList.size(); i++) {
            LrcInfo temp = mlrcList.get(i);
            if(temp.getStartTime() < time)
                ++index;

        }
        index -= 1;
        if(index<0){
            index=0;
        }
        return index;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewCenterX = (int)(w * 0.5);
        mViewHeight = h;
        mViewCenterY = (int)(h * 0.5);
    }
}
