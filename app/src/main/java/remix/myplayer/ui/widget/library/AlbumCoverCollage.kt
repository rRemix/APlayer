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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.ui.theme.LocalTheme

/** Keep this at screen scope so scrolling a lazy header away does not restart its cover requests. */
@Composable
fun rememberAlbumCoverPainters(
  songs: List<Song>,
  model: APlayerModel,
  size: Dp,
): List<Painter> {
  val pixelSize = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
  val coverRevision = rememberCoverRevision(model)
  val candidates = remember(songs) { songs.distinctBy { it.coverRequestKey } }
  val results = remember(model, candidates, coverRevision, pixelSize) {
    mutableStateMapOf<Pair<SongIdentity, String>, CoverResult>()
  }
  val requests = selectCoverSongs(candidates) { results[it.coverRequestKey] == CoverResult.Failed }
  requests.forEach { song ->
    val requestKey = song.coverRequestKey
    key(requestKey, coverRevision) {
      val baseRequest = rememberCoverRequest(song)
      val request = remember(baseRequest, pixelSize) {
        // There is no image layout here to resolve size; avoid decoding full-size artwork.
        baseRequest.newBuilder().size(pixelSize).build()
      }
      val painter = rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
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
  // Keep one placeholder until the selection settles, rather than repeatedly reshaping the grid.
  return if (requests.all { results[it.coverRequestKey] is CoverResult.Loaded }) {
    requests.map { (results.getValue(it.coverRequestKey) as CoverResult.Loaded).painter }
  } else {
    emptyList()
  }
}

@Composable
fun AlbumCoverCollage(covers: List<Painter>, modifier: Modifier = Modifier) {
  Box(modifier.aspectRatio(1f).background(LocalTheme.current.mainBackground)) {
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

internal fun selectCoverSongs(candidates: List<Song>, failed: (Song) -> Boolean): List<Song> =
  candidates.asSequence()
    // Filter failures before grouping, so another song in that album can supply its cover.
    .filterNot(failed)
    // Remote metadata has no MediaStore album ID; its default value cannot identify an album.
    .distinctBy { if (it.isLocal() && it.albumId > 0) it.albumId else it.songIdentity }
    .take(4)
    .toList()

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

private val Song.coverRequestKey: Pair<SongIdentity, String>
  get() = songIdentity to data
