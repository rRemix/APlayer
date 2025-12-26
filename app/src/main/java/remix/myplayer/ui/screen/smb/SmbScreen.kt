package remix.myplayer.ui.screen.smb

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.ui.clickWithRipple
import remix.myplayer.ui.dialog.AddSmbDialog
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.icon
import remix.myplayer.ui.widget.app.FAButton
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.PopupButton
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.viewmodel.SmbViewModel
import remix.myplayer.viewmodel.smbViewModel

@Composable
fun SmbScreen() {
  val vm = smbViewModel
  val smbList by vm.smbList.collectAsStateWithLifecycle()
  val theme = LocalTheme.current
  val nav = LocalNavController.current
  val context = LocalContext.current

  AddSmbDialog(vm) { alias, domain, account, pwd, server, share, editSmb ->
    if (alias.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.alias))
      return@AddSmbDialog
    }

    if (account.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.account))
      return@AddSmbDialog
    }

    if (pwd.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.pwd))
      return@AddSmbDialog
    }

    if (server.isEmpty()) {
      MessageNotifier.show(
        R.string.can_t_be_empty,
        context.getString(R.string.webdav_hint_server)
      )
      return@AddSmbDialog
    }

    if (share.isEmpty()) {
        MessageNotifier.show(
            R.string.can_t_be_empty,
            context.getString(R.string.share)
        )
        return@AddSmbDialog
    }

    if (editSmb != null) {
      val updated = editSmb.copy(
        alias = alias,
        domain = if (domain.isEmpty()) null else domain,
        account = account,
        pwd = pwd,
        server = server,
        share = share,
        lastPath = "", // Reset path or keep? Usually reset if server changes, but simple copy here.
      ).also { it.id = editSmb.id }
      vm.insertOrReplaceSmb(updated)
    } else {
      vm.insertOrReplaceSmb(Smb(alias, if (domain.isEmpty()) null else domain, account, pwd, server, share, ""))
    }
  }

  Scaffold(
    topBar = {
      CommonAppBar(stringResource(R.string.smb), actions = emptyList())
    },
    floatingActionButton = {
      FAButton(true) {
        vm.showAddSmbDialog()
      }
    },
    containerColor = theme.mainBackground
  ) { contentPadding ->

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
    ) {
      items(smbList, key = { it.id }) { smb ->
        SmbItem(smb) { res ->
          when (res) {
            R.string.connect -> {
              nav.navigate(smb)
            }

            R.string.edit -> {
              vm.showAddSmbDialog(smb)
            }

            R.string.delete -> {
              vm.deleteSmb(smb)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SmbItem(smb: Smb, onMenuClick: (Int) -> Unit) {
  val theme = LocalTheme.current
  val nav = LocalNavController.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .clickWithRipple(false) {
        nav.navigate(smb)
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.padding(start = 12.dp),
      painter = painterResource(R.drawable.icon_webdav), // Use same icon for now
      contentDescription = "IconSmbItem",
      tint = theme.icon()
    )

    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .weight(1f),
      horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(smb.alias)
      Spacer(Modifier.height(4.dp))
      TextSecondary(smb.account)
    }

    PopupButton(
      listOf(R.string.connect, R.string.edit, R.string.delete),
      contentDescription = "SmbPopupButton",
      onMenuClick = onMenuClick
    )
  }
}
