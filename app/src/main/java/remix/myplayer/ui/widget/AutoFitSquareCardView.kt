package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import remix.myplayer.App.Companion.context
import remix.myplayer.util.DensityUtil

class AutoFitSquareCardView : CardView {
  constructor(context: Context) : super(context) {}
  constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {}
  constructor(context: Context, attributeSet: AttributeSet?, i: Int) : super(context, attributeSet, i) {}

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)
    val sizeMeasureSpec = MeasureSpec
        .makeMeasureSpec(widthSize.coerceAtMost(heightSize), MeasureSpec.EXACTLY)
    super.onMeasure(sizeMeasureSpec, sizeMeasureSpec)
    //根据高宽比调整布局
    if (heightSize * 1f / widthSize > 1.2f) {
      val lp = layoutParams as RelativeLayout.LayoutParams
      lp.addRule(RelativeLayout.CENTER_VERTICAL)
      lp.topMargin = 0
      lp.bottomMargin = 0
    }
  }

  companion object {
    private val THRESHOLD = DensityUtil.dip2px(context, 40f)
  }
}