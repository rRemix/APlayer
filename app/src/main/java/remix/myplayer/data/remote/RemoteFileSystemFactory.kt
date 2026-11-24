package remix.myplayer.data.remote

import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.remote.smb.SmbFileSystem
import remix.myplayer.data.remote.webdav.WebDavFileSystem
import javax.inject.Inject

/** Configuration/storage remain protocol-specific; browsing uses the same API. */
class RemoteFileSystemFactory @Inject constructor() {
  fun create(source: Smb): RemoteReadableFileSystem = SmbFileSystem.fromConfig(source)
  fun create(source: WebDav): RemoteFileSystem = WebDavFileSystem(source)
}
