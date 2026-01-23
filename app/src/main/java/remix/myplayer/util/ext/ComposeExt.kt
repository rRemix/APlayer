package remix.myplayer.util.ext

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState

@Composable
fun ShowLyricTipDialog(onPositive: () -> Unit) {
//    if (lyricPrefs.tipShown) {
//      onPositive()
//    } else {
//
//    }

  val state = rememberDialogState(true)
  NormalDialog(
    dialogState = state,
    contentRes = R.string.local_lyric_tip,
    onPositive = {
      onPositive()
    }
  )
}

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
    count.coerceAtMost(GRID_MAX_SPAN_COUNT)
  }
}

@Composable
inline fun <reified VM : ViewModel> activityViewModel(): VM {
  val context = LocalContext.current
  return hiltViewModel(
    context as? ViewModelStoreOwner ?: error("context: $context is not a viewModelStoreOwner")
  )
}

@Composable
fun <T : Any> rememberMutableStateSetOf(vararg elements: T): SnapshotStateSet<T> {
  return rememberSaveable(saver = object : Saver<SnapshotStateSet<T>, Set<T>> {
    override fun restore(value: Set<T>): SnapshotStateSet<T> {
      return SnapshotStateSet<T>().also {
        it.addAll(value)
      }
    }

    override fun SaverScope.save(value: SnapshotStateSet<T>): Set<T>? {
      value.forEach { item ->
        require(canBeSaved(item)) { "item can't be saved" }
      }
      return if (value.isNotEmpty()) HashSet(value) else null
    }

  }) {
    SnapshotStateSet<T>().also {
      it.addAll(elements)
    }
  }
}

@Composable
fun <T : Any> rememberMutableStateListOf(vararg elements: T): SnapshotStateList<T> {
  return rememberSaveable(saver = snapshotStateListSaver()) {
    elements.toList().toMutableStateList()
  }
}

private fun <T : Any> snapshotStateListSaver() = listSaver<SnapshotStateList<T>, T>(
  save = { stateList -> stateList.toList() },
  restore = { it.toMutableStateList() },
)

@Composable
fun CenterInBox(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    content()
  }
}

inline fun <T> MutableStateFlow<T>.updateIf(
  condition: (T) -> Boolean,
  crossinline transform: (T) -> T
) {
  update {
    if (condition(it)) {
      transform(it)
    } else {
      it
    }
  }
}
