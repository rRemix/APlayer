package remix.myplayer.util

import android.annotation.SuppressLint
import android.content.*
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.Settings
import android.text.TextUtils
import androidx.annotation.WorkerThread
import com.facebook.common.util.ByteConstants
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotReadException
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.tag.images.ArtworkFactory
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.bean.mp3.Song.Companion.EMPTY_SONG
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.helper.MusicServiceRemote.deleteFromService
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.SPUtil.SETTING_KEY
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Created by taeja on 16-2-17.
 */
/**
 * 数据库工具类
 */
object MediaStoreUtil {
  private const val TAG = "MediaStoreUtil"

  @SuppressLint("StaticFieldLeak")
  private val mContext: Context = App.getContext()

  //扫描文件默认大小设置
  var SCAN_SIZE = 0
  private val BASE_PROJECTION = arrayOf(
      BaseColumns._ID,
      AudioColumns.TITLE,
      AudioColumns.TITLE_KEY,
      AudioColumns.DISPLAY_NAME,
      AudioColumns.TRACK,
      AudioColumns.SIZE,
      AudioColumns.YEAR,
      AudioColumns.DURATION,
      AudioColumns.DATE_ADDED,
      AudioColumns.DATA,
      AudioColumns.ALBUM_ID,
      AudioColumns.ALBUM,
      AudioColumns.ARTIST_ID,
      AudioColumns.ARTIST)

