package remix.myplayer.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import com.kyant.taglib.AudioProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.AudioTagFile
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.settingViewModel
import java.io.File

@Composable
fun SongDetailDialog() {
  val state by settingViewModel.songDetailState.collectAsStateWithLifecycle()
  val song = state.song

  var audioProperties by remember(song) {
    mutableStateOf<AudioProperties?>(null)
  }

  NormalDialog(
    dialogState = state.dialogState,
    title = stringResource(R.string.song_detail),
    negative = null,
    custom = {
      Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
          .padding(top = 18.dp)
          .verticalScroll(rememberScrollState())
      ) {
        DetailItem(R.string.song_path, song.data, true)
        DetailItem(R.string.song_name, song.showName)
        DetailItem(R.string.file_size, stringResource(R.string.cache_size, 1.0f * song.size / MB))
        DetailItem(
          R.string.format,
          song.data.substringAfterLast('.')
        )
        DetailItem(R.string.length, Util.getTime(song.duration))
        DetailItem(
          R.string.bitrate,
          if (song.isLocal()) "${audioProperties?.bitrate ?: 0} kb/s" else if (song is Song.Remote) "${song.bitRate} kb/s" else ""
        )
        DetailItem(
          R.string.sample_rate,
          if (song.isLocal()) "${audioProperties?.sampleRate ?: 0} Hz" else if (song is Song.Remote) "${song.sampleRate} Hz" else ""
        )
      }
    },
    positive = stringResource(R.string.close)
  )

  LaunchedEffect(song) {
    if (song.id > 0 && song.isLocal()) {
      try {
        audioProperties = withContext(Dispatchers.IO) {
          AudioTagFile.readAudioProperties(File(song.data))
        }
      } catch (ignore: Exception) {
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
