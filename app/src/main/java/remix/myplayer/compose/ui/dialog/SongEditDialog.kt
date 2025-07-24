package remix.myplayer.compose.ui.dialog

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.util.Util

@Composable
fun SongEditDialog() {
  val state by settingViewModel.songEditState.collectAsStateWithLifecycle()
  val song = state.song ?: return
  val activity = LocalActivity.current as? BaseActivity ?: return

  var title by remember {
    mutableStateOf(song.title)
  }
  var album by remember {
    mutableStateOf(song.album)
  }
  var artist by remember {
    mutableStateOf(song.artist)
  }
  var year by remember {
    mutableStateOf(song.year)
  }
  var track by remember {
    mutableStateOf(song.track)
  }
  var genre by remember {
    mutableStateOf(song.genre)
  }

  fun requestSaveAudioTag() {
    Util.requestSaveAudioTag(
      activity,
      song,
      title,
      album,
      artist,
      genre,
      year,
      track ?: ""
    )
  }

  NormalDialog(
    dialogState = state.dialogState,
    titleRes = R.string.song_edit,
    onPositive = {
      requestSaveAudioTag()
    },
    custom = {
      ProvideTextStyle(TextStyle(color = LocalTheme.current.textPrimary, fontSize = 18.sp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp)) {
          item {
            EditField(title, R.string.song_name_input_hint, isError = title.isEmpty()) {
              title = it
            }
          }
          item {
            EditField(album, R.string.album_input_hint) {
              album = it
            }
          }
          item {
            EditField(artist, R.string.artist_input_hint) {
              artist = it
            }
          }
          item {
            EditField(year, R.string.year_input_hint) {
              year = it
            }
          }
          item {
            EditField(track ?: "", R.string.track_number_input_hint) {
              track = it
            }
          }
          item {
            EditField(genre, R.string.genre_input_hint, isLast = true, onDone = {
              requestSaveAudioTag()
            }) {
              genre = it
            }
          }
        }
      }
    }
  )

  LaunchedEffect(song) {
    title = song.title
    album = song.album
    artist = song.artist
    genre = song.genre
    year = song.year
    track = song.track
  }
}

@Composable
fun EditField(
  value: String,
  labelRes: Int,
  isError: Boolean = false,
  isLast: Boolean = false,
  onDone: () -> Unit = {},
  onValueChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = value,
    singleLine = true,
    keyboardActions = KeyboardActions(onDone = {
      onDone()
    }),
    keyboardOptions = if (!isLast) KeyboardOptions(
      imeAction = ImeAction.Next
    ) else KeyboardOptions(
      imeAction = ImeAction.Done
    ),
    isError = isError,
    onValueChange = onValueChange,
    label = {
      TextPrimary(stringResource(labelRes))
    },
  )
}