package remix.myplayer.compose

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    if (count > GRID_MAX_SPAN_COUNT) GRID_MAX_SPAN_COUNT else count
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

fun Color.toHexString(withAlpha: Boolean = false): String {
  val argb = this.toArgb()
  return if (withAlpha) {
    String.format(
      "%08X",
      argb
    )
  } else {
    String.format(
      "%06X",
      argb and 0xFFFFFF  // 移除Alpha通道
    )
  }
}