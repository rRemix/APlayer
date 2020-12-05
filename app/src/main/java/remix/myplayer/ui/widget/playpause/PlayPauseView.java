package remix.myplayer.ui.widget.playpause;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import remix.myplayer.R;

public class PlayPauseView extends FrameLayout {

  private static final Property<PlayPauseView, Integer> COLOR =
      new Property<PlayPauseView, Integer>(Integer.class, "color") {
        @Override
        public Integer get(PlayPauseView v) {
          return v.getColor();
        }

        @Override
        public void set(PlayPauseView v, Integer value) {
          v.setColor(value);
        }
      };

  private static final long PLAY_PAUSE_ANIMATION_DURATION = 250L;

  private final PlayPauseDrawable mDrawable;
  private final Paint mPaint = new Paint();

  private Animator mAnimator;
  private int mBackgroundColor;
  private int mWidth;
  private int mHeight;

  public PlayPauseView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setWillNotDraw(false);
    mBackgroundColor = getResources().getColor(R.color.white);
    mPaint.setAntiAlias(true);
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setColor(mBackgroundColor);
    mDrawable = new PlayPauseDrawable(context);
    mDrawable.setCallback(this);

  }

  //    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
//        setMeasuredDimension(size, size);
//    }
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
//        int size= Math.round(pixels);
//        setMeasuredDimension(size,size);
  }

  @Override
  protected void onSizeChanged(final int w, final int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    mDrawable.setBounds(0, 0, w, h);
    mWidth = w;
    mHeight = h;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setOutlineProvider(new ViewOutlineProvider() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void getOutline(View view, Outline outline) {
          outline.setOval(0, 0, view.getWidth(), view.getHeight());
        }
      });
      setClipToOutline(true);
    }
  }

  private void setColor(int color) {
    mBackgroundColor = color;
    invalidate();
  }

  private int getColor() {
    return mBackgroundColor;
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return who == mDrawable || super.verifyDrawable(who);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    mPaint.setColor(mBackgroundColor);
    final float radius = Math.min(mWidth, mHeight) / 2f;
    canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint);
    mDrawable.draw(canvas);
  }

  public void setBackgroundColor(@ColorInt int color) {
    mBackgroundColor = color;
    mPaint.setColor(color);
    invalidate();
  }

  public void initState(boolean isPlay) {
    if (isPlay) {
      mDrawable.setPlay();
    } else {
      mDrawable.setPause();
    }
  }

  public void updateState(boolean isPlay, boolean withAnim) {
    if (mDrawable.isPlay() != isPlay) {
      return;
    }
    toggle(withAnim);
  }

  public void updateState(boolean isPlay) {
    updateState(isPlay, true);
  }

  public void toggle(boolean withAnim) {
    if (withAnim) {
      if (mAnimator != null) {
        mAnimator.cancel();
      }
      mAnimator = mDrawable.getPausePlayAnimator();
      mAnimator.setInterpolator(new DecelerateInterpolator());
      mAnimator.setDuration(PLAY_PAUSE_ANIMATION_DURATION);
      mAnimator.start();
    } else {
      final boolean isPlay = mDrawable.isPlay();
      initState(isPlay);
      invalidate();
    }
  }
}
