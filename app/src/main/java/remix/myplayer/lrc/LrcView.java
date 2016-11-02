package remix.myplayer.lrc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import java.util.ArrayList;

import remix.myplayer.model.LrcInfo;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2015/12/8.
 */
public class LrcView extends View {
    private static final String TAG = LrcView.class.getSimpleName();
    private ArrayList<LrcInfo> mLrcList;
    //普通歌词画笔
    private Paint mNormalPaint;
    //高亮歌词画笔
    private Paint mHightLightPaint;
    //拖动时水平线
    private Paint mHorizontalPaint;
    //高亮歌词
    private int mHightLightRow;
    //总共多少行歌词
    private int mTotalRow;
    //上下了两行文字间隔
    private int mSpacing = 40;
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
    //是否正在滑动
    private boolean mIsDragging = false;
    //外部viewpager是否正在滑动
    private boolean mIsViewPagerScroll = false;
    private onSeekListener mLrcListener;
    //滑动后新的时间
    private int mNewProgress;
    //歌词滚动的动画时间
    private final int ANIMDURATION = 1000;
    //最小滑动距离
    private final int MINOFFSET = 100;
    //每次绘制需要绘制的歌词，因为歌词的长度可能比屏幕更宽，所以需要多行绘制
    private ArrayList<String> mMultiLrc = new ArrayList<>();

    public LrcView(Context context) {
        super(context);
    }

    public void setOnSeekListener(onSeekListener l){
        mLrcListener = l;
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mNormalPaint = new Paint();
        mNormalPaint.setColor(Color.GRAY);
        mNormalTextSize = DensityUtil.dip2px(context,12);
        mNormalPaint.setTextSize(mNormalTextSize);
        mNormalPaint.setTextAlign(Paint.Align.CENTER);
        mNormalPaint.setAntiAlias(true);

        mHightLightPaint = new Paint();
        mHightLightTextSize = DensityUtil.dip2px(context,18);
        mHightLightPaint.setTextSize(mHightLightTextSize);
        mHightLightPaint.setTextAlign(Paint.Align.CENTER);
        mHightLightPaint.setAntiAlias(true);
        mHightLightPaint.setFakeBoldText(true);

        mHorizontalPaint = new Paint();
        mHorizontalPaint.setStrokeWidth(2);
        PathEffect effects = new DashPathEffect(new float[] { 1, 2,4,8}, 1);
        mHorizontalPaint.setPathEffect(effects);

        mScroller = new Scroller(getContext());
    }

    /**
     * 更新歌词列表
     * @param list
     */
    public void UpdateLrcList(ArrayList<LrcInfo> list){
        mLrcList = list;
        if(mLrcList != null)
            mTotalRow = mLrcList.size();
    }

    public void setViewPagerScroll(boolean isScroll){
        mIsViewPagerScroll = isScroll;
    }

    public void setHightLightColor(@ColorInt int color){
        if(mHightLightPaint != null)
            mHightLightPaint.setColor(color);
    }

    public void setNormalColor(@ColorInt int color){
        if(mNormalPaint != null)
            mNormalPaint.setColor(color);
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
            canvas.drawText("正在搜索歌词", mViewCenterX, getHeight() / 2, mNormalPaint);
            return;
        }
        if(mLrcList == null){
            scrollTo(0,0);
            canvas.drawText("暂无歌词", mViewCenterX, getHeight() / 2, mNormalPaint);
            return;
        }
        if(mHightLightRow == -1)
            return;
        if(mTotalRow == 0){
            //初始化将要绘制的歌词行数
            mTotalRow = (getHeight()/(mNormalTextSize + mSpacing)) + 4;
        }

        mMinRow = mHightLightRow - (mTotalRow - 1) / 2;
        mMaxRow = mHightLightRow + (mTotalRow - 1) / 2;
        mMinRow = Math.max(mMinRow, 0); //处理上边界
        mMaxRow = Math.min(mMaxRow, mLrcList.size() - 1); //处理下边界

