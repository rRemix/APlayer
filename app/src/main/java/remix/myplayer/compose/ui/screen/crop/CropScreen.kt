package remix.myplayer.compose.ui.screen.crop

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.CenterInBox
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.CommonAppBar
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.util.Constants
import java.io.File

@Composable
fun CropScreen(id: Long, type: Int) {
  val context = LocalContext.current
  val libraryVM = libraryViewModel
  val nav = LocalNavController.current
  val theme = LocalTheme.current
  var pickUri by rememberSaveable {
    mutableStateOf(Uri.EMPTY)
  }
  val scope = rememberCoroutineScope()

  val destination: Uri? = remember(id, type) {
    val cacheDir = DiskCache.getDiskCacheDir(context, "thumbnail")
    if (!cacheDir.exists() && !cacheDir.mkdir()) {
      null
    }
    val file = File(cacheDir, "$type-${id}.jpg")
    Uri.fromFile(file)
  }

  val state = rememberImageCropperState(uri = pickUri.toString())

  Scaffold(topBar = {
    CommonAppBar(stringResource(R.string.back), actions = emptyList())
  }) { contentPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
    ) {

      ImageCropper(
        state, modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )

      Row(
        modifier = Modifier
          .height(48.dp)
      ) {
        CenterInBox(
          modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .clickWithRipple(false) {
              nav.popBackStack()
            }
        ) {
          Text(stringResource(R.string.cancel), fontSize = 16.sp, color = theme.textSecondary)
        }
        CenterInBox(
          Modifier
            .fillMaxHeight()
            .weight(1f)
            .background(theme.primary)
            .clickWithRipple(false) {
              scope.launch {
                val success = state.crop(context, saveUri = destination ?: return@launch)
                if (success == true) {
                  libraryVM.fetchMedia(
                    clear = true,
                    updateAlbumVersion = type == Constants.ALBUM,
                    updateArtistVersion = type == Constants.ARTIST,
                    updatePlayListVersion = type == Constants.PLAYLIST,
                  )
                }
                nav.popBackStack()
              }
            }) {
          Text(stringResource(R.string.confirm), fontSize = 16.sp, color = theme.textPrimary)
        }
      }
    }
  }

  val pickLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      pickUri = uri
    }
  }
  LaunchedEffect(Unit) {
    pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
  }
}