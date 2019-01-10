package remix.myplayer.helper

import remix.myplayer.service.MusicService

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged()

  fun onServiceConnected(service: MusicService)

  fun onMetaChanged()

  fun onPlayStateChange()

  fun onServiceDisConnected()
}
