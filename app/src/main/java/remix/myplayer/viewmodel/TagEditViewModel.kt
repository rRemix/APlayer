package remix.myplayer.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.AudioTagFile
import remix.myplayer.helper.AudioTagWriter
import remix.myplayer.lyric.provider.EmbeddedProvider
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.util.ImageUtil
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TagEditViewModel @Inject constructor() : ViewModel() {

  private val _tagEditState = MutableStateFlow(TagEditState())
  val tagEditState = _tagEditState.asStateFlow()

  private var tagEditPickedBitmap: Bitmap? = null

  fun startTagEdit(song: Song) {
    tagEditPickedBitmap = null
    _tagEditState.value = TagEditState(
      song = song,
      tagFormState = TagEditState.TagFormState(),
      albumArtState = TagEditState.AlbumArtState(),
      isLoading = true,
      hasEdited = false
    )
    loadTagEditData(song)
  }

  private fun loadTagEditData(song: Song) {
    viewModelScope.launch {
      val propertyMap = withContext(Dispatchers.IO) {
        runCatching {
          AudioTagFile.readMetadata(File(song.data), readPictures = false)?.propertyMap
        }.getOrNull()
      }
      val lyrics = withContext(Dispatchers.IO) {
        runCatching {
          EmbeddedProvider.extractLyric(song)
        }.getOrDefault("")
      }
      val albumArt = ImageUtil.extractAlbumArt(song)
      _tagEditState.update { state ->
        if (state.song?.id != song.id) {
          return@update state
        }
        val updatedFormState = if (state.hasEdited) {
          state.tagFormState
        } else {
          TagEditState.TagFormState(
            title = AudioTagFile.firstValue(propertyMap, AudioTagFile.TITLE),
            album = AudioTagFile.firstValue(propertyMap, AudioTagFile.ALBUM),
            artist = AudioTagFile.firstValue(propertyMap, AudioTagFile.ARTIST),
            albumArtist = AudioTagFile.firstValue(propertyMap, AudioTagFile.ALBUM_ARTIST),
            composer = AudioTagFile.firstValue(propertyMap, AudioTagFile.COMPOSER),
            year = AudioTagFile.firstValue(propertyMap, AudioTagFile.DATE),
            track = AudioTagFile.firstValue(propertyMap, AudioTagFile.TRACK_NUMBER),
            disc = AudioTagFile.firstValue(propertyMap, AudioTagFile.DISC_NUMBER),
            genre = AudioTagFile.firstValue(propertyMap, AudioTagFile.GENRE),
            lyrics = lyrics
          )
        }
        val updatedAlbumArtState =
          if (!state.albumArtState.isCleared && tagEditPickedBitmap == null && state.albumArtState.albumArt == null) {
            state.albumArtState.copy(albumArt = albumArt)
          } else {
            state.albumArtState
          }
        state.copy(
          tagFormState = updatedFormState,
          albumArtState = updatedAlbumArtState,
          isLoading = false
        )
      }
    }
  }

  fun updateTagEditTitle(value: String) {
    updateTagEditForm { it.copy(title = value) }
  }

  fun updateTagEditAlbum(value: String) {
    updateTagEditForm { it.copy(album = value) }
  }

  fun updateTagEditArtist(value: String) {
    updateTagEditForm { it.copy(artist = value) }
  }

  fun updateTagEditAlbumArtist(value: String) {
    updateTagEditForm { it.copy(albumArtist = value) }
  }

  fun updateTagEditComposer(value: String) {
    updateTagEditForm { it.copy(composer = value) }
  }

  fun updateTagEditYear(value: String) {
    updateTagEditForm { it.copy(year = value) }
  }

  fun updateTagEditTrack(value: String) {
    updateTagEditForm { it.copy(track = value) }
  }

  fun updateTagEditDisc(value: String) {
    updateTagEditForm { it.copy(disc = value) }
  }

  fun updateTagEditGenre(value: String) {
    updateTagEditForm { it.copy(genre = value) }
  }

  fun updateTagEditLyrics(value: String) {
    updateTagEditForm { it.copy(lyrics = value) }
  }

  private fun updateTagEditForm(
    transform: (TagEditState.TagFormState) -> TagEditState.TagFormState
  ) {
    _tagEditState.update { state ->
      state.copy(
        tagFormState = transform(state.tagFormState),
        hasEdited = true
      )
    }
  }

  fun onTagEditAlbumArtPicked(bitmap: Bitmap) {
    tagEditPickedBitmap = bitmap
    _tagEditState.update { state ->
      state.copy(
        albumArtState = state.albumArtState.copy(
          albumArt = bitmap.asImageBitmap(),
          isCleared = false
        ),
        hasEdited = true
      )
    }
  }

  fun onTagEditAlbumArtCleared() {
    tagEditPickedBitmap = null
    _tagEditState.update { state ->
      state.copy(
        albumArtState = state.albumArtState.copy(
          albumArt = null,
          isCleared = true
        ),
        hasEdited = true
      )
    }
  }

  fun saveTagEdit(activity: BaseActivity) {
    val state = _tagEditState.value
    val song = state.song ?: return
    val form = state.tagFormState
    AudioTagWriter.requestSaveAudioTag(
      activity,
      song,
      form.title,
      form.album,
      form.artist,
      form.albumArtist,
      form.composer,
      form.genre,
      form.year,
      form.track,
      form.disc,
      form.lyrics,
      state.albumArtState.isCleared,
      tagEditPickedBitmap
    )
  }
}

@Stable
data class TagEditState(
  val song: Song? = null,
  val tagFormState: TagFormState = TagFormState(),
  val albumArtState: AlbumArtState = AlbumArtState(),
  val isLoading: Boolean = false,
  val hasEdited: Boolean = false
) {

  @Stable
  data class TagFormState(
    val title: String = "",
    val album: String = "",
    val artist: String = "",
    val albumArtist: String = "",
    val composer: String = "",
    val year: String = "",
    val track: String = "",
    val disc: String = "",
    val genre: String = "",
    val lyrics: String = ""
  )

  @Stable
  data class AlbumArtState(
    val albumArt: ImageBitmap? = null,
    val isCleared: Boolean = false
  )
}
