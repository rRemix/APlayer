package remix.myplayer.ui.misc

import android.view.View

/**
 * created by Remix on 2019/3/29
 */

abstract class DoubleClickListener : View.OnClickListener {

  override fun onClick(v: View) {
    val currentTimeMillis = System.currentTimeMillis()
    if (currentTimeMillis - lastClickTime < DOUBLE_TIME) {
      onDoubleClick(v)
    }
    lastClickTime = currentTimeMillis
  }

  abstract fun onDoubleClick(v: View)

  companion object {

    private val DOUBLE_TIME: Long = 600
    private var lastClickTime: Long = 0
  }
}
