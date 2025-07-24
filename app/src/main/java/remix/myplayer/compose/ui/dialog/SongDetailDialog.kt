package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.Util
import java.io.File

@Composable
fun SongDetailDialog() {
  val state by settingViewModel.songDetailState.collectAsStateWithLifecycle()
  val song = state.song

  var audioHeader by remember {
    mutableStateOf<AudioHeader?>(null)
  }

  NormalDialog(
    dialogState = state.dialogState,
    title = stringResource(R.string.song_detail),
    negative = null,
    custom = {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(top = 18.dp)
      ) {
        item {
          DetailItem(R.string.song_path, song.data, true)
        }
        item {
          DetailItem(R.string.song_name, song.showName)
        }
        item {
          DetailItem(R.string.file_size, stringResource(R.string.cache_size, 1.0f * song.size / MB))
        }
        item {
          DetailItem(
            R.string.format,
            if (song.isLocal()) audioHeader?.format ?: "" else song.data.substringAfterLast('.')
          )
        }
        item {
          DetailItem(R.string.length, Util.getTime(song.duration))
        }
        item {
          DetailItem(
            R.string.bitrate,
            if (song.isLocal()) "${audioHeader?.bitRate ?: 0} kb/s" else if (song is Song.Remote) "${song.bitRate} kb/s" else ""
          )
        }
        item {
          DetailItem(
            R.string.sample_rate,
            if (song.isLocal()) "${audioHeader?.sampleRate ?: 0} Hz" else if (song is Song.Remote) "${song.sampleRate} Hz" else ""
          )
        }
      }
    },
    positive = stringResource(R.string.close)
  )

  LaunchedEffect(song) {
    if (song.id > 0 && song.isLocal()) {
      audioHeader = withContext(Dispatchers.IO) {
        AudioFileIO.read(File(song.data)).audioHeader
      }
    }
  }
}

@Composable
private fun DetailItem(titleRes: Int, content: String, selectable: Boolean = false) {
  Row {
    Text(
      stringResource(titleRes),
      fontSize = 16.sp,
      fontWeight = FontWeight.Bold,
      color = LocalTheme.current.textSecondary
    )
    if (selectable) {
      SelectionContainer {
        Text(
          content,
          fontSize = 16.sp,
          maxLines = Int.MAX_VALUE,
          color = LocalTheme.current.textSecondary
        )
      }
    } else {
      Text(
        content,
        fontSize = 16.sp,
        maxLines = Int.MAX_VALUE,
        color = LocalTheme.current.textSecondary
      )
    }

  }
}