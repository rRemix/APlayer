package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.updateIf

private val loadingState = MutableStateFlow(LoadingState())

fun showLoading(cancelOutside: Boolean = true, loadingText: String = "") {
  loadingState.updateIf(
    condition = { !it.dialogState.isOpen },
    transform = {
      it.dialogState.show()
      it.copy(cancelOutside = cancelOutside, loadingText = loadingText)
    }
  )
}

fun updateLoadingText(loadingText: String) {
  loadingState.update {
    it.copy(loadingText = loadingText)
  }
}

fun dismissLoading() {
  loadingState.value.dialogState.dismiss()
}

fun CoroutineScope.runWithLoading(
  cancelOutside: Boolean = true,
  loadingText: String = "",
  func: suspend () -> Unit
) {
  launch {
    try {
      showLoading(cancelOutside, loadingText)
      func()
    } finally {
      dismissLoading()
    }
  }
}

@Composable
fun LoadingDialog(
  progressColor: Color = LocalTheme.current.primary,
) {
  val state by loadingState.collectAsStateWithLifecycle()

  BaseDialog(
    show = state.dialogState.isOpen,
    onDismissRequest = {
      dismissLoading()
    },
    cancelOutside = state.cancelOutside
  ) {
    Row(
      modifier = Modifier
        .padding(24.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      CircularProgressIndicator(
        color = progressColor,
        strokeWidth = 4.dp,
        modifier = Modifier
          .padding(end = 24.dp)
          .size(48.dp)
      )

      val text =
        if (state.loadingText.isEmpty()) LocalContext.current.getString(R.string.please_wait) else state.loadingText
      TextPrimary(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Stable
private data class LoadingState(
  val dialogState: DialogState = DialogState(),
  val loadingText: String = "",
  val cancelOutside: Boolean = false
)