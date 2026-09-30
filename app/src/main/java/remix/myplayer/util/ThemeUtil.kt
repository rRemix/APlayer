package remix.myplayer.util

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.AttrRes
import androidx.core.view.WindowCompat

object ThemeUtil {
  fun setLightNavigationBarAuto(activity: Activity, enabled: Boolean) {
    // 只更新图标外观，避免直接修改 systemUiVisibility 干扰 edge-to-edge 布局。
    val window = activity.window
    WindowCompat.getInsetsController(window, window.decorView)
      .isAppearanceLightNavigationBars = enabled
  }

  fun resolveColor(context: Context, @AttrRes attr: Int, fallback: Int): Int {
    val ta = context.theme.obtainStyledAttributes(intArrayOf(attr))
    var color: Int
    try {
      color = ta.getColor(0, fallback)
    } finally {
      ta.recycle()
    }
    return color
  }


  fun resolveDrawable(context: Context, @AttrRes attr: Int): Drawable? {
    val ta = context.theme.obtainStyledAttributes(intArrayOf(attr))
    var drawable: Drawable?
    try {
      drawable = ta.getDrawable(0)
    } finally {
      ta.recycle()
    }
    return drawable
  }
}
