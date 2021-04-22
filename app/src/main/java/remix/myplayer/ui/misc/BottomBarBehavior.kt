package remix.myplayer.ui.misc

import android.R
import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import remix.myplayer.App.Companion.context
import remix.myplayer.util.DensityUtil
import kotlin.math.abs

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 09:16
 */
class BottomBarBehavior : CoordinatorLayout.Behavior<View> {
  constructor() : super()
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  override fun layoutDependsOn(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
    return dependency is AppBarLayout
  }

  override fun onDependentViewChanged(parent: CoordinatorLayout, child: View, dependency: View): Boolean {
    val top = abs(dependency.top).toFloat()
    val context: Context = context
    val ta = context.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
    val actionBarSize = ta.getDimensionPixelSize(0, 0)
    ta.recycle()
    return if (actionBarSize > 0) {
      val bottomBarSize = DensityUtil.dip2px(context, 72f)
      child.translationY = top * bottomBarSize / actionBarSize
      true
    } else {
      false
    }
  }
}