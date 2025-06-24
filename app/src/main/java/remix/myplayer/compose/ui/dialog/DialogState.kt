package remix.myplayer.compose.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class DialogState(initialValue: Boolean = false) {
  var isOpen by mutableStateOf(initialValue)
    private set

  fun show() {
    isOpen = true
  }

  fun dismiss() {
    isOpen = false
  }

  companion object {
    fun Saver(): Saver<DialogState, *> {
      return Saver(
        save = {
          it.isOpen
        },
        restore = {
          DialogState(it)
        }
      )
    }
  }
}

@Composable
fun rememberDialogState(initial: Boolean = false): DialogState = rememberSaveable(
  saver = DialogState.Saver(),
  init = { DialogState(initial) }
)