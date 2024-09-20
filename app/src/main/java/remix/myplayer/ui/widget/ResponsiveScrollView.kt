package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
import kotlin.math.abs

class ResponsiveScrollView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null
) : ScrollView(context, attrs) {
  fun interface OnFlingEndListener {
    fun onFlingEnd(v: ResponsiveScrollView)
  }

  var onFlingEndListener: OnFlingEndListener? = null
  private var isBeingFlung: Boolean = false

  override fun fling(velocityY: Int) {
    isBeingFlung = true
    super.fling(velocityY)
  }

  override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
    if (isBeingFlung && (abs(t - oldt) <= 1 || t >= measuredHeight || t <= 0)) {
      isBeingFlung = false
      onFlingEndListener?.onFlingEnd(this)
    }
    super.onScrollChanged(l, t, oldl, oldt)
  }
}
