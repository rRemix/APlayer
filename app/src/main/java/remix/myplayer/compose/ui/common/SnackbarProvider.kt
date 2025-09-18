package remix.myplayer.compose.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState> {
  error("No SnackBarHostState provided.")
}

@Composable
fun ProvideSnackBarHostState(
  hostState: SnackbarHostState,
  content: @Composable () -> Unit
) {
  CompositionLocalProvider(LocalSnackBarHostState provides hostState) {
    content()
  }
}