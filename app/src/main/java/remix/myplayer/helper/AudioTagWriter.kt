package remix.myplayer.helper

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.kyant.taglib.Picture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object AudioTagWriter {

  data class PendingWriteRequest(
    val song: Song,
    val fieldMap: Map<String, String>,
    val deleteArtwork: Boolean = false,
    val newArtwork: Bitmap? = null
  )

  // Build the tag map and start the permission flow if needed.
  fun requestSaveAudioTag(
    activity: BaseActivity,
    song: Song,
    newTitle: String,
    newAlbum: String,
    newArtist: String,
    newAlbumArtist: String,
    newComposer: String,
    newGenre: String,
    newYear: String,
    newTrackNum: String,
    newDiscNum: String,
    newLyrics: String,
    deleteArtwork: Boolean = false,
    newArtwork: Bitmap? = null
  ) {
    Timber.v("requestSaveAudioTag, deleteArtwork: $deleteArtwork newArtwork: $newArtwork lyric: $newLyrics")
    val fieldMap = linkedMapOf(
      AudioTagFile.TITLE to newTitle,
      AudioTagFile.ALBUM to newAlbum,
      AudioTagFile.ARTIST to newArtist,
      AudioTagFile.ALBUM_ARTIST to newAlbumArtist,
      AudioTagFile.COMPOSER to newComposer,
      AudioTagFile.GENRE to newGenre,
      AudioTagFile.DATE to newYear,
      AudioTagFile.TRACK_NUMBER to newTrackNum,
      AudioTagFile.DISC_NUMBER to newDiscNum,
      AudioTagFile.LYRICS to newLyrics
    )

    val request = PendingWriteRequest(song, fieldMap, deleteArtwork, newArtwork)

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

          Timber.v("Fail to save tag: $e")
          MessageNotifier.show(R.string.save_error, e.toString())
        }
      }
    }
  }

  // Save tags on a worker thread and notify media store/UI.
  suspend fun saveAudioTag(
    context: Context,
    request: PendingWriteRequest,
    withFallback: Boolean = true
  ) =
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
    val intent = Intent(MusicService.TAG_CHANGE)
      .putExtra(BaseMusicActivity.EXTRA_OLD_SONG, request.song)
      .putExtra(
        BaseMusicActivity.EXTRA_NEW_SONG,
        request.song.copy(
          title = request.fieldMap[AudioTagFile.TITLE],
          album = request.fieldMap[AudioTagFile.ALBUM],
          artist = request.fieldMap[AudioTagFile.ARTIST],
          genre = request.fieldMap[AudioTagFile.GENRE],
          year = request.fieldMap[AudioTagFile.DATE],
          track = request.fieldMap[AudioTagFile.TRACK_NUMBER]
        )
      )
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
  }

  private fun writeAudioTag(context: Context, request: PendingWriteRequest, withFallback: Boolean) {
    val directResult = runCatching {
      updateAudioFile(File(request.song.data), request)
    }
    if (directResult.isSuccess) {
      return
    }
    if (!withFallback || !request.song.isLocal() || request.song.id <= 0) {
      throw directResult.exceptionOrNull()!!
    }

    Timber.v("Fallback to content uri write: ${request.song.data}")
    writeAudioTagByContentUri(context, request)
  }

  private fun updateAudioFile(file: File, request: PendingWriteRequest) {
    val propertyMap = AudioTagFile.readMetadata(file, readPictures = false)?.propertyMap
      ?: throw IOException("TagLib cannot read metadata from ${file.path}")
    request.fieldMap.forEach { (key, value) ->
      AudioTagFile.setValue(propertyMap, key, value)
    }
    AudioTagFile.requireSaved(AudioTagFile.savePropertyMap(file, propertyMap), "save metadata")

    if (request.newArtwork != null || request.deleteArtwork) {
      val pictures = request.newArtwork?.let { bitmap ->
        arrayOf(
          Picture(
            data = bitmapToByteArray(bitmap),
            description = "Front Cover",
            pictureType = "Front Cover",
            mimeType = "image/jpeg"
          )
        )
      } ?: emptyArray<Picture>()
      AudioTagFile.requireSaved(AudioTagFile.savePictures(file, pictures), "save artwork")
    }
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

      updateAudioFile(tempFile, request)

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

  private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    return stream.toByteArray()
  }
}
