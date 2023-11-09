package remix.myplayer.helper

import android.provider.MediaStore.Audio

object SortOrder {
  const val SONG_A_Z = Audio.Media.DEFAULT_SORT_ORDER
  const val SONG_Z_A = "$SONG_A_Z DESC"
  const val ARTIST_A_Z = Audio.Artists.DEFAULT_SORT_ORDER
  const val ARTIST_Z_A = "$ARTIST_A_Z DESC"
  const val ALBUM_A_Z = Audio.Albums.DEFAULT_SORT_ORDER
  const val ALBUM_Z_A = "$ALBUM_A_Z DESC"
  const val DATE = Audio.Media.DATE_MODIFIED
  const val DATE_DESC = "$DATE DESC"
  const val DISPLAY_NAME_A_Z = Audio.Media.DISPLAY_NAME
  const val DISPLAY_NAME_Z_A = "$DISPLAY_NAME_A_Z DESC"
//  const val DURATION = Audio.Media.DURATION
//  const val YEAR = Audio.Media.YEAR
  const val PLAYLIST_A_Z = "name"
  const val PLAYLIST_Z_A = "$PLAYLIST_A_Z DESC"
  const val PLAYLIST_DATE = "date"
  const val TRACK_NUMBER = Audio.Media.TRACK
  const val PLAYLIST_SONG_CUSTOM = "custom"
  const val GENRE_A_Z = Audio.Genres.DEFAULT_SORT_ORDER
  const val GENRE_Z_A = "${Audio.Genres.DEFAULT_SORT_ORDER} DESC"
  const val PLAY_COUNT = "play_count"
  const val PLAY_COUNT_DESC= "play_count desc"
}