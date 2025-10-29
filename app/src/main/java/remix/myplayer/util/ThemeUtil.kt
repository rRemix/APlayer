package remix.myplayer.util

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.AttrRes

object ThemeUtil {
  fun setLightNavigationBarAuto(activity: Activity, enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val decorView = activity.window.decorView
      var systemUiVisibility = decorView.getSystemUiVisibility()
      systemUiVisibility = if (enabled) {
        systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
      } else {
        systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
      }
      decorView.setSystemUiVisibility(systemUiVisibility)
    }
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