package remix.myplayer.compose.repo.usecase

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.repo.AbstractRepository.Companion.makeInStr
import remix.myplayer.compose.repo.PlayListRepository
import remix.myplayer.compose.repo.SongRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.MusicServiceRemote.deleteFromService
import remix.myplayer.misc.checkWorkerThread
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeleteSongUseCase @Inject constructor(
  private val settingPrefs: SettingPrefs,
  private val songRepo: SongRepository,
  private val playListRepo: PlayListRepository
) {

  suspend operator fun invoke(
    activity: BaseActivity?,
    models: List<APlayerModel>,
    deleteSource: Boolean
  ) =
    withContext(Dispatchers.IO) {
      if (activity == null || models.isEmpty()) {
        return@withContext
      }

      settingPrefs.deleteSource = deleteSource

      val isAllPlaylist = models.all { it is PlayList }
      if (isAllPlaylist) {
        for (model in models) {
          val playList = model as PlayList
          if (playList.isFavorite()) {
            ToastUtil.show(activity, R.string.mylove_cant_edit)
            continue
          }

          playListRepo.deletePlayList(model.id)
        }

        if (!deleteSource) {
          return@withContext
        }
      }

      // TODO 删除了正在播放的歌曲
      val songs = songRepo.getSongsByModels(models)
      val songIds = songs.map { it.id }

      val deleteId: MutableSet<String> = settingPrefs.deleteIds.toMutableSet().apply {
        addAll(songIds.map { it.toString() })
      }

      // save to sp
      settingPrefs.deleteIds = deleteId

      // remove from playQueue
      deleteFromService(songs)

      // remove from all playLists
      playListRepo.allPlayLists().first().forEach {
        it.audioIds.removeAll(songIds)
        playListRepo.updatePlayList(it)
      }

      // delete source if need
      if (deleteSource) {
        deleteSource(activity, songs)
      }

      // refresh ui
      activity.contentResolver.notifyChange(Audio.Media.EXTERNAL_CONTENT_URI, null)
    }

  private fun deleteSource(activity: BaseActivity, songs: List<Song>) {
    checkWorkerThread()

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val uris = songs.map { it.contentUri }
        val sender: IntentSender =
          MediaStore.createDeleteRequest(activity.contentResolver, uris).intentSender
        activity.deleteSongLauncher.launch(IntentSenderRequest.Builder(sender).build())
      } else {
        try {
          val count = activity.contentResolver.delete(
            Audio.Media.EXTERNAL_CONTENT_URI,
            Audio.Media._ID + " in(" + makeInStr(songs.map { it.id }) + ")",
            null
          )
          Timber.v("remove from mediaStore: $count")

          songs.forEach { song ->
            val file = File(song.data)
            if (file.exists() && file.canWrite()) {
              file.delete()
            }
          }

          ToastUtil.show(activity, R.string.delete_success)
        } catch (e: SecurityException) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
            activity.deleteSongLauncher.launch(
              IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            )
            return
          }
          throw e
        }
      }
      Timber.v("delete may success")
    } catch (e: Exception) {
      ToastUtil.show(activity, R.string.delete_error)
      Timber.v("delete failed: $e")
    }

  }
}