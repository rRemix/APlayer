package remix.myplayer.data.model.audio

import kotlinx.serialization.Serializable

/** UI/domain value; persistence entities and mutable collections stay in the data layer. */
@Serializable
data class PlayList(
  val id: Long,
  val name: String,
  val songIdentities: List<SongIdentity>,
  val date: Long
) : APlayerModel {
  fun isFavorite(): Boolean = id == 1L
  override fun getKey(): String = id.toString()
}
