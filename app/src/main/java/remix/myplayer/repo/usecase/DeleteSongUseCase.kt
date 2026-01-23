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
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.repo.AbstractRepository.Companion.makeInStrQuery
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
  private val songRepo: SongRepository,
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

      if (parent is PlayList) { // delete songs in playlist
        val audioIds = models.map {
          if (it is Song.Local) {
            it.id
          } else {
            -1
          }
        }
        parent.audioIds.removeAll(audioIds.toSet())

        playListRepo.updatePlayList(parent)

        if (!deleteSource) {
          activity.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
          return@withContext
        }
      } else if (models.all { it is PlayList }) { // delete playlists
        for (model in models) {
          val playList = model as PlayList
          if (playList.isFavorite()) {
            MessageNotifier.show(R.string.mylove_cant_edit)
            continue
          }

          playListRepo.deletePlayList(model.id)
        }

        if (!deleteSource) {
          return@withContext
        }
      }

      val songs = withContext(Dispatchers.IO) {
        songRepo.getSongsByModels(models)
      }
      val songIds = songs.map { it.id }

      if (songs.isNotEmpty()) {
        val deleteId: MutableSet<String> = settingPrefs.deleteIds.toMutableSet().apply {
          addAll(songIds.map { it.toString() })
        }

        // save to sp
        settingPrefs.deleteIds = deleteId

        // remove from playQueue
        MusicServiceRemote.removeFromQueue(songIds)

        // remove from all playLists
        playListRepo.removeAudioIdsFromAll(songIds)

        // delete source if need
        if (deleteSource) {
          deleteSource(activity, songs)
        }
      } else {
        MessageNotifier.show(R.string.delete_success)
      }

      // refresh ui
      activity.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
    }

  private suspend fun deleteSource(activity: BaseActivity, songs: List<Song>) = withContext(Dispatchers.IO){
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val uris = songs.map { it.contentUri }
        val sender: IntentSender =
          MediaStore.createDeleteRequest(activity.contentResolver, uris).intentSender
        activity.deleteSongLauncher.launch(IntentSenderRequest.Builder(sender).build())
      } else {
        // TODO 测试低版本
        try {
          val count = activity.contentResolver.delete(
            Audio.Media.EXTERNAL_CONTENT_URI,
            makeInStrQuery(songs.map { it.id }),
            null
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