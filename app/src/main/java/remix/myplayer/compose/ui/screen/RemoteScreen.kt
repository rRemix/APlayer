package remix.myplayer.compose.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.ui.activity.WebDavActivity

@Composable
fun RemoteScreen() {
  val context = LocalContext.current
  val items = listOf(Item(R.drawable.icon_webdav, R.string.webdav, {
    context.startActivity(Intent(context, WebDavActivity::class.java))
  }))

  LazyVerticalGrid(
    modifier = Modifier
      .fillMaxSize()
      .background(LocalTheme.current.mainBackground),
    columns = GridCells.Fixed(3),
    content = {
      items(items) {
        RemoteItem(it)
      }
    })
}

@Composable
private fun RemoteItem(item: Item) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickWithRipple(false) {
        item.onClick()
      },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
    ) {
      Icon(
        painter = painterResource(item.icon),
        contentDescription = stringResource(item.desc),
        tint = LocalTheme.current.iconColor,
        modifier = Modifier
          .padding(all = 32.dp)
          .fillMaxSize()
      )
    }
    TextPrimary(stringResource(item.desc))
  }
}

internal data class Item(val icon: Int, val desc: Int, val onClick: () -> Unit)

@Preview(showBackground = true)
@Composable
fun RemoteItemPreview() {
  APlayerTheme {
    RemoteItem(Item(R.drawable.icon_webdav, R.string.webdav, {

    }))
  }
}