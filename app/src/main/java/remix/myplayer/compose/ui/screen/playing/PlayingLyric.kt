package remix.myplayer.compose.ui.screen.playing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.lyric.LyricsLine
import remix.myplayer.compose.lyric.LyricsManager
import remix.myplayer.compose.lyric.LyricsManagerEntryPoint
import remix.myplayer.compose.lyric.provider.ILyricsProvider
import remix.myplayer.compose.lyric.provider.UriProvider
import remix.myplayer.compose.ui.common.LocalSnackBarHostState
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.ProgressAware
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.ui.widget.LyricsView
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun PlayingLyric(song: Song) {
  val context = LocalContext.current

  val musicVM = musicViewModel
  val snackBarHostState = LocalSnackBarHostState.current
  val scope = rememberCoroutineScope()

  val lyricsManager = remember {
    EntryPointAccessors.fromApplication(
      context.applicationContext,
      LyricsManagerEntryPoint::class.java
    ).lyricsManager()
  }
  val lyricSearcher = lyricsManager.lyricSearcher

  var uriProvider by remember {
    mutableStateOf<ILyricsProvider?>(null)
  }

  var searchTrigger by remember {
    mutableIntStateOf(0)
  }

  var searching by remember {
    mutableStateOf(false)
  }

  var lyrics by remember {
    mutableStateOf<List<LyricsLine>>(emptyList())
  }

  var panelState by remember {
    mutableStateOf(PanelState(false, 0))
  }

  var fontScale by remember {
    mutableFloatStateOf(lyricSearcher.lyricPrefs.fontScale)
  }

  var lyricOffset by remember {
    mutableLongStateOf(0L)
  }

  val showMessage = { message: String ->
    scope.launch {
      snackBarHostState.currentSnackbarData?.dismiss()
      snackBarHostState.showSnackbar(message)
    }
  }

  if (searching || lyrics.isEmpty()) {
    val placeholder = stringResource(if (searching) R.string.searching else R.string.no_lrc)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      TextSecondary(placeholder, fontSize = (LyricsView.DEFAULT_TEXT_SIZE * fontScale).sp)
    }
  } else {
    ProgressAware { progress, duration ->
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        AndroidView(
          factory = { context ->
            LyricsView(context).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
              visibility = View.INVISIBLE
//              this.lyrics = lyrics
//              this.offset = lyricOffset
//              this.scalingFactor = fontScale
              this.onSeekToListener = object : LyricsView.OnSeekToListener {
                override fun onSeekTo(progress: Long) {
                  musicVM.setProgress(progress)
                }
              }
            }
          },
          update = { v ->
            if (v.lyrics.isEmpty() && !lyrics.isEmpty()) {
              // 等待layout和scroll
              v.postDelayed({
                v.visibility = View.VISIBLE
              }, 50)
            }
            v.lyrics = lyrics
            v.scalingFactor = fontScale
            v.offset = lyricOffset
            if (lyrics.isNotEmpty()) {
              v.setProgress(progress, song.duration)
            }
          }
        )

        if (panelState.show) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OffsetButton(iconRes = R.drawable.ic_stat_1_24dp, text = "-0.5s") {
              lyricOffset = max(0, lyricOffset - 500)
              lyricsManager.offset = lyricOffset

              showMessage(
                context.getString(
                  if (lyricOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
                  String.format(Locale.getDefault(), "%.1f", lyricOffset / 1000f)
                )
              )
              panelState = panelState.copy(tick = panelState.tick + 1)
            }

            OffsetButton(iconRes = R.drawable.ic_refresh_24dp, text = "") {
              showMessage(context.getString(R.string.lyric_offset_reset))
              panelState = panelState.copy(tick = panelState.tick + 1)
              if (lyricOffset == 0L) {
                return@OffsetButton
              }
              lyricOffset = 0
              lyricsManager.offset = 0
            }

            OffsetButton(iconRes = R.drawable.ic_stat_minus_1_24dp, text = "+0.5s") {
              lyricOffset = min(6000L, lyricOffset + 500)
              lyricsManager.offset = lyricOffset
              showMessage(
                context.getString(
                  if (lyricOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
                  String.format(Locale.getDefault(), "%.1f", lyricOffset / 1000f)
                )
              )
              panelState = panelState.copy(tick = panelState.tick + 1)
            }
          }
        }
      }
    }
  }

  // 更新歌词
  LaunchedEffect(song, searchTrigger) {
    searching = true
    val job = lyricsManager.updateLyrics(song, uriProvider)
    job?.join()
    searching = false

    lyrics = lyricsManager.lyrics ?: emptyList()
    lyricOffset = lyricsManager.offset
    uriProvider = null
  }

  // 5s后offsetPanel隐藏
  LaunchedEffect(panelState) {
    if (panelState.show) {
      delay(5000)
      panelState = panelState.copy(show = false)
    }
  }

  DisposableEffect(Unit) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LyricsManager.ACTION_LYRIC) {
          val extra = intent.getIntExtra(LyricsManager.EXTRA_LYRIC, -1)

          when (extra) {
            LyricsManager.CHANGE_LYRIC -> {
              lyricsManager.clearCache(song)

              // 如果是手动选择则直接使用UriProvider解析
              val uri = intent.getParcelableExtra<Uri>(LyricsManager.EXTRA_LYRIC_URI)
              uriProvider = if (uri != null) {
                UriProvider(uri)
              } else {
                null
              }
              searchTrigger++
            }

            LyricsManager.CHANGE_LYRIC_FONT_SCALE -> {
              fontScale = lyricSearcher.lyricPrefs.fontScale
            }

            LyricsManager.SHOW_OFFSET_PANEL -> {
              panelState = panelState.copy(show = true)
            }
          }
        }
      }
    }
    registerLocalReceiver(receiver, IntentFilter(LyricsManager.ACTION_LYRIC))

    onDispose {
      unregisterLocalReceiver(receiver)
    }
  }
}

@Composable
fun OffsetButton(
  modifier: Modifier = Modifier,
  iconRes: Int,
  text: String,
  onClick: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .size(48.dp)
      .clickWithRipple(true) {
        onClick()
      },
  ) {
    Image(
      painter = painterResource(id = iconRes),
      contentDescription = "LyricOffset_${text}",
      colorFilter = ColorFilter.tint(LocalTheme.current.textPrimary)
    )
    if (text.isNotEmpty()) {
      TextPrimary(
        text = text,
        fontSize = 11.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}

private data class PanelState(
  val show: Boolean,
  val tick: Int = 0
)

@Composable
@Preview(showBackground = true)
fun OffsetButtonPreview() {
  OffsetButton(iconRes = R.drawable.ic_stat_1_24dp, text = "+0.5s") {

  }
}
