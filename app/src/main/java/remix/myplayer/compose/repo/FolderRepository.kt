package remix.myplayer.compose.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.util.PermissionUtil
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

interface FolderRepository {
  fun allFolders(): List<Folder>
}


class FolderRepoImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val songRepo: SongRepository,
  private val settingPrefs: SettingPrefs
) : FolderRepository, AbstractRepository(settingPrefs) {

  override fun allFolders(): List<Folder> {
    if (!PermissionUtil.hasNecessaryPermission()) {
      return Collections.emptyList()
    }

    val songs = songRepo.getSongs(null, null, settingPrefs.songSortOrder)
    val folders: MutableList<Folder> = ArrayList()
    val folderMap: MutableMap<String, MutableList<Song>> = LinkedHashMap()
    try {
      for (song in songs) {
        val parentPath = song.data.substring(0, song.data.lastIndexOf("/"))
        if (folderMap[parentPath] == null) {
          folderMap[parentPath] = ArrayList()
        }
        folderMap[parentPath]?.add(song)
      }

      for ((path, songs) in folderMap) {
        folders.add(Folder(path.substring(path.lastIndexOf("/") + 1), songs.size, path))
      }
    } catch (e: Exception) {
      Timber.v(e)
    }
    return folders
  }
}

