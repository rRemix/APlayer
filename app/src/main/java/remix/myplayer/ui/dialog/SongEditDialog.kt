package remix.myplayer.ui.dialog

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.lyric.provider.EmbeddedProvider
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.settingViewModel

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
  var lyrics by remember {
    mutableStateOf("")
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
      track ?: "",
      lyrics
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
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
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
            EditField(genre, R.string.genre_input_hint) {
              genre = it
            }
          }
          item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              EditField(year, R.string.year_input_hint, modifier = Modifier.weight(1f)) {
                year = it
              }
              EditField(
                track ?: "",
                R.string.track_number_input_hint,
                modifier = Modifier.weight(1f)
              ) {
                track = it
              }
            }
          }
          item {
            ProvideTextStyle(TextStyle(fontSize = 16.sp)) {
              EditField(
                lyrics,
                R.string.lyrics_input_hint,
                isLast = true,
                maxLine = 10,
                onDone = {
                  requestSaveAudioTag()
                }) {
                lyrics = it
              }
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

    if (song.id > 0 && song.isLocal()) {
      lyrics = withContext(Dispatchers.IO) {
        EmbeddedProvider.extractLyric(song)
      }
    }
  }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditField(
  value: String,
  labelRes: Int,
  isError: Boolean = false,
  isLast: Boolean = false,
  maxLine: Int = 1,
  modifier: Modifier = Modifier,
  contentType: ContentType? = null,
  keyboardType: KeyboardType = KeyboardType.Text,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onDone: () -> Unit = {},
  onValueChange: (String) -> Unit,
) {
  OutlinedTextField(
    value = value,
    maxLines = maxLine,
    keyboardActions = KeyboardActions(onDone = {
      onDone()
    }),
    keyboardOptions = if (!isLast) KeyboardOptions(
      imeAction = ImeAction.Next,
      keyboardType = keyboardType
    ) else KeyboardOptions(
      imeAction = ImeAction.Done,
      keyboardType = keyboardType
    ),
    visualTransformation = visualTransformation,
    isError = isError,
    onValueChange = onValueChange,
    label = {
      TextPrimary(stringResource(labelRes))
    },
    modifier = Modifier
      .semantics {
        if (contentType != null) {
          this.contentType = contentType
        }
      }
      .then(modifier)
  )
}