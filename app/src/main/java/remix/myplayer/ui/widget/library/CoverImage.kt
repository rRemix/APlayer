package remix.myplayer.ui.widget.library

import android.graphics.Bitmap
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import remix.myplayer.coil.coverUriResolver
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.ui.theme.LocalTheme

@Composable
fun rememberCoverRevision(model: APlayerModel): String {
  val context = LocalContext.current
  val resolver = remember(context) { context.coverUriResolver() }
  val key = resolver.cacheKey(model)
  val revisions = remember(resolver, key) { resolver.observeMemoryRevision(model) }
  val memoryVersion by revisions.collectAsStateWithLifecycle(initialValue = resolver.memoryRevision(model))
  return memoryVersion
}

@Composable
fun rememberCoverRequest(model: APlayerModel, allowHardware: Boolean = true): ImageRequest {
  val context = LocalContext.current
  val resolver = remember(context) { context.coverUriResolver() }
  val memoryVersion = rememberCoverRevision(model)
  return remember(context, model, memoryVersion, allowHardware) {
    resolver.request(model, memoryVersion).allowHardware(allowHardware).build()
  }
}

@Composable
fun CoverImage(
  modifier: Modifier = Modifier,
  model: APlayerModel,
  circle: Boolean = true,
  album: Boolean = true,
  onBitmap: ((Bitmap?) -> Unit)? = null
) {
  val request = rememberCoverRequest(model, allowHardware = onBitmap == null)
  val currentRequest by rememberUpdatedState(request)
  val currentCallback by rememberUpdatedState(onBitmap)
  val placeHolder =
    if (album) LocalTheme.current.albumPlaceHolder else LocalTheme.current.artistPlaceHolder
  AsyncImage(
    model = request,
    placeholder = painterResource(placeHolder),
    error = painterResource(placeHolder),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = if (circle) modifier.clip(CircleShape) else modifier,
    onLoading = { currentCallback?.invoke(null) },
    onSuccess = {
      if (it.result.request.memoryCacheKey == currentRequest.memoryCacheKey) {
        currentCallback?.invoke(it.result.image.toBitmap())
      }
    },
    onError = { currentCallback?.invoke(null) }
  )
}
