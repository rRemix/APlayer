package remix.myplayer.compose.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import remix.myplayer.compose.ui.theme.LocalTheme
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
internal fun BaseDialog(
  show: Boolean,
  onDismissRequest: (() -> Unit)?,
  content: @Composable () -> Unit
) {
  if (!show) {
    return
  }
  Dialog(onDismissRequest = {
    Timber.v("BaseDialog onDismissRequest")
    onDismissRequest?.invoke()
  }) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        // TODO
        .heightIn(max = with(LocalDensity.current) {
          (LocalConfiguration.current.screenHeightDp * 0.8).dp
        }),
      color = LocalTheme.current.dialogBackground,
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 8.dp,
    ) {
      content()
    }
  }
}