        try {
            int extraLine = 0;
            for(int i = mMinRow ;i < mMaxRow ;i++){
                mCenterY = mViewCenterY + i * (mSpacing + mNormalTextSize) + extraLine * mNormalTextSize;
                boolean isHighLight = i == mHightLightRow;
                String sentence = mLrcList.get(i).getSentence();
                mMultiLrc.clear();
                //判断歌词是否能显示完整
                float textLegth = isHighLight ?
                        mHightLightPaint.measureText(sentence) :
                        mNormalPaint.measureText(sentence);
                //字符串宽度大于屏幕宽度，分成两行绘制
                if(textLegth > getWidth()){
                    //寻找到屏幕能显示的最后一个字符
                    int end = sentence.length() - 1;
                    if(isHighLight){
                        while (mHightLightPaint.measureText(sentence,0,end) > getWidth() ){
                            end--;
                        }
                        mMultiLrc.add(sentence.substring(0,end - 1));
                        mMultiLrc.add(sentence.substring(end,sentence.length()));
                    } else {
                        while (mNormalPaint.measureText(sentence,0,end) > getWidth() ){
                            end--;
                        }
                        mMultiLrc.add(sentence.substring(0,end - 1));
                        mMultiLrc.add(sentence.substring(end,sentence.length()));
                    }
                } else {
                    //直接绘制
                    mMultiLrc.add(mLrcList.get(i).getSentence());
                }

                //绘制歌词
                for(int j = 0 ; j < mMultiLrc.size() ;j++){
                    canvas.drawText(mMultiLrc.get(j),
                            mViewCenterX,
                            mCenterY + j * (isHighLight ? mHightLightTextSize : mNormalTextSize) + j,
                            isHighLight ? mHightLightPaint : mNormalPaint);
                    if(j == 1)
                        extraLine++;
                }

                if(i == mHightLightRow && mIsDragging) {
                    //正在拖动画水平线
                    canvas.drawLine(0,
                            mCenterY + mHightLightTextSize / 2,
                            getWidth(),
                            mCenterY + mHightLightTextSize / 2,mHorizontalPaint);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 最后一次触摸的坐标
     */
    private float mLastMotionY = 0;
    /**
     * 是否需要刷新
     */
    private boolean mNeedUpdate = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mLrcList == null || mLrcList.size() == 0) {
            return true;
        }
        //外部viewpager正在滑动，不响应滑动
        if(mIsViewPagerScroll){
            return true;
        }
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                seekTo(event);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                computeNewProgress();
                mIsDragging = false;
                break;
        }
        return true;
    }

    /**
     * 上下滑动歌词控件
     * @param event
     */
    public void seekTo(MotionEvent event){
        float y = event.getY();
        float offsetY = y - mLastMotionY;

        //滑动距离过小
        if(Math.abs(offsetY) < MINOFFSET){
            return;
        }

        mNeedUpdate = true;
        mIsDragging = true;
        //计算滑动多少行
        int rowoffset = (int) (Math.abs(offsetY) / (mNormalTextSize + mSpacing));
        if(rowoffset == 0)
            return;
        //向上滑动，歌词向上移动
        if(offsetY > 0){
            mHightLightRow -= (rowoffset );
            mHightLightRow = Math.max(mHightLightRow,0);
        }
        //向下滑动,歌词向下移动
        if(offsetY < 0){
            mHightLightRow += (rowoffset );
            mHightLightRow = Math.min(mHightLightRow,mTotalRow - 1);
        }

        smoothScrollTo((mSpacing + mNormalTextSize) * mHightLightRow, 150);
//        scrollBy(0, (int) offsetY);
//        invalidate();
        mLastMotionY = event.getY();
    }

    /**
     * 根据滑动后的歌词高亮位置，计算歌曲播放进度
     */
    private void computeNewProgress() {
        if(mHightLightRow < mLrcList.size() - 1 && mNeedUpdate) {
            mNeedUpdate = false;
            mNewProgress = mLrcList.get(mHightLightRow).getStartTime();
            if(mLrcListener != null)
                mLrcListener.onLrcSeek(mNewProgress);
        }
    }

    /**
     *
     * @param progress
     * @param fromuser
     */
    public void seekTo(long progress, boolean fromuser){
        if(mLrcList == null || mLrcList.size() == 0){
            invalidate();
            return;
        }
        if(mIsDragging)
            return;
        if(!fromuser) {
            mHightLightRow = selectIndex(progress);
            smoothScrollTo((mSpacing + mNormalTextSize) * mHightLightRow, ANIMDURATION);
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
        if(mLrcList == null)
            return -1;
        int index=0;
        for(int i = 0; i < mLrcList.size(); i++) {
            LrcInfo temp = mLrcList.get(i);
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

    public void setSearching(boolean searching){
        mIsSearching = searching;
    }
}
