package remix.myplayer.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.MODE_LOOP
import remix.myplayer.misc.helper.MusicEventCallback
import remix.myplayer.repo.PlayQueueRepository
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.util.Util
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val playQueueRepository: PlayQueueRepository,
    private val settingPrefs: SettingPrefs,
) : ViewModel(), MusicEventCallback {

    private var serviceRef: WeakReference<MusicService>? = null

    val lastOp: Int
        get() = serviceRef?.get()?.operation ?: Command.NEXT

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    // 播放队列
    private val _playQueueSongs = MutableStateFlow<List<Song>>(emptyList())
    val playQueueSongs: StateFlow<List<Song>> = _playQueueSongs.asStateFlow()

    init {
        viewModelScope.launch {
            playQueueRepository.getAllSongs().collect {
                _playQueueSongs.value = it
            }
        }
    }

    fun updatePlaybackState(
        song: Song? = null,
        newSong: Song? = null,
        playing: Boolean? = null,
        playModel: Int? = null
    ) {
        _playbackState.update { current ->
            current.copy(
                song = song ?: current.song,
                nextSong = newSong ?: serviceRef?.get()?.nextSong ?: Song.EMPTY_SONG,
                playing = playing ?: current.playing,
                playMode = playModel ?: serviceRef?.get()?.playModel ?: MODE_LOOP
            )
        }
    }

    fun setPlayModel(mode: Int) {
        serviceRef?.get()?.playModel = mode
        updatePlaybackState()
    }

    fun setProgress(progress: Long) {
        serviceRef?.get()?.setProgress(progress)
    }

    fun getProgress(): Int {
        val service = serviceRef?.get() ?: return 0
        return service.progress.coerceAtMost(service.duration)
    }

    fun isPlayingSong(songId: Long): Boolean {
        return _playbackState.value.song.id == songId
    }

    fun removeFromQueue(id: Long) {
        viewModelScope.launch {
            val count = playQueueRepository.removeByAudioIds(listOf(id))
            if (count > 0 && id == _playbackState.value.song.id) {
                Util.sendCMDLocalBroadcast(Command.NEXT)
            }
        }
    }

    fun insertToQueue(queue: List<Song>) {
        viewModelScope.launch {
            val ids = playQueueRepository.insert(queue)
            MessageNotifier.show(R.string.add_song_playqueue_success, ids.filter { it != 0L }.size)
        }
    }

    private val _swatch = MutableStateFlow(defaultSwatch)
    val swatch = _swatch.asStateFlow()

    fun updateSwatch(bitmap: Bitmap?) {
        if (settingPrefs.playingScreenBackground != SettingPrefs.BACKGROUND_ADAPTIVE_COLOR) {
            return
        }
        if (bitmap == null) {
            _swatch.value = defaultSwatch
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            var newSwatch = defaultSwatch
            val palette = Palette.from(bitmap).generate()
            if (palette.mutedSwatch != null) {
                newSwatch = palette.mutedSwatch!!
            } else {
                val swatches = ArrayList<Swatch>(palette.swatches)
                swatches.sortWith(Comparator { o1, o2 -> o1.population.compareTo(o2.population) })
                newSwatch = if (swatches.isNotEmpty()) swatches[0] else defaultSwatch
            }
            _swatch.value = newSwatch
        }
    }

    fun isKeepScreenOn() = settingPrefs.keepScreenOn

    override fun onServiceConnected(service: MusicService) {
        Timber.v("onServiceConnected")
        serviceRef = WeakReference(service)
        onMetaChanged()
    }

    override fun onMetaChanged() {
        Timber.v("onMetaChanged")
        val service = serviceRef?.get() ?: return
        updatePlaybackState(
            song = service.currentSong,
            playing = service.isPlaying
        )
    }

    override fun onPlayStateChange() {
        Timber.v("onPlayStateChange")
        val service = serviceRef?.get() ?: return
        updatePlaybackState(playing = service.isPlaying)
    }

    override fun onServiceDisConnected() {
        Timber.v("onServiceDisConnected")
        serviceRef?.clear()
        serviceRef = null
    }

    override fun onMediaStoreChanged() {
        Timber.v("onMediaStoreChanged")
    }

    override fun onPermissionChanged(has: Boolean) {
        Timber.v("onPermissionChanged: $has")
    }

    override fun onPlayListChanged(name: String) {
    }

    override fun onTagChanged(oldSong: Song, newSong: Song) {
    }

    companion object {
        val defaultSwatch = Swatch(Color.GRAY, 100)
    }
}

@Stable
data class PlaybackState(
    val song: Song = Song.EMPTY_SONG,
    val nextSong: Song = Song.EMPTY_SONG,
    val playing: Boolean = false,
    val playMode: Int = MODE_LOOP
)