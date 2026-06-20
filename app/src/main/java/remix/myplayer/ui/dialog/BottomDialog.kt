package remix.myplayer.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import remix.myplayer.ui.theme.LocalTheme

@Composable
fun BottomDialog(
  visible: Boolean,
  onDismissRequest: (() -> Unit) = {},
  animationDurationMillis: Int = 220,
  content: @Composable BoxScope.() -> Unit
) {
  val visibleState = remember { MutableTransitionState(false) }
  visibleState.targetState = visible

  if (!visibleState.currentState && !visibleState.targetState) {
    return
  }

  val theme = LocalTheme.current
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    )
  ) {
    val view = LocalView.current
    SideEffect {
      val window = (view as? DialogWindowProvider)?.window ?: return@SideEffect
      @Suppress("DEPRECATION")
      window.navigationBarColor = android.graphics.Color.TRANSPARENT
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = theme.isLight
    }

    BoxWithConstraints(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.BottomCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onDismissRequest
          )
      )
      AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(),
        exit = slideOutVertically(
          animationSpec = tween(animationDurationMillis),
          targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(animationDurationMillis))
      ) {
        Surface(
          modifier = Modifier
            .fillMaxWidth(1f)
            .heightIn(max = maxHeight * 0.4f)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {}
            ),
          color = theme.dialogBackground,
          shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
          shadowElevation = 8.dp,
        ) {
          Box(modifier = Modifier.navigationBarsPadding()) {
            content()
          }
        }
      }
    }
  }
}
