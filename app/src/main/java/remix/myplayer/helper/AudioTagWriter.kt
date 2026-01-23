package remix.myplayer.helper

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.UnableToCreateFileException
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.EnumMap

object AudioTagWriter {

  data class PendingWriteRequest(
    val song: Song,
    val fieldMap: EnumMap<FieldKey, String>
  )

  // Build the tag map and start the permission flow if needed.
  fun requestSaveAudioTag(
    activity: BaseActivity,
    song: Song,
    newTitle: String,
    newAlbum: String,
    newArtist: String,
    newGenre: String,
    newYear: String,
    newTrackNum: String,
    newLyrics: String
  ) {
    val fieldMap = EnumMap<FieldKey, String>(FieldKey::class.java)
    fieldMap[FieldKey.TITLE] = newTitle
    fieldMap[FieldKey.ALBUM] = newAlbum
    fieldMap[FieldKey.ARTIST] = newArtist
    fieldMap[FieldKey.GENRE] = newGenre
    fieldMap[FieldKey.YEAR] = newYear
    fieldMap[FieldKey.TRACK] = newTrackNum
    fieldMap[FieldKey.LYRICS] = newLyrics

    val request = PendingWriteRequest(song, fieldMap)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      // Scoped storage: ask for write access to the specific file.
      activity.pendingWriteRequest = request
      activity.writeSongLauncher.launch(
        IntentSenderRequest.Builder(
          MediaStore.createWriteRequest(
            activity.contentResolver,
            listOf(song.contentUri)
          ).intentSender
        ).build()
      )
    } else {
      // Pre-R: try direct write and fall back to recoverable permission when needed.
      activity.lifecycleScope.launch {
        try {
          saveAudioTag(activity, request, false)
        } catch (e: Exception) {
          try {
            val songFD =
              activity.contentResolver.openFileDescriptor(
                song.contentUri,
                "w"
              )!! // test if we can write
            songFD.close()
          } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
              activity.pendingWriteRequest = request
              activity.writeSongLauncher.launch(
                IntentSenderRequest.Builder(
                  securityException.userAction.actionIntent.intentSender,
                ).build()
              )
              return@launch
            }

          }

          Timber.Forest.v("Fail to save tag: $e")
          MessageNotifier.show(R.string.save_error, e.toString())
        }
      }
    }
  }

  // Save tags on a worker thread and notify media store/UI.
  suspend fun saveAudioTag(context: Context, request: PendingWriteRequest, withFallback: Boolean = true) =
    withContext(Dispatchers.IO) {
      writeAudioTag(context, request, withFallback)
      MediaScannerConnection.scanFile(
        context,
        arrayOf(request.song.data), null
      ) { _, uri ->
        val notifyUri = uri ?: request.song.contentUri
        context.contentResolver.notifyChange(notifyUri, null)
        sendTagChangedBroadcast(context, request)
      }

      withContext(Dispatchers.Main) {
        MessageNotifier.show(R.string.save_success)
      }
    }

  private fun sendTagChangedBroadcast(context: Context, request: PendingWriteRequest) {
    val intent = Intent(MusicService.Companion.TAG_CHANGE)
      .putExtra(BaseMusicActivity.Companion.EXTRA_OLD_SONG, request.song)
      .putExtra(
        BaseMusicActivity.Companion.EXTRA_NEW_SONG,
        request.song.copy(
          title = request.fieldMap[FieldKey.TITLE],
          album = request.fieldMap[FieldKey.ALBUM],
          artist = request.fieldMap[FieldKey.ARTIST],
          genre = request.fieldMap[FieldKey.GENRE],
          year = request.fieldMap[FieldKey.YEAR],
          track = request.fieldMap[FieldKey.TRACK]
        )
      )
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
  }

  private fun applyTagFields(tag: Tag, fieldMap: EnumMap<FieldKey, String>) {
    for ((key, value) in fieldMap) {
      try {
        tag.setField(key, value)
      } catch (e: Exception) {
        Timber.Forest.v("setField($key, $value) failed: $e")
      }
    }
  }

  private fun writeAudioTag(context: Context, request: PendingWriteRequest, withFallback: Boolean) {
    val audioFile = AudioFileIO.read(File(request.song.data))
    val tag = audioFile.tagOrCreateAndSetDefault
    applyTagFields(tag, request.fieldMap)

    try {
      audioFile.commit()
      return
    } catch (e: CannotWriteException) {
      if (!withFallback || !shouldFallbackToContentUri(e, request)) {
        throw e
      }
    }

    Timber.Forest.v("Fallback to content uri write: ${request.song.data}")
    writeAudioTagByContentUri(context, request)
  }

  private fun shouldFallbackToContentUri(
    error: CannotWriteException,
    request: PendingWriteRequest
  ): Boolean {
    if (!request.song.isLocal() || request.song.id <= 0) {
      return false
    }
    if (error.cause is UnableToCreateFileException) {
      return true
    }
    val message = error.message ?: ""
    return message.contains("UnableToCreateFileException") ||
        message.contains("do not have permissions to create files")
  }

  private fun writeAudioTagByContentUri(context: Context, request: PendingWriteRequest) {
    val extension = request.song.data.substringAfterLast('.', "")
    val suffix = if (extension.isNotEmpty()) ".${extension}" else ".mp3"
    val tempFile = File.createTempFile("tag_edit_", suffix, context.cacheDir)
    try {
      // Copy the media data to a writable temp file.
      context.contentResolver.openInputStream(request.song.contentUri)?.use { input ->
        tempFile.outputStream().use { output ->
          input.copyTo(output)
        }
      } ?: throw IOException("Unable to open input stream for ${request.song.contentUri}")

      // Update tags on the temp file.
      val audioFile = AudioFileIO.read(tempFile)
      val tag = audioFile.tagOrCreateAndSetDefault
      applyTagFields(tag, request.fieldMap)
      audioFile.commit()

      // Overwrite the original file through contentUri.
      context.contentResolver.openFileDescriptor(request.song.contentUri, "rw")?.use { pfd ->
        FileInputStream(tempFile).use { input ->
          FileOutputStream(pfd.fileDescriptor).use { output ->
            output.channel.truncate(0)
            input.copyTo(output)
          }
        }
      } ?: throw IOException("Unable to open file descriptor for ${request.song.contentUri}")
    } finally {
      tempFile.delete()
    }
  }
}