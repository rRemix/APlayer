package remix.myplayer.db.room

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.SPUtil
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

/**
 * Created by remix on 2019/1/12
 */
class DatabaseRepository private constructor() {

  private val db = AppDatabase.getInstance(App.getContext().applicationContext)

  private val executors = Executors.newSingleThreadExecutor()

  fun runInTransaction(body: Runnable) {
    executors.execute {
      db.runInTransaction(body)
    }
  }

  /**
   * 插入多首歌曲到播放队列
   */
  fun insertToPlayQueue(audioIds: List<Int>): Single<Int> {
    return getPlayQueue()
        .map {
          //不重复添加
          val actual = audioIds.toMutableList()
          actual.removeAll(it)

          db.playQueueDao().insertPlayQueue(convertAudioIdsToPlayQueues(actual))

          actual.size
        }
  }

  /**
   * 从播放队列移除
   */
  fun deleteFromPlayQueue(audioIds: List<Int>): Single<Int> {
    return Single
        .fromCallable {
          db.playQueueDao().deleteSongs(audioIds)
        }

  }

  /**
   * 插入多首歌曲到播放列表
   */
  fun insertToPlayList(audioIds: List<Int>, name: String = "", playlistId: Int = -1): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name) ?: db.playListDao().selectById(playlistId)
          ?: throw IllegalArgumentException()
        }
        .map {
          //不重复添加
          val old = it.audioIds.size
          it.audioIds.addAll(audioIds)
          val count = it.audioIds.size - old
          db.playListDao().update(it)
          count
        }
  }


  /**
   * 从播放列表移除
   */
  fun deleteFromPlayList(audioIds: List<Int>, name: String = "", playlistId: Int = -1): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name) ?: db.playListDao().selectById(playlistId)
          ?: throw IllegalArgumentException()
        }
        .map {
          val old = it.audioIds.size
          it.audioIds.removeAll(audioIds)
          val count = old - it.audioIds.size
          db.playListDao().update(it)
          count
        }
  }

  /**
   * 从所有播放列表移除
   */
  fun deleteFromAllPlayList(songs: List<Song>): Completable {
    return getAllPlaylist()
        .flatMapCompletable { playLists ->
          CompletableSource {
            val audioIds = songs.map { song -> song.Id }
            playLists.forEach { playList ->
              deleteFromPlayList(audioIds, playList.name).subscribe()
            }
            it.onComplete()
          }
        }

  }

  /**
   * 插入播放列表
   */
  fun insertPlayList(name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().insertPlayList(PlayList(0, name, LinkedHashSet(), Date().time)).toInt()
        }

  }

  /**
   * 获取我的收藏播放列表
   */
  fun getMyLoveList(): Single<List<Int>> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(MyLove)
        }
        .map {
          it.audioIds.toList()
        }
  }

  /**
   * 获取播放列表
   */
  fun getPlayList(name: String): Single<PlayList> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name)
        }
  }

  /**
   * 获取播放列表
   */
  fun getPlayList(id: Int): Single<PlayList> {
    return Single
        .fromCallable {
          db.playListDao().selectById(id)
        }
  }

  /**
   * 获取所有播放列表
   */
  fun getAllPlaylist(): Single<List<PlayList>> {
    return Single
        .fromCallable {
          db.playListDao().selectAll()
        }
  }


  /**
   * 修改播放列表
   */
  fun updatePlayList(playList: PlayList): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().update(playList)
        }
  }

  /**
   * 更新列表内的歌曲id
   */
  fun updatePlayListAudios(playlistId: Int, audioIds: List<Int>): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().updateAudioIDs(playlistId, Gson().toJson(audioIds))
        }
  }

  /**
   * 是否是收藏的歌曲
   */
  fun isMyLove(audioId: Int): Single<Boolean> {
    return getPlayList(MyLove)
        .map { playList ->
          playList.audioIds.contains(audioId)
        }
  }

  /**
   * 添加或者删除收藏
   */
  fun toggleMyLove(audioId: Int): Single<Boolean> {
    return isMyLove(audioId)
        .flatMap {
          if (it) {
            deleteFromPlayList(arrayListOf(audioId))

          } else {
            insertToPlayList(arrayListOf(audioId))
          }
        }
        .map {
          it > 0
        }

  }

  /**
   * 获取播放队列
   */
  fun getPlayQueue(): Single<List<Int>> {
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
              .map {
                it.audio_id
              }
        }
  }

  /**
   * 获得播放队列对应的歌曲
   */
  fun getPlayQueueSongs(): Single<List<Song>> {
    val idsInQueue = ArrayList<Int>()
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
              .map {
                it.audio_id
              }
        }
        .flatMap {
          val inStr = makeInStr(it)
          idsInQueue.addAll(it)
          getSongsWithSort(inStr, "instr ('" + inStr + "'," + MediaStore.Audio.Media._ID + ")")
        }
        .doOnSuccess { songs ->
          //删除不存在的歌曲
          if (songs.size < idsInQueue.size) {
            val deleteIds = ArrayList<Int>()
            for (audioId in idsInQueue) {
              if (!songs.contains(Song(audioId))) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              db.playQueueDao().deleteSongs(deleteIds)
            }
          }
        }
  }

  /**
   * 清空播放队列
   */
  fun clearPlayQueue(): Single<Int> {
    return Single
        .fromCallable {
          db.playQueueDao().clear()
        }
  }


  private fun convertAudioIdsToPlayQueues(audioIds: List<Int>): List<PlayQueue> {
    val playQueues = ArrayList<PlayQueue>()
    for (audioId in audioIds) {
      playQueues.add(PlayQueue(0, audioId))
    }
    return playQueues
  }

  /**
   * 删除播放列表
   */
  fun deletePlayList(playListId: Int): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().deletePlayList(playListId)
        }
  }


  /**
   * 根据播放列表名字获得歌曲列表
   * @param force 是否强制启用自定义排序 即按照查询的顺序返回
   */
  fun getPlayListSongs(context: Context, playList: PlayList, force: Boolean = false): Single<List<Song>> {
    val idsInPlayList = ArrayList<Int>()

    return Single
        .just(playList)
        .flatMap {
          idsInPlayList.addAll(it.audioIds)
          val sort = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME,
              SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
              SortOrder.PlayListSongSortOrder.SONG_A_Z)
          //强制或者设置了自定义排序
          val customSort = force || sort == SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM

          val inStr = makeInStr(it.audioIds.toList())
          return@flatMap getSongsWithSort(inStr,
              if (customSort) "instr ('" + inStr + "'," + MediaStore.Audio.Media._ID + ")"
              else sort)
        }
        .doOnSuccess { songs ->
          //移除不存在的歌曲
          if (songs.size < idsInPlayList.size) {
            val deleteIds = ArrayList<Int>()
            for (audioId in idsInPlayList) {
              if (!songs.contains(Song(audioId))) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              deleteFromPlayList(deleteIds, playList.name).subscribe()
            }
          }
        }
  }

  private fun getSongsWithSort(selection: String, sort: String): Single<List<Song>> {
    return Single
        .fromCallable {
          return@fromCallable MediaStoreUtil.getSongs(MediaStore.Audio.Media._ID + " in(" + selection + ")",
              null,
              sort)
        }
  }

  private fun makeInStr(audioIds: List<Int>): String {
    val inStrBuilder = StringBuilder(127)

    for (i in audioIds.indices) {
      inStrBuilder.append(audioIds[i]).append(if (i != audioIds.size - 1) "," else " ")
    }

    return inStrBuilder.toString()
  }

  /**
   * 获取本机的播放列表 播放列表名字-歌曲ID列表
   */
  val playlistFromMediaStore: Map<String, List<Int>>
    get() {
      val map = HashMap<String, List<Int>>()
      var playlistCursor: Cursor? = null
      var songCursor: Cursor? = null
      try {
        playlistCursor = App.getContext().contentResolver
            .query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null)
        if (playlistCursor == null || playlistCursor.count == 0) {
          return map
        }
        while (playlistCursor.moveToNext()) {
          val helperList = java.util.ArrayList<Int>()
          songCursor = App.getContext().contentResolver.query(MediaStore.Audio.Playlists.Members
              .getContentUri("external", playlistCursor
                  .getInt(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)).toLong()), null, null, null, null)
          if (songCursor != null) {
            while (songCursor.moveToNext()) {
              helperList.add(songCursor
                  .getInt(songCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)))
            }
          }
          map[playlistCursor
              .getString(playlistCursor.getColumnIndex(MediaStore.Audio.PlaylistsColumns.NAME))] = helperList
          songCursor = null
        }
      } catch (e: Exception) {
        e.printStackTrace()
      } finally {
        if (playlistCursor != null && !playlistCursor.isClosed) {
          playlistCursor.close()
        }
        if (songCursor != null && !songCursor.isClosed) {
          songCursor.close()
        }
      }
      return map
    }


  companion object {
    @Volatile
    private var INSTANCE: DatabaseRepository? = null

    @JvmStatic
    fun getInstance(): DatabaseRepository =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: DatabaseRepository()
        }


    @JvmStatic
    val MyLove = App.getContext().getString(R.string.my_favorite)
  }
}