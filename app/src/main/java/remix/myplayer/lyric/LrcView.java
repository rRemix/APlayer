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
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

import java.util.List;

import remix.myplayer.APlayerApplication;

/***
 * 
 * 须知：
 * 在ViewGroup里面 scrollTo，scrollBy方法移动的是子View
 * 在View里面scrollTo，scrollBy方法移动的是View里面绘制的内容
 * 要点：
 * 1:歌词的上下平移用什么实现？
 *    用Scroller实现，Scroller只是一个工具而已，
 *    真正实现滚动效果的还是View的scrollTo方法
 * 2：歌词的水平滚动怎么实现？
 *    通过属性动画ValueAnimator控制高亮歌词绘制的x轴起始坐标
 * 
 * @author Ligang  2014/8/19
 *
 */
public class LrcView extends View implements ILrcView{
	/**所有的歌词***/
	private List<LrcRow> mLrcRows;
	/**默认文字的字体大小**/
	private static final float SIZE_FOR_DEFAULT_TEXT = 35;

	/**画高亮歌词的画笔***/
	private TextPaint mPaintForHighLightLrc;
	/**高亮歌词的默认字体大小***/
	private static final float DEFAULT_SIZE_FOR_HIGHT_LIGHT_LRC = 40;
	/**高亮歌词当前的字体大小***/
	private float mSizeForHighLightLrc = DEFAULT_SIZE_FOR_HIGHT_LIGHT_LRC;
	/**高亮歌词的默认字体颜色**/
	private static final int DEFAULT_COLOR_FOR_HIGHT_LIGHT_LRC = Color.BLACK;
	/**高亮歌词当前的字体颜色**/
	private int mColorForHighLightLrc = DEFAULT_COLOR_FOR_HIGHT_LIGHT_LRC;

	/**画其他歌词的画笔***/
	private TextPaint mPaintForOtherLrc;
	/**其他歌词的默认字体大小***/
	private static final float DEFAULT_SIZE_FOR_OTHER_LRC = 30;
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
	private float mCurSizeForTimeLine = 30;
	/**是否画时间线**/
	private boolean mIsDrawTimeLine = false;

