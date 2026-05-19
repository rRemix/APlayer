package remix.myplayer.misc.log

import timber.log.Timber

internal class LogTree : Timber.DebugTree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    // 1. Log to Logcat (Timber.DebugTree handles this well)
    super.log(priority, tag, message, t)

    // 2. Log to File
    LogFileWriter.log(priority, tag, message, t)
  }
}
