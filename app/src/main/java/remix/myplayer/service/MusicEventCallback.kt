package remix.myplayer.service

import remix.myplayer.data.model.audio.Song

interface MusicEventCallback {
  fun onMediaStoreChanged()

  fun onPermissionChanged(has: Boolean)

  fun onPlayListChanged(name: String)

  fun onServiceConnected(service: MusicService)

  fun onServiceDisConnected()

  fun onTagChanged(oldSong: Song?, newSong: Song)
}
