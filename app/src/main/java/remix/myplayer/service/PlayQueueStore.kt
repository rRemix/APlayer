package remix.myplayer.service

import kotlinx.coroutines.flow.first
import android.net.Uri
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.repo.SongQuery
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

  data class RestoredQueue(val songs: List<Song>, val position: Int, val hasSavedQueue: Boolean)

  suspend fun restore(): RestoredQueue {
    var queue = playQueueRepository.getAllSongs().first()
    val hasSavedQueue = playQueueRepository.getIdentities().isNotEmpty()
    if (queue.isEmpty() && !hasSavedQueue) {
      // 默认全部歌曲为播放列表
      queue = songRepository.getSongs(SongQuery(sortOrder = settingPrefs.songSortOrder))
      save(queue)
    }

    return RestoredQueue(queue, restoreLastSong(queue), hasSavedQueue)
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
        val song = queue[i]
        if (lastSong == song.getKey() || (song.isLocal() && lastSong == song.sourceKey) ||
          (song.sourceId.value.startsWith("webdav:") &&
            (RemotePaths.withoutCredentials(lastSong) == song.data || Uri.parse(song.data).let { uri ->
              lastSong == "${uri.scheme}://${uri.authority}${uri.path}"
            }))) {
          settingPrefs.lastSong = song.getKey()
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
