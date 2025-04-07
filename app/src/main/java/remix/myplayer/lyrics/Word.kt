package remix.myplayer.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class Word(
  val time: Long, // in ms
  val content: String
)