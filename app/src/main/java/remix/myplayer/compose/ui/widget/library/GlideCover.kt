package remix.myplayer.compose.ui.widget.library

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.glide.UriFetcher.albumVersion
import remix.myplayer.glide.UriFetcher.artistVersion
import remix.myplayer.glide.UriFetcher.playListVersion

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GlideCover(
  modifier: Modifier = Modifier,
  model: APlayerModel,
  circle: Boolean = true,
  album: Boolean = true
) {
  var coverModifier = modifier
  if (circle) {
    coverModifier = modifier.clip(CircleShape)
  }
  val placeHolder = if (album) LocalTheme.current.albumPlaceHolder else LocalTheme.current.artistPlaceHolder
  GlideImage(
    model = model,
    failure = placeholder(placeHolder),
    loading = placeholder(placeHolder),
    contentDescription = null,
    contentScale = ContentScale.Crop,
    modifier = coverModifier
  ) {
    return@GlideImage when (model) {
      is Artist -> it.signature(ObjectKey(artistVersion))
      is PlayList -> it.signature(ObjectKey(playListVersion))
      else -> it
        .signature(ObjectKey(albumVersion))
    }
  }
}