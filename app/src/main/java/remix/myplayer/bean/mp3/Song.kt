package remix.myplayer.bean.mp3

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import remix.myplayer.App
import remix.myplayer.util.SPUtil

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


  val showName: String?
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
//    if (duration <= 0) {
//      val ijkMediaPlayer = IjkMediaPlayer()
//      try {
//        val file = File(url)
//        if (!file.exists()) {
//          return duration
//        }
//        ijkMediaPlayer.dataSource = url
//        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
//        ijkMediaPlayer.prepareAsync()
//        Thread.sleep(20)
//        duration = ijkMediaPlayer.duration
//        Timber.v("duration: %s", duration)
//        if (duration > 0) {
//          val contentValues = ContentValues()
//          contentValues.put(MediaStore.Audio.Media.DURATION, duration)
//          val updateCount = App.getContext().contentResolver
//              .update(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                  contentValues, MediaStore.Audio.Media._ID + "=?", arrayOf(id.toString() + ""))
//          Timber.v("UpdateCount: %s", updateCount)
//        }
//      } catch (e: Exception) {
//        Timber.v(e)
//      }
//
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
