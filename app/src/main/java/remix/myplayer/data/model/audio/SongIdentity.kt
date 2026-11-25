package remix.myplayer.data.model.audio

import kotlinx.serialization.Serializable

@Serializable
data class SourceId(val value: String) : java.io.Serializable {
  val isRemote: Boolean get() = value.startsWith("webdav:") || value.startsWith("smb:")

  companion object {
    val MEDIASTORE = SourceId("mediastore:external")
  }
}

/** Persist identity independently of the current URL, credentials and metadata. */
@Serializable
data class SongIdentity(val sourceId: SourceId, val sourceKey: String) : java.io.Serializable {
  // A length prefix prevents ambiguity when source IDs and paths contain separators.
  val key: String get() = "${sourceId.value.length}:${sourceId.value}$sourceKey"

  companion object {
    fun local(id: Long) = SongIdentity(SourceId.MEDIASTORE, id.toString())
  }
}
