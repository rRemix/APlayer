package remix.myplayer.compose.nav

import android.os.SystemClock
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import remix.myplayer.App
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

/**
 * app在前台时显示snackbar，否则展示toast
 */
object UiMessageDispatcher {

  private const val MIN_INTERVAL_MS = 1000L

  @Volatile
  private var lastShownAt = 0L

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
  internal val messages = _messages.asSharedFlow()

  fun show(message: String) {
    val now = SystemClock.uptimeMillis()
    if (now - lastShownAt < MIN_INTERVAL_MS) {
      return
    }
    lastShownAt = now

    if (Util.isAppOnForeground) {
      _messages.tryEmit(message)
    } else {
      ToastUtil.show(App.context, message)
    }
  }

  fun show(@StringRes resId: Int, vararg formatArgs: Any) {
    val msg = App.context.getString(resId, *formatArgs)
    show(msg)
  }
}