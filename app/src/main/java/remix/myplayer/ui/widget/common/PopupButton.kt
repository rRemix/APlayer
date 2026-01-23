package remix.myplayer.ui.widget.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.popupButton
import remix.myplayer.util.ext.clickWithRipple

@Composable
fun PopupButton(menu: List<Int>, contentDescription: String? = null, onMenuClick: (Int) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .clickWithRipple {
        expanded = !expanded
      }
      .size(48.dp)
  ) {
    DropdownMenu(
      modifier = Modifier.wrapContentSize(Alignment.TopEnd),
      expanded = expanded,
      containerColor = LocalTheme.current.dialogBackground,
      onDismissRequest = {
        expanded = !expanded
      }
    ) {
      menu.map { res ->
        DropdownMenuItem(
          text = { TextPrimary(stringResource(res)) },
          onClick = {
            expanded = !expanded
            onMenuClick(res)
          }
        )
      }
    }

    Image(
      painter = painterResource(R.drawable.icon_player_more),
      contentDescription = contentDescription,
      colorFilter = ColorFilter.tint(LocalTheme.current.popupButton())
    )
  }
}