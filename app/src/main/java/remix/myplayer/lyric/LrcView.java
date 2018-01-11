         package remix.myplayer.lyric;

         import android.content.Context;
         import android.graphics.Canvas;
         import android.graphics.Color;
         import android.os.Handler;
         import android.support.annotation.ColorInt;
         import android.text.Layout;
         import android.text.StaticLayout;
         import android.text.TextPaint;
         import android.util.AttributeSet;
         import android.util.Log;
         import android.view.MotionEvent;
         import android.view.View;
         import android.view.ViewConfiguration;
         import android.view.animation.DecelerateInterpolator;
         import android.view.animation.Interpolator;
         import android.widget.Scroller;

         import java.util.List;

         import remix.myplayer.lyric.bean.LrcRow;

/**
 * Created by Remix on 2018/1/3.
 */

public class LrcView extends View implements ILrcView{
    /**所有的歌词***/
    private List<LrcRow> mLrcRows;

    /**画高亮歌词的画笔***/
    private TextPaint mPaintForHighLightLrc;
    /**高亮歌词的默认字体大小***/
    public static final float DEFAULT_SIZE_FOR_HIGH_LIGHT_LRC = 35;
    /**歌词间默认的行距**/
//	private static final float DEFAULT_PADDING = 50;
    public static final float DEFAULT_PADDING = 55;
    /** 跨行歌词之间额外的行距*/
    public static final float DEFAULT_SPACING_PADDING = 0;
    /** 跨行歌词之间行距倍数*/
    public static final  float DEFAULT_SPACING_MULT = 0.8f;
    /**高亮歌词当前的字体大小***/
    private float mSizeForHighLightLrc = DEFAULT_SIZE_FOR_HIGH_LIGHT_LRC;
    /**高亮歌词的默认字体颜色**/
    private static final int DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC = Color.BLACK;
    /**高亮歌词当前的字体颜色**/
    private int mColorForHighLightLrc = DEFAULT_COLOR_FOR_HIGH_LIGHT_LRC;

    /**画其他歌词的画笔***/
    private TextPaint mPaintForOtherLrc;
    /**其他歌词的默认字体大小***/
    private static final float DEFAULT_SIZE_FOR_OTHER_LRC = 35;
    /**其他歌词当前的字体大小***/
    private float mSizeForOtherLrc = DEFAULT_SIZE_FOR_OTHER_LRC;
    /**其他歌词的默认字体颜色**/
    private static final int DEFAULT_COLOR_FOR_OTHER_LRC = Color.GRAY;
    /**高亮歌词当前的字体颜色**/
    private int mColorForOtherLrc = DEFAULT_COLOR_FOR_OTHER_LRC;


    /**画时间线的画笔***/
    private TextPaint mPaintForTimeLine;
    /***时间线的颜色**/
    private int mTimeLineColor = Color.GRAY;
    /**时间文字大小**/
    private float mSizeForTimeLine = 30;
    /**是否画时间线**/
    private boolean mIsDrawTimeLine = false;

    /**每一句歌词之间的行距**/
    private float mLinePadding = DEFAULT_PADDING;

    /**歌词的最大缩放比例**/
    public static final float MAX_SCALING_FACTOR = 1.5f;
    /**歌词的最小缩放比例**/
    public static final float MIN_SCALING_FACTOR = 0.5f;
    /**默认缩放比例**/
    private static final float DEFAULT_SCALING_FACTOR = 1.0f;
    /**歌词的当前缩放比例**/
    private float mCurScalingFactor = DEFAULT_SCALING_FACTOR;
    /**实现歌词竖直方向平滑滚动的辅助对象**/
    private Scroller mScroller;
    /** 插值器*/
    private Interpolator DEFAULT_INTERPOLATOR = new DecelerateInterpolator();
    /***移动一句歌词的持续时间**/
    private static final int DURATION_FOR_LRC_SCROLL = 1000;
    /***停止触摸时 如果View需要滚动 时的持续时间**/
    private static final int DURATION_FOR_ACTION_UP = 400;

    /**控制文字缩放的因子**/
    private float mCurFraction = 0;
    private int mTouchSlop;

    /**外部viewpager是否正在滑动*/
    private boolean mIsViewPagerScroll = false;

