package remix.myplayer.data.model.audio

enum class MusicSourceType { MEDIASTORE, WEBDAV, SMB }

data class MusicSource(
  val id: SourceId,
  val type: MusicSourceType,
  val name: String,
  /** Credential-free access root. */
  val root: String
)
