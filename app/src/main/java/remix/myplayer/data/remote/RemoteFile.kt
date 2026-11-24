package remix.myplayer.data.remote

/** Decoded, slash-separated, root-relative paths. Never contains credentials. */
data class RemoteFile(
  val name: String,
  val path: String,
  val isDirectory: Boolean,
  val size: Long = 0L,
  val lastModified: Long = 0L
) {
  val isAudio: Boolean
    get() = !isDirectory && name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

  companion object {
    /** Container formats accepted for remote browsing, regardless of source protocol. */
    private val AUDIO_EXTENSIONS = setOf(
      "mp3", "m4a", "m4b", "aac", "flac", "wav", "aif", "aiff", "ogg", "opus", "ape"
    )
  }
}
