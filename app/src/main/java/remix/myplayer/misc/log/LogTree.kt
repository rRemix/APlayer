package remix.myplayer.misc.log

import android.util.Log
import org.slf4j.LoggerFactory
import timber.log.Timber

internal class LogTree : Timber.DebugTree() {

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    val logger = LoggerFactory.getLogger(tag ?: "root")
    when (priority) {
      Log.VERBOSE -> logger.trace(message)
      Log.DEBUG -> logger.debug(message)
      Log.INFO -> logger.info(message)
      Log.WARN -> logger.warn(message)
      Log.ERROR -> logger.error(message)
    }
  }
}