package remix.myplayer.ui.screen.setting.logic.common

import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.repo.SongQuery
import remix.myplayer.repo.PathMatch
import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ImportPlayListLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settingVM = settingViewModel
  val libraryVM = libraryViewModel

  val chooseM3ULauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        scope.launch(Dispatchers.IO) {
          val stream = try {
            context.contentResolver.openInputStream(uri)
          } catch (e: SecurityException) {
            MessageNotifier.show(R.string.import_fail, e.message ?: e.toString())
            null
          } ?: return@launch
          stream.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
              val audioIds = ArrayList<SongIdentity>()
              reader.lineSequence().forEach { line ->
                val path = line.trim().replace('\\', '/')
                if (path.isEmpty() || path.startsWith("#")) return@forEach
                val query = SongQuery(path = path)
                val exact = libraryVM.loadSongs(query)
                val matches = if (exact.isNotEmpty()) exact else
                  libraryVM.loadSongs(query.copy(pathMatch = PathMatch.SUFFIX))
                // A basename can refer to several files; never silently import an arbitrary one.
                val song = matches.singleOrNull() ?: return@forEach
                if (song.valid()) audioIds.add(song.songIdentity)
              }
              if (audioIds.isNotEmpty()) {
                settingVM.showAddSongToPlayListDialog(
                  audioIds,
                  DocumentFile.fromSingleUri(context, uri)?.name?.removeSuffix(".m3u") ?: ""
                )
              }
            }
          }
        }
      }
    }

  NormalPreference(
    stringResource(R.string.playlist_import),
    stringResource(R.string.playlist_import_tip)
  ) {
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
      putExtra(EXTRA_ALLOW_MULTIPLE, true)
      type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
      addCategory(Intent.CATEGORY_OPENABLE)
    }
    if (intent.resolveActivity(context.packageManager) == null) {
      MessageNotifier.show(R.string.activity_not_found_tip)
      return@NormalPreference
    }
    chooseM3ULauncher.launch(intent)
  }
}