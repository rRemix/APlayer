package remix.myplayer.compose.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.prefs.SettingPrefs
import remix.myplayer.compose.repo.HistoryRepository
import remix.myplayer.compose.repo.SongRepository
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
  private val historyRepo: HistoryRepository,
  private val songRepo: SongRepository,
  private val settingPrefs: SettingPrefs,
) : ViewModel() {

  private val _refreshTrigger = MutableStateFlow(0)

  @OptIn(ExperimentalCoroutinesApi::class)
  val historySongs: StateFlow<List<Song>> = _refreshTrigger.flatMapLatest { sortOrder ->
    historyRepo.getAllHistories(settingPrefs.historySortOrder)
  }.map { histories ->
    histories.map { history ->
      history.audio_id
    }.mapNotNull { id ->
      songRepo.song(id)
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun refreshSortOrder() {
    _refreshTrigger.value = _refreshTrigger.value + 1
  }

}