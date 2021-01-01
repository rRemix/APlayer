package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.content.ContentValues
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import kotlinx.android.parcel.Parcelize
import remix.myplayer.App
import remix.myplayer.util.SPUtil
import timber.log.Timber

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 歌曲信息
 */
@Parcelize
data class Song(
    val id: Int,
    val displayName: String,
    val title: String,
    val album: String,
    val albumId: Long,
    val artist: String,
    val artistId: Long,
    private var duration: Long,
    val realTime: String,
    val url: String,
    val size: Long,
    val year: String?,
    val titleKey: String?,
    val addTime: Long) : Parcelable {

  val contentUri: Uri
    get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())

  val showName: String
    get() = if (!SHOW_DISPLAYNAME) title else displayName

  override fun toString(): String {
    return "Song{" +
        "id=" + id +
        ", title='" + title + '\''.toString() +
        ", displayName='" + displayName + '\''.toString() +
        ", album='" + album + '\''.toString() +
        ", albumId=" + albumId +
        ", artist='" + artist + '\''.toString() +
        ", duration=" + duration +
        ", realTime='" + realTime + '\''.toString() +
        ", url='" + url + '\''.toString() +
        ", size=" + size +
        ", year=" + year +
        '}'.toString()
  }


  fun getDuration(): Long {
//    if (duration <= 0 && id > 0 && url.isNotEmpty()) {
//      val metadataRetriever = MediaMetadataRetriever()
//      try {
//        metadataRetriever.setDataSource(url)
//        duration = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
//        if (duration > 0) {
//          val contentValues = ContentValues()
//          contentValues.put(MediaStore.Audio.Media.DURATION, duration)
//          val updateCount = App.getContext().contentResolver.update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//              contentValues, MediaStore.Audio.Media._ID + "=?", arrayOf(id.toString() + ""))
//          Timber.tag("Song").v("updateDuration, dur: $duration  count: $updateCount")
//        }
//      } catch (e: Exception) {
//        Timber.tag("Song").v("updateDuration failed: $e")
//      } finally {
//        metadataRetriever.release()
//      }
//    }
    return duration
  }

  fun setDuration(duration: Long) {
    this.duration = duration
  }

  override fun hashCode(): Int {
    var result = id
    result = 31 * result + displayName.hashCode()
    result = 31 * result + title.hashCode()
    result = 31 * result + album.hashCode()
    result = 31 * result + albumId.hashCode()
    result = 31 * result + artist.hashCode()
    result = 31 * result + artistId.hashCode()
    result = 31 * result + duration.hashCode()
    result = 31 * result + realTime.hashCode()
    result = 31 * result + url.hashCode()
    result = 31 * result + size.hashCode()
    result = 31 * result + year.hashCode()
    result = 31 * result + titleKey.hashCode()
    result = 31 * result + addTime.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Song) return false

    if (id != other.id) return false
    if (displayName != other.displayName) return false
    if (title != other.title) return false
    if (album != other.album) return false
    if (albumId != other.albumId) return false
    if (artist != other.artist) return false
    if (artistId != other.artistId) return false
    if (duration != other.duration) return false
    if (realTime != other.realTime) return false
    if (url != other.url) return false
    if (size != other.size) return false
    if (year != other.year) return false
    if (titleKey != other.titleKey) return false
    if (addTime != other.addTime) return false

    return true
  }


  companion object {
    @JvmStatic
    val EMPTY_SONG = Song(-1, "", "", "", -1, "", -1, -1, "", "", -1, "", "", -1)

    //所有列表是否显示文件名
    @JvmStatic
    var SHOW_DISPLAYNAME = SPUtil
        .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME,
            false)
  }
}
