package remix.myplayer.data.remote.smb

import remix.myplayer.data.remote.RemoteFile

/** Only native bridge; no UI, credentials or paths are persisted here. */
object SmbNativeBridge {
  init { System.loadLibrary("aplayer_smb") }

  external fun checkConnection(host: String, share: String, user: String, password: String, domain: String)
  external fun list(host: String, share: String, user: String, password: String, domain: String, path: String): Array<RemoteFile>
  external fun stat(host: String, share: String, user: String, password: String, domain: String, path: String): RemoteFile
  external fun delete(host: String, share: String, user: String, password: String, domain: String, path: String, isDirectory: Boolean)
  external fun open(host: String, share: String, user: String, password: String, domain: String, path: String): Long
  external fun size(handle: Long): Long
  external fun read(handle: Long, position: Long, buffer: ByteArray, offset: Int, length: Int): Int
  external fun close(handle: Long)
}
