package remix.myplayer.service

import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_SHUFFLE
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.repo.SongRepository
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Util
import timber.log.Timber
import javax.inject.Inject

/**
 * created by Remix on 2019-09-26
 */

class PlayQueue @Inject constructor(
  private val songRepository: SongRepository,
  private val playQueueRepository: PlayQueueRepository,
  private val settingPrefs: SettingPrefs,
) : CoroutineScope by CoroutineScope(Dispatchers.Main + SupervisorJob()) {

  private var loaded = false

  // 当前播放队列
  private val _playingQueue = ArrayList<Song>()
  val playingQueue: List<Song>
    get() = _playingQueue

  // 原始的队列
  private val _originalQueue = ArrayList<Song>()
  val originalQueue: List<Song>
    get() = _originalQueue

  // 下一首歌曲的位置
  private var nextPosition = 0

  // 当前播放的位置
  var position = 0
    private set

  // 当前正在播放的歌曲
  var song: Song = Song.EMPTY_SONG

  // 下一首歌曲
  var nextSong: Song = Song.EMPTY_SONG

  fun makeList() {
    synchronized(this) {
      if (settingPrefs.playModel == MODE_SHUFFLE) {
        makeShuffleList()
      } else {
        makeNormalList()
      }
    }
    Timber.v("makeList, size: ${_playingQueue.size}")
  }


  private fun makeNormalList() {
    if (_originalQueue.isEmpty()) {
      return
    }
    _playingQueue.clear()
    _playingQueue.addAll(_originalQueue)
    Timber.v("makeNormalList, queue: ${_playingQueue.size}")
  }

  private fun makeShuffleList() {
    if (_originalQueue.isEmpty()) {
      return
    }

    _playingQueue.clear()
    _playingQueue.addAll(_originalQueue)

    if (position >= 0) {
      _playingQueue.shuffle()
      if (position < _playingQueue.size) {
        val removeSong = _playingQueue.removeAt(position)
        _playingQueue.add(0, removeSong)
      }
    } else {
      _playingQueue.shuffle()
    }

    Timber.v("makeShuffleList, queue: ${_playingQueue.size}")
  }

  suspend fun restoreIfNecessary() = withContext(Dispatchers.IO) {
    if (!loaded && _playingQueue.isEmpty()) {
      val queue = playQueueRepository.getAllSongs().first()
      if (queue.isNotEmpty()) {
        _originalQueue.addAll(queue)
        _playingQueue.addAll(_originalQueue)
        makeList()
      } else {
        // 默认全部歌曲为播放列表
        setPlayQueue(songRepository.allSongs())
      }

      restoreLastSong()
      loaded = true
    }
  }

  /**
   * 初始化上一次退出时时正在播放的歌曲
   */
  private fun restoreLastSong() {
    if (_originalQueue.isEmpty()) {
      return
    }
    // 读取上次退出时正在播放的歌曲的id或者是路径
    val lastSong = settingPrefs.lastSong
    // 上次退出时正在播放的歌曲是否还存在
    var exist = false
    // 上次退出时正在播放的歌曲的pos
    var pos = 0
    // 查找上次退出时的歌曲是否还存在
    if (lastSong.isNotEmpty()) {
      for (i in _originalQueue.indices) {
        if (lastSong == _originalQueue[i].id.toString() || lastSong == _originalQueue[i].data) {
          exist = true
          pos = i
          break
        }
      }
    }

    // 上次退出时保存的正在播放的歌曲未失效
    if (exist) {
      setUpDataSource(_originalQueue[pos], pos)
    } else {
      //重新找到一个歌曲id
      setUpDataSource(_originalQueue[0], 0)
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

  fun setPlayQueue(songs: List<Song>) {
    synchronized(this) {
      _originalQueue.clear()
      _originalQueue.addAll(songs)
    }

    makeList()
    saveQueue()
  }

  /**
   * 添加到下一首播放
   */
  fun addNextSong(nextSong: Song) {
    // 添加到播放队列
    if (nextSong == this.nextSong) {
      MessageNotifier.show(R.string.already_add_to_next_song)
      return
    }

    synchronized(this) {
      if (_playingQueue.contains(nextSong)) {
        _playingQueue.remove(nextSong)
        _playingQueue.add(if (position + 1 < playingQueue.size) position + 1 else 0, nextSong)
      } else {
        _playingQueue.add(_playingQueue.indexOf(song) + 1, nextSong)
      }
      if (_originalQueue.contains(nextSong)) {
        _originalQueue.remove(nextSong)
        _originalQueue.add(if (position + 1 < _originalQueue.size) position + 1 else 0, nextSong)
      } else {
        _originalQueue.add(_originalQueue.indexOf(song) + 1, nextSong)
      }
    }
    updateNextSong()
    saveQueue()
  }

  fun addSongs(songs: List<Song>) {
    synchronized(this) {
      _playingQueue.addAll(songs)
      _originalQueue.addAll(songs)
    }
    saveQueue(clear = false)
  }

  fun removeAll(deleteSongs: List<Song>) {
    synchronized(this) {
      val toRemove = deleteSongs.toSet()
      _playingQueue.removeAll(toRemove)
      _originalQueue.removeAll(toRemove)
    }
    saveQueue()
  }

  /**
   * 直接设置position
   */
  fun setPosition(pos: Int) {
    position = pos
    // 随机播放模式重置下随机队列
    if (settingPrefs.playModel == MODE_SHUFFLE) {
      makeShuffleList()
    }
    song = _originalQueue[position]
  }

  /**
   * 根据当前播放歌曲重新定位position
   */
  fun rePosition() {
    val newPosition = _originalQueue.indexOf(song)
    if (newPosition >= 0) {
      position = newPosition
    }
  }

  fun next() {
    position = nextPosition
    song = nextSong.copy()
    updateNextSong()
  }

  fun previous() {
    if (--position < 0) {
      position = _playingQueue.size - 1
    }
    if (position == -1 || position > _playingQueue.size - 1) {
      return
    }
    song = _playingQueue[position]
    updateNextSong()
  }

  private fun saveQueue(clear: Boolean = true) = launch {
    if (clear) {
      playQueueRepository.clear()
    }
    if (!_originalQueue.isEmpty()) {
      playQueueRepository.insert(_originalQueue)
    }
  }

  fun updateNextSong() {
    if (_playingQueue.isEmpty()) {
      nextSong = Song.EMPTY_SONG
    } else {
      synchronized(this) {
        nextPosition = position + 1
        if (nextPosition >= _playingQueue.size) {
          nextPosition = 0
        }
        nextSong = _playingQueue[nextPosition]
      }
    }

    Util.sendLocalBroadcast(Intent(ACTION_UPDATE_NEXT))
    Timber.v("updateNextSong, curPos: $position nextPos: $nextPosition nextSong=${nextSong.title}\n }")
  }

  fun size(): Int {
    return _playingQueue.size
  }

  fun isEmpty(): Boolean {
    return _playingQueue.isEmpty()
  }

  companion object {

    private const val TAG = "PlayQueue"

    const val ACTION_UPDATE_NEXT = "remix.myplayer.update.next_song"
  }
}