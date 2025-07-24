package remix.myplayer.compose.ui.screen.playing

import android.content.Intent
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.CenterInBox
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.dialog.BottomSheetDialog
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.MusicState
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.widget.playpause.PlayPauseView
import remix.myplayer.util.Constants.MODE_LOOP
import remix.myplayer.util.Constants.MODE_REPEAT
import remix.myplayer.util.Constants.MODE_SHUFFLE
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

private val itemRes = mapOf(
  MODE_LOOP to Pair(R.drawable.play_btn_loop, R.string.model_normal),
  MODE_SHUFFLE to Pair(R.drawable.play_btn_shuffle, R.string.model_random),
  MODE_REPEAT to Pair(R.drawable.play_btn_loop_one, R.string.model_repeat)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayingControl(
  modifier: Modifier = Modifier,
  musicState: MusicState,
  swatch: Palette.Swatch
) {
  val musicVM = musicViewModel
  val context = LocalContext.current
  Row(
    modifier = modifier
      .fillMaxSize(),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val swatchColor = Color(swatch.rgb)
    var playMode = musicState.playMode
    ControlButton(onClick = {
      val newMode = if (playMode == MODE_REPEAT) MODE_LOOP else playMode + 1
      musicVM.setPlayModel(newMode)
      ToastUtil.show(context, itemRes[newMode]!!.second)
    }) {
      Image(
        painter = painterResource(itemRes[playMode]!!.first),
        contentDescription = "PlayingMode",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }

    ControlButton(onClick = {
      Util.sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_CONTROL,
          Command.PREV
        )
      )
    }) {
      Image(
        painter = painterResource(R.drawable.play_btn_pre),
        contentDescription = "PlayingPrev",
        colorFilter = ColorFilter.tint(swatchColor)
      )
    }

    ControlButton(onClick = {
      Util.sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_CONTROL,
          Command.TOGGLE
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
          it.updateState(musicState.playing, true)
        }
      )
    }

    ControlButton(onClick = {
      Util.sendLocalBroadcast(
        Intent(MusicService.ACTION_CMD).putExtra(
          MusicService.EXTRA_CONTROL,
          Command.NEXT
        )
      )
    }) {
      Image(
        painter = painterResource(R.drawable.play_btn_next),
        contentDescription = "PlayingNext",
        colorFilter = ColorFilter.tint(swatchColor)
      )
    }

    val state = rememberModalBottomSheetState()
    PlayQueueDialog(state, musicState)

    val scope = rememberCoroutineScope()
    ControlButton(onClick = {
      scope.launch {
        state.show()
      }
    }) {
      Image(
        painter = painterResource(R.drawable.play_btn_normal_list),
        contentDescription = "PlayingPlayQueue",
        colorFilter = ColorFilter.tint(swatchColor.copy(0.5f))
      )
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayQueueDialog(
  state: SheetState,
  musicState: MusicState
) {
  val playingVM = musicViewModel
  val songs by playingVM.playQueueSongs.collectAsStateWithLifecycle()

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
      items(songs, key = { it.id }) { song ->
        val valid = song != Song.EMPTY_SONG
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(50.dp)) {
          Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
              .padding(horizontal = 16.dp)
              .weight(1f)
          ) {
            if (!valid) {
              TextPrimary(stringResource(R.string.song_lose_effect))
            } else {
              TextPrimary(
                song.title,
                color = if (song == musicState.song) LocalTheme.current.secondary else LocalTheme.current.textPrimary
              )
              TextSecondary(song.artist)
            }
          }

          if (valid) {
            CenterInBox(
              modifier = Modifier
                .clickWithRipple {
                  // TODO 删除后恢复高度和索引
                  playingVM.removeFromQueue(song.id)
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
  }
}

@Composable
private fun RowScope.ControlButton(
  onClick: () -> Unit,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxHeight()
      .aspectRatio(1f)
      .clickWithRipple {
        onClick()
      }, contentAlignment = Alignment.Center
  ) {
    content()
  }
}
