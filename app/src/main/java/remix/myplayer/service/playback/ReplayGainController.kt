package remix.myplayer.service.playback

import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.ReplayGain
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.helper.AudioTagFile
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads ReplayGain tags and applies the resulting gain through the
 * [ReplayGainAudioProcessor] shared with the ExoPlayer audio sink.
 */
@OptIn(UnstableApi::class)
@Singleton
class ReplayGainController @Inject constructor(
  private val settingPrefs: SettingPrefs,
) {

  val processor = ReplayGainAudioProcessor()

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val cache = LruCache<String, ReplayGain>(512)

  private var requestId = 0L

  /**
   * Compute and apply the ReplayGain for [song]. Called on track transitions
   * and when the ReplayGain settings change.
   */
  fun applyFor(song: Song?) {
    val request = ++requestId
    if (song !is Song.Local || !settingPrefs.replayGainEnabled) {
      processor.setGain(null)
      return
    }
    cache.get(song.data)?.let {
      applyGain(it)
      return
    }
    processor.setGain(null)
    scope.launch {
      val gain = withContext(Dispatchers.IO) {
        runCatching { AudioTagFile.readReplayGain(File(song.data)) }.getOrElse {
          Timber.w(it, "Fail to read ReplayGain from ${song.data}")
          null
        }
      }
      // cache missing tags as empty to avoid re-reading the file on every transition
      cache.put(song.data, gain ?: ReplayGain())
      if (request == requestId && settingPrefs.replayGainEnabled) {
        applyGain(gain)
      }
    }
  }

  private fun applyGain(gain: ReplayGain?) {
    val preferAlbumGain = settingPrefs.replayGainMode == SettingPrefs.REPLAY_GAIN_MODE_ALBUM
    val selectedAlbumGain = if (preferAlbumGain) {
      gain?.albumGainDb != null
    } else {
      gain?.trackGainDb == null && gain?.albumGainDb != null
    }
    val tagGainDb = if (selectedAlbumGain) gain?.albumGainDb else gain?.trackGainDb
    val gainDb = tagGainDb?.plus(settingPrefs.replayGainPreampDb)
      ?: settingPrefs.replayGainMissingGainDb
    val peak = if (selectedAlbumGain) gain?.albumPeak else gain?.trackPeak
    processor.setGain(gainDb, peak, settingPrefs.replayGainPeakProtection)
  }
}
