package remix.myplayer.ui.screen.crop

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.util.ext.clickableWithoutRipple


@Composable
fun CropScreen(
  destinationUri: Uri,
  onCropSuccess: () -> Unit,
  onCancel: () -> Unit
) {
  val context = LocalContext.current
  BackHandler(onBack = onCancel)

  var pickUri by rememberSaveable {
    mutableStateOf(Uri.EMPTY)
  }
  val scope = rememberCoroutineScope()

  val state = rememberImageCropperState(uri = pickUri.toString())

  Scaffold(
    topBar = {
      CommonAppBar(stringResource(R.string.back), onBack = onCancel, actions = emptyList())
    },
    floatingActionButton = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(color = LocalTheme.current.secondary, shape = CircleShape)
          .clickableWithoutRipple {
            scope.launch {
              val success = state.crop(context, saveUri = destinationUri)
              if (success == true) {
                onCropSuccess()
              } else {
                onCancel()
              }
            }
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painterResource(R.drawable.ic_save_white_24dp),
          contentDescription = "CustomSortSave",
          tint = Color.White
        )
      }
    }) { contentPadding ->
    ImageCropper(
      state, modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(bottom = 72.dp)
    )
  }

  val pickLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    if (uri != null) {
      pickUri = uri
      state.load(pickUri)
    } else {
      onCancel()
    }
  }
  LaunchedEffect(Unit) {
    pickLauncher.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
  }
}