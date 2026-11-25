package remix.myplayer.data.model.audio

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri

/** An immutable library snapshot. Reading a property never accesses settings, files or the network. */
data class Song(
  val sourceId: SourceId,
  val sourceKey: String,
  val data: String,
  val displayName: String,
  val title: String = displayName,
  val album: String = "",
  val albumId: Long = 0,
  val artist: String = "",
  val artistId: Long = 0,
  val duration: Long = 0,
  val size: Long = 0,
  val year: String = "",
  val genre: String = "",
  val track: String? = null,
  /** Epoch milliseconds, including values mapped from MediaStore's seconds. */
  val dateModified: Long = 0,
  val addedAt: Long = 0,
  val bitRate: String = "",
  val sampleRate: String = ""
) : APlayerModel {
  val songIdentity: SongIdentity get() = SongIdentity(sourceId, sourceKey)
  val mediaStoreId: Long? get() = sourceKey.toLongOrNull().takeIf { isLocal() }

  val contentUri: Uri
    get() = if (isLocal()) ContentUris.withAppendedId(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, requireNotNull(mediaStoreId)
    ) else data.toUri()

  val artUri: Uri
    get() = ContentUris.withAppendedId("content://media/external/audio/albumart".toUri(), albumId)

  fun isLocal(): Boolean = sourceId == SourceId.MEDIASTORE
  fun isRemote(): Boolean = sourceId.isRemote
  fun valid(): Boolean = if (isLocal()) (mediaStoreId ?: -1) > 0 else isRemote() && sourceKey.isNotEmpty()
  fun showName(showDisplayName: Boolean): String = if (showDisplayName) displayName else title
  override fun getKey(): String = songIdentity.key

  companion object {
    @JvmStatic
    val EMPTY_SONG = Song(SourceId.MEDIASTORE, "-1", "", "")
  }
}
