package remix.myplayer.service

import android.content.Intent
import android.support.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.misc.checkMainThread
import remix.myplayer.misc.checkWorkerThread
import remix.myplayer.misc.log.LogObserver
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.util.Constants
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber

/**
 * created by Remix on 2019-09-26
 */
class PlayQueue(private val service: MusicService) : CoroutineScope by MainScope() {
  private val repository = DatabaseRepository.getInstance()

  private var loaded = false

  // 当前播放队列
  private val playQueue = ArrayList<Song>()
  // 原始的队列
  private val originalQueue = ArrayList<Song>()

  // 下一首歌曲的位置
  private var nextPosition = 0

  // 当前播放的位置
  private var position = 0

  // 当前正在播放的歌曲
  var song = Song.EMPTY_SONG
    private set

  // 下一首歌曲
  var nextSong = Song.EMPTY_SONG
    private set

//  // 播放模式
//  var playModel: Int = Constants.PLAY_LOOP
//    set(newPlayModel) {
//      Timber.v("修改播放模式: $newPlayModel")
////      service.updateAppwidget()
//      SPUtil.putValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, newPlayModel)
//      makeList()
//      field = newPlayModel
////      service.updateQueueItem()
//    }

  fun makeList() {
    checkMainThread()
    if (service.playModel == Constants.PLAY_SHUFFLE) {
      makeShuffleList()
    } else {
      makeNormalList()
    }
  }


  private fun makeNormalList() {
    playQueue.clear()
    playQueue.addAll(originalQueue)
    Timber.v("makeNormalList, queue: ${playQueue.size}")
  }

  private fun makeShuffleList() {
    playQueue.clear()
    playQueue.addAll(originalQueue)
    playQueue.shuffle()
    Timber.v("makeShuffleList, queue: ${playQueue.size}")
  }


  @WorkerThread
  @Synchronized
  fun restoreIfNecessary() {
    checkWorkerThread()
    if (!loaded && playQueue.isEmpty()) {
      originalQueue.addAll(repository.getPlayQueueSongs().blockingGet())
      playQueue.addAll(originalQueue)

      restoreLastSong()
      loaded = true
    }
  }

  /**
   * 初始化上一次退出时时正在播放的歌曲
   */
  private fun restoreLastSong() {
    if (originalQueue.isEmpty()) {
      return
    }
    //读取上次退出时正在播放的歌曲的id
    val lastId = SPUtil.getValue(service, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, -1)
    //上次退出时正在播放的歌曲是否还存在
    var isLastSongExist = false
    //上次退出时正在播放的歌曲的pos
    var pos = 0
    //查找上次退出时的歌曲是否还存在
    if (lastId != -1) {
      try {
        for (i in originalQueue.indices) {
          if (lastId == originalQueue[i].id) {
            isLastSongExist = true
            pos = i
            break
          }
        }
      } catch (e: Exception) {
        Timber.v("restoreLastSong error: ${e.message}")
      }
    }

    //上次退出时保存的正在播放的歌曲未失效
    if (isLastSongExist) {
      setUpDataSource(originalQueue[pos], pos)
    } else {
      //重新找到一个歌曲id
      setUpDataSource(originalQueue[0], 0)
    }
  }

  /**
   * 初始化mediaplayer
   */
  private fun setUpDataSource(lastSong: Song?, pos: Int) {
    if (lastSong == null) {
      return
    }
    //初始化当前播放歌曲
    Timber.v("当前歌曲:%s", lastSong.title)
    song = lastSong
    position = pos
    updateNextSong()
  }

  private fun getSongAt(position: Int): Song {
//    return playQueue.getOrElse(position) {
//      Timber.v("getSongAt, 返回默认值")
//      Song.EMPTY_SONG
//    }
    return originalQueue.getOrElse(position) {
      Song.EMPTY_SONG
    }
  }

  fun getPlayingQueue(): List<Song> {
    return playQueue
  }

  fun setPlayQueue(songs: List<Song>) {
    originalQueue.clear()
    originalQueue.addAll(songs)

    makeList()
    saveQueue()
  }

  fun addSong(song: Song) {
    playQueue.add(song)
    originalQueue.add(song)
    saveQueue()
  }

  fun addSong(position: Int, song: Song) {
    playQueue.add(position, song)
    originalQueue.add(position, song)
    saveQueue()
  }

  fun setPosition(pos: Int) {
    position = pos
    song = getSongAt(position)
  }

  private fun saveQueue() {
    launch {
      repository.clearPlayQueue()
          .flatMap {
            repository.insertToPlayQueue(originalQueue.map { song.id })
          }
          .subscribe(LogObserver())
    }
  }

  private fun updateNextSong() {
    checkMainThread()

    if (playQueue.isEmpty() && playQueue.isEmpty()) {
      ToastUtil.show(service, R.string.list_is_empty)
      return
    }

    nextPosition = position + 1
    if (nextPosition >= playQueue.size) {
      nextPosition = 0
    }
    nextSong = playQueue[nextPosition]
    Timber.v("updateNextSong, song=$nextSong")


    Util.sendLocalBroadcast(Intent(PlayerActivity.ACTION_UPDATE_NEXT))
    Timber.v("updateNextSong, curPos: $position nextPos: $nextPosition song=$nextSong")
  }

  fun isEmpty(): Boolean {
    return playQueue.isEmpty()
  }


}