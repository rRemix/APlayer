package remix.myplayer.data.remote

import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.MusicSource

/** UI-independent identity and location information for one configured remote source. */
sealed interface RemoteBrowserSource {
  val key: String
  val alias: String
  val root: String
  val lastUrl: String
  val musicSource: MusicSource
  fun buildPathStack(): List<String>

  data class SmbSource(val config: Smb) : RemoteBrowserSource {
    override val key: String get() = "smb:${config.id}"
    override val alias: String get() = config.alias
    override val root: String get() = config.getRoot()
    override val lastUrl: String get() = config.lastUrl
    override val musicSource: MusicSource get() = config.musicSource()
    override fun buildPathStack(): List<String> = config.buildPathStack(lastUrl)
  }

  data class WebDavSource(val config: WebDav) : RemoteBrowserSource {
    override val key: String get() = "webdav:${config.id}"
    override val alias: String get() = config.alias
    override val root: String get() = config.getRoot()
    override val lastUrl: String get() = config.lastUrl
    override val musicSource: MusicSource get() = config.musicSource()
    override fun buildPathStack(): List<String> = config.buildPathStack(lastUrl)
  }
}
