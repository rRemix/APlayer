package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.LinkedList;

import remix.myplayer.model.LrcInfo;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2015/12/8.
 */
public class LrcView extends View {
    private static final String TAG = LrcView.class.getSimpleName();
    private LinkedList<LrcInfo> mlrcList;
    //普通歌词画笔
    private Paint mPaint;
    //高亮歌词画笔
    private Paint mHPaint;
    //拖动时水平线
    private Paint mHorizontalPaint;
    //高亮歌词
    private int mHightLightRow;
    //总共多少行歌词
    private int mTotalRow;
    //上下了两行文字间隔
    private int mSpacing = 50;
    //控件宽度
    private int mViewCenterX = 0;
    //控件高度
    private int mViewHeight = 0;
    //控件高度中心点
    private int mViewCenterY;
    //辅助滑动类
    private Scroller mScroller;
    //高亮与非高亮歌词字体大小
    private int mHightLightTextSize = 45;
    private int mNormalTextSize = 30;
    //当前绘制歌词的高度
    private int mCenterY;
    //当前绘制的最小和最大行数
    private int mMinRow;
    private int mMaxRow;
    //是否正在搜索歌词
    private boolean mIsSearching = false;
    //是否正在拖动
    private boolean mIsDragging = false;
    private LrcInterface mInterface;
    //滑动后新的时间
    private int mNewProgress;
    //歌词滚动的动画时间
    private final int ANIM_DURATION = 1000;


    public LrcView(Context context) {
        super(context);
    }

    public interface LrcInterface{
        void onSeek(int progress);
    }
    public void setInterface(LrcInterface l){
        mInterface = l;
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setColor(Color.GRAY);

        mNormalTextSize = DensityUtil.dip2px(context,10);
        mPaint.setTextSize(mNormalTextSize);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);

        mHPaint = new Paint();
        mHightLightTextSize = DensityUtil.dip2px(context,16);
        mHPaint.setTextSize(mHightLightTextSize);
        mHPaint.setTextAlign(Paint.Align.CENTER);
        mHPaint.setAntiAlias(true);
        mHPaint.setFakeBoldText(true);

        mHorizontalPaint = new Paint();
        mHorizontalPaint.setStrokeWidth(2);
        PathEffect effects = new DashPathEffect(new float[] { 1, 2,4,8}, 1);
        mHorizontalPaint.setPathEffect(effects);

        mScroller = new Scroller(getContext());
    }

    public void UpdateLrc(LinkedList<LrcInfo> list){
        mlrcList = list;
        if(mlrcList != null)
            mTotalRow = mlrcList.size();
    }

    public void setHightLightColor(@ColorInt int color){
        if(mHPaint != null)
            mHPaint.setColor(color);
    }

    public void setNormalColor(@ColorInt int color){
        if(mPaint != null)
            mPaint.setColor(color);
    }

    public void setHorizontalColor(@ColorInt int color){
        if(mHorizontalPaint != null)
            mHorizontalPaint.setColor(color);
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
        if(mHightLightRow == -1)
            return;
        if(mTotalRow == 0){
            //初始化将要绘制的歌词行数
            mTotalRow = (getHeight()/(mNormalTextSize + mSpacing)) + 4;
        }

        mMinRow = mHightLightRow - (mTotalRow-1) / 2;
        mMaxRow = mHightLightRow + (mTotalRow-1) / 2;
        mMinRow = Math.max(mMinRow, 0); //处理上边界
        mMaxRow = Math.min(mMaxRow, mlrcList.size() - 1); //处理下边界

        try {
            for(int i = mMinRow ;i < mMaxRow ;i++){
                mCenterY = mViewCenterY + i * (mSpacing + mNormalTextSize);
                if(i == mHightLightRow) {
                    //高亮歌词
                    canvas.drawText(mlrcList.get(i).getSentence(), mViewCenterX, mCenterY, mHPaint);
                    //正在拖动画水平线
                    if(mIsDragging){
                        canvas.drawLine(getPaddingLeft(),
                                mCenterY + mHightLightTextSize / 2,
                                getWidth() - getPaddingRight(),
                                mCenterY + mHightLightTextSize / 2,mHorizontalPaint);
                    }
                } else {
                    //非高亮
                    canvas.drawText(mlrcList.get(i).getSentence(), mViewCenterX, mCenterY, mPaint);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private float mLastMotionY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        LogUtil.d("LrcView","eventX:" + event.getX() + " eventY:" + event.getY());
        LogUtil.d("LrcView","scrollX:" + getScrollX() + " scrollY:" + getScrollY());
        if(mlrcList == null || mlrcList.size() == 0) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = event.getY();
                mIsDragging = true;
                break;
            case MotionEvent.ACTION_MOVE:
                seekTo(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                computeNewProgress();
                if(mInterface != null)
                    mInterface.onSeek(mNewProgress);
                mIsDragging = false;
                break;
        }
        boolean ret = super.onTouchEvent(event);
        return true;
    }

    /**
     * 根据滑动后的歌词高亮位置，计算歌曲播放进度
     */
    private void computeNewProgress() {
        if(mHightLightRow < mlrcList.size() - 1)
            mNewProgress = mlrcList.get(mHightLightRow).getStartTime();
        LogUtil.d(TAG,"MusicProgress:" + MusicService.getProgress() + " NewProgress:" + mNewProgress);
//        for(int i = 0 ; i < mHightLightRow;i++){
//            mNewProgress += mlrcList.get(i).getDuration();
//        }
    }

    /**
     *
     * @param event
     */
    public void seekTo(MotionEvent event){
        float y = event.getY();
        float offsetY = y - mLastMotionY;
        //滑动距离过小
        if(Math.abs(offsetY) < 50){
            return;
        }
        //计算滑动多少行
        int rowoffset = (int) (Math.abs(offsetY) / (mNormalTextSize + mSpacing));
        if(rowoffset == 0)
            return;
        //向上滑动，歌词向上移动
        if(offsetY > 0){
            mHightLightRow -= rowoffset;
        }
        //向下滑动,歌词向下移动
        if(offsetY < 0){
            mHightLightRow += rowoffset;
        }
//        scrollTo(0,(mSpacing + mNormalTextSize) * mHightLightRow);
        smoothScrollTo((mSpacing + mNormalTextSize) * mHightLightRow, 20);
        invalidate();
        mLastMotionY = event.getY();
    }

    /**
     *
     * @param progress
     * @param fromuser
     */
    public void seekTo(long progress, boolean fromuser){
        if(mlrcList == null || mlrcList.size() == 0){
            invalidate();
            return;
        }
        if(mIsDragging)
            return;
        if(!fromuser) {
            mHightLightRow = selectIndex(progress);
            smoothScrollTo((mSpacing + mNormalTextSize) * mHightLightRow, ANIM_DURATION);
            invalidate();
        }

    }

    /**
     * 计算滚动距离
     * @param dstY
     * @param duration
     */
    private void smoothScrollTo(int dstY,int duration){
        int oldScrollY = getScrollY();
        int offset = dstY - oldScrollY;
        mScroller.startScroll(getScrollX(), oldScrollY, getScrollX(), offset, duration);
    }

    /**
     * 平滑滚动到某个位置
     */
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

    public int selectIndex(long time) {
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
