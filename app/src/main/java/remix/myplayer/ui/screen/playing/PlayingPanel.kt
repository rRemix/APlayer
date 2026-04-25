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

    val keepScreenOn =
      settingViewModel.settingsState.collectAsStateWithLifecycle().value.playingScreen.keepScreenOn
    val window = LocalActivity.current?.window
    DisposableEffect(pagerState.currentPage, isVisible, keepScreenOn) {
      if (pagerState.currentPage == 1 && isVisible && keepScreenOn) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      onDispose {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }

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
  Column(
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

    val keepScreenOn =
      settingViewModel.settingsState.collectAsStateWithLifecycle().value.playingScreen.keepScreenOn
    val window = LocalActivity.current?.window
    DisposableEffect(isVisible, keepScreenOn) {
      if (isVisible && keepScreenOn) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      onDispose {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }

    PlayingSeekbarWithText(swatch)

    PlayingControl(Modifier.weight(1f), playbackState, swatch)
  }
}
