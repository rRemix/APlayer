package remix.myplayer.compose.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.db.room.dao.PlayQueueDao
import remix.myplayer.db.room.model.PlayQueue
import remix.myplayer.misc.checkWorkerThread
import timber.log.Timber
import javax.inject.Inject

interface PlayQueueRepository {

  fun getAllSongs(): Flow<List<Song>>
  suspend fun remove(audioIds: List<Long>): Int
  suspend fun insert(queue: List<Song>): LongArray
}

class PlayQueueRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  settingPrefs: SettingPrefs,
  private val songRepo: SongRepository,
  private val playQueueDao: PlayQueueDao,
) : PlayQueueRepository, AbstractRepository(settingPrefs) {

  override fun getAllSongs(): Flow<List<Song>> {
    return playQueueDao.selectAllSuspend()
      .map {
        it to getSongsInQueue(it)
      }.flowOn(Dispatchers.IO)
      .map { pair ->
        val queue = pair.first
        val songs = pair.second
        //删除不存在的歌曲
        if (songs.size < queue.size) {
          Timber.v("删除播放队列中不存在的歌曲")
          val deleteIds = ArrayList<Long>()
          val existIds = songs.map { it.id }

          for (item in queue) {
            if (!existIds.contains(item.audio_id)) {
              deleteIds.add(item.audio_id)
            }
          }

          Timber.tag(TAG).v("deleteIds: $deleteIds")
          if (deleteIds.isNotEmpty()) {
            remove(deleteIds)
          }
        }

        songs
      }
  }

  // TODO 大量数据拆分
  override suspend fun remove(audioIds: List<Long>) =
    playQueueDao.deleteSongsSuspend(audioIds)

  override suspend fun insert(songs: List<Song>): LongArray {
    val oldQueue = playQueueDao.selectAllSuspend().first()

    // 不重复添加
    val actual = songs.toMutableList()
    val keys = oldQueue.map {
      if (it.audio_id > 0) it.audio_id.toString() else it.data
    }
    //不重复添加
    actual.removeAll {
      (it.isLocal() && keys.contains(it.id.toString())) || (it.isRemote() && keys.contains(it.data))
    }

    return playQueueDao.insertPlayQueueSuspend(actual.map { song ->
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
  }

  private fun getSongsInQueue(queues: List<PlayQueue>): List<Song> {
    checkWorkerThread()
    if (queues.isEmpty()) {
      return emptyList()
    }
    val isLocal = queues.all {
      it.audio_id > 0
    }
    if (isLocal) {
      val ids = queues.map { it.audio_id }
      val songs =
        songRepo.getSongs(makeInStrQuery(ids), null, null)

      // 按照查询顺序返回
      val tempArray: Array<Song> = Array(ids.size) { Song.EMPTY_SONG }

      songs.forEachIndexed { index, song ->
        tempArray[ids.indexOf(song.id)] = song
      }

      return tempArray
        .filter { it.id != Song.EMPTY_SONG.id }
    } else {
      return queues.map {
        Song.Remote(it.title, it.data, it.account ?: "", it.pwd ?: "")
      }
    }
  }

  companion object {

    private const val TAG = "PlayQueueRepo"
  }
}