  @JvmStatic
  fun getAllArtist(): List<Artist> {
    if (!Util.hasStoragePermissions()) {
      return ArrayList()
    }
    val artistMaps: MutableMap<Long, MutableList<Artist>> = LinkedHashMap()
    val artists: MutableList<Artist> = ArrayList()
    try {
      mContext.contentResolver
          .query(Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(Audio.Media.ARTIST_ID,
              Audio.Media.ARTIST),
              baseSelection,
              baseSelectionArgs,
              SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.ARTIST_SORT_ORDER,
                  SortOrder.ArtistSortOrder.ARTIST_A_Z)).use { cursor ->
            if (cursor != null) {
              while (cursor.moveToNext()) {
                try {
                  val artistId = cursor.getLong(0)
                  if (artistMaps[artistId] == null) {
                    artistMaps[artistId] = ArrayList()
                  }
                  artistMaps[artistId]?.add(Artist(artistId, cursor.getString(1), 0))
                } catch (ignored: Exception) {
                }
              }
              for ((_, value) in artistMaps) {
                try {
                  val artist = value[0]
                  artist.count = value.size
                  artists.add(artist)
                } catch (e: Exception) {
                  Timber.v("addArtist failed: $e")
                }
              }
            }
          }
    } catch (e: Exception) {
      Timber.v("getAllArtist failed: $e")
    }
    return artists
  }

  @JvmStatic
  fun getAllAlbum(): List<Album> {
    if (!Util.hasStoragePermissions()) {
      return ArrayList()
    }
    val albumMaps: MutableMap<Long, MutableList<Album>> = LinkedHashMap()
    val albums: MutableList<Album> = ArrayList()
    try {
      mContext.contentResolver
          .query(Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(Audio.Media.ALBUM_ID,
              Audio.Media.ALBUM,
              Audio.Media.ARTIST_ID,
              Audio.Media.ARTIST),
              baseSelection,
              baseSelectionArgs,
              SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.ALBUM_SORT_ORDER,
                  SortOrder.AlbumSortOrder.ALBUM_A_Z)).use { cursor ->
            if (cursor != null) {
              while (cursor.moveToNext()) {
                try {
                  val albumId = cursor.getLong(0)
                  if (albumMaps[albumId] == null) {
                    albumMaps[albumId] = ArrayList()
                  }
                  albumMaps[albumId]?.add(Album(albumId,
                      cursor.getString(1),
                      cursor.getLong(2),
                      cursor.getString(3),
                      0))
                } catch (ignored: Exception) {
                }
              }
              for ((_, value) in albumMaps) {
                try {
                  val album = value[0]
                  album.count = value.size
                  albums.add(album)
                } catch (e: Exception) {
                  Timber.v("addAlbum failed: $e")
                }
              }
            }
          }
    } catch (e: Exception) {
      Timber.v("getAllAlbum failed: $e")
    }
    return albums
  }

  @JvmStatic
  fun getAllSong(): List<Song> {
    return getSongs(null,
        null,
        SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SONG_SORT_ORDER,
            SortOrder.SongSortOrder.SONG_A_Z))
  }

  @JvmStatic
  fun getLastAddedSong(): List<Song> {
    val today = Calendar.getInstance()
    today.time = Date()
    return getSongs(Audio.Media.DATE_ADDED + " >= ?", arrayOf((today.timeInMillis / 1000 - 3600 * 24 * 7).toString()),
        Audio.Media.DATE_ADDED)
  }

  /**
   * 获得所有歌曲id
   */
  val allSongsId: List<Int>
    get() = getSongIds(
        null,
        null,
        SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SONG_SORT_ORDER,
            SortOrder.SongSortOrder.SONG_A_Z))

  @JvmStatic
  fun getAllFolder(): List<Folder> {
    val folders: MutableList<Folder> = ArrayList()
    if (!Util.hasStoragePermissions()) {
      return folders
    }
    val folderMap: MutableMap<Int, MutableList<String>?> = LinkedHashMap()
    val baseSelection = baseSelection
    val selection = if (!TextUtils.isEmpty(baseSelection)) "$baseSelection and media_type = 2" else "media_type = 2"
    try {
      mContext.contentResolver
          .query(MediaStore.Files.getContentUri("external"),
              null, selection, baseSelectionArgs, null)
          .use { cursor ->
            if (cursor != null) {
              while (cursor.moveToNext()) {
                val data = cursor
                    .getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA))
                val parentId = cursor
                    .getInt(cursor.getColumnIndex(MediaStore.Files.FileColumns.PARENT))
                val parentPath = data.substring(0, data.lastIndexOf("/"))
                if (!folderMap.containsKey(parentId)) {
                  folderMap[parentId] = ArrayList(listOf(parentPath))
                } else {
                  folderMap[parentId]?.add(parentPath)
                }
              }

              //转换
              for ((key, value) in folderMap) {
                val parentPath = value?.get(0) ?: ""
                val folder = Folder(
                    parentPath.substring(parentPath.lastIndexOf("/") + 1),
                    folderMap[key]?.size ?: 0,
                    parentPath,
                    key.toLong())
                folders.add(folder)
              }
            }
          }
    } catch (e: Exception) {
      Timber.v(e)
    }
    return folders
  }

  /**
   * 根据歌手或者专辑id获取所有歌曲
   *
   * @param id 歌手id 专辑id
   * @param type 1:专辑  2:歌手
   * @return 对应所有歌曲的id
   */
  @JvmStatic
  fun getSongsByArtistIdOrAlbumId(id: Long, type: Int): List<Song> {
    var selection: String? = null
    var sortOrder: String? = null
    var selectionValues: Array<String?>? = null
    if (type == Constants.ALBUM) {
      selection = Audio.Media.ALBUM_ID + "=?"
      selectionValues = arrayOf(id.toString() + "")
      sortOrder = SPUtil.getValue(mContext, SETTING_KEY.NAME,
          SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z)
    }
    if (type == Constants.ARTIST) {
      selection = Audio.Media.ARTIST_ID + "=?"
      selectionValues = arrayOf(id.toString() + "")
      sortOrder = SPUtil.getValue(mContext, SETTING_KEY.NAME,
          SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
          SortOrder.ChildHolderSongSortOrder.SONG_A_Z)
    }
    return getSongs(selection, selectionValues, sortOrder)
  }

  /**
   * 根据记录集获得歌曲详细
   *
   * @param cursor 记录集
   * @return 拼装后的歌曲信息
   */
  @JvmStatic
  @WorkerThread
  fun getSongInfo(cursor: Cursor?): Song {
    if (cursor == null || cursor.columnCount <= 0) {
      return EMPTY_SONG
    }
    val duration = cursor.getLong(cursor.getColumnIndex(Audio.Media.DURATION))
    val data = cursor.getString(cursor.getColumnIndex(Audio.Media.DATA))
    val id = cursor.getInt(cursor.getColumnIndex(Audio.Media._ID))
    return Song(
        id,
        Util.processInfo(cursor.getString(cursor.getColumnIndex(Audio.Media.DISPLAY_NAME)),
            Util.TYPE_DISPLAYNAME),
        Util.processInfo(cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE)),
            Util.TYPE_SONG),
        Util.processInfo(cursor.getString(cursor.getColumnIndex(Audio.Media.ALBUM)),
            Util.TYPE_ALBUM),
        cursor.getLong(cursor.getColumnIndex(Audio.Media.ALBUM_ID)),
        Util.processInfo(cursor.getString(cursor.getColumnIndex(Audio.Media.ARTIST)),
            Util.TYPE_ARTIST),
        cursor.getLong(cursor.getColumnIndex(Audio.Media.ARTIST_ID)),
        duration,
        Util.getTime(duration),
        data,
        cursor.getLong(cursor.getColumnIndex(Audio.Media.SIZE)),
        cursor.getString(cursor.getColumnIndex(Audio.Media.YEAR)),
        cursor.getString(cursor.getColumnIndex(Audio.Media.TITLE_KEY)),
        cursor.getLong(cursor.getColumnIndex(Audio.Media.DATE_ADDED)))
  }

  fun getSongIdsByParentId(parentId: Long): List<Int> {
    val ids: MutableList<Int> = ArrayList()
    mContext.contentResolver
        .query(MediaStore.Files.getContentUri("external"), arrayOf("_id"), "parent = $parentId", null, null).use { cursor ->
          if (cursor != null) {
            while (cursor.moveToNext()) {
              ids.add(cursor.getInt(0))
            }
          }
        }
    return ids
  }

  /**
   * 根据文件夹名字
   */
  @JvmStatic
  fun getSongsByParentId(parentId: Long): List<Song> {
    val ids = getSongIdsByParentId(parentId)
    if (ids.isEmpty()) {
      return ArrayList()
    }
    val selection = StringBuilder(127)
    selection.append(Audio.Media._ID + " in (")
    for (i in ids.indices) {
      selection.append(ids[i]).append(if (i == ids.size - 1) ") " else ",")
    }
    return getSongs(selection.toString(), null, SPUtil.getValue(mContext, SETTING_KEY.NAME,
        SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
        SortOrder.ChildHolderSongSortOrder.SONG_A_Z))
  }

  /**
   * 根据专辑id查询歌曲详细信息
   *
   * @param albumId 歌曲id
   * @return 对应歌曲信息
   */
  fun getSongByAlbumId(albumId: Long): Song {
    return getSong(Audio.Media.ALBUM_ID + "=?", arrayOf(albumId.toString() + ""))
  }

  /**
   * 根据歌曲id查询歌曲详细信息
   *
   * @param id 歌曲id
   * @return 对应歌曲信息
   */
  @JvmStatic
  fun getSongById(id: Int): Song {
    return getSong(Audio.Media._ID + "=?", arrayOf(id.toString() + ""))
  }

  fun getSongsByIds(ids: List<Int?>?): List<Song> {
    val songs: List<Song> = ArrayList()
    if (ids == null || ids.isEmpty()) {
      return songs
    }
    val selection = StringBuilder(127)
    selection.append(Audio.Media._ID + " in (")
    for (i in ids.indices) {
      selection.append(ids[i]).append(if (i == ids.size - 1) ") " else ",")
    }
    return getSongs(selection.toString(), null)
  }

  private fun insertAlbumArt(context: Context, albumId: Long, path: String) {
    val contentResolver = context.contentResolver
    val artworkUri = Uri.parse("content://media/external/audio/albumart")
    contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null)
    val values = ContentValues()
    values.put("album_id", albumId)
    values.put("_data", path)
    contentResolver.insert(artworkUri, values)
  }

  private fun deleteAlbumArt(context: Context, albumId: Int) {
    val contentResolver = context.contentResolver
    val localUri = Uri.parse("content://media/external/audio/albumart")
    contentResolver.delete(ContentUris.withAppendedId(localUri, albumId.toLong()), null, null)
  }

  @WorkerThread
  @Throws(TagException::class, ReadOnlyFileException::class, CannotReadException::class, InvalidAudioFrameException::class, IOException::class, CannotWriteException::class)
  fun saveArtwork(context: Context, albumId: Long, artFile: File) {
    val song = getSongByAlbumId(albumId)
    val audioFile = AudioFileIO.read(File(song.url))
    val tag = audioFile.tagOrCreateAndSetDefault
    val artwork = ArtworkFactory.createArtworkFromFile(artFile)
    tag.deleteArtworkField()
    tag.setField(artwork)
    audioFile.commit()
    insertAlbumArt(context, albumId, artFile.absolutePath)
  }

  /**
   * 删除歌曲
   *
   * @param data 删除参数 包括歌曲id、专辑id、艺术家id、播放列表id、parentId
   * @param type 删除类型 包括单个歌曲、专辑、艺术家、文件夹、播放列表
   * @return 是否歌曲数量
   */
  @Deprecated("")
  fun delete(data: Int, type: Int, deleteSource: Boolean): Int {
    var where: String? = null
    var arg: Array<String?>? = null
    when (type) {
      Constants.SONG -> {
        where = Audio.Media._ID + "=?"
        arg = arrayOf(data.toString() + "")
      }
      Constants.ALBUM, Constants.ARTIST -> if (type == Constants.ALBUM) {
        where = Audio.Media.ALBUM_ID + "=?"
        arg = arrayOf(data.toString() + "")
      } else {
        where = Audio.Media.ARTIST_ID + "=?"
        arg = arrayOf(data.toString() + "")
      }
      Constants.FOLDER -> {
        val ids = getSongIdsByParentId(data.toLong())
        val selection = StringBuilder(127)
        //                for(int i = 0 ; i < ids.size();i++){
//                    selection.append(MediaStore.Audio.Media._ID).append(" = ").append(ids.get(i)).append(i != ids.size() - 1 ? " or " : " ");
//                }
        selection.append(Audio.Media._ID + " in (")
        var i = 0
        while (i < ids.size) {
          selection.append(ids[i]).append(if (i == ids.size - 1) ") " else ",")
          i++
        }
        where = selection.toString()
        arg = null
      }
    }
    return delete(getSongs(where, arg), deleteSource)
  }

  /**
   * 删除指定歌曲
   */
  @WorkerThread
  fun delete(songs: List<Song>?, deleteSource: Boolean): Int {
    //保存是否删除源文件
    SPUtil.putValue(App.getContext(), SETTING_KEY.NAME, SETTING_KEY.DELETE_SOURCE,
        deleteSource)
    if (songs == null || songs.isEmpty()) {
      return 0
    }

    //删除之前保存的所有移除歌曲id
    val deleteId: MutableSet<String> = HashSet(
        SPUtil.getStringSet(mContext, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG))
    //保存到sp
    for (temp in songs) {
      deleteId.add(temp.id.toString() + "")
    }
    SPUtil.putStringSet(mContext, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG,
        deleteId)
    //从播放队列和全部歌曲移除
    deleteFromService(songs)
    getInstance().deleteFromAllPlayList(songs).subscribe()

    //删除源文件
    if (deleteSource) {
      deleteSource(songs)
    }

    //刷新界面
    mContext.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
    return songs.size
  }

  /**
   * 删除源文件
   */
  fun deleteSource(songs: List<Song>?) {
    if (songs == null || songs.isEmpty()) {
      return
    }
    for (song in songs) {
      mContext.contentResolver.delete(Audio.Media.EXTERNAL_CONTENT_URI,
          Audio.Media._ID + "=?", arrayOf(song.toString() + ""))
      Util.deleteFileSafely(File(song.url))
    }
  }

  /**
   * 过滤移出的歌曲以及铃声等
   */
  @JvmStatic
  val baseSelection: String
    get() {
      val deleteIds = SPUtil
          .getStringSet(mContext, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG)
      val blacklist = SPUtil
          .getStringSet(mContext, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST)
      val baseSelection = " _data != '' AND " + Audio.Media.SIZE + " > " + SCAN_SIZE
      if (deleteIds.isEmpty() && blacklist.isEmpty()) {
        return baseSelection
      }
      val builder = StringBuilder(baseSelection)
      var i = 0
      if (!deleteIds.isEmpty()) {
        builder.append(" AND ")
        for (id in deleteIds) {
          if (i == 0) {
            builder.append(Audio.Media._ID).append(" not in (")
          }
          builder.append(id)
          builder.append(if (i != deleteIds.size - 1) "," else ")")
          i++
        }
      }
      if (!blacklist.isEmpty()) {
        builder.append(" AND ")
        i = 0
        for (path in blacklist) {
          builder.append(Audio.Media.DATA + " NOT LIKE ").append(" ? ")
          builder.append(if (i != blacklist.size - 1) " AND " else "")
          i++
        }
      }
      return builder.toString()
    }

  val baseSelectionArgs: Array<String?>
    get() {
      val blacklist = SPUtil
          .getStringSet(mContext, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST)
      val selectionArgs = arrayOfNulls<String>(blacklist.size)
      val iterator: Iterator<String> = blacklist.iterator()
      var i = 0
      while (iterator.hasNext()) {
        selectionArgs[i] = iterator.next() + "%"
        i++
      }
      return selectionArgs
    }

  /**
   * 根据路径获得歌曲id
   */
  fun getSongIdByUrl(url: String?): Int {
    return getSongId(Audio.Media.DATA + " = ?", arrayOf(url))
  }

  @JvmStatic
  fun getSongByUrl(url: String?): Song {
    return getSong(Audio.Media.DATA + " = ?", arrayOf(url))
  }

  /**
   * 设置铃声
   */
  @JvmStatic
  fun setRing(context: Context?, audioId: Int) {
    try {
      val cv = ContentValues()
      cv.put(Audio.Media.IS_RINGTONE, true)
      cv.put(Audio.Media.IS_NOTIFICATION, false)
      cv.put(Audio.Media.IS_ALARM, false)
      cv.put(Audio.Media.IS_MUSIC, true)
      // 把需要设为铃声的歌曲更新铃声库
      if (mContext.contentResolver.update(Audio.Media.EXTERNAL_CONTENT_URI, cv,
              MediaStore.MediaColumns._ID + "=?", arrayOf(audioId.toString() + "")) > 0) {
        val newUri = ContentUris
            .withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, audioId.toLong())
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, newUri)
        ToastUtil.show(context, R.string.set_ringtone_success)
      } else {
        ToastUtil.show(context, R.string.set_ringtone_error)
      }
    } catch (e: Exception) {
      //没有权限
      if (e is SecurityException) {
        ToastUtil.show(context, R.string.please_give_write_settings_permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (!Settings.System.canWrite(mContext)) {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + mContext.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Util.isIntentAvailable(mContext, intent)) {
              mContext.startActivity(intent)
            }
          }
        }
      }
    }
  }

  fun makeSongCursor(selection: String?, selectionValues: Array<String?>?,
                     sortOrder: String?): Cursor? {
    var selection = selection
    var selectionValues = selectionValues
    selection = if (selection != null && selection.trim { it <= ' ' } != "") {
      "$selection AND $baseSelection"
    } else {
      baseSelection
    }
    if (selectionValues == null) {
      selectionValues = arrayOfNulls(0)
    }
    val baseSelectionArgs = baseSelectionArgs
    val newSelectionValues = arrayOfNulls<String>(selectionValues.size + baseSelectionArgs.size)
    System.arraycopy(selectionValues, 0, newSelectionValues, 0, selectionValues.size)
    if (newSelectionValues.size - selectionValues.size >= 0) {
      System.arraycopy(baseSelectionArgs, 0,
          newSelectionValues, selectionValues.size,
          newSelectionValues.size - selectionValues.size)
    }

    return try {
      mContext.contentResolver.query(Audio.Media.EXTERNAL_CONTENT_URI,
          BASE_PROJECTION, selection, newSelectionValues, sortOrder)
    } catch (e: SecurityException) {
      null
    }
  }

  @JvmStatic
  fun getSongId(selection: String?, selectionValues: Array<String?>?): Int {
    val songs = getSongs(selection, selectionValues, null)
    return if (songs.isNotEmpty()) songs[0].id else -1
  }

  @JvmStatic
  fun getSongIds(selection: String?, selectionValues: Array<String?>?): List<Int> {
    return getSongIds(selection, selectionValues, null)
  }

  @JvmStatic
  fun getSongIds(selection: String?, selectionValues: Array<String?>?,
                 sortOrder: String?): List<Int> {
    if (!Util.hasStoragePermissions()) {
      return ArrayList()
    }
    val ids: MutableList<Int> = ArrayList()
    try {
      makeSongCursor(selection, selectionValues, sortOrder).use { cursor ->
        if (cursor != null && cursor.count > 0) {
          while (cursor.moveToNext()) {
            ids.add(cursor.getInt(cursor.getColumnIndex(Audio.Media._ID)))
          }
        }
      }
    } catch (ignore: Exception) {
    }
    return ids
  }

  @JvmStatic
  fun getSong(selection: String?, selectionValues: Array<String?>?): Song {
    val songs = getSongs(selection, selectionValues, null)
    return if (songs.isNotEmpty()) songs[0] else EMPTY_SONG
  }

  @JvmStatic
  fun getSongs(selection: String?, selectionValues: Array<String?>?,
               sortOrder: String?): List<Song> {
    if (!Util.hasStoragePermissions()) {
      return ArrayList()
    }
    val songs: MutableList<Song> = ArrayList()
    try {
      makeSongCursor(selection, selectionValues, sortOrder).use { cursor ->
        if (cursor != null && cursor.count > 0) {
          while (cursor.moveToNext()) {
            songs.add(getSongInfo(cursor))
          }
        }
      }
    } catch (e: Exception) {
      Timber.v(e)
    }
    return songs
  }

  @JvmStatic
  fun getSongs(selection: String?, selectionValues: Array<String?>?): List<Song> {
    return getSongs(selection, selectionValues, SPUtil
        .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SONG_SORT_ORDER, null))
  }

  /**
   * 歌曲数量
   */
  val count: Int
    get() {
      try {
        mContext.contentResolver
            .query(Audio.Media.EXTERNAL_CONTENT_URI, arrayOf(Audio.Media._ID), baseSelection,
                baseSelectionArgs,
                null).use { cursor ->
              if (cursor != null) {
                return cursor.count
              }
            }
      } catch (e: Exception) {
        Timber.v(e)
      }
      return 0
    }

  init {
    SCAN_SIZE = SPUtil
        .getValue(App.getContext(), SETTING_KEY.NAME, SETTING_KEY.SCAN_SIZE,
            ByteConstants.MB)
  }
}