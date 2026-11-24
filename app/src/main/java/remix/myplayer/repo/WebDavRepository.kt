package remix.myplayer.repo

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.db.room.AppDatabase
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.service.MusicServiceRemote
import java.io.IOException
import javax.inject.Inject

interface WebDavRepository {
  fun allWebDav(): Flow<List<WebDav>>
  suspend fun insertOrReplace(webDav: WebDav): Long
  suspend fun updateLastUrl(webDav: WebDav, url: String)
  suspend fun delete(webDav: WebDav): Int
  suspend fun deleteResource(
    webDav: WebDav, path: String, isDirectory: Boolean, deleteRemote: suspend (String) -> Unit
  )
}

class WebDavRepoImpl internal constructor(
  private val database: AppDatabase,
  private val covers: CoverPrefs,
  private val removePlayingSongs: suspend ((SongIdentity) -> Boolean) -> Unit
) : WebDavRepository {
  @Inject
  constructor(database: AppDatabase, covers: CoverPrefs) : this(database, covers, { matches ->
    withContext(Dispatchers.Main) { MusicServiceRemote.removeFromQueue(matches) }
  })

  private val mutex = Mutex()
  private val dao get() = database.webDavDao()

  override fun allWebDav(): Flow<List<WebDav>> = dao.selectAll()

  override suspend fun insertOrReplace(webDav: WebDav) = mutex.withLock { dao.insertOrReplace(webDav) }

  override suspend fun updateLastUrl(webDav: WebDav, url: String) = mutex.withLock {
    dao.updateLastUrl(webDav.id, webDav.server, url)
  }

  override suspend fun delete(webDav: WebDav): Int = withContext(Dispatchers.IO) {
    mutex.withLock {
      withContext(NonCancellable) {
        removeReferences(webDav, deleteSource = true) { true }
      }
    }
  }

  override suspend fun deleteResource(
    webDav: WebDav, path: String, isDirectory: Boolean, deleteRemote: suspend (String) -> Unit
  ) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val current = dao.byId(webDav.id) ?: throw IOException("Music source no longer exists")
      if (current.musicSource().root != webDav.musicSource().root ||
        current.account != webDav.account || current.pwd != webDav.pwd) {
        throw IOException("Music source changed; reopen its folder")
      }
      val root = current.musicSource().root
      val key = RemotePaths.relativePath(root, path).trimEnd('/')
      val url = RemotePaths.resolve(root, key).let { if (isDirectory && !it.endsWith('/')) "$it/" else it }
      deleteRemote(url)
      // Complete local cleanup after confirmed DELETE success, even if the page has been closed.
      withContext(NonCancellable) {
        removeReferences(current) { sourceKey ->
          sourceKey == key || isDirectory && (key.isEmpty() || sourceKey.startsWith("$key/"))
        }
      }
    }
    Unit
  }

  private suspend fun removeReferences(
    webDav: WebDav, deleteSource: Boolean = false, includes: (String) -> Boolean
  ): Int {
    val sourceId = webDav.musicSource().id
    val matches: (SongIdentity) -> Boolean = { it.sourceId == sourceId && includes(it.sourceKey) }
    val (affected, count) = database.withTransaction {
      val playlists = database.playListDao().selectAllOnce().map { it.toModel() }
        .filter { list -> list.songIdentities.any(matches) }
      val keys = (playlists.flatMap { it.songIdentities } + database.playQueueDao().get().map { it.identity() })
        .filterTo(hashSetOf(), matches)
      database.playListDao().removeSongsFromAll(keys)
      database.playQueueDao().remove(keys.toList())
      playlists.map { it.id } to if (deleteSource) dao.delete(webDav) else 0
    }
    covers.invalidate(affected.map { "playlist:$it" })
    // Match the live queue too: it may contain entries whose latest save is still pending.
    removePlayingSongs(matches)
    return count
  }
}
