package remix.myplayer.service.playback

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.PlaybackRecordPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.playbackSourceKey
import remix.myplayer.repo.PlayListRepository
import remix.myplayer.repo.SongRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class ProgressManager @Inject constructor(
  private val settingPrefs: SettingPrefs,
  private val playbackRecordPrefs: PlaybackRecordPrefs,
  private val songRepository: SongRepository,
  private val playListRepository: PlayListRepository
) {

  private var progressJob: Job? = null
  private var resumeJob: Job? = null
  private var sourceJob: Job? = null

  private var source: Source? = null

  fun start(scope: CoroutineScope, playbackProvider: () -> Playback) {
    if (progressJob?.isActive == true) {
      return
    }
    progressJob = scope.launch {
      while (isActive) {
        val playback = playbackProvider()
        val progress = playback.position
        if (settingPrefs.playAtBreakPoint && progress > 0) {
          settingPrefs.lastProgress = progress.toInt()
        }
        saveSource(playback)

        delay(INTERVAL_SAVE_PROGRESS)
      }
    }
  }

  fun stop() {
    progressJob?.cancel()
    progressJob = null
    cancelResume()
    sourceJob?.cancel()
    sourceJob = null
    source = null
  }

  fun isPlayingFrom(model: APlayerModel): Boolean {
    val key = model.playbackSourceKey()
    return key != null && source?.key == key
  }

  fun switchSource(
    scope: CoroutineScope,
    playback: Playback,
    model: APlayerModel? = null
  ) {
    cancelResume()
    saveSource(playback)
    val key = model.playbackSourceKey()
    val nextSource = when {
      key == null -> null
      model is PlayList -> Source.Playlist(key, model.id, model.audioIds.toSet())
      model is Folder -> Source.Folder(key, model.path)
      else -> null
    }
    bindSource(scope, nextSource)
  }

  suspend fun restoreSource(
    scope: CoroutineScope,
    hasSavedQueue: Boolean,
    canRestore: () -> Boolean
  ): Boolean {
    val key = playbackRecordPrefs.lastPlaybackSourceKey.takeIf { hasSavedQueue }
    val restoredSource = try {
      when {
        key == null -> null
        key.startsWith("playlist:") -> {
          val id = key.removePrefix("playlist:").toLongOrNull()
          val playlist = id?.let { playListRepository.observePlayList(it).first() }
          playlist?.let { Source.Playlist(key, it.id, it.audioIds.toSet()) }
        }
        key.startsWith("folder:") -> {
          val path = key.removePrefix("folder:")
          path.takeIf { it.isNotEmpty() }?.let { Source.Folder(key, it) }
        }
        else -> null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Unable to restore playback source %s", key)
      null
    }
    // 查询歌单期间用户可能已建立新队列，不能覆盖新来源。
    if (!canRestore()) return false
    bindSource(scope, restoredSource, activated = false)
    return true
  }

  private fun bindSource(
    scope: CoroutineScope,
    nextSource: Source?,
    activated: Boolean = true
  ) {
    sourceJob?.cancel()
    sourceJob = null
    source = nextSource
    nextSource?.isActivated = activated
    playbackRecordPrefs.lastPlaybackSourceKey = nextSource?.key
    if (nextSource !is Source.Playlist) return

    sourceJob = scope.launch {
      try {
        playListRepository.observePlayList(nextSource.id)
          .map { it?.audioIds?.toSet().orEmpty() }
          .distinctUntilChanged()
          .collect { songIds ->
            if (source === nextSource) {
              nextSource.songIds = songIds
            }
          }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.w(e, "Unable to observe playback source %s", nextSource.key)
      }
    }
  }

  fun activateSource() {
    source?.isActivated = true
  }

  fun activateAndSave(playback: Playback) {
    activateSource()
    saveSource(playback)
  }

  fun saveSource(playback: Playback) {
    val source = source?.takeIf { it.isActivated } ?: return
    if (!playback.isPrepared) return
    val song = if (playback.isEnded) {
      playback.getPlaylist().firstOrNull { source.contains(it) }
    } else {
      playback.currentSong?.takeIf { source.contains(it) }
    } ?: return
    playbackRecordPrefs.savePlaybackRecord(
      source.key,
      PlaybackRecordPrefs.PlaybackRecord(song.id, if (playback.isEnded) 0L else playback.position)
    )
  }

  suspend fun hasResumableRecord(model: APlayerModel): Boolean {
    val key = model.playbackSourceKey() ?: return false
    val record = playbackRecordPrefs.getPlaybackRecord(key) ?: return false
    return try {
      withContext(Dispatchers.IO) {
        songRepository.getSongsByModels(listOf(model)).any { it.id == record.songId }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.w(e, "Unable to check playback record %s", key)
      false
    }
  }

  fun resumeFrom(
    scope: CoroutineScope,
    model: APlayerModel,
    playback: Playback,
    onContinue: () -> Unit,
    onRestore: (songs: List<Song>, index: Int, positionMs: Long) -> Unit
  ) {
    cancelResume()
    val key = model.playbackSourceKey() ?: return
    val currentSong = playback.currentSong
    val currentSource = source
    val containsCurrentSong = currentSong != null && currentSource?.contains(currentSong) == true

    if (currentSource?.isActivated == true && currentSource.key == key &&
      !playback.isEnded && containsCurrentSong
    ) {
      onContinue()
      return
    }

    val record = playbackRecordPrefs.getPlaybackRecord(key) ?: return
    resumeJob = scope.launch {
      try {
        val songs = withContext(Dispatchers.IO) {
          songRepository.getSongsByModels(listOf(model))
        }
        val index = songs.indexOfFirst { it.id == record.songId }
        if (index < 0) return@launch
        // 回调切换来源时会取消待恢复任务，先清空引用，避免取消当前协程。
        resumeJob = null
        onRestore(songs, index, record.positionMs.coerceAtLeast(0L))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.w(e, "Unable to resume %s", key)
      }
    }
  }

  fun cancelResume() {
    resumeJob?.cancel()
    resumeJob = null
  }

  private sealed class Source(val key: String) {
    // 只恢复队列来源时不保存独立进度；实际使用后激活，暂停时仍保持激活。
    var isActivated: Boolean = true

    abstract fun contains(song: Song): Boolean

    class Playlist(key: String, val id: Long, var songIds: Set<Long>) : Source(key) {
      override fun contains(song: Song): Boolean = song.isLocal() && song.id in songIds
    }

    class Folder(key: String, private val path: String) : Source(key) {
      override fun contains(song: Song): Boolean =
        song.isLocal() && song.data.substringBeforeLast('/', missingDelimiterValue = "") == path
    }
  }

  private companion object {
    private val INTERVAL_SAVE_PROGRESS = 1000L.milliseconds
  }
}
