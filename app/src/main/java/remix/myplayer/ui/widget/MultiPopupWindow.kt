package remix.myplayer.ui.widget

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import remix.myplayer.R
import remix.myplayer.util.StatusBarUtil

class MultiPopupWindow(activity: Activity) : PopupWindow(activity) {

    init {
        contentView = LayoutInflater.from(activity).inflate(R.layout.toolbar_multi, activity.window.decorView as ViewGroup, false)
        width = ViewGroup.LayoutParams.MATCH_PARENT
        val ta = activity.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarSize = ta.getDimensionPixelSize(0, 0)
        ta.recycle()
        height = StatusBarUtil.getStatusBarHeight(activity) + actionBarSize
        setBackgroundDrawable(BitmapDrawable())
        isFocusable = false
        isOutsideTouchable = false
    }

    fun show(parent: View) {
        showAsDropDown(parent, 0, 0)
    }
}