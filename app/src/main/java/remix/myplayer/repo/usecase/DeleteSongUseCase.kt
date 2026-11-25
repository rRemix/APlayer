package remix.myplayer.repo.usecase

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.repo.SongRepository
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteSongUseCase @Inject constructor(
  private val settingPrefs: SettingPrefs,
  private val songRepository: SongRepository,
  private val playListRepo: PlayListRepository,
  private val playQueueRepo: PlayQueueRepository
) {

  suspend operator fun invoke(
    activity: BaseActivity?,
    models: List<APlayerModel>,
    deleteSource: Boolean,
    parent: APlayerModel?
  ) =
    withContext(Dispatchers.Main) {
      if (activity == null || models.isEmpty()) {
        return@withContext
      }

      settingPrefs.deleteSource = deleteSource

      val songs = removePlaylistReferences(models, deleteSource, parent) {
        MessageNotifier.show(R.string.mylove_cant_edit)
      }
      if (!deleteSource && (parent is PlayList || models.all { it is PlayList })) {
        if (parent is PlayList) activity.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
        return@withContext
      }

      val songIds = songs.map { it.songIdentity }

      if (songs.isNotEmpty()) {
        val deleteId: MutableSet<String> = settingPrefs.deleteIds.toMutableSet().apply {
          addAll(songs.mapNotNull { it.mediaStoreId?.toString() })
        }

        // save to sp
        settingPrefs.deleteIds = deleteId

        // remove from playQueue
        MusicServiceRemote.removeFromQueue(songIds)

        // remove from all playLists
        playListRepo.removeSongsFromAll(songIds)

        // delete source if need
        if (deleteSource) {
          deleteSource(activity, songs.filter { it.isLocal() })
        }
      } else {
        MessageNotifier.show(R.string.delete_success)
      }

      // refresh ui
      activity.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
    }

  /** Capture selected songs before removing the playlist rows and their cascaded tracks. */
  internal suspend fun removePlaylistReferences(
    models: List<APlayerModel>,
    deleteSource: Boolean,
    parent: APlayerModel?,
    onFavoriteProtected: () -> Unit
  ): List<Song> {
    // A protected favorite must not leak into the subsequent source-file deletion either.
    val targets = models.filterNot { it is PlayList && it.isFavorite() }
    val songs = if (deleteSource || (parent !is PlayList && !models.all { it is PlayList })) {
      withContext(Dispatchers.IO) { songRepository.getSongs(targets) }
        .distinctBy { it.songIdentity }
    } else {
      emptyList()
    }
    if (parent is PlayList) {
      val keys = models.filterIsInstance<Song>().mapTo(hashSetOf()) { it.songIdentity }
      playListRepo.removeSongs(parent.id, keys)
    } else if (models.all { it is PlayList }) {
      models.filterIsInstance<PlayList>().forEach { playlist ->
        if (playlist.isFavorite()) onFavoriteProtected() else playListRepo.deletePlayList(playlist.id)
      }
    }
    return songs
  }

  private suspend fun deleteSource(activity: BaseActivity, songs: List<Song>) = withContext(Dispatchers.IO){
    if (songs.isEmpty()) return@withContext
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val uris = songs.map { it.contentUri }
        val sender: IntentSender =
          MediaStore.createDeleteRequest(activity.contentResolver, uris).intentSender
        activity.deleteSongLauncher.launch(IntentSenderRequest.Builder(sender).build())
      } else {
        // TODO 测试低版本
        try {
          val ids = songs.mapNotNull { it.mediaStoreId }
          if (ids.isEmpty()) return@withContext
          val count = activity.contentResolver.delete(
            Audio.Media.EXTERNAL_CONTENT_URI,
            "${Audio.Media._ID} IN (${ids.joinToString { "?" }})",
            ids.map { it.toString() }.toTypedArray()
          )
          Timber.v("remove from mediaStore: $count")

          songs.forEach { song ->
            val file = File(song.data)
            if (file.exists() && file.canWrite()) {
              file.delete()
            }
          }

          MessageNotifier.show(R.string.delete_success)
        } catch (e: SecurityException) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            activity.deleteSongLauncher.launch(
              IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            )
            return@withContext
          }
          throw e
        }
      }
      Timber.v("delete may success")
    } catch (e: Exception) {
      MessageNotifier.show(R.string.delete_error)
      Timber.v("delete failed: $e")
    }
  }
}
