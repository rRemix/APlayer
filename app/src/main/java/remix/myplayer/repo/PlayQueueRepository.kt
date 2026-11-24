package remix.myplayer.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.PlayQueueDao
import remix.myplayer.data.db.room.entity.PlayQueue
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import javax.inject.Inject

interface PlayQueueRepository {
  fun getAllSongs(): Flow<List<Song>>
  suspend fun getIdentities(): List<SongIdentity>
  suspend fun removeByIdentities(keys: List<SongIdentity>): Int
  suspend fun insert(queue: List<Song>): LongArray
  suspend fun replace(queue: List<Song>)
  suspend fun clear()
}

class PlayQueueRepoImpl @Inject constructor(
  private val songRepo: SongRepository,
  private val playQueueDao: PlayQueueDao,
  private val metadata: SongMetadataRepository,
  private val sources: MusicSourceRepository
) : PlayQueueRepository {
  override fun getAllSongs(): Flow<List<Song>> = combine(playQueueDao.selectAll(), songRepo.observeSongs(), metadata.revision, sources.observeSources()) { rows, _, _, _ ->
    withContext(Dispatchers.IO) { songRepo.resolveSongs(rows.map { it.identity() }) }
  }
  override suspend fun getIdentities(): List<SongIdentity> = playQueueDao.get().map { it.identity() }
  override suspend fun removeByIdentities(keys: List<SongIdentity>): Int = playQueueDao.remove(keys)
  override suspend fun insert(queue: List<Song>): LongArray =
    playQueueDao.append(queue.filter { it.valid() }.map { it.songIdentity })
  override suspend fun replace(queue: List<Song>) = playQueueDao.replace(
    queue.filter { it.valid() }.distinctBy { it.songIdentity }.mapIndexed { index, song ->
      PlayQueue(song.sourceId.value, song.sourceKey, index)
    }
  )
  override suspend fun clear() { playQueueDao.clear() }
}
