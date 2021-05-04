package remix.myplayer.bean.mp3

import android.content.ContentUris
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
    val id: Long,
    val displayName: String,
    val title: String,
    val album: String,
    val albumId: Long,
    val artist: String,
    val artistId: Long,
    private var _duration: Long,
    val data: String,
    val size: Long,
    val year: String,
    private var _genre: String?,
    val track: String?,
    val dateModified: Long) : Parcelable, APlayerModel {

  val duration: Long
    get() {
      if (_duration <= 0 && id > 0 && data.isNotEmpty()) {
        val metadataRetriever = MediaMetadataRetriever()
        try {
          metadataRetriever.setDataSource(data)
          _duration =
            metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!
              .toLong()
        } catch (e: Exception) {
          Timber.v("Fail to get duration: $e")
        } finally {
          metadataRetriever.release()
        }
      }
      return _duration
    }

  val genre: String
    get() {
      if (_genre.isNullOrEmpty() && id > 0 && data.isNotEmpty()) {
        val metadataRetriever = MediaMetadataRetriever()
        try {
          metadataRetriever.setDataSource(data)
          _genre = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)!!
        } catch (e: Exception) {
          Timber.v("Fail to get genre: $e")
        } finally {
          metadataRetriever.release()
        }
      }
      return _genre ?: ""
    }

  val contentUri: Uri
    get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong())

  val artUri: Uri
    get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), albumId)

  val showName: String
    get() = if (!SHOW_DISPLAYNAME) title else displayName

  override fun getKey(): String {
    return albumId.toString()
  }

  companion object {
    @JvmStatic
    val EMPTY_SONG = Song(-1, "", "", "", -1, "", -1, -1, "", -1, "", "", "", -1)

    //所有列表是否显示文件名
    @JvmStatic
    var SHOW_DISPLAYNAME = SPUtil
        .getValue(App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME,
            false)
  }
}
