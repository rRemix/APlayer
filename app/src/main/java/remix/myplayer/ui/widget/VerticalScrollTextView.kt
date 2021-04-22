package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView

/**
 * 滚动换行
 */
class VerticalScrollTextView : AppCompatTextView {
  private var orientation = VERTICAL

  constructor(context: Context) : super(context) {}
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

  fun setOrientation(orientation: Int) {
    require(!(orientation != HORIZONTAL && orientation != VERTICAL)) { "Invalid orientation" }
    this.orientation = orientation
  }

  fun setTextWithAnimation(@StringRes textRes: Int) {
    setTextWithAnimation(resources.getString(textRes))
  }

  fun setTextWithAnimation(text: CharSequence?) {
    setText(text)
  }

  companion object {
    const val VERTICAL = 0
    const val HORIZONTAL = 1
    private const val DURATION = 2000
  }
}