package remix.myplayer.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.playlistSortOrderFlow
import remix.myplayer.helper.ItemsSorter
import remix.myplayer.helper.SortOrder
import java.util.Date
import javax.inject.Inject

interface PlayListRepository {

  fun allPlayLists(): Flow<List<PlayList>>
  suspend fun addSongsToPlayList(audioIds: List<Long>, playListName: String): Int
  suspend fun insertPlayList(name: String): Long
  suspend fun updatePlayList(playList: PlayList): Int
  suspend fun deletePlayList(id: Long): Int
  suspend fun isFavorite(id: Long): Boolean
  suspend fun toggleFavorite(id: Long): Boolean
  suspend fun getFavorite(): PlayList?
  suspend fun removeAudioIdsFromAll(audioIds: List<Long>): Int
  suspend fun checkPlayListExist(name: String): Boolean
}

class PlayListRepoImpl @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val playListDao: PlayListDao,
  private val settingPrefs: SettingPrefs
) : PlayListRepository, AbstractRepository(settingPrefs) {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun allPlayLists(): Flow<List<PlayList>> {
    return settingPrefs
      .playlistSortOrderFlow()
      .flatMapLatest { sortOrder ->
        val orderByKey = when (sortOrder) {
          SortOrder.PLAYLIST_A_Z -> "name"
          SortOrder.PLAYLIST_Z_A -> "name desc"
          SortOrder.PLAYLIST_DATE -> "date"
          else -> "name"
        }

        playListDao.selectAll(orderByKey)
          .map { list ->
            ItemsSorter.sortedPlayLists(list, sortOrder)
          }
      }
  }

  override suspend fun addSongsToPlayList(audioIds: List<Long>, playListName: String): Int {
    val playList = playListDao.selectByName(playListName)
      ?: throw IllegalArgumentException("No Playlist Found")

    // 不重复添加
    val old = playList.audioIds.size
    playList.audioIds.addAll(audioIds.filter { !playList.audioIds.contains(it) })
    val count = playList.audioIds.size - old
    playListDao.update(playList)
    return count
  }

  override suspend fun insertPlayList(name: String) =
    playListDao.insert(PlayList(0, name, ArrayList(), Date().time))

  override suspend fun updatePlayList(playList: PlayList) = playListDao.update(playList)

  override suspend fun deletePlayList(id: Long) = playListDao.delete(id)

  override suspend fun isFavorite(id: Long): Boolean {
    val p = getFavorite()
    return p?.audioIds?.contains(id) == true
  }

  override suspend fun toggleFavorite(id: Long): Boolean {
    val p = getFavorite()
      ?: return false // 如果没有收藏夹则返回

    val favorite = if (p.audioIds.contains(id)) {
      p.audioIds.remove(id)
      false
    } else {
      p.audioIds.add(id)
      true
    }

    playListDao.update(p)
    return favorite
  }

  override suspend fun getFavorite() = playListDao.getFavorite()

  override suspend fun removeAudioIdsFromAll(audioIds: List<Long>): Int {
    if (audioIds.isEmpty()) return 0
    val toRemove = audioIds.toSet()
    val updated = playListDao.selectAll("name").first().mapNotNull { pl ->
      val filtered = pl.audioIds.filterNot { it in toRemove }
      if (filtered.size != pl.audioIds.size) pl.copy(audioIds = ArrayList(filtered)) else null
    }
    return if (updated.isEmpty()) 0 else playListDao.update(updated)
  }

  override suspend fun checkPlayListExist(name: String): Boolean {
    return playListDao.exists(name)
  }
}
