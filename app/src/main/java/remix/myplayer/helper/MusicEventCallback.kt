package remix.myplayer.helper

import remix.myplayer.bean.mp3.Song
import remix.myplayer.service.MusicService

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged(name: String)

  fun onServiceConnected(service: MusicService)

  fun onMetaChanged()

  fun onPlayStateChange()

  fun onServiceDisConnected()

  fun onTagChanged(oldSong: Song, newSong: Song)
}
