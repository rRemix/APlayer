package remix.myplayer.service

import kotlinx.coroutines.flow.first
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.repo.SongRepository
import javax.inject.Inject

/**
 * created by Remix on 2019-09-26
 */

class PlayQueueStore @Inject constructor(
  private val songRepository: SongRepository,
  private val playQueueRepository: PlayQueueRepository,
  private val settingPrefs: SettingPrefs,
) {

  suspend fun restore(): Pair<List<Song>, Int> {
    var queue = playQueueRepository.getAllSongs().first()
    if (queue.isEmpty()) {
      // 默认全部歌曲为播放列表
      queue = songRepository.allSongs()
      save(queue)
    }

    val pos = restoreLastSong(queue)
    return queue to pos
  }

  /**
   * 初始化上一次退出时时正在播放的歌曲
   */
  private fun restoreLastSong(queue: List<Song>): Int {
    if (queue.isEmpty()) {
      return 0
    }
    // 读取上次退出时正在播放的歌曲的id或者是路径
    val lastSong = settingPrefs.lastSong
    // 上次退出时正在播放的歌曲的pos
    var pos = 0
    // 查找上次退出时的歌曲是否还存在
    if (lastSong.isNotEmpty()) {
      for (i in queue.indices) {
        if (lastSong == queue[i].id.toString() || lastSong == queue[i].data) {
          pos = i
          break
        }
      }
    }
    return pos
  }

  suspend fun save(queue: List<Song>) {
    if (queue.isNotEmpty()) {
      playQueueRepository.replace(queue)
    } else {
      playQueueRepository.clear()
    }
  }

  companion object {

    private const val TAG = "PlayQueue"
  }
}
