package remix.myplayer.ui.widget.playpause

import android.animation.Animator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Property
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import remix.myplayer.R

class PlayPauseView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
  private val mDrawable: PlayPauseDrawable
  private val mPaint = Paint()
  private var mAnimator: Animator? = null
  private var mBackgroundColor: Int
  private var mWidth = 0
  private var mHeight = 0

  //    @Override
  //    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
  //        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  //        final int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
  //        setMeasuredDimension(size, size);
  //    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    //        float pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
//        int size= Math.round(pixels);
//        setMeasuredDimension(size,size);
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    mDrawable.setBounds(0, 0, w, h)
    mWidth = w
    mHeight = h
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      outlineProvider = object : ViewOutlineProvider() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun getOutline(view: View, outline: Outline) {
          outline.setOval(0, 0, view.width, view.height)
        }
      }
      clipToOutline = true
    }
  }

  private var color: Int
    get() = mBackgroundColor
    private set(color) {
      mBackgroundColor = color
      invalidate()
    }

  override fun verifyDrawable(who: Drawable): Boolean {
    return who === mDrawable || super.verifyDrawable(who)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    mPaint.color = mBackgroundColor
    val radius = mWidth.coerceAtMost(mHeight) / 2f
    canvas.drawCircle(mWidth / 2f, mHeight / 2f, radius, mPaint)
    mDrawable.draw(canvas)
  }

  override fun setBackgroundColor(@ColorInt color: Int) {
    mBackgroundColor = color
    mPaint.color = color
    invalidate()
  }

  private fun initState(isPlay: Boolean) {
    if (isPlay) {
      mDrawable.setPlay()
    } else {
      mDrawable.setPause()
    }
  }

  @JvmOverloads
  fun updateState(isPlay: Boolean, withAnim: Boolean = true) {
    if (mDrawable.isPlay != isPlay) {
      return
    }
    toggle(withAnim)
  }

  fun toggle(withAnim: Boolean) {
    if (withAnim) {
      if (mAnimator != null) {
        mAnimator?.cancel()
      }
      mAnimator = mDrawable.pausePlayAnimator
      mAnimator?.interpolator = DecelerateInterpolator()
      mAnimator?.duration = PLAY_PAUSE_ANIMATION_DURATION
      mAnimator?.start()
    } else {
      val isPlay = mDrawable.isPlay
      initState(isPlay)
      invalidate()
    }
  }

  companion object {
    private val COLOR: Property<PlayPauseView, Int> = object : Property<PlayPauseView, Int>(Int::class.java, "color") {
      override fun get(v: PlayPauseView): Int {
        return v.color
      }

      override fun set(v: PlayPauseView, value: Int) {
        v.color = value
      }
    }
    private const val PLAY_PAUSE_ANIMATION_DURATION = 250L
  }

  init {
    setWillNotDraw(false)
    mBackgroundColor = resources.getColor(R.color.white)
    mPaint.isAntiAlias = true
    mPaint.style = Paint.Style.FILL
    mPaint.color = mBackgroundColor
    mDrawable = PlayPauseDrawable(context)
    mDrawable.callback = this
  }
}