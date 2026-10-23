package remix.myplayer.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生成并持久化一个稳定的设备 ID，用于跨端事件标识。
 */
@Singleton
class DeviceIdProvider @Inject constructor(
  @ApplicationContext context: Context
) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val cached: String by lazy {
    prefs.getString(KEY_DEVICE_ID, null) ?: generate().also {
      prefs.edit().putString(KEY_DEVICE_ID, it).apply()
    }
  }

  fun deviceId(): String = cached

  private fun generate(): String = "android-" + UUID.randomUUID().toString().replace("-", "")

  companion object {
    private const val PREFS_NAME = "play_event"
    private const val KEY_DEVICE_ID = "device_id"
  }
}
