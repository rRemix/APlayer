package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout

class AutoFitSquareFrameLayout : FrameLayout {
  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
    val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
    val sizeMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.min(widthSize, heightSize), View.MeasureSpec.EXACTLY)
    super.onMeasure(sizeMeasureSpec, sizeMeasureSpec)
    //根据高宽比调整布局
    if (heightSize * 1f / widthSize > 1.2f) {
      val lp = layoutParams as RelativeLayout.LayoutParams
      lp.addRule(RelativeLayout.CENTER_VERTICAL)
      lp.topMargin = 0
      lp.bottomMargin = 0
    }
  }
}
