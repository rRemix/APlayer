package remix.myplayer.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore.Audio
import dagger.hilt.android.EntryPointAccessors
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.nav.UiMessageDispatcher
import remix.myplayer.compose.repo.SongRepositoryEntryPoint
import remix.myplayer.service.MusicService
import java.io.File

object MusicUtil {

  private val songRepo = EntryPointAccessors.fromApplication(
    App.context,
    SongRepositoryEntryPoint::class.java
  ).songRepository()

  fun makeCmdIntent(cmd: Int, shuffle: Boolean): Intent {
    return Intent(MusicService.ACTION_CMD).putExtra(MusicService.Companion.EXTRA_CONTROL, cmd)
      .putExtra(MusicService.Companion.EXTRA_SHUFFLE, shuffle)
  }

  fun makeCmdIntent(cmd: Int): Intent {
    return makeCmdIntent(cmd, false)
  }

  fun playFromUri(context: Context, uri: Uri) {
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
        songs = songRepo.getSongs(Audio.Media.DATA + " = ?", arrayOf(songFile.absolutePath))
      }
    }
    if (!songs.isNullOrEmpty()) {
      context.startService(
        Intent(context, MusicService::class.java).run {
          action = MusicService.ACTION_PLAY_FROM_URI
          putExtra(
            MusicService.EXTRA_SONG,
            songs.first()
          )
        }
      )
//      service.startService()
//      setPlayQueue(
//        songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
//          .putExtra(MusicService.Companion.EXTRA_POSITION, 0)
//      )
    } else {
      UiMessageDispatcher.show(R.string.play_failed)
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
}