package remix.myplayer.ui.screen.playing

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.popupButton
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.PlayingScreenValue
import remix.myplayer.viewmodel.mainViewModel

@Composable
@Stable
internal fun PlayingTopBar(song: Song, swatch: Palette.Swatch) {
  val mainVM = mainViewModel
  val scope = rememberCoroutineScope()
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
          scope.launch {
            mainVM.playingScreenState.animateTo(PlayingScreenValue.Hidden)
          }
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

      val detail = when {
        artist == "" -> {
          song.album
        }

        album == "" -> {
          song.artist
        }

        else -> {
          String.format("%s-%s", song.artist, song.album)
        }
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
