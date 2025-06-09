package remix.myplayer.compose

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import remix.myplayer.misc.isPortraitOrientation

private const val PORTRAIT_SPAN_COUNT = 2
private const val GRID_MAX_SPAN_COUNT = 6

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun spanCount(): Int {
  val portraitOrientation = LocalContext.current.isPortraitOrientation()
  return if (portraitOrientation) {
    PORTRAIT_SPAN_COUNT
  } else {
    val count = LocalConfiguration.current.screenWidthDp / 180
    if (count > GRID_MAX_SPAN_COUNT) GRID_MAX_SPAN_COUNT else count
  }
}

@Composable
inline fun <reified VM : ViewModel> activityViewModel(): VM {
  val context = LocalContext.current
  return hiltViewModel(context as? ViewModelStoreOwner ?: error("context: $context is not a viewModelStoreOwner"))
}