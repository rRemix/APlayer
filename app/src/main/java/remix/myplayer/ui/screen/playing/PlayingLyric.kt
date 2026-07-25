package remix.myplayer.ui.screen.playing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.lyric.LyricLine
import remix.myplayer.lyric.LyricManager
import remix.myplayer.lyric.provider.ILyricsProvider
import remix.myplayer.lyric.provider.UriProvider
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.ProgressAware
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.settingViewModel
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun PlayingLyric(song: Song) {
  val context = LocalContext.current

  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val lyricsManager = settingVM.lyricManager

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
    mutableStateOf<List<LyricLine>>(emptyList())
  }

  var panelState by remember {
    mutableStateOf(PanelState(false, 0))
  }

  var lyricOffset by remember {
    mutableLongStateOf(0L)
  }

  if (searching || lyrics.isEmpty()) {
    val placeholder = stringResource(if (searching) R.string.searching else R.string.no_lrc)
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      TextSecondary(
        placeholder,
        fontSize = (DEFAULT_TEXT_SIZE * settingState.lyric.fontScale).sp
      )
    }
  } else {
    ProgressAware { progress, duration ->
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        LyricContainer(
          Modifier.fillMaxSize(),
          lyrics,
          progress,
          duration,
          lyricOffset,
          settingState.lyric.fontScale,
        )

        if (panelState.show) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OffsetButton(iconRes = R.drawable.ic_stat_1_24dp, text = "-0.5s") {
              lyricOffset = max(-6000L, lyricOffset - 500)
              lyricsManager.offset = lyricOffset

              MessageNotifier.show(
                context.getString(
                  R.string.lyric_delay_x_second,
                  String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(lyricOffset) / 1000f)
                )
              )
              panelState = panelState.copy(tick = panelState.tick + 1)
            }

            OffsetButton(iconRes = R.drawable.ic_refresh_24dp, text = "") {
              MessageNotifier.show(R.string.lyric_offset_reset)
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
              MessageNotifier.show(
                context.getString(
                  R.string.lyric_advance_x_second,
                  String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(lyricOffset) / 1000f)
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

  val currentSong by rememberUpdatedState(song)
  DisposableEffect(Unit) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LyricManager.ACTION_LYRIC) {
          val extra = intent.getIntExtra(LyricManager.EXTRA_LYRIC, -1)

          when (extra) {
            LyricManager.CHANGE_LYRIC -> {
              if (!currentSong.valid()) {
                return
              }

              lyricsManager.clearCache(currentSong)

              // 如果是手动选择则直接使用UriProvider解析
              val uri = intent.getParcelableExtra<Uri>(LyricManager.EXTRA_LYRIC_URI)
              uriProvider = if (uri != null) {
                UriProvider(context, uri)
              } else {
                null
              }
              searchTrigger++
            }

            LyricManager.SHOW_OFFSET_PANEL -> {
              panelState = panelState.copy(show = true)
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
