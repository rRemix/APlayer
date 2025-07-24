package remix.myplayer.bean.mp3

import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import okhttp3.Credentials
import remix.myplayer.App
import remix.myplayer.util.SPUtil
import timber.log.Timber
import java.io.Serial
import androidx.core.net.toUri

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

  private var triedDuration = false
  val duration: Long
    get() {
      if (!triedDuration && _duration <= 0 && data.isNotEmpty() && this !is Remote) {
        triedDuration = true
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

  private var triedGenre = false
  val genre: String
    get() {
      if (!triedGenre && _genre.isNullOrEmpty() && id > 0 && data.isNotEmpty() && this !is Remote) {
        triedGenre = true
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
      data.toUri()
    }

  val artUri: Uri
    get() = ContentUris.withAppendedId("content://media/external/audio/albumart/".toUri(), albumId)

  val showName: String
    get() = if (!SHOW_DISPLAYNAME) title else displayName

  override fun getKey(): String {
    return id.toString()
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
        Remote(title, album, artist, _duration, data, size, year, _genre ?: "", track, dateModified, account, pwd)
      }

    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Song) return false

    if (id != other.id) return false
    if (albumId != other.albumId) return false
    if (artistId != other.artistId) return false
    if (dateModified != other.dateModified) return false
    if (title != other.title) return false
    if (album != other.album) return false
    if (artist != other.artist) return false
    if (data != other.data) return false
    if (year != other.year) return false
    if (track != other.track) return false
    if (_duration != other._duration) return false
    if (_genre != other._genre) return false

    return true
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + albumId.hashCode()
    result = 31 * result + artistId.hashCode()
    result = 31 * result + dateModified.hashCode()
    result = 31 * result + title.hashCode()
    result = 31 * result + album.hashCode()
    result = 31 * result + artist.hashCode()
    result = 31 * result + data.hashCode()
    result = 31 * result + year.hashCode()
    result = 31 * result + (track?.hashCode() ?: 0)
    result = 31 * result + _duration.hashCode()
    result = 31 * result + _genre.hashCode()
    return result
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
    override fun toString(): String {
      return "LocalSong(id='$id', data='$data')"
    }

    companion object {
      @Serial
      private val serialVersionUID: Long = -6482253600425978196
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

      return data == other.data
    }

    override fun hashCode(): Int {
      return data.hashCode()
    }

    override fun toString(): String {
      return "RemoteSong(data='$data')"
    }

    companion object {
      @Serial
      private const val serialVersionUID: Long = 13111653236476560L
    }
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 6842734564265523984

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
