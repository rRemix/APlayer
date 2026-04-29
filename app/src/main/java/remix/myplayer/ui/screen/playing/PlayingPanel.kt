package remix.myplayer.ui.screen.playing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import kotlinx.coroutines.launch
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.lyric.LyricManager
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.util.ext.isTablet
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun PlayingPanel(isVisible: Boolean) {
  PlayingContainer {
    val context = LocalContext.current
    if (context.isPortraitOrientation()) {
      Portrait(isVisible)
    } else {
      Landscape(isVisible)
    }
  }
}

@Composable
private fun Portrait(isVisible: Boolean) {
  Column(
    modifier = Modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(30.dp)
  ) {
    val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()
    val swatch by playbackViewModel.swatch.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    PlayingTopBar(playbackState.song, swatch)

    val pagerState = rememberPagerState { 2 }

    HorizontalPager(
      pagerState,
      modifier = Modifier.weight(7f),
      beyondViewportPageCount = 1
    ) { page ->
      when (page) {
        0 -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxSize()
              .clipToBounds()
          ) {
            PlayingCover(
              modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
              song = playbackState.song
            )
          }
        }

        else -> {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .fillMaxSize()
              .clipToBounds()
          ) {
            PlayingLyric(playbackState.song)
          }
        }
      }
    }

    PlayingIndicator(pagerState, swatch)

    PlayingSeekbarWithText(swatch)

    val playingScreenBottom =
      settingViewModel.settingsState.collectAsStateWithLifecycle().value.playingScreen.bottom
    val showBottomBar = playingScreenBottom != SettingPrefs.BOTTOM_SHOW_NONE
    PlayingControl(Modifier.weight(if (showBottomBar) 1f else 2f), playbackState, swatch)

    if (showBottomBar) {
      PlayingUtilityBar(
        Modifier
          .weight(1.5f)
          .fillMaxWidth()
          .padding(top = 12.dp),
        playingScreenBottom,
        playbackState,
        swatch
      )
    }

    KeepPlayingScreenOn(isVisible && pagerState.currentPage == 1)

    DisposableEffect(Unit) {
      val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          if (intent.action == LyricManager.ACTION_LYRIC) {
            val extra = intent.getIntExtra(LyricManager.EXTRA_LYRIC, -1)
            if (extra == LyricManager.SHOW_OFFSET_PANEL) {
              scope.launch {
                pagerState.animateScrollToPage(1)
              }
            }
          }
        }
      }
      registerLocalReceiver(receiver, IntentFilter(LyricManager.ACTION_LYRIC))
      onDispose {
        unregisterLocalReceiver(receiver)
      }
    }
  }
}

@Composable
private fun PlayingIndicator(
  pagerState: PagerState,
  swatch: Palette.Swatch
) {
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    val highLightColor = Color(swatch.rgb)
    val normalColor = highLightColor.copy(0.3f)
    Box(
      modifier = Modifier
        .width(8.dp)
        .height(2.dp)
        .background(if (pagerState.currentPage == 0) highLightColor else normalColor)
    )
    Box(
      modifier = Modifier
        .width(8.dp)
        .height(2.dp)
        .background(if (pagerState.currentPage == 1) highLightColor else normalColor)
    )
  }
}

@Composable
private fun Landscape(isVisible: Boolean) {
  val context = LocalContext.current
  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isCompactLandscape = maxHeight < 420.dp || maxWidth > maxHeight * 2.2f
    val useExpandedControls = context.isTablet() || maxWidth >= 1200.dp
    if (isCompactLandscape) {
      CompactLandscape(isVisible, useExpandedControls)
    } else {
      RegularLandscape(isVisible)
    }
  }
}

@Composable
private fun RegularLandscape(isVisible: Boolean) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()
    val swatch by playbackViewModel.swatch.collectAsStateWithLifecycle()

    PlayingTopBar(playbackState.song, swatch)

    Row(modifier = Modifier.weight(3f)) {
      Box(
        modifier = Modifier
          .weight(1f)
          .clipToBounds(),
        contentAlignment = Alignment.Center
      ) {
        PlayingCover(
          modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f),
          song = playbackState.song
        )
      }
      Box(
        modifier = Modifier
          .weight(1f)
          .clipToBounds(),
        contentAlignment = Alignment.Center
      ) {
        PlayingLyric(playbackState.song)
      }
    }

    KeepPlayingScreenOn(isVisible)

    PlayingSeekbarWithText(swatch)

    PlayingControl(Modifier.weight(1f), playbackState, swatch)
  }
}

@Composable
private fun CompactLandscape(isVisible: Boolean, useExpandedControls: Boolean) {
  KeepPlayingScreenOn(isVisible)

  Row(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()
    val swatch by playbackViewModel.swatch.collectAsStateWithLifecycle()

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      PlayingTopBar(playbackState.song, swatch)

      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clipToBounds(),
        contentAlignment = Alignment.Center
      ) {
        PlayingCover(
          modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = if (useExpandedControls) 12.dp else 0.dp)
            .aspectRatio(1f),
          song = playbackState.song
        )
      }

      PlayingSeekbarWithText(swatch)

      PlayingControl(
        modifier = Modifier
          .fillMaxWidth()
          .height(if (useExpandedControls) 92.dp else 64.dp),
        playbackUiState = playbackState,
        swatch = swatch,
        iconSize = if (useExpandedControls) 48.dp else null,
        playPauseSize = if (useExpandedControls) 80.dp else 56.dp,
        buttonSize = if (useExpandedControls) 92.dp else null
      )
    }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .clipToBounds(),
      contentAlignment = Alignment.Center
    ) {
      PlayingLyric(playbackState.song)
    }
  }
}

@Composable
private fun KeepPlayingScreenOn(shouldKeepScreenOn: Boolean) {
  val keepScreenOn =
    settingViewModel.settingsState.collectAsStateWithLifecycle().value.playingScreen.keepScreenOn
  val window = LocalActivity.current?.window
  DisposableEffect(shouldKeepScreenOn, keepScreenOn) {
    if (shouldKeepScreenOn && keepScreenOn) {
      window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    onDispose {
      window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
  }
}
