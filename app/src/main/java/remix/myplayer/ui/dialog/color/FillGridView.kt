package remix.myplayer.ui.dialog.color

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

/**
 * @author Aidan Follestad (afollestad)
 */
class FillGridView : GridView {
  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

  public override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val expandSpec = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2,
        MeasureSpec.AT_MOST)
    super.onMeasure(widthMeasureSpec, expandSpec)
  }
}