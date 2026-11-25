package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Folder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRecordPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "PlaybackRecord") {

  var lastPlaybackSourceKey: String?
    get() = sp.getString("last_playback_source_key", null)
    set(value) {
      sp.edit { putString("last_playback_source_key", value) }
    }

  @Serializable
  data class PlaybackRecord(val songIdentity: SongIdentity, val positionMs: Long)

  @Serializable
  private data class LegacyRecord(val songId: Long, val positionMs: Long)

  fun getPlaybackRecord(sourceKey: String): PlaybackRecord? =
    sp.getString(sourceKey, null)?.let {
      runCatching { Json.decodeFromString<PlaybackRecord>(it) }.getOrNull()
        ?: runCatching { Json.decodeFromString<LegacyRecord>(it) }.getOrNull()?.let { legacy ->
          PlaybackRecord(SongIdentity.local(legacy.songId), legacy.positionMs).also { record ->
            savePlaybackRecord(sourceKey, record)
          }
        }
    }

  fun savePlaybackRecord(sourceKey: String, record: PlaybackRecord) {
    val json = Json.encodeToString(record)
    if (sp.getString(sourceKey, null) != json) {
      sp.edit { putString(sourceKey, json) }
    }
  }
}

fun APlayerModel?.playbackSourceKey(): String? = when (this) {
  is PlayList -> "playlist:$id"
  is Folder -> "folder:$path"
  else -> null
}
