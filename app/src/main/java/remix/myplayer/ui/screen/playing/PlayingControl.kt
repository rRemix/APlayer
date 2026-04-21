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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.palette.graphics.Palette
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs.Companion.REPEAT_MODE_OFF
import remix.myplayer.data.prefs.SettingPrefs.Companion.REPEAT_MODE_ONE
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.playback.PlaybackUiState
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.playpause.PlayPauseView
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.viewmodel.playbackViewModel

@Composable
internal fun PlayingControl(
  modifier: Modifier = Modifier,
  playbackUiState: PlaybackUiState,
  swatch: Palette.Swatch
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val activeColor = Color(swatch.rgb).copy(0.5f)
    val inactiveColor = activeColor.copy(0.12f)
    val repeatMode = playbackUiState.repeatMode
    val shuffleEnabled = playbackUiState.shuffleEnabled
    val viewModel = playbackViewModel

    ControlButton(onClick = {
      sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_COMMAND,
          Command.TOGGLE_REPEAT
        )
      )
    }) {
      Image(
        painter = painterResource(if (repeatMode == REPEAT_MODE_ONE) {
          R.drawable.ic_repeat_one_24dp
        } else {
          R.drawable.ic_repeat_24dp
        }),
        contentDescription = "PlayingRepeat",
        colorFilter = ColorFilter.tint(
          modeTint(enabled = repeatMode != REPEAT_MODE_OFF, activeColor, inactiveColor)
        )
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
      }
    ) {
      Image(
        painter = painterResource(R.drawable.play_btn_pre),
        contentDescription = "PlayingPrev",
        colorFilter = ColorFilter.tint(activeColor)
      )
    }

    ControlButton(onClick = {
      sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_COMMAND,
          Command.PLAY_PAUSE
        )
      )
    }) {
      val size = with(LocalDensity.current) { 56.dp.roundToPx() }
      AndroidView(
        factory = {
          PlayPauseView(it).apply {
            setBackgroundColor(Color.Transparent.toArgb())
            layoutParams = ViewGroup.LayoutParams(size, size)
          }
        },
        update = {
          it.setBackgroundColor(swatch.rgb)
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
      }
    ) {
      Image(
        painter = painterResource(R.drawable.play_btn_next),
        contentDescription = "PlayingNext",
        colorFilter = ColorFilter.tint(activeColor)
      )
    }

    ControlButton(onClick = {
      sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_COMMAND,
          Command.TOGGLE_SHUFFLE
        )
      )
    }) {
      Image(
        painter = painterResource(R.drawable.ic_shuffle_white_24dp),
        contentDescription = "PlayingShuffle",
        colorFilter = ColorFilter.tint(
          modeTint(enabled = shuffleEnabled, activeColor, inactiveColor)
        )
      )
    }
  }
}

private fun modeTint(enabled: Boolean, activeColor: Color, inactiveTint: Color): Color {
  return if (enabled) activeColor else inactiveTint
}

@Composable
private fun RowScope.ControlButton(
  onClick: () -> Unit,
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

  Box(
    modifier = modifier
      .weight(1f, LocalContext.current.isPortraitOrientation())
      .aspectRatio(1f),
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}
