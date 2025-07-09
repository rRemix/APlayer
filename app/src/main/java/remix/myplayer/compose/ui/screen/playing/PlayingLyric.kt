package remix.myplayer.compose.ui.screen.playing

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyric.LrcView
import remix.myplayer.lyric.LyricSearcher
import remix.myplayer.lyric.bean.LrcRow

// TODO
@Composable
internal fun PlayingLyric(song: Song) {
  val lyricSearcher = remember {
    LyricSearcher()
  }

  var lyricText by remember {
    mutableStateOf<Int?>(R.string.no_lrc)
  }
  var lyricRows by remember {
    mutableStateOf<List<LrcRow>?>(null)
  }

  LaunchedEffect(song) {
    lyricText = R.string.searching
    val temp = try {
      withContext(Dispatchers.IO) {
        lyricSearcher.setSong(song).getLyricObservable().blockingFirst()
      }
    } catch (ignore: Exception) {
      null
    }
    if (temp == null || temp.isEmpty()) {
      lyricText = R.string.no_lrc
      return@LaunchedEffect
    }
    lyricText = null
    lyricRows = temp
  }

  AndroidView(
    factory = { context ->
      LrcView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
    },
    update = {
      if (lyricText != null) {
        it.setText(lyricText!!)
      } else {
        it.setLrcRows(lyricRows)
      }
    }
  )
}