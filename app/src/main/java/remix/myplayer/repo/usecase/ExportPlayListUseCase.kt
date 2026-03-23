package remix.myplayer.repo.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.repo.SongRepository
import remix.myplayer.ui.nav.MessageNotifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportPlayListUseCase @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val songRepo: SongRepository
) {

  suspend operator fun invoke(playList: PlayList?, uri: Uri) =
    withContext(Dispatchers.IO) {
      if (playList == null) {
        return@withContext
      }

      val header = "#EXTM3U"
      val entry = "#EXTINF:"
      val sep = ","

      val songs = songRepo.getSongsByModels(listOf(playList))
      try {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter().use { bw ->
          if (bw == null) throw IllegalStateException("openOutputStream failed")
          bw.write(header)
          for (song in songs) {
            bw.newLine()
            bw.write(entry + song.duration + sep + song.artist + " - " + song.title)
            bw.newLine()
            bw.write(song.data)
          }
        }
        MessageNotifier.show(R.string.export_success)
      } catch (e: Exception) {
        MessageNotifier.show(R.string.export_fail, e.toString())
      }
    }
}
