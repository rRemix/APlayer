package remix.myplayer.repo.usecase

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore.Audio
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.misc.MediaScanner
import remix.myplayer.repo.SongRepository
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayFromUriUseCase @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val songRepo: SongRepository
) {

  suspend operator fun invoke(uri: Uri) = withContext(Dispatchers.IO) {
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
        val path = getFilePathFromUri(uri)
        if (path != null) {
          songFile = File(path)
        }
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

  private fun getFilePathFromUri(uri: Uri): String? {
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
