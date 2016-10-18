package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.Scroller;
import android.widget.TextView;

import java.util.LinkedList;

import remix.myplayer.model.LrcInfo;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2015/12/8.
 */
public class LrcView extends TextView {
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
    //控件宽度
    private int mViewCenterX = 0;
    //控件高度
    private int mViewHeight = 0;
    //控件高度中心点
    private int mViewCenterY;
    //辅助滑动类
    private Scroller mScroller;
    //高亮与非高亮歌词字体大小
    private int mHTextSize = 45;
    private int mLTextSize = 30;
    //绘制歌词的垂直中心高度
    private int mCenterY;
    //当前绘制的最小和最大行数
    private int mMinRow;
    private int mMaxRow;
    //是否正在搜索歌词
    private boolean mIsSearching = false;
    public LrcView(Context context) {
        super(context);
        mInstance = this;
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mInstance = this;
        mPaint = new Paint();
        mPaint.setColor(Color.GRAY);

        mLTextSize = DensityUtil.dip2px(context,10);
        mPaint.setTextSize(mLTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);

        mHPaint = new Paint();
        mHPaint.setColor(Color.BLACK);
        mHTextSize = DensityUtil.dip2px(context,15);
        mHPaint.setTextSize(mHTextSize);
        mHPaint.setTextAlign(Paint.Align.CENTER);
        mHPaint.setAntiAlias(true);
        mHPaint.setFakeBoldText(true);

        mScroller = new Scroller(getContext());
    }

    public void UpdateLrc(LinkedList<LrcInfo> list){
        mlrcList = list;
        if(mlrcList != null)
            mTotalRow = mlrcList.size();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mIsSearching){
            scrollTo(0,0);
            canvas.drawText("正在搜索歌词", mViewCenterX, getHeight() / 2, mPaint);
            return;
        }
        if(mlrcList == null){
            scrollTo(0,0);
            canvas.drawText("暂无歌词", mViewCenterX, getHeight() / 2, mPaint);
            return;
        }
        if(mCurRow == -1)
            return;
        if(mTotalRow == 0){
            //初始化将要绘制的歌词行数
            mTotalRow = (getHeight()/(mLTextSize + mInterval)) + 4;
        }

        mMinRow = mCurRow - (mTotalRow-1) / 2;
        mMaxRow = mCurRow + (mTotalRow-1) / 2;
        mMinRow = Math.max(mMinRow, 0); //处理上边界
        mMaxRow = Math.min(mMaxRow, mlrcList.size() - 1); //处理下边界
        try {
            mHPaint.setColor(AudioHolderActivity.mHColor);
            mPaint.setColor(AudioHolderActivity.mHColor);
        } catch (Exception e){
            e.printStackTrace();
        }

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

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void seekTo(int progress,boolean fromuser){

        if(mlrcList == null || mlrcList.size() == 0){
            invalidate();
            return;
        }
//        if(!fromuser && !AudioHolderActivity.mIsDragSeekBar){
//            mCurRow = selectIndex(progress);
//            smoothScrollTo((mInterval + mLTextSize) * mCurRow, 800);
//            invalidate();
//        }

        if(!fromuser) {
            mCurRow = selectIndex(progress);
            smoothScrollTo((mInterval + mLTextSize) * mCurRow, 800);
            invalidate();
        } else {
//            Log.d(TAG,"from user");
//            scrollTo(getScrollX(), (mInterval + mLTextSize) * mCurRow);
        }

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
        if(index < 0){
            index = 0;
        }
        return index;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewCenterX = (int)(w * 0.5);
        mViewHeight = h;
        mViewCenterY = (int)(h * 0.5);
    }

    public void setIsSearching(boolean searching){
        mIsSearching = searching;
    }
}
