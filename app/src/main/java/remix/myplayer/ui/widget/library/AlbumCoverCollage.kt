package remix.myplayer.ui.widget.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.AsyncImagePainter
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.ui.theme.LocalTheme

/** Use the song list's cover source, keeping one successful song per album and up to four covers. */
@Composable
fun AlbumCoverCollage(
  songs: List<Song>,
  modifier: Modifier = Modifier,
  model: APlayerModel,
) {
  val coverRevision = rememberCoverRevision(model)
  val candidates = remember(songs) { songs.distinctBy { it.coverRequestKey } }
  val results = remember(model, candidates, coverRevision) {
    mutableStateMapOf<Pair<Long, String>, CoverResult>()
  }
  val requests = candidates.asSequence()
    .filter { results[it.coverRequestKey] != CoverResult.Failed }
    // Filter failures before grouping, so another song in that album can supply its cover.
    .distinctBy { it.albumId }
    .take(4)
    .toList()
  // Keep one placeholder until the selection settles, rather than repeatedly reshaping the grid.
  val covers = if (requests.all { results[it.coverRequestKey] is CoverResult.Loaded }) {
    requests.map { (results.getValue(it.coverRequestKey) as CoverResult.Loaded).painter }
  } else {
    emptyList()
  }
  Box(modifier.aspectRatio(1f).background(LocalTheme.current.mainBackground)) {
    requests.forEach { song ->
      val requestKey = song.coverRequestKey
      key(requestKey, coverRevision) {
        // Keep successful painters alive while drawing them in the collage below.
        SubcomposeAsyncImage(
          model = rememberCoverRequest(song),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize()
        ) {
          val state by painter.state.collectAsState()
          val result = when (val current = state) {
            is AsyncImagePainter.State.Success -> CoverResult.Loaded(current.painter)
            is AsyncImagePainter.State.Error -> CoverResult.Failed
            else -> null
          }
          LaunchedEffect(result, results) {
            if (result == null) {
              results.remove(requestKey)
            } else {
              results[requestKey] = result
            }
          }
        }
      }
    }
    when (covers.size) {
      0 -> Image(
        painter = painterResource(LocalTheme.current.albumPlaceHolder),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      1 -> Image(
        painter = covers[0],
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      else -> {
        val left = if (covers.size == 4) listOf(covers[0], covers[2]) else listOf(covers[0])
        val right = if (covers.size == 4) listOf(covers[1], covers[3]) else covers.drop(1)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          CoverColumn(left, Modifier.weight(1f).fillMaxHeight())
          CoverColumn(right, Modifier.weight(1f).fillMaxHeight())
        }
      }
    }
  }
}

@Composable
private fun CoverColumn(covers: List<Painter>, modifier: Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
    covers.forEach { painter ->
      Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      )
    }
  }
}

private sealed interface CoverResult {
  data class Loaded(val painter: Painter) : CoverResult
  data object Failed : CoverResult
}

private val Song.coverRequestKey: Pair<Long, String>
  get() = id to data