	/**歌词间默认的行距**/
//	private static final float DEFAULT_PADDING = 50;
    private static final float DEFAULT_PADDING = 55;
	/**每一句歌词之间的行距**/
	private float mLinePadding = DEFAULT_PADDING;
	/** 跨行歌词之间额外的行距*/
	private float mSpacingPadding = 5;

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
	/***移动一句歌词的持续时间**/
	private static final int DURATION_FOR_LRC_SCROLL = 650;
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
		mScroller = new Scroller(getContext());
		mPaintForHighLightLrc = new TextPaint();
		mPaintForHighLightLrc.setAntiAlias(true);
		mPaintForHighLightLrc.setColor(mColorForHighLightLrc);
        mSizeForHighLightLrc = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,19, APlayerApplication.getContext().getResources().getDisplayMetrics());
		mPaintForHighLightLrc.setTextSize(mSizeForHighLightLrc);
		mPaintForHighLightLrc.setFakeBoldText(true);

		mPaintForOtherLrc = new TextPaint();
        mPaintForOtherLrc.setAntiAlias(true);
		mPaintForOtherLrc.setColor(mColorForOtherLrc);

        mSizeForOtherLrc = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,13, APlayerApplication.getContext().getResources().getDisplayMetrics());
		mPaintForOtherLrc.setTextSize(mSizeForOtherLrc);

		mPaintForTimeLine = new TextPaint();
        mPaintForTimeLine.setAntiAlias(true);
        mCurSizeForTimeLine = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,13, APlayerApplication.getContext().getResources().getDisplayMetrics());
        mPaintForTimeLine.setTextSize(mCurSizeForTimeLine);
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

		if(mTotalRow == 0){
			//初始化将要绘制的歌词行数
			mTotalRow = (int) (getHeight() / (mSizeForOtherLrc + mLinePadding)) + 4;
		}
		//因为不需要将所有歌词画出来
		int minRow = mCurRow - (mTotalRow - 1) / 2;
		int maxRow = mCurRow + (mTotalRow - 1) / 2;
		minRow = Math.max(minRow, 0); //处理上边界
		maxRow = Math.min(maxRow, mLrcRows.size() - 1); //处理下边界

        final int availableWidth = getWidth();
        for(int i = 0 ; i <= minRow;i++){
            String originalText = mLrcRows.get(i).getContent();
            String[] multiText = originalText.split("\n");
            for(String temp : multiText){
                float textWidth = (i == mCurRow ? mPaintForHighLightLrc : mPaintForOtherLrc).measureText(temp);
                int lineNumber = (int) Math.ceil(textWidth / availableWidth);
                //根据一句歌词所占的行数计算出下一行歌词绘制的y坐标
                mRowY += lineNumber * (i == mCurRow ? mPaintForHighLightLrc : mPaintForOtherLrc).getTextSize() + mSpacingPadding;
            }
            mRowY += mLinePadding;
        }

        mRowY = getHeight() / 2 + minRow * (mSizeForOtherLrc + mLinePadding);
		for (int i = minRow; i <= maxRow; i++) {
			if(i == mCurRow){   //画高亮歌词
                drawText(canvas,mPaintForHighLightLrc,availableWidth,mLrcRows.get(i).getContent());
			}else{  //普通歌词
				//计算歌词透明度
                int alpha = (int)(0xff /  Math.pow(Math.abs(i - mCurRow),1.2f));
                mPaintForOtherLrc.setAlpha(alpha);
                drawText(canvas,mPaintForOtherLrc, availableWidth, mLrcRows.get(i).getContent());
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
     * 以换行符(有翻译)为分割绘制歌词
     * @param canvas
     * @param textPaint
     * @param availableWidth
     * @param originalText
     */
    private void drawText(Canvas canvas,TextPaint textPaint,int availableWidth, String originalText) {
	    String[] multiText = originalText.split("\n");
	    for(String temp : multiText){
            float textWidth = textPaint.measureText(temp);
            StaticLayout staticLayout = new StaticLayout(temp, textPaint, availableWidth,textWidth > availableWidth ? Layout.Alignment.ALIGN_CENTER : Layout.Alignment.ALIGN_NORMAL, 1.0F, mSpacingPadding, true);

            float textX = textWidth > availableWidth ? 0 : (availableWidth - textWidth) / 2;
            canvas.save();
            canvas.translate(textX,mRowY);
            staticLayout.draw(canvas);
            canvas.restore();
            //根据一句歌词所占的行数计算出下一行歌词绘制的y坐标
            mRowY += staticLayout.getLineCount() * textPaint.getTextSize() + mSpacingPadding;
        }
        mRowY += mLinePadding;
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
                        int currentRow = (int) (getScrollY() / (mSizeForOtherLrc + mLinePadding));
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
                    }else if(getScrollY() > mLrcRows.size() * (mSizeForOtherLrc + mLinePadding) - mLinePadding){
                        smoothScrollTo((int) (mLrcRows.size() * (mSizeForOtherLrc + mLinePadding) - mLinePadding),DURATION_FOR_ACTION_UP);
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
		invalidate();
	}

	/**当前高亮歌词的行号**/
	private int mCurRow =-1;
	/**上一次的高亮歌词的行号**/
	private int mLastRow = -1;
	
	@Override
	public void seekTo(int progress,boolean fromSeekBar,boolean fromSeekBarByUser) {
		if(mLrcRows == null || mLrcRows.size() == 0){
			return;
		} 
		//如果是由seekbar的进度改变触发 并且这时候处于拖动状态，则返回
		if(fromSeekBar && mCanDrag){
			return;
		}
		for (int i = mLrcRows.size()-1; i >= 0; i--) {
			if(progress >= mLrcRows.get(i).getTime()){
				if(mCurRow != i){
					mLastRow = mCurRow;
					mCurRow = i;
					log("mCurRow=i="+mCurRow);
					if(fromSeekBarByUser){
						if(!mScroller.isFinished()){
							mScroller.forceFinished(true);
						}
						scrollTo(getScrollX(), (int) (mCurRow * (mSizeForOtherLrc + mLinePadding)));
					}else{
						smoothScrollTo( (int) (mCurRow * (mSizeForOtherLrc + mLinePadding)), DURATION_FOR_LRC_SCROLL);
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
		mSizeForHighLightLrc = DEFAULT_SIZE_FOR_HIGHT_LIGHT_LRC * mCurScalingFactor;
		mSizeForOtherLrc = DEFAULT_SIZE_FOR_OTHER_LRC * mCurScalingFactor;
		mLinePadding = DEFAULT_PADDING * mCurScalingFactor;
		mTotalRow = (int) (getHeight() / (mSizeForOtherLrc + mLinePadding)) + 3;
		log("mRowTotal="+ mTotalRow);
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
