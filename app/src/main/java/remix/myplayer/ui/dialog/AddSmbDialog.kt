package remix.myplayer.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.Smb
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import remix.myplayer.viewmodel.SmbViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddSmbDialog(
  vm: SmbViewModel = viewModel(),
  onPositive: (String, String, String, String, String, String, Smb?) -> Unit
) {
  val state by vm.addSmbState.collectAsStateWithLifecycle()

  val alias = state.alias
  val domain = state.domain
  val account = state.account
  val pwd = state.pwd
  val server = state.server
  val share = state.share

  fun reset() {
    vm.updateAddSmbState("", "", "", "", "", "")
  }

  NormalDialog(
    dialogState = state.dialogState,
    onDismissRequest = {
      reset()
    },
    titleRes = R.string.smb, // Need to make sure this string exists, or use string resource for "SMB"
    positiveRes = if (state.editSmb == null) R.string.add else R.string.update,
    onPositive = {
      reset()
      onPositive(alias, domain, account, pwd, server, share, state.editSmb)
    },
    negativeRes = null,
    custom = {
      Column {
        EditField(alias, R.string.alias, isError = alias.isEmpty()) {
          vm.updateAddSmbState(alias = it)
        }
        EditField(server, R.string.webdav_hint_server, isError = server.isEmpty()) {
          vm.updateAddSmbState(server = it)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.weight(1f)) {
            EditField(share, R.string.share, isError = share.isEmpty()) {
              vm.updateAddSmbState(share = it)
            }
          }
          Spacer(modifier = Modifier.width(8.dp))
          Box {
            TextButton(
              onClick = { vm.listShares() },
              enabled = !state.isLoadingShares && server.isNotEmpty()
            ) {
              if (state.isLoadingShares) {
                 CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                 Text("List") // TODO: use string resource
              }
            }
            DropdownMenu(
              expanded = state.showShareSelection,
              onDismissRequest = { vm.dismissShareSelection() }
            ) {
              if (state.availableShares.isEmpty()) {
                 DropdownMenuItem(text = { Text("No shares found") }, onClick = { vm.dismissShareSelection() })
              } else {
                 state.availableShares.forEach { shareName ->
                   DropdownMenuItem(
                     text = { Text(shareName) },
                     onClick = {
                        vm.updateAddSmbState(share = shareName)
                        vm.dismissShareSelection()
                     }
                   )
                 }
              }
            }
          }
        }
        EditField(domain, R.string.domain, isError = false) { // Need string for "Domain"
          vm.updateAddSmbState(domain = it)
        }
        EditField(
          account,
          R.string.account,
          isError = account.isEmpty(),
          contentType = ContentType.Username
        ) {
          vm.updateAddSmbState(account = it)
        }
        EditField(
          pwd,
          R.string.pwd,
          isError = pwd.isEmpty(),
          contentType = ContentType.Password,
          keyboardType = KeyboardType.Password,
          visualTransformation = PasswordVisualTransformation()
        ) {
          vm.updateAddSmbState(pwd = it)
        }
      }
    }
  )
}
