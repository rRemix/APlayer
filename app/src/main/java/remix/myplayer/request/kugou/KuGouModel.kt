package remix.myplayer.request.kugou

data class KuGouSong(
  val id: Long,
  val hash: String,
  val title: String,
  val artists: List<String>,
  val album: String?,
  val durationMs: Long
)

data class KugouLyricCandidate(
  val id: Long,
  val accesskey: String,
  val durationMs: Long,
  val score: Int
)