package remix.myplayer.ui.screen.crop

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.ui.theme.LocalTheme

class ImageCropState(private val progressColor: Int) {

  internal var uri: Uri = Uri.EMPTY

  private var cropView by mutableStateOf<CropImageView?>(null)

//  var aspectRatio by mutableStateOf(1 to 1)
  var isAutoZoomEnabled by mutableStateOf(true)
//  var isFixAspectRatio by mutableStateOf(true)
  var cropShape by mutableStateOf(CropImageView.CropShape.RECTANGLE)
  var guidelines by mutableStateOf(CropImageView.Guidelines.ON)

  internal fun load(newUri: Uri) {
    uri = newUri
    if (cropView?.imageUri != uri) {
      cropView?.setImageUriAsync(uri)
    }
  }

  internal fun applyConfig() {
//    cropView?.setAspectRatio(aspectRatio.first, aspectRatio.second)
//    cropView?.setFixedAspectRatio(isFixAspectRatio)
    cropView?.isAutoZoomEnabled = isAutoZoomEnabled
    cropView?.cropShape = cropShape
    cropView?.guidelines = guidelines
    cropView?.setImageCropOptions(CropImageOptions(progressBarColor = progressColor))
  }

  suspend fun crop(
    context: Context,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 90,
    saveUri: Uri
  ) = withContext(Dispatchers.IO) {
    cropView?.getCroppedImage()?.let { bitmap ->
      context.contentResolver.openOutputStream(saveUri)?.use { stream ->
        return@let bitmap.compress(format, quality, stream)
      }
    }
  }

  internal fun viewFactory(context: Context): View {
    val layout = FrameLayout(context).apply {
      layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    val cropView = CropImageView(context).apply {
      this@ImageCropState.cropView = this
    }
    layout.addView(
      cropView,
      LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    )

    return layout
  }

  internal fun viewUpdate(view: View) {
    load(uri)
    applyConfig()
  }

  internal fun viewRelease(view: View) {
    cropView?.clearImage()
    cropView = null
  }
}

@Composable
fun rememberImageCropperState(
  progressColor: Int = LocalTheme.current.primary.toArgb(),
  uri: String? = null
): ImageCropState {
  val state = remember { ImageCropState(progressColor) }
  if (uri != null) {
    LaunchedEffect(uri) {
      state.load(uri.toUri())
    }

    LaunchedEffect(
//      state.aspectRatio,
      state.isAutoZoomEnabled,
      state.cropShape,
      state.guidelines
    ) {
      state.applyConfig()
    }
  }
  return state
}

@Composable
fun ImageCropper(
  state: ImageCropState,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier) {
    AndroidView(
      modifier = Modifier
        .fillMaxSize(),
      factory = state::viewFactory,
      update = state::viewUpdate,
      onRelease = state::viewRelease
    )
  }
}