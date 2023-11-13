package remix.myplayer.db.room

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.Gson
import com.tencent.bugly.crashreport.CrashReport
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.reactivex.Single
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.model.History
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.SPUtil
import timber.log.Timber
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet

/**
 * Created by remix on 2019/1/12
 */
class DatabaseRepository private constructor() {

  private val db = AppDatabase.getInstance(App.context.applicationContext)

  private val executors = Executors.newSingleThreadExecutor()

  private var myLoveId: Long = 0
    get() {
      if (field <= 0) {
        field = db.playListDao().selectAll().getOrNull(0)?.id ?: 0
      }
      return field
    }

  fun runInTransaction(block: () -> Unit) {
    executors.execute {
      db.runInTransaction(block)
    }
  }

  fun insertToPlayQueue(audioIds: List<Long>): Single<Int> {
    val actual = audioIds.toMutableList()
    return getPlayQueue()
      .map { playQueue ->
        playQueue.map { item ->
          item.audio_id
        }
      }
      .map {
        //不重复添加
        actual.removeAll(it.toSet())

        db.playQueueDao().insertPlayQueue(
          actual.map { id -> PlayQueue(id) })

        actual.size
      }
  }

  /**
   * 插入多首歌曲到播放队列
   */
  fun insertSongsToPlayQueue(songs: List<Song>): Single<Int> {
    val actual = songs.toMutableList()
    return getPlayQueue()
        .map { queue ->
          val keys = queue.map {
            if (it.audio_id > 0) it.audio_id.toString() else it.data
          }
          //不重复添加
          actual.removeAll {
            (it.isLocal() && keys.contains(it.id.toString())) || (it.isRemote() && keys.contains(it.data))
          }

          db.playQueueDao().insertPlayQueue(actual.map { song ->
            if (song.isLocal()) {
              PlayQueue(song.id, song.title, song.data)
            } else {
              PlayQueue(song.hashCode().toLong(), song.title, song.data).apply {
                if (song is Song.Remote) {
                  account = song.account
                  pwd = song.pwd
                }
              }
            }
          })

          actual.size
        }
  }

  /**
   * 从播放队列移除
   */
  fun deleteFromPlayQueue(audioIds: List<Long>): Single<Int> {
    return Single
        .fromCallable {
          deleteFromPlayQueueInternal(audioIds)
        }
  }


  private fun deleteFromPlayQueueInternal(audioIds: List<Long>): Int {
    if (audioIds.isEmpty()) {
      return 0
    }
    return db.runInTransaction(Callable {
      var count = 0
      val length = audioIds.size / MAX_ARGUMENT_COUNT + 1
      for (i in 0 until length) {
        val lastIndex = if ((i + 1) * MAX_ARGUMENT_COUNT < audioIds.size) (i + 1) * MAX_ARGUMENT_COUNT else 1
        try {
          count += db.playQueueDao().deleteSongs(audioIds.subList(i * MAX_ARGUMENT_COUNT, lastIndex))
        } catch (e: Exception) {
          Timber.e(e)
          CrashReport.postCatchedException(e)
        }
      }
      Timber.v("deleteFromPlayQueueInternal, count: $count")
      return@Callable count
    })
  }

