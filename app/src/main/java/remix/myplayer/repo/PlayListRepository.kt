package remix.myplayer.repo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.playlistSortOrderFlow
import remix.myplayer.helper.ItemsSorter
import javax.inject.Inject
import remix.myplayer.data.db.room.entity.PlayList as PlayListEntity

interface PlayListRepository {
  fun allPlayLists(): Flow<List<PlayList>>
  fun observePlayList(id: Long): Flow<PlayList?>
  suspend fun addSongsToPlayList(keys: List<SongIdentity>, playListName: String): Int
  suspend fun insertPlayList(name: String): Long
  /** Rename only: a stale UI snapshot must not overwrite concurrently added tracks. */
  suspend fun updatePlayList(playList: PlayList): Int
  suspend fun updateSongOrder(id: Long, keys: List<SongIdentity>)
  suspend fun removeSongs(id: Long, keys: Set<SongIdentity>): Int
  suspend fun deletePlayList(id: Long): Int
  suspend fun isFavorite(key: SongIdentity): Boolean
  suspend fun toggleFavorite(key: SongIdentity): Boolean
  suspend fun getFavorite(): PlayList?
  suspend fun removeSongsFromAll(keys: List<SongIdentity>): Int
  suspend fun checkPlayListExist(name: String): Boolean
}

class PlayListRepoImpl @Inject constructor(
  private val playListDao: PlayListDao,
  private val settingPrefs: SettingPrefs,
  private val coverPrefs: CoverPrefs
) : PlayListRepository {
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun allPlayLists(): Flow<List<PlayList>> = settingPrefs.playlistSortOrderFlow()
    .flatMapLatest { sort -> playListDao.selectAll().map { rows ->
      ItemsSorter.sortedPlayLists(rows.map { it.toModel() }, sort)
    } }

  override fun observePlayList(id: Long): Flow<PlayList?> =
    playListDao.observeById(id).map { it?.toModel() }

  override suspend fun addSongsToPlayList(keys: List<SongIdentity>, playListName: String): Int {
    val list = requireNotNull(playListDao.selectByName(playListName)) { "No Playlist Found" }
    return playListDao.addSongs(list.id, keys).also { if (it > 0) invalidate(list.id) }
  }
  override suspend fun insertPlayList(name: String): Long =
    playListDao.insert(PlayListEntity(0, name, System.currentTimeMillis()))
  override suspend fun updatePlayList(playList: PlayList): Int =
    playListDao.rename(playList.id, playList.name)
  override suspend fun updateSongOrder(id: Long, keys: List<SongIdentity>) {
    playListDao.updateSongOrder(id, keys)
    invalidate(id)
  }
  override suspend fun removeSongs(id: Long, keys: Set<SongIdentity>): Int =
    playListDao.removeSongs(id, keys).also { if (it > 0) invalidate(id) }
  override suspend fun deletePlayList(id: Long): Int =
    playListDao.delete(id).also { if (it > 0) invalidate(id) }
  override suspend fun isFavorite(key: SongIdentity): Boolean =
    playListDao.contains(1L, key.sourceId.value, key.sourceKey)
  override suspend fun toggleFavorite(key: SongIdentity): Boolean {
    if (playListDao.selectById(1L) == null) return false
    return playListDao.toggle(1L, key).also { invalidate(1L) }
  }
  override suspend fun getFavorite(): PlayList? = playListDao.selectById(1L)?.toModel()
  override suspend fun removeSongsFromAll(keys: List<SongIdentity>): Int {
    val identities = keys.toSet()
    val affected = playListDao.selectAll().first().map { it.toModel() }
      .filter { list -> list.songIdentities.any { it in identities } }
    return playListDao.removeSongsFromAll(identities).also { count ->
      if (count > 0) affected.forEach { invalidate(it.id) }
    }
  }
  override suspend fun checkPlayListExist(name: String): Boolean = playListDao.exists(name)
  private fun invalidate(id: Long) = coverPrefs.invalidate("playlist:$id")
}
