package remix.myplayer.ui.screen.playing

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_LOOP
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_REPEAT
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_SHUFFLE
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_POSITION
import remix.myplayer.service.playback.PlaybackUiState
import remix.myplayer.ui.dialog.BottomDialog
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.playpause.PlayPauseView
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.Util
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.CenterInBox
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.viewmodel.playbackViewModel

private val itemRes = mapOf(
  MODE_LOOP to Pair(R.drawable.play_btn_loop, R.string.model_normal),
  MODE_SHUFFLE to Pair(R.drawable.play_btn_shuffle, R.string.model_random),
  MODE_REPEAT to Pair(R.drawable.play_btn_loop_one, R.string.model_repeat)
)

@Composable
internal fun PlayingControl(
  modifier: Modifier = Modifier,
  playbackUiState: PlaybackUiState,
  swatch: Palette.Swatch,
  iconSize: Dp? = null,
  playPauseSize: Dp = 56.dp,
  buttonSize: Dp? = null
) {
  Row(
    modifier = modifier
      .fillMaxSize(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val swatchColor = Color(swatch.rgb)
    val playMode = playbackUiState.playMode
    val viewModel = playbackViewModel
    val iconModifier = if (iconSize != null) Modifier.size(iconSize) else Modifier
    ControlButton(
      onClick = {
        val newMode = if (playMode == MODE_REPEAT) MODE_LOOP else playMode + 1
        MessageNotifier.show(itemRes[newMode]!!.second)
        Util.sendCMDLocalBroadcast(Command.CHANGE_MODEL)
      },
      buttonSize = buttonSize
    ) {
      Image(
        modifier = iconModifier,
        painter = painterResource(itemRes[playMode]!!.first),
        contentDescription = "PlayingMode",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }

    ControlButton(
      onClick = {
        sendLocalBroadcast(
          Intent(MusicService.ACTION_CMD).putExtra(
            MusicService.EXTRA_COMMAND,
            Command.SKIP_TO_PREVIOUS
          )
        )
      },
      onLongPressStart = {
        viewModel.startContinuousSeek(forward = false)
      },
      onLongPressEnd = {
        viewModel.stopContinuousSeek()
      },
      buttonSize = buttonSize
    ) {
      Image(
        modifier = iconModifier,
        painter = painterResource(R.drawable.play_btn_pre),
        contentDescription = "PlayingPrev",
        colorFilter = ColorFilter.tint(swatchColor)
      )
    }

    ControlButton(
      onClick = {
        sendLocalBroadcast(
          Intent(MusicService.ACTION_CMD).putExtra(
            MusicService.EXTRA_COMMAND,
            Command.PLAY_PAUSE
          )
        )
      },
      buttonSize = buttonSize
    ) {
      val size = with(LocalDensity.current) { playPauseSize.roundToPx() }
      AndroidView(
        factory = {
          PlayPauseView(it).apply {
            setBackgroundColor(Color.Transparent.toArgb())
            layoutParams = ViewGroup.LayoutParams(size, size)
          }
        },
        update = {
          it.setBackgroundColor(swatch.rgb)
          if (it.layoutParams.width != size || it.layoutParams.height != size) {
            it.layoutParams = ViewGroup.LayoutParams(size, size)
          }
          it.updateState(playbackUiState.isPlaying, true)
        }
      )
    }

    ControlButton(
      onClick = {
        sendLocalBroadcast(
          Intent(MusicService.ACTION_CMD).putExtra(
            MusicService.EXTRA_COMMAND,
            Command.SKIP_TO_NEXT
          )
        )
      },
      onLongPressStart = {
        viewModel.startContinuousSeek(forward = true)
      },
      onLongPressEnd = {
        viewModel.stopContinuousSeek()
      },
      buttonSize = buttonSize
    ) {
      Image(
        modifier = iconModifier,
        painter = painterResource(R.drawable.play_btn_next),
        contentDescription = "PlayingNext",
        colorFilter = ColorFilter.tint(swatchColor)
      )
    }

    var showPlayQueue by remember { mutableStateOf(false) }
    PlayQueueDialog(
      visible = showPlayQueue,
      onDismissRequest = { showPlayQueue = false },
      musicState = playbackUiState
    )

    ControlButton(
      onClick = {
        showPlayQueue = true
      },
      buttonSize = buttonSize
    ) {
      Image(
        modifier = iconModifier,
        painter = painterResource(R.drawable.play_btn_normal_list),
        contentDescription = "PlayingPlayQueue",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }
  }
}

@Composable
private fun PlayQueueDialog(
  visible: Boolean,
  onDismissRequest: () -> Unit,
  musicState: PlaybackUiState
) {
  val playbackVM = playbackViewModel
  val playbackState by playbackVM.playbackUiState.collectAsStateWithLifecycle()
  val songs by playbackVM.playQueueSongs.collectAsStateWithLifecycle()

  BottomDialog(
    visible = visible,
    onDismissRequest = onDismissRequest
  ) {
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
                onDismissRequest()
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

      LaunchedEffect(visible) {
        if (visible) {
          val index = songs.indexOfFirst { it.id == playbackState.song.id }
          if (index != -1) {
            lazyState.scrollToItem(index)
          }
        }
      }
    }
  }
}

@Composable
private fun RowScope.ControlButton(
  onClick: () -> Unit,
  buttonSize: Dp? = null,
  onLongPressStart: (() -> Unit)? = null,
  onLongPressEnd: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val onClick by rememberUpdatedState(onClick)
  val onLongPressStart by rememberUpdatedState(onLongPressStart)
  val onLongPressEnd by rememberUpdatedState(onLongPressEnd)
  var longPressActive by remember { mutableStateOf(false) }

  LaunchedEffect(interactionSource) {
    interactionSource.interactions.collect { interaction ->
      if ((interaction is PressInteraction.Release || interaction is PressInteraction.Cancel) &&
        longPressActive
      ) {
        longPressActive = false
        onLongPressEnd?.invoke()
      }
    }
  }

  val modifier = if (onLongPressStart != null && onLongPressEnd != null) {
    Modifier
      .clip(CircleShape)
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(color = LocalTheme.current.ripple),
        onClick = { onClick() },
        onLongClick = {
          longPressActive = true
          onLongPressStart?.invoke()
        }
      )
  } else {
    Modifier.clickWithRipple { onClick() }
  }
  val isPortrait = LocalContext.current.isPortraitOrientation()
  val buttonModifier = if (buttonSize != null) {
    modifier
      .weight(1f, isPortrait)
      .size(buttonSize)
  } else {
    modifier
      .weight(1f, isPortrait)
      .aspectRatio(1f)
  }

  Box(
    modifier = buttonModifier,
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}
