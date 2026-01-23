package remix.myplayer.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.ui.dialog.AddSmbDialog
import remix.myplayer.ui.dialog.AddWebDavDialog
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.icon
import remix.myplayer.ui.widget.common.PopupButton
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.SmbViewModel
import remix.myplayer.viewmodel.WebDavViewModel
import remix.myplayer.viewmodel.smbViewModel
import remix.myplayer.viewmodel.webDavViewModel

@Composable
fun RemoteScreen() {
  val nav = LocalNavController.current

  val webDavVM = webDavViewModel
  val smbVM = smbViewModel
  val webdavList by webDavVM.webDavList.collectAsStateWithLifecycle()
  val smbList by smbVM.smbList.collectAsStateWithLifecycle()

  LazyColumn(modifier = Modifier.fillMaxSize()) {
    if (webdavList.isNotEmpty()) {
      item {
        ListHeader(R.string.webdav)
      }
      items(webdavList, key = { "webdav_${it.id}" }) { webDav ->
        WebDavItem(webDav) { res ->
          when (res) {
            R.string.connect -> {
              nav.navigate(webDav)
            }

            R.string.edit -> {
              webDavVM.showAddWebDavDialog(webDav)
            }

            R.string.delete -> {
              webDavVM.deleteWebDav(webDav)
            }
          }
        }
      }
    }

    if (smbList.isNotEmpty()) {
      item {
        ListHeader(R.string.smb)
      }
      items(smbList, key = { "smb_${it.id}" }) { smb ->
        SmbItem(smb) { res ->
          when (res) {
            R.string.connect -> {
              nav.navigate(smb)
            }

            R.string.edit -> {
              smbVM.showAddSmbDialog(smb)
            }

            R.string.delete -> {
              smbVM.deleteSmb(smb)
            }
          }
        }
      }
    }
  }

  Dialogs(webDavVM, smbVM)
}

@Composable
private fun ListHeader(p: Int) {
  TextPrimary(
    stringResource(p), modifier = Modifier
      .padding(horizontal = 16.dp)
      .padding(top = 16.dp, bottom = 8.dp)
  )
}

@Composable
private fun Dialogs(webDavVM: WebDavViewModel, smbVM: SmbViewModel) {
  val context = LocalContext.current

  AddWebDavDialog { alias, account, pwd, server, editWebDav ->
    if (alias.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.alias))
      return@AddWebDavDialog
    }

    if (account.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.account))
      return@AddWebDavDialog
    }

    if (pwd.isEmpty()) {
      MessageNotifier.show(R.string.can_t_be_empty, context.getString(R.string.pwd))
      return@AddWebDavDialog
    }

    if (server.isEmpty()) {
      MessageNotifier.show(
        R.string.can_t_be_empty,
        context.getString(R.string.webdav_hint_server)
      )
      return@AddWebDavDialog
    }

    if (editWebDav != null) {
      val updated = editWebDav.copy(
        alias = alias,
        account = account,
        pwd = pwd,
        server = server,
        lastUrl = server,
      ).also { it.id = editWebDav.id }
      webDavVM.insertOrReplaceWebDav(updated)
    } else {
      webDavVM.insertOrReplaceWebDav(WebDav(alias, account, pwd, server.removeSuffix("/"), server))
    }
  }

  AddSmbDialog(smbVM) { alias, domain, account, pwd, server, share, editSmb ->
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

    val lastUrl = server.removeSuffix("/") + "/" + share
    if (editSmb != null) {
      val updated = editSmb.copy(
        alias = alias,
        domain = domain.ifEmpty { null },
        account = account,
        pwd = pwd,
        server = server,
        share = share,
        lastUrl = lastUrl,
      ).also { it.id = editSmb.id }
      smbVM.insertOrReplaceSmb(updated)
    } else {
      smbVM.insertOrReplaceSmb(
        Smb(
          alias,
          domain.ifEmpty { null },
          account,
          pwd,
          server,
          share,
          lastUrl
        )
      )
    }
  }
}

@Composable
private fun WebDavItem(webDav: WebDav, onMenuClick: (Int) -> Unit) {
  val theme = LocalTheme.current
  val nav = LocalNavController.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .clickWithRipple(false) {
        nav.navigate(webDav)
      }
      .background(theme.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.padding(start = 12.dp),
      painter = painterResource(R.drawable.icon_webdav),
      contentDescription = "IconWebDavItem",
      tint = theme.icon()
    )

    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .weight(1f),
      horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(webDav.alias)
      Spacer(Modifier.height(4.dp))
      TextSecondary(webDav.account)
    }

    PopupButton(
      listOf(R.string.connect, R.string.edit, R.string.delete),
      contentDescription = "WebDavPopupButton",
      onMenuClick = onMenuClick
    )
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
      }
      .background(theme.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.padding(start = 12.dp),
      painter = painterResource(R.drawable.icon_webdav),
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