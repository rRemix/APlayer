package remix.myplayer.compose.ui.screen.setting.logic.other

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.App.Companion.IS_GOOGLEPLAY
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.ArrowPreference
import remix.myplayer.misc.AppInfo
import remix.myplayer.misc.SystemInfo
import remix.myplayer.misc.tryLaunch
import remix.myplayer.misc.zipFrom
import remix.myplayer.misc.zipOutputStream
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File

@Composable
fun FeedbackLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val state = rememberDialogState()

  ArrowPreference(R.string.feedback_info) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.send_log,
    positiveRes = R.string.yes,
    negativeRes = R.string.no,
    neutralRes = R.string.cancel,
    onPositive = {
      send(context, scope, true)
    },
    onNegative = {
      send(context, scope, false)
    }
  )
}


fun send(context: Context, scope: CoroutineScope, sendLog: Boolean) {
  val emailIntent = Intent()
  emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback))
  emailIntent.putExtra(
    Intent.EXTRA_TEXT,
    "\n\n\nApp info:\n${AppInfo.prettyPrinted}\n\nSystem info:\n${SystemInfo.prettyPrinted}"
  )

  scope.tryLaunch(catch = {
    Timber.w(it)
    ToastUtil.show(context, R.string.send_error, it.toString())
  }, block = {
    if (sendLog) {
      withContext(Dispatchers.IO) {
        try {
          val zipFile =
            File("${Environment.getExternalStorageDirectory().absolutePath}/Android/data/${context.packageName}/logs.zip")
          zipFile.delete()
          zipFile.createNewFile()
          zipFile.zipOutputStream().zipFrom(
            "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/${context.packageName}/logs",
            "${context.applicationInfo.dataDir}/shared_prefs"
          )
          if (zipFile.length() > 0) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
              FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                zipFile
              )
            } else {
              "file://${zipFile.absoluteFile}".toUri()
            }
            emailIntent.action = Intent.ACTION_SEND
            emailIntent.type = "application/octet-stream"
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
            emailIntent.putExtra(
              Intent.EXTRA_EMAIL,
              arrayOf(if (!IS_GOOGLEPLAY) "568920427@qq.com" else "rRemix.me@gmail.com")
            )
          }
        } catch (e: Exception) {
          Timber.w(e)
        }
      }
    } else {
      emailIntent.action = Intent.ACTION_SENDTO
      emailIntent.data = "mailto:rRemix.me@gmail.com".toUri()
    }

    Util.startActivitySafely(context, emailIntent)
  })
}