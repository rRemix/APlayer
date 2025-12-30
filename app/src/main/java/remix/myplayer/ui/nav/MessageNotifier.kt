package remix.myplayer.ui.nav

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import remix.myplayer.App
import remix.myplayer.util.Util

/**
 * app在前台时显示snackbar，否则展示toast
 */
object MessageNotifier {

  private const val MIN_INTERVAL_MS = 1000L

  @Volatile
  private var lastShownAt = 0L

  private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
  internal val messages = _messages.asSharedFlow()

  private val mainHandler = Handler(Looper.getMainLooper())

  fun show(message: String) {
    val now = SystemClock.uptimeMillis()
    if (now - lastShownAt < MIN_INTERVAL_MS) {
      return
    }
    lastShownAt = now

    if (Util.isAppOnForeground) {
      _messages.tryEmit(message)
    } else {
      showToast(message)
    }
  }


  fun show(@StringRes resId: Int, vararg formatArgs: Any) {
    val msg = if (formatArgs.isNotEmpty()) {
      App.context.getString(resId, *formatArgs)
    } else {
      App.context.getString(resId)
    }
    show(msg)
  }

  private fun showToast(message: String) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      Toast.makeText(App.context, message, Toast.LENGTH_SHORT).show()
    } else {
      mainHandler.post {
        Toast.makeText(App.context, message, Toast.LENGTH_SHORT).show()
      }
    }
  }
}