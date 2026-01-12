package remix.myplayer.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.provider.MediaStore.Audio
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
import java.io.File
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
              val audioIds = ArrayList<Long>()
              reader.lineSequence().forEachIndexed { i, path ->
                val entry = "#EXTINF:"
                if (i != 0 && !path.startsWith(entry)) {
                  val file = File(path)
                  val song = if (file.exists() && file.isFile) {
                    libraryVM.loadSong(Audio.Media.DATA + " = ?", arrayOf(path)).firstOrNull()
                  } else {
                    libraryVM.loadSong(
                      Audio.Media.DATA + " like ?",
                      arrayOf("%" + path.replace("\\", "/"))
                    ).firstOrNull()
                  } ?: return@forEachIndexed
                  if (song.id > 0) {
                    audioIds.add(song.id)
                  }
                }
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