package remix.myplayer.ui.screen.playing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.ui.nav.DetailScreenRoute
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.albumDetailRoute
import remix.myplayer.ui.nav.artistDetailRoute
import remix.myplayer.ui.nav.openSongLibraryDetail
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.popupButton
import remix.myplayer.util.ext.clickWithRipple

@Composable
@Stable
internal fun PlayingTopBar(song: Song, swatch: Palette.Swatch) {
  val nav = LocalNavController.current
  val theme = LocalTheme.current
  val titleColor = if (theme.isLight) Color(swatch.titleTextColor) else theme.textPrimary
  val bodyColor = if (theme.isLight) Color(swatch.bodyTextColor) else theme.textSecondary
  val tintColor = if (theme.isLight) titleColor else theme.popupButton()

  Row(modifier = Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .padding(8.dp)
        .size(40.dp)
        .clickWithRipple {
          nav.popBackStack()
        }
    ) {
      Image(
        painter = painterResource(R.drawable.icon_player_back),
        colorFilter = ColorFilter.tint(tintColor),
        contentDescription = "PlayingBack"
      )
    }

    Column(
      modifier = Modifier
        .weight(1f),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val title = song.title
      val artist = song.artist
      val album = song.album

      val detail = buildAnnotatedString {
        fun appendDetail(label: String, tag: String, route: DetailScreenRoute?) {
          if (route == null) {
            append(label)
          } else {
            // An explicit empty style prevents Material's default link color and underline.
            withLink(LinkAnnotation.Clickable(tag, styles = TextLinkStyles()) {
              nav.openSongLibraryDetail(route)
            }) {
              append(label)
            }
          }
        }

        appendDetail(artist, "artist", song.artistDetailRoute())
        if (artist.isNotEmpty() && album.isNotEmpty()) append("-")
        appendDetail(album, "album", song.albumDetailRoute())
      }

      Text(
        title.ifEmpty { stringResource(R.string.unknown_song) },
        color = titleColor,
        fontSize = 16.sp,
        maxLines = 1
      )
      Text(detail, color = bodyColor, fontSize = 14.sp, maxLines = 1, textAlign = TextAlign.Center)
    }

    var expanded by remember { mutableStateOf(false) }

    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .padding(8.dp)
        .size(40.dp)
        .clickWithRipple {
          expanded = !expanded
        }
    ) {
      Image(
        painter = painterResource(R.drawable.icon_player_more),
        colorFilter = ColorFilter.tint(tintColor),
        contentDescription = "PlayingMore"
      )

      PlayingDropDownMenu(expanded, song) {
        expanded = false
      }
    }
  }
}
