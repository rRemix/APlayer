package remix.myplayer.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.viewmodel.webDavViewModel

@Composable
fun AddWebDavDialog(onPositive: (String, String, String, String, WebDav?) -> Unit) {
  val vm = webDavViewModel
  val state by vm.addWebDavState.collectAsStateWithLifecycle()

  val alias = state.alias
  val account = state.account
  val pwd = state.pwd
  val server = state.server

  fun reset() {
    vm.updateAddWebDavState("", "", "", "")
  }

  NormalDialog(
    dialogState = state.dialogState,
    onDismissRequest = {
      reset()
    },
    titleRes = R.string.webdav,
    positiveRes = if (state.editWebDav == null) R.string.add else R.string.update,
    onPositive = {
      reset()
      onPositive(alias, account, pwd, server, state.editWebDav)
    },
    negativeRes = null,
    custom = {
      Column {
        EditField(alias, R.string.alias, isError = alias.isEmpty()) {
          vm.updateAddWebDavState(alias = it)
        }
        EditField(account, R.string.account, isError = account.isEmpty()) {
          vm.updateAddWebDavState(account = it)
        }
        EditField(pwd, R.string.pwd, isError = pwd.isEmpty()) {
          vm.updateAddWebDavState(pwd = it)
        }
        EditField(server, R.string.webdav_hint_server, isError = server.isEmpty()) {
          vm.updateAddWebDavState(server = it)
        }
      }
    }
  )
}