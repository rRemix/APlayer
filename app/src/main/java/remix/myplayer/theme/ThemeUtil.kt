package remix.myplayer.theme

import android.content.Context
import android.support.annotation.AttrRes
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.StatusBarUtil

object ThemeUtil {

    @JvmStatic
    fun resolveColor(context: Context, @AttrRes attr: Int): Int {
        return resolveColor(context, attr, 0)
    }

    @JvmStatic
    fun resolveColor(context: Context, @AttrRes attr: Int, fallback: Int): Int {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attr))

        val color: Int
        try {
            color = a.getColor(0, fallback)
        } finally {
            a.recycle()
        }

        return color
    }

    @JvmStatic
    fun isWindowBackgroundDark(context: Context): Boolean {
        return !ColorUtil.isColorLight(resolveColor(context, android.R.attr.windowBackground))
    }
}