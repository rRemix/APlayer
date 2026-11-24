package remix.myplayer.data.model.audio

/** Tag-reader result. Loading state and concurrent reads belong to the repository. */
data class SongMetadata(
  val title: String,
  val album: String,
  val artist: String,
  val duration: Long,
  val year: String,
  val genre: String,
  val track: String?,
  val bitRate: String = "",
  val sampleRate: String = ""
) {
  fun applyTo(song: Song): Song = song.copy(
    title = title, album = album, artist = artist, duration = duration,
    year = year, genre = genre, track = track, bitRate = bitRate, sampleRate = sampleRate
  )
}
