package remix.myplayer.compose.ui.widget.library.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.bean.mp3.Album
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.ui.widget.popup.LibraryItemPopupButton

@Composable
fun ListAlbum(modifier: Modifier = Modifier, album: Album) {
  Row(
    modifier = modifier
      .combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = LocalTheme.current.ripple),
        onClick = {},
        onLongClick = {}
      )
      .background(LocalTheme.current.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlideCover(
      modifier = Modifier
        .padding(start = 8.dp)
        .size(42.dp),
      model = album,
      circle = false
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(horizontal = 8.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      TextPrimary(text = album.album)
      TextSecondary(text = stringResource(R.string.song_count_2, album.artist, album.count))
    }

    LibraryItemPopupButton(model = album)

  }
}
