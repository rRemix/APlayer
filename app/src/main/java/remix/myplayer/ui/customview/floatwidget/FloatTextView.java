package remix.myplayer.ui.customview.floatwidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import remix.myplayer.lyric.LrcRow;
import remix.myplayer.util.DensityUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/11 14:22
 */

public class FloatTextView extends android.support.v7.widget.AppCompatTextView {
    private Context mContext;
    /** 画笔*/
    private Paint mPaint;
    /** 当前x坐标*/
    private float mCurTextXForHighLightLrc;
    /** 当前的歌词*/
    private LrcRow mCurLrcRow;
    /** 当前歌词的字符串所占的控件*/
    private Rect mTextRect = new Rect();
    /***
     * 监听属性动画的数值值的改变
     */
    ValueAnimator.AnimatorUpdateListener mUpdateListener = new ValueAnimator.AnimatorUpdateListener() {

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mCurTextXForHighLightLrc = (Float) animation.getAnimatedValue();
            invalidate();
        }
    };

    public FloatTextView(Context context) {
        super(context);
        init(context);
    }
    public FloatTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mPaint = getPaint();
//        int color = ThemeStore.getMaterialPrimaryColor();
        mPaint.setShadowLayer(DensityUtil.dip2px(mContext,1.0f),0,1, Color.argb(51,0,0,0));
    }

    public FloatTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    /**控制歌词水平滚动的属性动画***/
    private ValueAnimator mAnimator;
    /**
     * 开始水平滚动歌词
     * @param endX 歌词第一个字的最终的x坐标
     * @param duration 滚动的持续时间
     */
    private void startScrollLrc(float endX,long duration){
        if(mAnimator == null){
            mAnimator = ValueAnimator.ofFloat(0,endX);
            mAnimator.addUpdateListener(mUpdateListener);
        }else{
            mCurTextXForHighLightLrc = 0;
            mAnimator.cancel();
            mAnimator.setFloatValues(0,endX);
        }
        mAnimator.setDuration(duration);
        mAnimator.setStartDelay((long) (duration * 0.1)); //延迟执行属性动画
        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mCurLrcRow == null)
            return;
        canvas.drawText(mCurLrcRow.getContent(), mCurTextXForHighLightLrc,(getHeight() + mTextRect.height()) / 2, getPaint());
    }

    public void setLrcRow(LrcRow lrcRow){
        mCurLrcRow = lrcRow;
        if(mCurLrcRow != null){
            Paint paint = getPaint();
            if(paint == null)
                return;
            String text = mCurLrcRow.getContent();
            paint.getTextBounds(text,0,text.length(),mTextRect);
//            float textWidth = getPaint().measureText(text);//用画笔测量歌词的宽度
            float textWidth = mTextRect.width();
            if(textWidth > getWidth()){
                //如果歌词宽度大于view的宽，则需要动态设置歌词的起始x坐标，以实现水平滚动
                startScrollLrc(getWidth() - textWidth, (long) (mCurLrcRow.getTotalTime() * 0.6));
            }else{
                //如果歌词宽度小于view的宽，则让歌词居中显示
                mCurTextXForHighLightLrc = (getWidth() - textWidth) / 2;
            }
            invalidate();
        }
    }

}
