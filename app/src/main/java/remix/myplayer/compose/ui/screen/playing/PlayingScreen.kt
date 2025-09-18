package remix.myplayer.compose.ui.screen.playing

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.compose.viewmodel.playingViewModel
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.misc.isPortraitOrientation

@Composable
fun PlayingScreen() {
  PlayingContainer {
    val context = LocalContext.current
    if (context.isPortraitOrientation()) {
      Portrait()
    } else {
      Landscape()
    }
  }
}

@Composable
private fun Portrait() {
  Column(
    modifier = Modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(30.dp)
  ) {
    val musicState by musicViewModel.musicState.collectAsStateWithLifecycle()
    val song = musicState.song

    val playingVM = playingViewModel
    val swatch by playingVM.swatch.collectAsStateWithLifecycle()

    PlayingTopBar(song, swatch)

    val pagerState = rememberPagerState { 2 }

    HorizontalPager(pagerState, modifier = Modifier.weight(7f)) { page ->
      when (page) {
        0 -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            PlayingCover(
              modifier = Modifier
                .padding(start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(elevation = 8.dp)
                .clip(RoundedCornerShape(8.dp)),
              song = musicState.song
            )
          }
        }

        else -> {
          PlayingLyric(song)
        }
      }
    }

    PlayingIndicator(pagerState, swatch)

    PlayingSeekbarWithText(swatch)

    val showBottomBar =
      settingViewModel.settingPrefs.playingScreenBottom != SettingPrefs.BOTTOM_SHOW_NONE
    PlayingControl(Modifier.weight(if (showBottomBar) 1f else 2f), musicState, swatch)

    if (showBottomBar) {
      PlayingBottomBar(
        Modifier
          .weight(1.5f)
          .fillMaxWidth()
          .padding(top = 12.dp),
        musicState,
        swatch
      )
    }

    val window = LocalActivity.current?.window
    DisposableEffect(pagerState.currentPage) {
      if (pagerState.currentPage == 1 && playingVM.isKeepScreenOn()) {
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
      onDispose {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
private fun Landscape() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    val musicState by musicViewModel.musicState.collectAsStateWithLifecycle()
    val song = musicState.song

    val swatch by playingViewModel.swatch.collectAsStateWithLifecycle()

    PlayingTopBar(song, swatch)

    Row(modifier = Modifier.weight(3f)) {
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        PlayingCover(
          modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .shadow(elevation = 8.dp)
            .clip(RoundedCornerShape(8.dp)),
          song = musicState.song
        )
      }
      Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
        PlayingLyric(song)
      }
    }

    PlayingSeekbarWithText(swatch)

    PlayingControl(Modifier.weight(1f), musicState, swatch)
  }
}

