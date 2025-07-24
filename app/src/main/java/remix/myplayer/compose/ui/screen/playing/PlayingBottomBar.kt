package remix.myplayer.compose.ui.screen.playing

import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.AudioManager.STREAM_MUSIC
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.CenterInBox
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.prefs.SettingPrefs.Companion.BOTTOM_SHOW_BOTH
import remix.myplayer.compose.prefs.SettingPrefs.Companion.BOTTOM_SHOW_NEXT
import remix.myplayer.compose.prefs.SettingPrefs.Companion.BOTTOM_SHOW_NONE
import remix.myplayer.compose.prefs.SettingPrefs.Companion.BOTTOM_SHOW_VOLUME
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.LineSlider
import remix.myplayer.compose.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.compose.viewmodel.MusicState
import remix.myplayer.compose.viewmodel.settingViewModel

@Composable
internal fun PlayingBottomBar(modifier: Modifier, musicState: MusicState, swatch: Palette.Swatch) {
  val settingVM = settingViewModel

  Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
    val swatchColor = Color(swatch.rgb)
    val playingScreenBottom = settingVM.settingPrefs.playingScreenBottom
    assert(playingScreenBottom != BOTTOM_SHOW_NONE)

    var nextSongIsVisible by remember {
      mutableStateOf(playingScreenBottom == BOTTOM_SHOW_NEXT)
    }
    var volumeIsVisible by remember {
      mutableStateOf(playingScreenBottom != BOTTOM_SHOW_NEXT)
    }

    var refreshKey by remember {
      mutableIntStateOf(0)
    }

    AnimatedVisibility(nextSongIsVisible, enter = fadeIn(), exit = fadeOut()) {
      NextSong(musicState, swatchColor) {
        if (playingScreenBottom != BOTTOM_SHOW_NEXT) {
          nextSongIsVisible = false
          volumeIsVisible = true
          refreshKey++
        }
      }
    }
    AnimatedVisibility(volumeIsVisible, enter = fadeIn(), exit = fadeOut()) {
      VolumeSeekbar(swatchColor) {
        if (playingScreenBottom != BOTTOM_SHOW_VOLUME) {
          refreshKey++
        }
      }
    }

    LaunchedEffect(refreshKey) {
      if (refreshKey == 0 && playingScreenBottom == BOTTOM_SHOW_BOTH || refreshKey > 0) {
        // nextSong -> GONE
        // volume -> VISIBLE
        delay(3000)
        nextSongIsVisible = true
        volumeIsVisible = false
      }
    }
  }
}

@Composable
private fun NextSong(musicState: MusicState, swatchColor: Color, onClick: (() -> Unit)? = null) {
  CenterInBox(
    modifier = Modifier
      .padding(horizontal = 36.dp)
      .fillMaxWidth()
      .background(swatchColor.copy(0.1f), shape = RoundedCornerShape(2.dp))
      .clickableWithoutRipple {
        onClick?.invoke()
      }
  ) {
    Text(
      text = stringResource(R.string.next_song, musicState.nextSong.title),
      color = Color(
        if (LocalTheme.current.isLight) {
          "#a8a8a8"
        } else {
          "#e5e5e5"
        }.toColorInt()
      ),
      fontSize = 14.sp,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(vertical = 8.dp)
    )
  }
}

@Composable
private fun VolumeSeekbar(swatchColor: Color, onValueChange: () -> Unit) {
  val context = LocalContext.current
  val audioManager = remember {
    context.getSystemService(AUDIO_SERVICE) as AudioManager
  }
  Row(
    horizontalArrangement = Arrangement.spacedBy(40.dp),
    modifier = Modifier.padding(horizontal = 18.dp)
  ) {
    CenterInBox(
      Modifier
        .size(48.dp)
        .clickWithRipple {
          audioManager.adjustStreamVolume(
            STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_PLAY_SOUND
          )
        }) {
      Image(
        painter = painterResource(R.drawable.ic_volume_down_black_24dp),
        contentDescription = "PlayingBottomBarVolumeDown",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }

    VolumeSeekBar(audioManager, swatchColor, onValueChange)

    CenterInBox(
      Modifier
        .size(48.dp)
        .clickWithRipple {
          audioManager.adjustStreamVolume(
            STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_PLAY_SOUND
          )
        }) {
      Image(
        painter = painterResource(R.drawable.ic_volume_up_black_24dp),
        contentDescription = "PlayingBottomBarVolumeUp",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }
  }
}

@Composable
private fun RowScope.VolumeSeekBar(
  audioManager: AudioManager,
  swatchColor: Color,
  onValueChange: () -> Unit
) {
  var min by remember {
    mutableIntStateOf(0)
  }
  var max by remember {
    mutableIntStateOf(1)
  }
  var current by remember {
    mutableIntStateOf(0)
  }

  LineSlider(
    value = current.toFloat(),
    valueRange = min.toFloat()..max.toFloat(),
    onValueChange = {
      current = it.toInt()
      onValueChange()
    },
    onValueChangeFinished = {
      audioManager.setStreamVolume(
        STREAM_MUSIC,
        current,
        AudioManager.FLAG_PLAY_SOUND
      )
    },
    modifier = Modifier
      .height(48.dp)
      .weight(1f),
    properties = defaultLineSliderProperties.copy(
      trackHeight = 2.dp,
      trackBackgroundColor = playingTrackBackgroundColor,
      trackProgressColor = swatchColor,
      thumbColor = swatchColor,
      thumbWidth = 2.dp,
      thumbHeight = 6.dp,
      thumbShape = RectangleShape
    )
  )

  LaunchedEffect(Unit) {
    min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      audioManager.getStreamMinVolume(STREAM_MUSIC)
    } else 0
    max = audioManager.getStreamMaxVolume(STREAM_MUSIC)
  }

  val scope = rememberCoroutineScope()
  DisposableEffect(Unit) {
    scope.launch {
      while (isActive) {
        current = audioManager.getStreamVolume(STREAM_MUSIC)
        delay(1000)
      }
    }

    onDispose {

    }
  }
}