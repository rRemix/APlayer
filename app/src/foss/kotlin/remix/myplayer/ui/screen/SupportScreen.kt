package remix.myplayer.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.Util
import remix.myplayer.util.ext.clickWithRipple

private data class DonationItem(val icon: Int, val titleRes: Int, val onClick: (Activity) -> Unit)

@Composable
fun SupportScreen() {
  val theme = LocalTheme.current
  val activity = LocalActivity.current ?: return
  val scope = rememberCoroutineScope()

  val dialogState = rememberDialogState()
  NormalDialog(
    dialogState, titleRes = R.string.support_develop,
    positiveRes = R.string.jump_alipay_account,
    negativeRes = R.string.cancel,
    contentRes = R.string.donate_tip,
    onPositive = { AlipayUtil.startAlipayClient(activity) }
  )

  val items = listOf(
    DonationItem(R.drawable.icon_wechat_donate, R.string.wechat) { act ->
      scope.launch {
        Util.saveToAlbum(act, R.drawable.icon_wechat_qrcode, "wechat_qrCode.png")
      }
    },
    DonationItem(R.drawable.icon_alipay_donate, R.string.alipay) { act ->
      dialogState.show()
    },
    DonationItem(R.drawable.icon_paypal_donate, R.string.paypal) { act ->
      val intent = Intent("android.intent.action.VIEW")
      intent.data = "https://www.paypal.me/rRemix".toUri()
      Util.startActivitySafely(act, intent)
    },
  )

  Scaffold(
    topBar = {
      CommonAppBar(
        title = stringResource(R.string.support_develop),
        actions = emptyList()
      )
    },
    containerColor = theme.mainBackground
  ) { contentPadding ->
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      modifier = Modifier.padding(contentPadding),
      contentPadding = PaddingValues(16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(items) { item ->
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clickWithRipple(false) { item.onClick(activity) },
          color = LocalTheme.current.mainBackground,
          shape = RoundedCornerShape(8.dp),
          shadowElevation = 8.dp
        )
        {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Image(
              painter = painterResource(item.icon),
              contentDescription = "Support${stringResource(item.titleRes)}"
            )
            TextSecondary(
              text = stringResource(item.titleRes)
            )
          }
        }
      }
    }
  }
}