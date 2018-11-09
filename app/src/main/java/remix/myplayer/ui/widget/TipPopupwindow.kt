package remix.myplayer.ui.widget

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import remix.myplayer.R
import remix.myplayer.util.StatusBarUtil

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/11/11 14:10
 */

class TipPopupwindow(context: Context?) : PopupWindow() {
    private var yOffset = 0

    init {

        contentView = LayoutInflater.from(context).inflate(R.layout.popup_multi_tip, null)
        width = ViewGroup.LayoutParams.MATCH_PARENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        setBackgroundDrawable(BitmapDrawable())
        isFocusable = false
        isOutsideTouchable = true

        val ta = context!!.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val actionBarSize = ta.getDimensionPixelSize(0, 0)
        ta.recycle()
        yOffset = StatusBarUtil.getStatusBarHeight(context) + actionBarSize
    }

    fun show(parent: View) {
        showAsDropDown(parent, 0, yOffset)
        //两秒钟后关闭
        Handler().postDelayed({ dismiss() }, 2000)
    }
}
