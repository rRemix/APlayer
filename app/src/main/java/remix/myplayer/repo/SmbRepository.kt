package remix.myplayer.repo

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.dao.SmbDao
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.remote.remotePath
import remix.myplayer.service.MusicServiceRemote
import java.io.IOException
import javax.inject.Inject

interface SmbRepository {
  fun allSmb(): Flow<List<Smb>>
  suspend fun insertOrReplace(smb: Smb): Long
  suspend fun delete(smb: Smb): Int
  suspend fun deleteResource(smb: Smb, path: String, isDirectory: Boolean, deleteRemote: suspend (String) -> Unit)
}

/** Same post-delete queue/playlist consistency guarantees as WebDAV. */
class SmbRepoImpl @Inject constructor(
  private val database: AppDatabase,
  private val covers: CoverPrefs,
  private val dao: SmbDao
) : SmbRepository {
  private val mutex = Mutex()

  override fun allSmb(): Flow<List<Smb>> = dao.selectAll()

  override suspend fun insertOrReplace(smb: Smb) = mutex.withLock { dao.insertOrReplace(smb) }

  override suspend fun delete(smb: Smb): Int = withContext(Dispatchers.IO) {
    mutex.withLock {
      withContext(NonCancellable) { removeReferences(smb, deleteSource = true) { true } }
    }
  }

  override suspend fun deleteResource(
    smb: Smb, path: String, isDirectory: Boolean, deleteRemote: suspend (String) -> Unit
  ) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val current = dao.byId(smb.id) ?: throw IOException("Music source no longer exists")
      if (current.getRoot() != smb.getRoot() || current.account != smb.account ||
        current.pwd != smb.pwd || current.domain != smb.domain) {
        throw IOException("Music source changed; reopen its folder")
      }
      val key = remotePath(path)
      require(key.isNotEmpty()) { "Cannot delete SMB source root" }
      deleteRemote(key)
      // A successful remote DELETE must be reflected locally even if the UI gets canceled.
      withContext(NonCancellable) {
        removeReferences(current) { sourceKey ->
          sourceKey == key || isDirectory && sourceKey.startsWith("$key/")
        }
      }
    }
    Unit
  }

  private suspend fun removeReferences(
    smb: Smb, deleteSource: Boolean = false, includes: (String) -> Boolean
  ): Int {
    val sourceId = smb.musicSource().id
    val matches: (SongIdentity) -> Boolean = { it.sourceId == sourceId && includes(it.sourceKey) }
    val (affected, count) = database.withTransaction {
      val playlists = database.playListDao().selectAllOnce().map { it.toModel() }
        .filter { playlist -> playlist.songIdentities.any(matches) }
      val keys = (playlists.flatMap { it.songIdentities } + database.playQueueDao().get().map { it.identity() })
        .filterTo(hashSetOf(), matches)
      database.playListDao().removeSongsFromAll(keys)
      database.playQueueDao().remove(keys.toList())
      playlists.map { it.id } to if (deleteSource) dao.delete(smb) else 0
    }
    covers.invalidate(affected.map { "playlist:$it" })
    withContext(Dispatchers.Main) { MusicServiceRemote.removeFromQueue(matches) }
    return count
  }
}
