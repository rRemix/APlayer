package remix.myplayer.compose.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import remix.myplayer.compose.ui.theme.LocalTheme
import timber.log.Timber

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
        .wrapContentHeight(),
      color = LocalTheme.current.dialogBackground,
      shape = RoundedCornerShape(12.dp),
      shadowElevation = 8.dp,
    ) {
      content()
    }
  }
}