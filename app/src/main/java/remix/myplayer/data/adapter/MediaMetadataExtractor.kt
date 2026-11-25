package remix.myplayer.data.adapter

import android.media.MediaMetadataRetriever
import android.os.Build
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongMetadata

/** Parse tags from a retriever whose data source was already opened by the caller. */
fun MediaMetadataRetriever.readSongMetadata(song: Song): SongMetadata {
  fun tag(key: Int): String? = extractMetadata(key)
  return SongMetadata(
    title = tag(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: song.title,
    album = tag(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: song.album,
    artist = tag(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: song.artist,
    duration = tag(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: song.duration,
    year = tag(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: song.year,
    genre = tag(MediaMetadataRetriever.METADATA_KEY_GENRE) ?: song.genre,
    track = tag(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) ?: song.track,
    bitRate = tag(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()?.div(1000)?.toString().orEmpty(),
    sampleRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
      tag(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE).orEmpty() else song.sampleRate
  )
}
