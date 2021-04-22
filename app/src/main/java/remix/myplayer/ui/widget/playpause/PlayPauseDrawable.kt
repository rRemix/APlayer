package remix.myplayer.ui.widget.playpause

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Property
import remix.myplayer.R

class PlayPauseDrawable(context: Context) : Drawable() {
  private val mLeftPauseBar = Path()
  private val mRightPauseBar = Path()
  private val mPaint = Paint()
  private val mBounds = RectF()
  private val mPauseBarWidth: Float
  private val mPauseBarHeight: Float
  private val mPauseBarDistance: Float
  private var mWidth = 0f
  private var mHeight = 0f
  private var mProgress = 1f
  var isPlay = true

  override fun onBoundsChange(bounds: Rect) {
    super.onBoundsChange(bounds)
    mBounds.set(bounds)
    mWidth = mBounds.width()
    mHeight = mBounds.height()
  }

  override fun draw(canvas: Canvas) {
    mLeftPauseBar.rewind()
    mRightPauseBar.rewind()

    // The current distance between the two pause bars.
    val barDist = lerp(mPauseBarDistance, 0f, mProgress) - 1
    // The current width of each pause bar.
    val barWidth = lerp(mPauseBarWidth, mPauseBarHeight / 1.75f, mProgress)
    // The current position of the left pause bar's top left coordinate.
    val firstBarTopLeft = lerp(0f, barWidth, mProgress)
    // The current position of the right pause bar's top right coordinate.
    val secondBarTopRight = lerp(2 * barWidth + barDist, barWidth + barDist, mProgress)

    // Draw the left pause bar. The left pause bar transforms into the
    // top half of the play button triangle by animating the position of the
    // rectangle's top left coordinate and expanding its bottom width.
    mLeftPauseBar.moveTo(0f, 0f)
    mLeftPauseBar.lineTo(firstBarTopLeft, -mPauseBarHeight)
    mLeftPauseBar.lineTo(barWidth, -mPauseBarHeight)
    mLeftPauseBar.lineTo(barWidth, 0f)
    mLeftPauseBar.close()

    // Draw the right pause bar. The right pause bar transforms into the
    // bottom half of the play button triangle by animating the position of the
    // rectangle's top right coordinate and expanding its bottom width.
    mRightPauseBar.moveTo(barWidth + barDist, 0f)
    mRightPauseBar.lineTo(barWidth + barDist, -mPauseBarHeight)
    mRightPauseBar.lineTo(secondBarTopRight, -mPauseBarHeight)
    mRightPauseBar.lineTo(2 * barWidth + barDist, 0f)
    mRightPauseBar.close()
    canvas.save()

    // Translate the play button a tiny bit to the right so it looks more centered.
    canvas.translate(lerp(0f, mPauseBarHeight / 8f, mProgress), 0f)

    // (1) Pause --> Play: rotate 0 to 90 degrees clockwise.
    // (2) Play --> Pause: rotate 90 to 180 degrees clockwise.
    val rotationProgress = if (isPlay) 1 - mProgress else mProgress
    val startingRotation: Float = if (isPlay) 90F else 0.toFloat()
    canvas.rotate(lerp(startingRotation, startingRotation + 90, rotationProgress), mWidth / 2f,
        mHeight / 2f)

    // Position the pause/play button in the center of the drawable's bounds.
    canvas.translate(mWidth / 2f - (2 * barWidth + barDist) / 2f,
        mHeight / 2f + mPauseBarHeight / 2f)

    // Draw the two bars that form the animated pause/play button.
    canvas.drawPath(mLeftPauseBar, mPaint)
    canvas.drawPath(mRightPauseBar, mPaint)
    canvas.restore()
  }

  val pausePlayAnimator: Animator
    get() {
      val animator: Animator = ObjectAnimator.ofFloat(this, PROGRESS, if (isPlay) 1F else 0.toFloat(), if (isPlay) 0F else 1F)
      animator.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          isPlay = !isPlay
        }
      })
      return animator
    }
  private var progress: Float
    get() = mProgress
    private set(progress) {
      mProgress = progress
      invalidateSelf()
    }

  override fun setAlpha(alpha: Int) {
    mPaint.alpha = alpha
    invalidateSelf()
  }

  override fun setColorFilter(cf: ColorFilter?) {
    mPaint.colorFilter = cf
    invalidateSelf()
  }

  override fun getOpacity(): Int {
    return PixelFormat.TRANSLUCENT
  }

  fun setPlay() {
    isPlay = true
    mProgress = 1f
  }

  fun setPause() {
    isPlay = false
    mProgress = 0f
  }

  companion object {
    private val PROGRESS: Property<PlayPauseDrawable, Float> = object : Property<PlayPauseDrawable, Float>(Float::class.java, "progress") {
      override fun get(d: PlayPauseDrawable): Float {
        return d.progress
      }

      override fun set(d: PlayPauseDrawable, value: Float) {
        d.progress = value
      }
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private fun lerp(a: Float, b: Float, t: Float): Float {
      return a + (b - a) * t
    }
  }

  init {
    val res = context.resources
    mPaint.isAntiAlias = true
    mPaint.style = Paint.Style.FILL
    mPaint.color = Color.WHITE
    mPauseBarWidth = res.getDimensionPixelSize(R.dimen.pause_bar_width).toFloat()
    mPauseBarHeight = res.getDimensionPixelSize(R.dimen.pause_bar_height).toFloat()
    mPauseBarDistance = res.getDimensionPixelSize(R.dimen.pause_bar_distance).toFloat()
  }
}