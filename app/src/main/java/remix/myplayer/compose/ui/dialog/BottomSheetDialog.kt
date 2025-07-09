package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import remix.myplayer.compose.ui.theme.LocalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetDialog(
  state: SheetState,
  onDismissRequest: (() -> Unit) = {},
  content: @Composable () -> Unit
) {
  if (!state.isVisible) {
    return
  }

  ModalBottomSheet(
    sheetState = state,
    dragHandle = null,
    onDismissRequest = onDismissRequest,
    sheetMaxWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.dp },
    containerColor = LocalTheme.current.dialogBackground,
    shape = RectangleShape,
    modifier = Modifier.padding(top = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.6f)
  ) {
    content()
  }
}