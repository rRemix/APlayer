package remix.myplayer.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.PlayQueueDao
import remix.myplayer.data.db.room.entity.PlayQueue
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import javax.inject.Inject

interface PlayQueueRepository {

  fun getAllSongs(): Flow<List<Song>>
  suspend fun removeByAudioIds(audioIds: List<Long>): Int
  suspend fun remove(playQueues: List<PlayQueue>): Int
  suspend fun insert(queue: List<Song>): LongArray
  suspend fun replace(queue: List<Song>)
  suspend fun clear()
}

class PlayQueueRepoImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
  settingPrefs: SettingPrefs,
  private val songRepo: SongRepository,
  private val playQueueDao: PlayQueueDao,
) : PlayQueueRepository, AbstractRepository(settingPrefs) {

  override fun getAllSongs(): Flow<List<Song>> {
    return playQueueDao.selectAll()
      .flowOn(Dispatchers.IO)
      .map { queue ->
        withContext(Dispatchers.IO) { getSongsInQueue(queue) }
      }
  }

  override suspend fun removeByAudioIds(audioIds: List<Long>) =
    playQueueDao.deleteSongs(audioIds)

  override suspend fun remove(playQueues: List<PlayQueue>) = playQueueDao.delete(playQueues)

  override suspend fun insert(queue: List<Song>): LongArray {
    val oldQueue = playQueueDao.selectAll().first()

    // 不重复添加
    val oldAudioIds = oldQueue.map { it.audio_id }.toSet()
    return playQueueDao.insert(
      queue
        .filter { it.id !in oldAudioIds }
        // 某一首歌曲可能同时存在于列表A、B，然后添加到播放队列
        .distinctBy { it.id }
        .map { song ->
          toPlayQueueEntity(song)
        })
  }

  override suspend fun replace(queue: List<Song>) {
    playQueueDao.replace(
      queue
        .distinctBy { it.id }
        .map { song ->
          toPlayQueueEntity(song)
        }
    )
  }

  private fun toPlayQueueEntity(song: Song): PlayQueue {
    return PlayQueue(song.id, song.title, song.data).apply {
      if (song is Song.Remote) {
        account = song.account
        pwd = song.pwd
      }
    }
  }

  override suspend fun clear() {
    playQueueDao.clear()
  }

  private suspend fun getSongsInQueue(queues: List<PlayQueue>): List<Song> {
    checkWorkerThread()
    if (queues.isEmpty()) {
      return emptyList()
    }

    val songs = mutableListOf<Song>()
    val pendingDelete = mutableListOf<PlayQueue>()

    val local = queues.filter { it.audio_id > 0 }
    val remote = queues.filter { it.audio_id <= 0 }

    // 处理本地歌曲
    if (local.isNotEmpty()) {
      val localSongs = songRepo.getSongs(makeInStrQuery(local.map { it.audio_id }), null, null)

      // 按照 queues 的顺序添加歌曲，保证顺序一致
      val songMap = localSongs.associateBy { it.id }
      local.forEach { queue ->
        songMap.getOrElse(queue.audio_id) { Song.EMPTY_SONG }.let { song ->
          if (song.valid()) {
            songs.add(song)
          } else {
            pendingDelete.add(queue)
          }
        }
      }
    }

    // 处理远程歌曲
    remote.forEach { queue ->
      val remoteSong = Song.Remote(
        queue.title,
        queue.data,
        queue.account ?: "",
        queue.pwd ?: ""
      )
      if (remoteSong.valid()) {
        songs.add(remoteSong)
      } else {
        pendingDelete.add(queue)
      }
    }

    if (pendingDelete.isNotEmpty()) {
      Timber.tag(TAG).v("删除不存在歌曲: ${remove(pendingDelete)}")
    }
    return songs
  }

  companion object {

    private const val TAG = "PlayQueueRepo"
  }
}
