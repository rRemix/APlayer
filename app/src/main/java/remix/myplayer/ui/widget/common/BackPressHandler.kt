package remix.myplayer.ui.widget.common

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

@Composable
fun BackPressHandler(
  enabled: Boolean = true,
  onBackPressed: () -> Unit
) {
  val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
  val backCallback = remember {
    object : OnBackPressedCallback(enabled) {
      override fun handleOnBackPressed() {
        onBackPressed()
      }
    }
  }

  LaunchedEffect(enabled) {
    backCallback.isEnabled = enabled
  }

  DisposableEffect(dispatcher) {
    dispatcher?.addCallback(backCallback)
    onDispose {
      backCallback.remove()
    }
  }
}