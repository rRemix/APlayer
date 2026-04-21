package remix.myplayer.viewmodel.settings

import remix.myplayer.data.prefs.SettingPrefs


enum class SortCategory {
  SONG, ALBUM, ARTIST, PLAYLIST, GENRE, FOLDER, HISTORY,
  ALBUM_DETAIL, ARTIST_DETAIL, PLAYLIST_DETAIL, GENRE_DETAIL, FOLDER_DETAIL;

  fun getOrder(settingPrefs: SettingPrefs, playlistId: Long? = null): String {
    return when (this) {
      SONG -> settingPrefs.songSortOrder
      ALBUM -> settingPrefs.albumSortOrder
      ARTIST -> settingPrefs.artistSortOrder
      PLAYLIST -> settingPrefs.playlistSortOrder
      GENRE -> settingPrefs.genreSortOrder
      FOLDER -> settingPrefs.folderSortOrder
      HISTORY -> settingPrefs.historySortOrder
      ALBUM_DETAIL -> settingPrefs.albumDetailSortOrder
      ARTIST_DETAIL -> settingPrefs.artistDetailSortOrder
      PLAYLIST_DETAIL -> {
        if (playlistId != null) {
          settingPrefs.getPlayListDetailSortOrder(playlistId)
        } else {
          settingPrefs.playListDetailSortOrder
        }
      }
      GENRE_DETAIL -> settingPrefs.genreDetailSortOrder
      FOLDER_DETAIL -> settingPrefs.folderDetailSortOrder
    }
  }

  fun saveOrder(newOrder: String, settingPrefs: SettingPrefs, playlistId: Long? = null): Boolean {
    val old = getOrder(settingPrefs, playlistId)
    if (old != newOrder) {
      when (this) {
        SONG -> settingPrefs.songSortOrder = newOrder
        ALBUM -> settingPrefs.albumSortOrder = newOrder
        ARTIST -> settingPrefs.artistSortOrder = newOrder
        PLAYLIST -> settingPrefs.playlistSortOrder = newOrder
        GENRE -> settingPrefs.genreSortOrder = newOrder
        FOLDER -> settingPrefs.folderSortOrder = newOrder
        HISTORY -> settingPrefs.historySortOrder = newOrder
        ALBUM_DETAIL -> settingPrefs.albumDetailSortOrder = newOrder
        ARTIST_DETAIL -> settingPrefs.artistDetailSortOrder = newOrder
        PLAYLIST_DETAIL -> {
          if (playlistId != null) {
            settingPrefs.setPlayListDetailSortOrder(playlistId, newOrder)
          } else {
            settingPrefs.playListDetailSortOrder = newOrder
          }
        }
        GENRE_DETAIL -> settingPrefs.genreDetailSortOrder = newOrder
        FOLDER_DETAIL -> settingPrefs.folderDetailSortOrder = newOrder
      }
      return true
    }
    return false
  }
}
