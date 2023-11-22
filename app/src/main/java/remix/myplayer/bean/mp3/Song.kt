package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import okhttp3.Credentials
import remix.myplayer.App
import remix.myplayer.util.SPUtil
import timber.log.Timber

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 歌曲信息
 */
sealed class Song(
  val id: Long,
  var displayName: String,
  var title: String,
  var album: String,
  var albumId: Long,
  var artist: String,
  var artistId: Long,
  private var _duration: Long,
  var data: String,
  var size: Long,
  var year: String,
  private var _genre: String?,
  var track: String?,
  var dateModified: Long
) : APlayerModel {

  fun isLocal(): Boolean {
    return this is Local
  }

  fun isRemote(): Boolean {
    return this is Remote
  }

  fun updateMetaData(
    title: String,
    album: String,
    artist: String,
    duration: Long,
    year: String,
    genre: String,
    track: String,
    dateModified: Long
  ) {
    this.title = title
    this.album = album
    this.artist = artist
    this._duration = duration
    this.year = year
    this._genre = genre
    this.track = track
    this.dateModified = dateModified
  }

  val duration: Long
    get() {
      if (_duration <= 0 && data.isNotEmpty() && this !is Remote) {
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
    get() = if (isLocal()) {
      ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    } else {
      Uri.parse(data)
    }

  val artUri: Uri
    get() = ContentUris.withAppendedId(
      Uri.parse("content://media/external/audio/albumart/"),
      albumId
    )

  val showName: String
    get() = if (!SHOW_DISPLAYNAME) title else displayName

  override fun getKey(): String {
    return albumId.toString()
  }

  fun copy(): Song {
    return when (this) {
      is Local -> {
        Local(
          id,
          displayName,
          title,
          album,
          albumId,
          artist,
          artistId,
          _duration,
          data,
          size,
          year,
          _genre,
          track,
          dateModified
        )
      }

      is Remote -> {
        Remote(title, album, artist, duration, data, size, year, genre, track, dateModified, account, pwd)
      }

    }
  }

  class Local(
    id: Long,
    displayName: String,
    title: String,
    album: String,
    albumId: Long,
    artist: String,
    artistId: Long,
    _duration: Long,
    data: String,
    size: Long,
    year: String,
    _genre: String?,
    track: String?,
    dateModified: Long
  ) : Song(
    id,
    displayName,
    title,
    album,
    albumId,
    artist,
    artistId,
    _duration,
    data,
    size,
    year,
    _genre,
    track,
    dateModified
  ) {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Song

      if (id != other.id) return false

      return true
    }

    override fun hashCode(): Int {
      return id.hashCode()
    }

    override fun toString(): String {
      return "LocalSong(id='$id', data='$data')"
    }
  }

  class Remote(
    title: String,
    album: String,
    artist: String,
    duration: Long,
    data: String,
    size: Long,
    year: String,
    genre: String,
    track: String?,
    dateModified: Long,
    val account: String,
    val pwd: String
  ) : Song(
    data.hashCode().toLong(), title, title, album, 0L, artist, 0L, duration, data, size, year, genre, track, dateModified
  ) {
    val headers by lazy {
      mapOf(
        "Authorization" to Credentials.basic(account, pwd)
      )
    }
    var bitRate: String = ""
    var sampleRate: String = ""

    constructor(title: String, data: String, account: String, pwd: String): this(title, data, 0L, 0L, account, pwd)

    constructor(title: String, data: String, size: Long, dateModified: Long, account: String, pwd: String) : this(
      title,
      "",
      "",
      0L,
      data,
      size,
      "",
      "",
      "",
      dateModified,
      account,
      pwd
    )

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Song

      if (data != other.data) return false

      return true
    }

    override fun hashCode(): Int {
      return data.hashCode()
    }

    override fun toString(): String {
      return "RemoteSong(data='$data')"
    }
  }

  companion object {
    @JvmStatic
    val EMPTY_SONG = Local(-1, "", "", "", -1, "", -1, -1, "", -1, "", "", "", -1)

    //所有列表是否显示文件名
    @JvmStatic
    var SHOW_DISPLAYNAME = SPUtil
      .getValue(
        App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME,
        false
      )
  }
}
