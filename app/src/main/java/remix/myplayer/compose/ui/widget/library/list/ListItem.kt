package remix.myplayer.compose.ui.widget.library.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.ui.widget.popup.LibraryItemPopupButton

@Composable
fun ListItem(
  modifier: Modifier = Modifier,
  model: APlayerModel,
  text1: String,
  text2: String,
  selected: Boolean,
  onClick: () -> Unit,
  onLongClick: () -> Unit
) {
  val theme = LocalTheme.current
  Row(
    modifier = modifier
      .combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = theme.ripple),
        onClick = onClick,
        onLongClick = onLongClick
      )
      .background(if (selected) theme.select else theme.mainBackground),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlideCover(
      modifier = Modifier
        .padding(start = 8.dp)
        .size(42.dp),
      model = model,
      circle = false
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(horizontal = 8.dp),
      verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(text = text1)
      Spacer(modifier = Modifier.height(6.dp))
      TextSecondary(text = text2)
    }

    LibraryItemPopupButton(model = model)

  }
}
