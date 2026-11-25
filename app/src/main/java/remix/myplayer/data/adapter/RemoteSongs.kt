package remix.myplayer.data.adapter

import androidx.core.net.toUri
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.MusicSource
import remix.myplayer.data.model.audio.MusicSourceType
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId

/** Canonical keys are decoded paths relative to a source root; locators encode each segment once. */
object RemotePaths {
  fun withoutCredentials(url: String): String {
    val uri = url.toUri()
    return uri.buildUpon().encodedAuthority(uri.encodedAuthority?.substringAfterLast('@'))
      .clearQuery().fragment(null).build().toString()
  }

  fun relativePath(root: String, path: String): String {
    val rootPath = root.toUri().path.orEmpty().trimEnd('/')
    val filePath = if (path.contains("://")) path.toUri().path.orEmpty() else path
    return when {
      filePath == rootPath -> ""
      rootPath.isNotEmpty() && filePath.startsWith("$rootPath/") -> filePath.removePrefix("$rootPath/")
      else -> filePath.trimStart('/')
    }
  }

  fun resolve(root: String, key: String): String {
    val builder = withoutCredentials(root).trimEnd('/').toUri().buildUpon()
    key.split('/').forEach { builder.appendPath(it) }
    return builder.build().toString()
  }
}

fun WebDav.musicSource() = MusicSource(
  SourceId("webdav:$id"), MusicSourceType.WEBDAV, alias,
  RemotePaths.withoutCredentials(getRoot()).trimEnd('/')
)

fun Smb.musicSource() = MusicSource(
  SourceId("smb:$id"), MusicSourceType.SMB, alias,
  RemotePaths.withoutCredentials(server).trimEnd('/').toUri().buildUpon()
    .appendPath(share.trim('/', '\\')).build().toString()
)

fun MusicSource.song(path: String, name: String, size: Long = 0, modifiedAt: Long = 0): Song {
  val key = RemotePaths.relativePath(root, path)
  return Song(
    sourceId = id, sourceKey = key, data = RemotePaths.resolve(root, key),
    displayName = name, title = name.substringBeforeLast('.', name), size = size,
    dateModified = modifiedAt
  )
}
