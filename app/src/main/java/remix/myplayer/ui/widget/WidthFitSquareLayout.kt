package remix.myplayer.ui.widget

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class WidthFitSquareLayout : FrameLayout {
  private var forceSquare = true

  constructor(context: Context) : super(context) {}
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,
              defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, if (forceSquare) widthMeasureSpec else heightMeasureSpec)
  }

  fun forceSquare(forceSquare: Boolean) {
    this.forceSquare = forceSquare
    requestLayout()
  }
}