    /** 错误提示文字 */
    private String mText = "正在搜索";
    /** 当前纵坐标*/
    private float mRowY;


    public LrcView(Context context) {
        super(context);
        init();
    }
    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * 初始化画笔等
     */
    @Override
    public void init() {
        mScroller = new Scroller(getContext(),DEFAULT_INTERPOLATOR);
        mPaintForHighLightLrc = new TextPaint();
        mPaintForHighLightLrc.setAntiAlias(true);
        mPaintForHighLightLrc.setColor(mColorForHighLightLrc);
//        mSizeForHighLightLrc = TypedValue.applyDimen sion(TypedValue.COMPLEX_UNIT_SP,19, APlayerApplication.getContext().getResources().getDisplayMetrics());
        mSizeForHighLightLrc = DEFAULT_SIZE_FOR_HIGH_LIGHT_LRC;
        mPaintForHighLightLrc.setTextSize(mSizeForHighLightLrc);
        mPaintForHighLightLrc.setFakeBoldText(true);

        mPaintForOtherLrc = new TextPaint();
        mPaintForOtherLrc.setAntiAlias(true);
        mPaintForOtherLrc.setColor(mColorForOtherLrc);

//        mSizeForOtherLrc = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,13, APlayerApplication.getContext().getResources().getDisplayMetrics());
        mSizeForOtherLrc = DEFAULT_SIZE_FOR_OTHER_LRC;
        mPaintForOtherLrc.setTextSize(mSizeForOtherLrc);

        mPaintForTimeLine = new TextPaint();
        mPaintForTimeLine.setAntiAlias(true);
//        mSizeForTimeLine = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,13, APlayerApplication.getContext().getResources().getDisplayMetrics());
        mSizeForTimeLine = DEFAULT_SIZE_FOR_OTHER_LRC;
        mPaintForTimeLine.setTextSize(mSizeForTimeLine);
        mPaintForTimeLine.setColor(mTimeLineColor);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    private int mTotalRow;
    @Override
    protected void onDraw(Canvas canvas) {
        if(mLrcRows == null || mLrcRows.size() == 0){
            //画默认的显示文字
            float textWidth = mPaintForOtherLrc.measureText(mText);
            float textX = (getWidth() - textWidth ) / 2;
            mPaintForOtherLrc.setAlpha(0xff);
            canvas.drawText(mText, textX, getHeight() / 2, mPaintForOtherLrc);
            return;
        }

        final int availableWidth = getWidth();
        mRowY = getHeight() / 2;
        for (int i = 0; i < mLrcRows.size(); i++) {
            if(i == mCurRow){   //画高亮歌词
                drawLrcRow(canvas,mPaintForHighLightLrc,availableWidth,mLrcRows.get(i));
            }else{  //普通歌词
                //计算歌词透明度
//                int alpha = (int)(0xff /  Math.pow(Math.abs(i - mCurRow),1.2f));
//                mPaintForOtherLrc.setAlpha(alpha);
                drawLrcRow(canvas,mPaintForOtherLrc, availableWidth, mLrcRows.get(i));
            }
        }
        //画时间线和时间
        if(mIsDrawTimeLine){
            float y = getHeight() / 2 + getScrollY();
            canvas.drawText(mLrcRows.get(mCurRow).getTimeStr(), 0, y - 5, mPaintForTimeLine);
            canvas.drawLine(0, y, getWidth(), y, mPaintForTimeLine);
        }
    }

    /**
     * 分割绘制歌词
     * @param canvas
     * @param textPaint
     * @param availableWidth
     * @param lrcRow
     */
    private void drawLrcRow(Canvas canvas, TextPaint textPaint, int availableWidth, LrcRow lrcRow) {
        drawText(canvas,textPaint,availableWidth,lrcRow.getContent());
        mRowY += lrcRow.getContentHeight();
        if(lrcRow.hasTranslate()){
            drawText(canvas,textPaint,availableWidth,lrcRow.getTranslate());
            mRowY += lrcRow.getTranslateHeight();
        }
//        mRowY += lrcRow.getHeight();
        mRowY += mLinePadding;
    }

    /**
     * 分割绘制歌词
     * @param canvas
     * @param textPaint
     * @param availableWidth
     * @param text
     */
    private void drawText(Canvas canvas, TextPaint textPaint, int availableWidth, String text) {
        float textWidth = textPaint.measureText(text);
        StaticLayout staticLayout = new StaticLayout(text, textPaint, availableWidth,textWidth > availableWidth ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL,
                DEFAULT_SPACING_MULT, DEFAULT_SPACING_PADDING, true);

        float textX = textWidth > availableWidth ? 0 : (availableWidth - textWidth) / 2;
        canvas.save();
        canvas.translate(textX,mRowY);
        staticLayout.draw(canvas);
        canvas.restore();
        //根据一句歌词所占的行数计算出下一行歌词绘制的y坐标
        int height = (int) ((staticLayout.getLineCount() * textPaint.getTextSize()) + DEFAULT_SPACING_PADDING);
        int height1 = staticLayout.getHeight();
//        mRowY += height1;
    }

    /**是否可拖动歌词**/
    private boolean mCanDrag = false;
    /**事件的第一次的y坐标**/
    private float mFirstY;
    /**事件的上一次的y坐标**/
    private float mLastY;
    private float mLastX;
    /** 长按runnable*/
    private Runnable mLongPressRunnable = new LongPressRunnable();
    private Handler mHandler = new Handler();

    private class LongPressRunnable implements Runnable{
        @Override
        public void run() {
            if(mOnLrcClickListener != null) {
                mOnLrcClickListener.onLongClick();
            }
        }
    }

    private boolean hasLrc(){
        return mLrcRows != null && mLrcRows.size() > 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//		if(mLrcRows == null || mLrcRows.size() == 0){
//			return false;
//		}
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //没有歌词
                if(hasLrc()){
                    mFirstY = event.getRawY();
                    mLastX = event.getRawX();
                }
                mLongPressRunnable = new LongPressRunnable();
                mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_MOVE:
                if(hasLrc()){
                    if(!mCanDrag){
                        if(Math.abs(event.getRawY() - mFirstY) > mTouchSlop && Math.abs(event.getRawY() - mFirstY) > Math.abs(event.getRawX() - mLastX)){
                            mCanDrag = true;
                            mIsDrawTimeLine = true;
                            mScroller.forceFinished(true);
                            mCurFraction = 1;
                        }
                        mLastY = event.getRawY();
                    }
                    if(mCanDrag){
                        mHandler.removeCallbacks(mLongPressRunnable);
                        float offset = event.getRawY() - mLastY;//偏移量
                        if( getScrollY() - offset < 0){
                            if(offset > 0){
                                offset = offset / 3;
                            }
                        }else if(getScrollY() - offset > mLrcRows.size() * (mSizeForOtherLrc + mLinePadding) - mLinePadding){
                            if(offset < 0 ){
                                offset = offset / 3;
                            }
                        }
                        scrollBy(getScrollX(), -(int)offset);
                        mLastY = event.getRawY();
                        //根据滚动后的距离 查找歌词
//                        int currentRow = (int) (getScrollY() / (mSizeForOtherLrc + mLinePadding));
                        int currentRow = getRowByScrollY();
                        currentRow = Math.min(currentRow, mLrcRows.size() - 1);
                        currentRow = Math.max(currentRow, 0);
                        seekTo(mLrcRows.get(currentRow).getTime(), false,false);
                        return true;
                    }
                    mLastY = event.getRawY();
                } else {
                    mHandler.removeCallbacks(mLongPressRunnable);
                }
                break;
            case MotionEvent.ACTION_UP:
                if(!mCanDrag){
                    if(mLongPressRunnable == null && mOnLrcClickListener != null){
                        mOnLrcClickListener.onClick();
                    }
                    mHandler.removeCallbacks(mLongPressRunnable);
                    mLongPressRunnable = null;
                }else{
                    if(onSeekToListener!= null && mCurRow != -1){
                        onSeekToListener.onSeekTo(mLrcRows.get(mCurRow).getTime());
                    }
                    if(getScrollY() < 0){
                        smoothScrollTo(0,DURATION_FOR_ACTION_UP);
                    }else if(getScrollY() > getScrollYByRow(mCurRow)){
                        smoothScrollTo(getScrollYByRow(mCurRow),DURATION_FOR_ACTION_UP);
                    }
                    mCanDrag = false;
                    mIsDrawTimeLine = false;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mHandler.removeCallbacks(mLongPressRunnable);
                mLongPressRunnable = null;
                break;
        }
        return true;
    }
    /**
     * 为LrcView设置歌词List集合数据
     */
    @Override
    public void setLrcRows(List<LrcRow> lrcRows) {
        reset();
        mLrcRows = lrcRows;
        if(mLrcRows != null){
            //计算每一行歌词所占的高度
//            for(LrcRow lrcRow : mLrcRows){
//                int height = 0;
//                String combine = lrcRow.getContent() + (!TextUtils.isEmpty(lrcRow.getTranslate()) ? "\t" + lrcRow.getTranslate() : "");
//                String[] multiText = combine.split("\t");
//                for (String text : multiText) {
//                    float textWidth = mPaintForOtherLrc.measureText(text);
//                    int lineNumber = (int) Math.ceil(textWidth / getWidth());
//
//                    height += lineNumber * mPaintForOtherLrc.getTextSize() + DEFAULT_SPACING_PADDING;
//                }
//                lrcRow.setHeight(height);
//            }
            for(LrcRow lrcRow : mLrcRows){
                lrcRow.setContentHeight(getSingleLineHeight(lrcRow.getContent()));
                if(lrcRow.hasTranslate()){
                    lrcRow.setTranslateHeight(getSingleLineHeight(lrcRow.getTranslate()));
                }
                lrcRow.setTotalHeight(lrcRow.getTranslateHeight() + lrcRow.getContentHeight());
            }
        }
        invalidate();
    }

    private int getSingleLineHeight(String text){
        float textWidth = mPaintForOtherLrc.measureText(text);
        StaticLayout staticLayout = new StaticLayout(text, mPaintForOtherLrc, getWidth(),textWidth > getWidth() ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL,
                DEFAULT_SPACING_MULT, DEFAULT_SPACING_PADDING, true);
        return staticLayout.getHeight();
    }

    /**当前高亮歌词的行号**/
    private int mCurRow = -1;
    /**上一次的高亮歌词的行号**/
    private int mLastRow = -1;

    /**
     * 到第n行所滚动过的距离
     * @return
     */
    private int getScrollYByRow(int row){
        int scrollY = 0;
        for(int i = 0 ;i < mLrcRows.size() &&  i < row;i++){
            scrollY += mLrcRows.get(i).getTotalHeight() + mLinePadding;
        }
        return scrollY;
    }

    /**
     * 根据当前行数计算滑动距离
     * @return
     */
    private int getRowByScrollY(){
        int totalY = 0;
        int line;
        for(line = 0; line < mLrcRows.size();line++){
            totalY += mLinePadding + mLrcRows.get(line).getTotalHeight();
            if(totalY >= getScrollY())
                return line;
        }
        return line - 1;
    }

    @Override
    public void seekTo(int progress,boolean fromSeekBar,boolean fromSeekBarByUser) {
        if(mLrcRows == null || mLrcRows.size() == 0){
            return;
        }
        //如果是由seekbar的进度改变触发 并且这时候处于拖动状态，则返回
        if(fromSeekBar && mCanDrag){
            return;
        }
        for (int i = mLrcRows.size() - 1; i >= 0; i--) {
            if(progress >= mLrcRows.get(i).getTime()){
                if(mCurRow != i){
                    mLastRow = mCurRow;
                    mCurRow = i;
                    log("mCurRow=i="+mCurRow);
                    if(fromSeekBarByUser){
                        if(!mScroller.isFinished()){
                            mScroller.forceFinished(true);
                        }
                        scrollTo(getScrollX(), getScrollYByRow(mCurRow));
                    }else{
                        smoothScrollTo(getScrollYByRow(mCurRow), DURATION_FOR_LRC_SCROLL);
                    }
                    //如果高亮歌词的宽度大于View的宽，就需要开启属性动画，让它水平滚动
//					float textWidth = mPaintForHighLightLrc.measureText(mLrcRows.get(mCurRow).getContent());
//					log("textWidth="+textWidth+"getWidth()=" + getWidth());
//					if(textWidth > getWidth()){
//						if(fromSeekBarByUser){
//							mScroller.forceFinished(true);
//						}
//						log("开始水平滚动歌词:" + mLrcRows.get(mCurRow).getContent());
//						startScrollLrc(getWidth() - textWidth, (long) (mLrcRows.get(mCurRow).getTotalTime() * 0.6));
//					}
                    invalidate();
                }
                break;
            }
        }

    }

    /**
     * 设置歌词的缩放比例
     */
    @Override
    public void setLrcScalingFactor(float scalingFactor) {
        mCurScalingFactor = scalingFactor;
        mSizeForHighLightLrc = DEFAULT_SIZE_FOR_HIGH_LIGHT_LRC * mCurScalingFactor;
        mSizeForOtherLrc = DEFAULT_SIZE_FOR_OTHER_LRC * mCurScalingFactor;
        mLinePadding = DEFAULT_PADDING * mCurScalingFactor;
        mTotalRow = (int) (getHeight() / (mSizeForOtherLrc + mLinePadding)) + 3;
        scrollTo(getScrollX(), (int) (mCurRow * (mSizeForOtherLrc + mLinePadding)));
        invalidate();
        mScroller.forceFinished(true);
    }

    /**
     * 重置
     */
    @Override
    public void reset() {
        if(!mScroller.isFinished()){
            mScroller.forceFinished(true);
        }
        mTotalRow = 0;
        mLrcRows = null;
        scrollTo(getScrollX(), 0);
        invalidate();
    }


    /**
     * 平滑的移动到某处
     * @param dstY
     */
    private void smoothScrollTo(int dstY,int duration){
        int oldScrollY = getScrollY();
        int offset = dstY - oldScrollY;
        mScroller.startScroll(getScrollX(), oldScrollY, getScrollX(), offset, duration);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (!mScroller.isFinished()) {
            if (mScroller.computeScrollOffset()) {
                int oldY = getScrollY();
                int y = mScroller.getCurrY();
                if (oldY != y && !mCanDrag) {
                    scrollTo(getScrollX(), y);
                }
                mCurFraction = mScroller.timePassed() * 3f / DURATION_FOR_LRC_SCROLL;
                mCurFraction = Math.min(mCurFraction, 1F);
                invalidate();
            }
        }
    }
    /**
     * 返回当前的歌词缩放比例
     * @return
     */
    public float getCurScalingFactor() {
        return mCurScalingFactor;
    }

    private OnSeekToListener onSeekToListener;
    public void setOnSeekToListener(OnSeekToListener onSeekToListener) {
        this.onSeekToListener = onSeekToListener;
    }

    public void setText(String text) {
        mText = text;
        reset();
    }

    public interface OnSeekToListener{
        void onSeekTo(int progress);
    }

    private OnLrcClickListener mOnLrcClickListener;
    public void setOnLrcClickListener(OnLrcClickListener mOnLrcClickListener) {
        this.mOnLrcClickListener = mOnLrcClickListener;
    }

    public interface OnLrcClickListener{
        void onClick();
        void onLongClick();
    }

    public void log(Object o){
        Log.d("LrcView", o+"");
    }

    /**
     * 外部viewpager是否正在滑动
     * @param scrolling
     */
    public void setViewPagerScroll(boolean scrolling){
        mIsViewPagerScroll = scrolling;
    }

    /**
     * 设置高亮歌词颜色
     * @param color
     */
    public void setHighLightColor(@ColorInt int color){
        mColorForHighLightLrc = color;
        if(mPaintForHighLightLrc != null){
            mPaintForHighLightLrc.setColor(mColorForHighLightLrc);
        }
    }

    /**
     * 设置非高亮歌词颜色
     * @param color
     */
    public void setOtherColor(@ColorInt int color){
        mColorForOtherLrc = color;
        if(mPaintForOtherLrc != null)
            mPaintForOtherLrc.setColor(mColorForOtherLrc);
    }

    /**
     * 设置时间线颜色
     * @param color
     */
    public void setTimeLineColor(@ColorInt int color){
        mTimeLineColor = color;
        if(mPaintForTimeLine != null)
            mPaintForTimeLine.setColor(color);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if(mHandler != null)
            mHandler.removeCallbacksAndMessages(null);
    }


}