  /**
   * 插入多首歌曲到播放列表
   */
  fun insertToPlayList(audioIds: List<Long>, playlistId: Long = -1): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectById(playlistId)
              ?: throw IllegalArgumentException("No Playlist Found")
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
   * 插入多首歌曲到播放列表
   */
  fun insertToPlayList(audioIds: List<Long>, name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name) ?: throw IllegalArgumentException("No Playlist Found")
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
  fun deleteFromPlayList(audioIds: List<Long>, name: String): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectByName(name)
          /**?: db.playListDao().selectById(playlistId)*/
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
   * 从播放列表移除
   */
  fun deleteFromPlayList(audioIds: List<Long>, playlistId: Long): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().selectById(playlistId) ?: throw IllegalArgumentException()
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
            val audioIds = songs.map { song -> song.id }
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
  fun getMyLoveList(): Single<List<Long>> {
    return Single
        .fromCallable {
          db.playListDao().selectById(myLoveId)
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
  fun getPlayList(id: Long): Single<PlayList> {
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
   * 获取所有播放列表
   */
  fun getSortPlayList(sortQuery: String): Single<List<PlayList>> {
    return Single
        .fromCallable {
          db.playListDao().runtimeQuery(SimpleSQLiteQuery(sortQuery))
        }
        .onErrorReturn {
          ArrayList()
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
  fun updatePlayListAudios(playlistId: Long, audioIds: List<Long>): Single<Int> {
    return Single
        .fromCallable {
          db.playListDao().updateAudioIDs(playlistId, Gson().toJson(audioIds))
        }
  }

  /**
   * 是否是收藏的歌曲
   */
  fun isMyLove(audioId: Long): Single<Boolean> {
    return getPlayList(myLoveId)
        .map { playList ->
          playList.audioIds.contains(audioId)
        }
  }

  /**
   * 添加或者删除收藏
   */
  fun toggleMyLove(audioId: Long): Single<Boolean> {
    return isMyLove(audioId)
        .flatMap {
          if (it) {
            deleteFromPlayList(arrayListOf(audioId), myLoveId)

          } else {
            insertToPlayList(arrayListOf(audioId), myLoveId)
          }
        }
        .map {
          it > 0
        }

  }

  /**
   * 获取播放队列
   */
  fun getPlayQueue(): Single<List<PlayQueue>> {
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
        }
  }

  /**
   * 获得播放队列对应的歌曲
   */
  fun getPlayQueueSongs(): Single<List<Song>> {
    val queues = ArrayList<PlayQueue>()
    return Single
        .fromCallable {
          db.playQueueDao().selectAll()
        }
        .doOnSuccess {
          queues.addAll(it)
        }
        .flatMap {
          getSongsInQueue(CUSTOMSORT, it)
        }
        .doOnSuccess { songs ->
          //删除不存在的歌曲
          if (songs.size < queues.size) {
            Timber.v("删除播放队列中不存在的歌曲")
            val deleteIds = ArrayList<Long>()
            val existIds = songs.map { it.id }

            for (item in queues) {
              if (!existIds.contains(item.audio_id)) {
                deleteIds.add(item.audio_id)
              }
            }

            if (deleteIds.isNotEmpty()) {
              deleteFromPlayQueueInternal(deleteIds)
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

  /**
   * 删除播放列表
   */
  fun deletePlayList(playListId: Long): Single<Int> {
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

    return Single
        .just(playList)
        .flatMap {
          val sort = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME,
              SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
              SortOrder.SONG_A_Z)
          //强制或者设置了自定义排序
          val actualSort = if (force || sort == SortOrder.PLAYLIST_SONG_CUSTOM)
            CUSTOMSORT else sort

          return@flatMap getSongsWithSort(actualSort, it.audioIds.toList())
        }
        .doOnSuccess { songs ->
          //移除不存在的歌曲
          if (songs.size < playList.audioIds.size) {
            Timber.v("删除播放列表中不存在的歌曲")
            val deleteIds = ArrayList<Long>()
            val existIds = songs.map { it.id }

            for (audioId in playList.audioIds) {
              if (!existIds.contains(audioId)) {
                deleteIds.add(audioId)
              }
            }

            if (deleteIds.isNotEmpty()) {
              deleteFromPlayList(deleteIds, playList.name).subscribe()
            }
          }
        }
  }

  private fun getSongsInQueue(sort: String, queues: List<PlayQueue>): Single<List<Song>> {
    if (queues.isEmpty()) {
      return Single.just(Collections.emptyList())
    }
    val isLocal = queues.all {
      it.audio_id > 0
    }
    if (isLocal) {
      val ids = queues.map { it.audio_id }
      return Single
        .fromCallable {
          val customSort = sort == CUSTOMSORT
          val inStr = makeInStr(ids)
          val songs = MediaStoreUtil.getSongs(
            MediaStore.Audio.Media._ID + " in(" + inStr + ")",
            null,
            if (customSort) null else sort)
          val tempArray: Array<Song> = Array(ids.size) { Song.EMPTY_SONG }

          songs.forEachIndexed { index, song ->
            tempArray[if (CUSTOMSORT == sort) ids.indexOf(song.id) else index] = song
          }

          tempArray
            .filter { it.id != Song.EMPTY_SONG.id }
        }
    } else {
      return Single
        .fromCallable {
          queues.map {
            Song.Remote(it.title, it.data, it.account ?: "", it.pwd ?: "")
          }
        }
    }
  }

  private fun getSongsWithSort(sort: String, ids: List<Long>): Single<List<Song>> {
    return Single
        .fromCallable {
          if (ids.isEmpty()) {
            return@fromCallable Collections.emptyList<Song>()
          }
          val customSort = sort == CUSTOMSORT
          val inStr = makeInStr(ids)
          val songs = MediaStoreUtil.getSongs(
              MediaStore.Audio.Media._ID + " in(" + inStr + ")",
              null,
              if (customSort) null else sort)
          val tempArray: Array<Song> = Array(ids.size) { Song.EMPTY_SONG }

          songs.forEachIndexed { index, song ->
            tempArray[if (CUSTOMSORT == sort) ids.indexOf(song.id) else index] = song
          }

          tempArray
              .filter { it.id != Song.EMPTY_SONG.id }
        }
  }

  private fun makeInStr(audioIds: List<Long>): String {
    val inStrBuilder = StringBuilder(127)

    for (i in audioIds.indices) {
      inStrBuilder.append(audioIds[i]).append(if (i != audioIds.size - 1) "," else " ")
    }

    return inStrBuilder.toString()
  }

  fun updateHistory(song: Song): Single<Int> {
    //先判断是否存在
    return Single
        .fromCallable {
          db.historyDao().selectByAudioId(song.id)
        }
        //没有就新建
        .onErrorResumeNext(Single.fromCallable {
          val newHistory = History(0, song.id, 0, 0)
          val id = db.historyDao().insertHistory(newHistory)
          newHistory.copy(id = id.toInt())
        })
        .map {
          db.historyDao().update(it.copy(play_count = it.play_count + 1, last_play = Date().time))
        }

  }

//  fun getHistorySongs(): Single<List<Song>> {
//    return Single
//        .fromCallable {
//          val sortOrder = SPUtil.getValue(
//            App.context,
//            SPUtil.SETTING_KEY.NAME,
//            SPUtil.SETTING_KEY.HISTORY_SORT_ORDER,
//            SortOrder.PLAY_COUNT
//          )
//          db.historyDao().selectAll(sortOrder)
//        }
//        .map {
//          val songs = ArrayList<Song>()
//          for (history in it) {
//            val song = MediaStoreUtil.getSongById(history.audio_id)
//            if (song != Song.EMPTY_SONG) {
//              songs.add(song)
//            }
//          }
//          songs
//        }
//  }

  /**
   * 获取本机的播放列表 播放列表名字-歌曲ID列表
   */
  val playlistFromMediaStore: Map<String, List<Long>>
    get() {
      val map = HashMap<String, List<Long>>()
      var playlistCursor: Cursor? = null
      var songCursor: Cursor? = null
      try {
        playlistCursor = App.context.contentResolver
            .query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null)
        if (playlistCursor == null || playlistCursor.count == 0) {
          return map
        }
        while (playlistCursor.moveToNext()) {
          val helperList = ArrayList<Long>()
          songCursor = App.context.contentResolver.query(MediaStore.Audio.Playlists.Members
              .getContentUri("external", playlistCursor
                  .getInt(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID)).toLong()), null, null, null, null)
          if (songCursor != null) {
            while (songCursor.moveToNext()) {
              helperList.add(songCursor
                  .getLong(songCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID)))
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

    private const val CUSTOMSORT = "CUSTOMSORT"
    private const val MAX_ARGUMENT_COUNT = 300
  }
}