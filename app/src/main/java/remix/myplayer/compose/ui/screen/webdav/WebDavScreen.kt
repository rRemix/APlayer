package remix.myplayer.compose.ui.screen.webdav

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.common.LocalSnackBarHostState
import remix.myplayer.compose.ui.dialog.AddWebDavDialog
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.icon
import remix.myplayer.compose.ui.widget.app.FAButton
import remix.myplayer.compose.ui.widget.common.CommonAppBar
import remix.myplayer.compose.ui.widget.common.PopupButton
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.webDavViewModel
import remix.myplayer.db.room.model.WebDav

@Composable
fun WebDavScreen() {
  val vm = webDavViewModel
  val webdavList by vm.webDavList.collectAsStateWithLifecycle()
  val theme = LocalTheme.current
  val nav = LocalNavController.current
  val snackBarHostState = LocalSnackBarHostState.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val showMessage = { message: String ->
    scope.launch {
      snackBarHostState.currentSnackbarData?.dismiss()
      snackBarHostState.showSnackbar(message)
    }
  }

  AddWebDavDialog { alias, account, pwd, server, editWebDav ->
    if (alias.isEmpty()) {
      showMessage(context.getString(R.string.can_t_be_empty, context.getString(R.string.alias)))
      return@AddWebDavDialog
    }

    if (account.isEmpty()) {
      showMessage(context.getString(R.string.can_t_be_empty, context.getString(R.string.account)))
      return@AddWebDavDialog
    }

    if (pwd.isEmpty()) {
      showMessage(context.getString(R.string.can_t_be_empty, context.getString(R.string.pwd)))
      return@AddWebDavDialog
    }

    if (server.isEmpty()) {
      showMessage(
        context.getString(
          R.string.can_t_be_empty,
          context.getString(R.string.webdav_hint_server)
        )
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
      vm.insertOrReplaceWebDav(updated)
    } else {
      vm.insertOrReplaceWebDav(WebDav(alias, account, pwd, server.removeSuffix("/"), server))
    }
  }

  Scaffold(
    topBar = {
      CommonAppBar(stringResource(R.string.webdav), actions = emptyList())
    },
    floatingActionButton = {
      FAButton(true) {
        vm.showAddWebDavDialog()
      }
    },
    containerColor = theme.mainBackground
  ) { contentPadding ->

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
    ) {
      items(webdavList, key = { it.id }) { webDav ->
        WebDavItem(webDav) { res ->
          when (res) {
            R.string.connect -> {
              nav.navigate(webDav)
            }

            R.string.edit -> {
              vm.showAddWebDavDialog(webDav)
            }

            R.string.delete -> {
              vm.deleteWebDav(webDav)
            }
          }
        }
      }
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
      },
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

