package remix.myplayer.data.adapter

import android.media.MediaMetadataRetriever
import android.os.Build
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongMetadata
import remix.myplayer.repo.MusicSourceRepository
import remix.myplayer.repo.SongMetadataReader
import remix.myplayer.service.playback.SmbMediaDataSource
import javax.inject.Inject

class AndroidSongMetadataReader @Inject constructor(
  private val sources: MusicSourceRepository
) : SongMetadataReader {
  override suspend fun read(song: Song): SongMetadata {
    var smb: SmbMediaDataSource? = null
    val retriever = MediaMetadataRetriever()
    try {
      val source = sources.resolve(song)
      if (song.sourceId.value.startsWith("smb:") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        smb = SmbMediaDataSource(source.locator)
        retriever.setDataSource(smb)
      } else if (song.isLocal()) {
        retriever.setDataSource(song.data)
      } else {
        retriever.setDataSource(source.locator, source.headers)
      }
      return retriever.readSongMetadata(song)
    } finally {
      runCatching { retriever.release() }
      runCatching { smb?.close() }
    }
  }
}
