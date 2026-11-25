package remix.myplayer.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.prefs.PrefKeys
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.observe
import remix.myplayer.helper.ItemsSorter
import javax.inject.Inject

interface FolderRepository { fun observeFolders(): Flow<List<Folder>> }

class FolderRepoImpl @Inject constructor(
  private val songs: SongRepository,
  private val settings: SettingPrefs
) : FolderRepository {
  override fun observeFolders(): Flow<List<Folder>> = songs.observeSongs().map { songs ->
    songs.groupBy { it.data.substringBeforeLast('/', "") }.map { (id, tracks) ->
      Folder(id.substringAfterLast('/'), tracks.size, id)
    }
  }.combine(settings.observe(PrefKeys.Setting.FOLDER_SORT_ORDER) { folderSortOrder }) { items, order ->
    ItemsSorter.sortedFolders(items, order)
  }.flowOn(Dispatchers.Default)
}
