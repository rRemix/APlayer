package remix.myplayer.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import remix.myplayer.compose.ui.theme.LocalTheme
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
    count.coerceAtMost(GRID_MAX_SPAN_COUNT)
  }
}

@Composable
fun Modifier.clickWithRipple(circle: Boolean = true, onClick: () -> Unit): Modifier {
  var modifier = this
  if (circle) {
    modifier = modifier.clip(CircleShape)
  }
  return modifier.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = ripple(color = LocalTheme.current.ripple), onClick = onClick
  )
}

@Composable
fun Modifier.clickableWithoutRipple(
  interactionSource: MutableInteractionSource = MutableInteractionSource(),
  onClick: () -> Unit
) = this.clickable(
  interactionSource = interactionSource,
  indication = null,
) {
  onClick()
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