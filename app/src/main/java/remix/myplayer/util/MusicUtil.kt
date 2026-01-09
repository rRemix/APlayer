package remix.myplayer.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.Song
import remix.myplayer.misc.MediaScanner
import remix.myplayer.repo.SongRepositoryEntryPoint
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.File

object MusicUtil {

  private val songRepo = EntryPointAccessors.fromApplication(
    App.context,
    SongRepositoryEntryPoint::class.java
  ).songRepository()

  fun makeCmdIntent(cmd: Int, shuffle: Boolean): Intent {
    return Intent(MusicService.ACTION_CMD).putExtra(MusicService.EXTRA_COMMAND, cmd)
      .putExtra(MusicService.EXTRA_SHUFFLE, shuffle)
  }

  fun makeCmdIntent(cmd: Int): Intent {
    return makeCmdIntent(cmd, false)
  }

  suspend fun playFromUri(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
    Timber.v("playFromUri, uri: $uri")
    var songs: List<Song>? = null
    if (uri.scheme != null && uri.authority != null) {
      when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> {
          var songId: String? = null
          if (uri.authority == "com.android.providers.media.documents") {
            songId = getSongIdFromMediaProvider(uri)
          } else if (uri.authority == "media") {
            songId = uri.lastPathSegment
          }

          songs = songId?.toLongOrNull()
            ?.let { songRepo.song(it) }
            ?.let { listOf(it) }
        }

        else -> {
          // 通过displayName查找
          val displayName = uri.lastPathSegment
          if (!displayName.isNullOrEmpty()) {
            songs = songRepo.getSongs("${Audio.Media.DISPLAY_NAME} LIKE ?", arrayOf(displayName))
          }
        }
      }
    }
    if (songs.isNullOrEmpty()) {
      var songFile: File? = null
      if (uri.authority != null && uri.authority == "com.android.externalstorage.documents") {
        val path = uri.path?.split(":".toRegex(), 2)?.get(1)
        if (path != null) {
          songFile = File(getExternalStorageDirectory(), path)
        }
      }
      if (songFile == null) {
        val path = getFilePathFromUri(context, uri)
        if (path != null)
          songFile = File(path)
      }
      if (songFile == null && uri.path != null) {
        songFile = File(uri.path!!)
      }
      if (songFile != null) {
        Timber.v("playFromUri, songFile: $songFile")
        songs = songRepo.getSongs(Audio.Media.DATA + " = ?", arrayOf(songFile.absolutePath))
        if (songs.isEmpty()) {
          // 有可能是刚添加的歌曲，扫描一次再查询
          withTimeout(2_000) {
            MediaScanner(context).scanSingleFile(context, songFile)
          }?.let {
            context.contentResolver.notifyChange(it, null)
            Timber.v("playFromUri scanUri: $it")
          }

          songs = songRepo.getSongs(Audio.Media.DATA + " = ?", arrayOf(songFile.absolutePath))
        }
      }
    }
    Timber.v("playFromUri songs: $songs")
    if (!songs.isNullOrEmpty()) {
      context.startService(
        Intent(context, MusicService::class.java).run {
          action = MusicService.ACTION_CMD
          putExtra(MusicService.EXTRA_COMMAND, Command.PLAY_TEMP)
          putExtra(
            MusicService.EXTRA_SONG,
            songs.first()
          )
        }
      )
    } else {
      MessageNotifier.show(R.string.play_failed, "")
    }
  }

  private fun getFilePathFromUri(context: Context, uri: Uri): String? {
    var cursor: Cursor? = null
    val column = "_data"
    val projection = arrayOf(column)

    try {
      cursor = context.contentResolver.query(uri, projection, null, null, null)
      if (cursor != null && cursor.moveToFirst()) {
        val columnIndex = cursor.getColumnIndexOrThrow(column)
        return cursor.getString(columnIndex)
      }
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      cursor?.close()
    }
    return null
  }

  private fun getSongIdFromMediaProvider(uri: Uri): String {
    return DocumentsContract.getDocumentId(uri).split(":".toRegex())
      .dropLastWhile { it.isEmpty() }.toTypedArray()[1]
  }

  @Suppress("Deprecation")
  private fun getExternalStorageDirectory(): File {
    return Environment.getExternalStorageDirectory()
  }

  /**
   * 导出播放列表
   */
  suspend fun exportPlayListToFile(context: Context, playList: PlayList?, uri: Uri) =
    withContext(Dispatchers.IO) {
      if (playList == null) {
        return@withContext
      }

      val header = "#EXTM3U"
      val entry = "#EXTINF:"
      val sep = ","

      val songs = songRepo.getSongsByModels(listOf(playList))
      try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { bw ->
          if (bw == null) throw IllegalStateException("openOutputStream failed")
          bw.write(header)
          for (song in songs) {
            bw.newLine()
            bw.write(entry + song.duration + sep + song.artist + " - " + song.title)
            bw.newLine()
            bw.write(song.data)
          }
        }
        MessageNotifier.show(R.string.export_success)
      } catch (e: Exception) {
        MessageNotifier.show(R.string.export_fail, e.toString())
      }
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