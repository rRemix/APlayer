package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton

/**
 * Created by taeja on 16-3-18.
 */
/**
 * 自定义Button 主要用于嵌套与ViewPagrer
 */
class PagerButton : AppCompatButton {
  constructor(context: Context) : super(context) {}
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

  override fun onTouchEvent(event: MotionEvent): Boolean {
    //ACTION_MOVE事件继续向上传递,否则ViewPager无法滑动
    return if (event.action == MotionEvent.ACTION_MOVE) {
      true
    } else super.onTouchEvent(event)
  }
}