package remix.myplayer.data.remote

import java.io.Closeable
import java.io.IOException

/** Operates on decoded paths relative to the configured source root. */
interface RemoteFileSystem {
  suspend fun checkConnection()
  suspend fun list(path: String = ""): List<RemoteFile>
  suspend fun stat(path: String): RemoteFile
  /** Delete one file or an EMPTY directory. Never recursively deletes. */
  suspend fun delete(path: String, isDirectory: Boolean)
}

/** Seekable reader used by native SMB and the audio-metadata adapter. */
interface RemoteFileReader : Closeable {
  val size: Long
  /** Returns -1 at end of file; never returns 0 for a nonempty request. */
  fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int
}

interface RemoteReadableFileSystem : RemoteFileSystem {
  fun open(path: String): RemoteFileReader
}

sealed class RemoteFileException(message: String, cause: Throwable? = null) : IOException(message, cause) {
  class NotFound(cause: Throwable? = null) : RemoteFileException("Remote file not found", cause)
  class PermissionDenied(cause: Throwable? = null) : RemoteFileException("Remote access denied", cause)
}

/** Prevents absolute paths, parent traversal and paths outside the source root. */
fun remotePath(path: String): String {
  require(!path.startsWith('/')) { "Absolute remote paths are not allowed" }
  require('\\' !in path && '\u0000' !in path) { "Invalid remote path" }
  val segments = path.split('/').filter { it.isNotEmpty() }
  require(segments.none { it == "." || it == ".." }) { "Path traversal is not allowed" }
  return segments.joinToString("/")
}
