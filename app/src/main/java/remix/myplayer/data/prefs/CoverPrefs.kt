package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoverPrefsEntryPoint {
  fun coverPrefs(): CoverPrefs
}

@Singleton
class CoverPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, name = "Cover") {

  fun getAlbumVersion(): Int = sp.getInt(KEY_ALBUM_VERSION, 0)

  fun putAlbumVersion(value: Int) {
    sp.edit { putInt(KEY_ALBUM_VERSION, value) }
  }

  fun getArtistVersion(): Int = sp.getInt(KEY_ARTIST_VERSION, 0)

  fun putArtistVersion(value: Int) {
    sp.edit { putInt(KEY_ARTIST_VERSION, value) }
  }

  fun getPlayListVersion(): Int = sp.getInt(KEY_PLAYLIST_VERSION, 0)

  fun putPlayListVersion(value: Int) {
    sp.edit { putInt(KEY_PLAYLIST_VERSION, value) }
  }

  fun putCover(key: String, value: String) {
    sp.edit { putString(key, value) }
  }

  fun getCover(key: String, default: String = ""): String {
    return sp.getString(key, default) ?: default
  }

  fun clearCoverUris() {
    val versionKeys = setOf(KEY_ALBUM_VERSION, KEY_ARTIST_VERSION, KEY_PLAYLIST_VERSION)
    sp.edit {
      sp.all.keys.filterNot { versionKeys.contains(it) }.forEach { remove(it) }
    }
  }

  fun clearAll() {
    clear()
  }

  companion object {
    private const val KEY_ALBUM_VERSION = "album_version"
    private const val KEY_ARTIST_VERSION = "artist_version"
    private const val KEY_PLAYLIST_VERSION = "playlist_version"
  }
}
