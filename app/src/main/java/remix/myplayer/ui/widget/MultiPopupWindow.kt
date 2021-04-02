package remix.myplayer.ui.widget

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.appcompat.widget.TooltipCompat
import remix.myplayer.R
import remix.myplayer.databinding.ToolbarMultiBinding
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.util.StatusBarUtil

class MultiPopupWindow(activity: Activity) : PopupWindow(activity) {
  val binding: ToolbarMultiBinding = ToolbarMultiBinding.inflate(
    LayoutInflater.from(activity),
    activity.window.decorView as ViewGroup,
    false
  )

  init {
    contentView = binding.root
    width = ViewGroup.LayoutParams.MATCH_PARENT
    val ta = activity.obtainStyledAttributes(intArrayOf(R.attr.actionBarSize))
    val actionBarSize = ta.getDimensionPixelSize(0, 0)
    ta.recycle()
    height = StatusBarUtil.getStatusBarHeight(activity) + actionBarSize
    isFocusable = false
    isOutsideTouchable = false

    setBackgroundDrawable(ColorDrawable(ThemeStore.materialPrimaryColor))
    binding.multiTitle.setTextColor(ThemeStore.colorOnPrimary)
    arrayOf(
      binding.multiClose,
      binding.multiPlaylist,
      binding.multiQueue,
      binding.multiDelete,
      binding.multiMore
    ).forEach {
      TintHelper.setTintAuto(it, ThemeStore.colorOnPrimary, false)
    }
    arrayOf(
      binding.multiPlaylist,
      binding.multiQueue,
      binding.multiDelete,
      binding.multiMore
    ).forEach {
      TooltipCompat.setTooltipText(it, it.contentDescription)
    }
  }

  fun show(parent: View) {
    showAsDropDown(parent, 0, 0)
  }
}