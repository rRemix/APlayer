package remix.myplayer.util

import android.annotation.SuppressLint
import timber.log.Timber

object SystemPropertiesUtil {
  private const val TAG = "SystemPropertiesUtil"

  @SuppressLint("PrivateApi")
  private val clazz = Class.forName("android.os.SystemProperties")

  fun get(key: String): String {
    try {
      return clazz.getDeclaredMethod("get", String::class.java).invoke(null, key) as String
    } catch (t: Throwable) {
      Timber.tag(TAG).e(t, "Failed to invoke get")
      return ""
    }
  }
}
