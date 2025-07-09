package remix.myplayer.compose.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import remix.myplayer.compose.prefs.SettingPrefs
import javax.inject.Inject

@HiltViewModel
class PlayingViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @ApplicationContext private val context: Context,
  val settingPrefs: SettingPrefs,
) : ViewModel() {
//  private val _coverBitmap = MutableStateFlow<Bitmap?>(null)
//  val coverBitmap = _coverBitmap.asStateFlow()

  fun updateSwatch(bitmap: Bitmap?) {
//    _coverBitmap.value = bitmap

    if (bitmap == null) {
      _swatch.value = defaultSwatch
      return
    }

    // make new swatch
    viewModelScope.launch(Dispatchers.IO) {
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

  private val _swatch = MutableStateFlow(defaultSwatch)
  val swatch = _swatch.asStateFlow()

  fun isKeepScreenOn() = settingPrefs.keepScreenOn

  companion object {

    val defaultSwatch = Swatch(Color.GRAY, 100)
  }
}