package remix.myplayer.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.Settings
import androidx.core.net.toUri
import remix.myplayer.R
import remix.myplayer.service.MusicService
import remix.myplayer.ui.nav.MessageNotifier

object MusicUtil {

  fun makeCmdIntent(cmd: Int, shuffle: Boolean): Intent {
    return Intent(MusicService.ACTION_CMD).putExtra(MusicService.EXTRA_COMMAND, cmd)
      .putExtra(MusicService.EXTRA_SHUFFLE, shuffle)
  }

  fun makeCmdIntent(cmd: Int): Intent {
    return makeCmdIntent(cmd, false)
  }

  /**
   * 设置铃声
   */
  fun setRing(context: Context, audioId: Long) {
    try {
      val cv = ContentValues()
      cv.put(Audio.Media.IS_RINGTONE, true)
      cv.put(Audio.Media.IS_NOTIFICATION, false)
      cv.put(Audio.Media.IS_ALARM, false)
      cv.put(Audio.Media.IS_MUSIC, true)
      // 把需要设为铃声的歌曲更新铃声库
      if (context.contentResolver.update(
          Audio.Media.EXTERNAL_CONTENT_URI, cv,
          MediaStore.MediaColumns._ID + "=?", arrayOf(audioId.toString() + "")
        ) > 0
      ) {
        val newUri = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, audioId)
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri)
        MessageNotifier.show(R.string.set_ringtone_success)
      } else {
        MessageNotifier.show(R.string.set_ringtone_error)
      }
    } catch (e: Exception) {
      // 没有权限
      if (e is SecurityException) {
        MessageNotifier.show(R.string.please_give_write_settings_permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (!Settings.System.canWrite(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = ("package:" + context.packageName).toUri()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Util.startActivitySafely(context, intent)
          }
        }
      }
    }
  }
}
