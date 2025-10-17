package remix.myplayer.compose.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.db.room.dao.PlayListDao
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.ItemsSorter
import java.util.Date
import javax.inject.Inject

interface PlayListRepository {
  fun allPlayLists(): Flow<List<PlayList>>
  suspend fun addSongsToPlayList(audioIds: List<Long>, playListName: String): Int
  suspend fun insertPlayList(name: String): Long
  suspend fun updatePlayList(playList: PlayList): Int
  suspend fun deletePlayList(id: Long): Int
  suspend fun isFavorite(id: Long): Boolean
  suspend fun toggleFavorite(id: Long)
  suspend fun getFavorite(): PlayList?
  suspend fun removeAudioIdsFromAll(audioIds: List<Long>): Int
}

class PlayListRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val playListDao: PlayListDao,
  private val settingPrefs: SettingPrefs
) : PlayListRepository, AbstractRepository(settingPrefs) {

  override fun allPlayLists(): Flow<List<PlayList>> {
//    if (!PermissionUtil.hasNecessaryPermission()) {
//      return flow { emptyList<PlayList>() }
//    }

    var sortOrder = settingPrefs.playlistSortOrder
    sortOrder = when (sortOrder) {
      SortOrder.PLAYLIST_A_Z -> {
        "name"
      }

      SortOrder.PLAYLIST_Z_A -> {
        "name DESC"
      }

      else -> {
        sortOrder
      }
    }
    val playlists = playListDao.selectAllOrderBy(orderBy = sortOrder)

    return playlists.map {
      if (forceSort) {
        ItemsSorter.sortedPlayLists(it, sortOrder)
      } else {
        it
      }
    }
  }

  override suspend fun addSongsToPlayList(audioIds: List<Long>, playListName: String): Int {
    val playList = playListDao.selectByName(playListName)
      ?: throw IllegalArgumentException("No Playlist Found")

    // 不重复添加
    val old = playList.audioIds.size
    playList.audioIds.addAll(audioIds)
    val count = playList.audioIds.size - old
    playListDao.update(playList)
    return count
  }

  override suspend fun insertPlayList(name: String) =
    playListDao.insert(PlayList(0, name, ArrayList<Long>(), Date().time))

  override suspend fun updatePlayList(playList: PlayList) = playListDao.update(playList)

  override suspend fun deletePlayList(id: Long) = playListDao.delete(id)

  override suspend fun isFavorite(id: Long): Boolean {
    val p = getFavorite()
    return p?.audioIds?.contains(id) == true
  }

  override suspend fun toggleFavorite(id: Long) {
    val p = getFavorite()
      ?: return // 如果没有收藏夹则返回

    if (p.audioIds.contains(id)) {
      p.audioIds.remove(id)
    } else {
      p.audioIds.add(id)
    }

    playListDao.update(p)

  }

  override suspend fun getFavorite() = playListDao.getFavorite()

  override suspend fun removeAudioIdsFromAll(audioIds: List<Long>): Int {
    var totalUpdated = 0
    audioIds.forEach {
      totalUpdated += playListDao.removeAudioIdFromAll(it)
    }
    return totalUpdated
  }
}