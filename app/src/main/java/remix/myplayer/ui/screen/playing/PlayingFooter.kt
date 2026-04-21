package remix.myplayer.ui.screen.playing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_POSITION
import remix.myplayer.service.playback.PlaybackUiState
import remix.myplayer.ui.dialog.BottomSheetDialog
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.Util
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.CenterInBox
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.viewmodel.playbackViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayingFooter(
  modifier: Modifier = Modifier,
  playbackUiState: PlaybackUiState,
  swatch: Palette.Swatch
) {
  val isPortrait = LocalContext.current.isPortraitOrientation()
  val swatchColor = Color(swatch.rgb)
  val activeTint = swatchColor.copy(alpha = 0.72f)
  val inactiveTint = swatchColor.copy(alpha = 0.42f)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        end = if (isPortrait) 16.dp else 36.dp,
        bottom = 4.dp
      ),
    horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
    verticalAlignment = Alignment.CenterVertically
  ) {
    FooterButton(
      iconRes = if (playbackUiState.isFavorite) {
        R.drawable.ic_favorite_filled_24dp
      } else {
        R.drawable.ic_favorite_24dp
      },
      contentDescription = "PlayingFavorite",
      tint = if (playbackUiState.isFavorite) activeTint else inactiveTint,
      buttonSize = if (isPortrait) 40.dp else 36.dp,
      onClick = {
        if (!playbackUiState.song.isLocal()) {
          return@FooterButton
        }
        Util.sendCMDLocalBroadcast(Command.LOVE)
      }
    )

    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    PlayQueueDialog(state, playbackUiState)

    FooterButton(
      iconRes = R.drawable.ic_format_list_bulleted_white_24dp,
      contentDescription = "PlayingPlayQueue",
      tint = activeTint,
      buttonSize = if (isPortrait) 40.dp else 36.dp,
      onClick = {
        scope.launch {
          state.show()
        }
      }
    )
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayQueueDialog(
  state: SheetState,
  musicState: PlaybackUiState
) {
  val scope = rememberCoroutineScope()
  val playbackVM = playbackViewModel
  val playbackState by playbackVM.playbackUiState.collectAsStateWithLifecycle()
  val songs by playbackVM.playQueueSongs.collectAsStateWithLifecycle()

  BottomSheetDialog(state) {
    Column {
      CenterInBox(
        modifier = Modifier
          .height(48.dp)
          .fillMaxWidth()
      ) {
        TextPrimary(
          stringResource(R.string.play_queue, songs.size),
          fontSize = 18.sp,
          textAlign = TextAlign.Center
        )
      }
    }

    val lazyState = rememberLazyListState()
    LazyColumn(state = lazyState) {
      itemsIndexed(songs, key = { _, song -> song.id }) { pos, song ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .height(50.dp)
            .clickWithRipple(false) {
              sendLocalBroadcast(
                makeCmdIntent(Command.PLAY_AT)
                  .putExtra(EXTRA_POSITION, pos)
              )
              scope.launch { state.hide() }
            }) {
          Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .padding(horizontal = 16.dp)
              .weight(1f)
          ) {
            if (!song.valid()) {
              TextPrimary(stringResource(R.string.song_lose_effect))
            } else {
              TextPrimary(
                song.title,
                color = if (song == musicState.song) LocalTheme.current.secondary else LocalTheme.current.textPrimary
              )
              TextSecondary(song.artist)
            }
          }

          if (song.valid()) {
            CenterInBox(
              modifier = Modifier
                .clickWithRipple {
                  playbackVM.removeFromQueue(song.id)
                }
                .padding(8.dp)
            ) {
              Image(
                painter = painterResource(R.drawable.icon_playqueue_delete),
                contentDescription = "PlayQueueDelete"
              )
            }
          }
        }
      }
    }

    LaunchedEffect(state.isVisible) {
      if (state.isVisible) {
        val index = songs.indexOfFirst { it.id == playbackState.song.id }
        if (index != -1) {
          lazyState.scrollToItem(index)
        }
      }
    }
  }
}

@Composable
private fun FooterButton(
  iconRes: Int,
  contentDescription: String,
  tint: Color,
  buttonSize: Dp,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(buttonSize)
      .clickWithRipple(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Image(
      painter = painterResource(iconRes),
      contentDescription = contentDescription,
      colorFilter = ColorFilter.tint(tint)
    )
  }
}
