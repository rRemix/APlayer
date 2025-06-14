package remix.myplayer.compose.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import remix.myplayer.compose.prefs.Setting
import remix.myplayer.db.room.dao.PlayListDao
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.SortOrder
import remix.myplayer.util.ItemsSorter
import remix.myplayer.util.PermissionUtil
import java.util.Date
import javax.inject.Inject

interface PlayListRepository {
  fun allPlayLists(): Flow<List<PlayList>>
  suspend fun addSongsToPlayList(audioIds: List<Long>, playlistId: Long): Int
  suspend fun insertPlayList(name: String): Long
}

class PlayListRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val playListDao: PlayListDao,
  private val setting: Setting
) : PlayListRepository, AbstractRepository(setting) {

  override fun allPlayLists(): Flow<List<PlayList>> {
//    if (!PermissionUtil.hasNecessaryPermission()) {
//      return flow { emptyList<PlayList>() }
//    }

    var sortOrder = setting.playlistSortOrder
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

  override suspend fun addSongsToPlayList(audioIds: List<Long>, playlistId: Long): Int {
    val playList = playListDao.selectByIdSuspend(playlistId) ?: throw IllegalArgumentException("No Playlist Found")

    //不重复添加
    val old = playList.audioIds.size
    playList.audioIds.addAll(audioIds)
    val count = playList.audioIds.size - old
    playListDao.updateSuspend(playList)
    return count
  }

  override suspend fun insertPlayList(name: String): Long {
    return playListDao.insertPlayListSuspend(PlayList(0, name, LinkedHashSet(), Date().time))
  }
}