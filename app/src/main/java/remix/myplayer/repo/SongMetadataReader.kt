package remix.myplayer.repo

import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongMetadata

/** Platform I/O boundary; caching and in-flight task ownership stay in SongMetadataRepository. */
fun interface SongMetadataReader {
  suspend fun read(song: Song): SongMetadata
}
