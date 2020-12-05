package remix.myplayer.ui.widget.desktop;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import remix.myplayer.lyric.bean.LrcRow;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/11 14:22
 */

public class DesktopLyricTextView extends androidx.appcompat.widget.AppCompatTextView {

  private static final int DELAY_MAX = 100;

  /**
   * 当前x坐标
   */
  private float mCurTextXForHighLightLrc;
  /**
   * 当前的歌词
   */
  private LrcRow mCurLrcRow;
  /**
   * 当前歌词的字符串所占的控件
   */
  private Rect mTextRect = new Rect();
  /**
   * 垂直便宜
   */
  private int mOffsetY;
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

  public DesktopLyricTextView(Context context) {
    this(context, null);
  }

  public DesktopLyricTextView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public DesktopLyricTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
  }

  /**
   * 控制歌词水平滚动的属性动画
   ***/
  private ValueAnimator mAnimator;

  /**
   * 开始水平滚动歌词
   *
   * @param endX 歌词第一个字的最终的x坐标
   * @param duration 滚动的持续时间
   */
  private void startScrollLrc(float endX, long duration) {
    if (mAnimator == null) {
      mAnimator = ValueAnimator.ofFloat(0, endX);
      mAnimator.addUpdateListener(mUpdateListener);
    } else {
      mCurTextXForHighLightLrc = 0;
      mAnimator.cancel();
      mAnimator.setFloatValues(0, endX);
    }
    mAnimator.setDuration(duration);
    long delay = (long) (duration * 0.1);
    mAnimator.setStartDelay(delay > DELAY_MAX ? DELAY_MAX : delay); //延迟执行属性动画
    mAnimator.start();
  }

  @Override
  public void setTextColor(ColorStateList colors) {
    super.setTextColor(colors);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mCurLrcRow == null) {
      return;
    }
    canvas.drawText(mCurLrcRow.getContent(),
        mCurTextXForHighLightLrc,
        (getHeight() - getPaint().getFontMetrics().top - getPaint().getFontMetrics().bottom) / 2,
        getPaint());
  }

  public void setLrcRow(@NonNull LrcRow lrcRow) {
    if (lrcRow.getTime() != 0 && mCurLrcRow != null && mCurLrcRow.getTime() == lrcRow.getTime()) {
      return;
    }
    mCurLrcRow = lrcRow;
    stopAnimation();
//        setText(mCurLrcRow.getContent());

    TextPaint paint = getPaint();
    if (paint == null) {
      return;
    }
    String text = mCurLrcRow.getContent();
    paint.getTextBounds(text, 0, text.length(), mTextRect);
    float textWidth = mTextRect.width();
    mOffsetY = (int) ((mTextRect.bottom + mTextRect.top - paint.getFontMetrics().bottom - paint
        .getFontMetrics().top) / 2);
    if (textWidth > getWidth()) {
      //如果歌词宽度大于view的宽，则需要动态设置歌词的起始x坐标，以实现水平滚动
      startScrollLrc(getWidth() - textWidth, (long) (mCurLrcRow.getTotalTime() * 0.85));
    } else {
      //如果歌词宽度小于view的宽，则让歌词居中显示
      mCurTextXForHighLightLrc = (getWidth() - textWidth) / 2;
      invalidate();
    }

  }

  public void stopAnimation() {
    if (mAnimator != null && mAnimator.isRunning()) {
      mAnimator.cancel();
    }
    invalidate();
  }
}
