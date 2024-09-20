package remix.myplayer.lyrics

import kotlinx.serialization.Serializable

@Serializable
data class Word(
  val time: Int, // in ms
  val content: String
)