package remix.myplayer.compose.ui.widget.library.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.ui.widget.popup.LibraryItemPopupButton

@Composable
fun GridItem(model: APlayerModel, text1: String, text2: String? = null, onClick: () -> Unit, onLongClick: () -> Unit) {
  Column(
    modifier = Modifier
      .padding(start = 3.dp, top = 4.dp, end = 3.dp, bottom = 4.dp)
      .fillMaxWidth()
      .combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = LocalTheme.current.ripple),
        onClick = { onClick() },
        onLongClick = { onLongClick() }
      )
      .background(LocalTheme.current.mainBackground)
  ) {
    GlideCover(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(2.dp)),
      model = model,
      circle = false
    )

    Row(
      modifier = Modifier
        .height(58.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier
          .padding(start = 10.dp)
          .fillMaxHeight()
          .weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        TextPrimary(text = text1)
        if (text2 != null) {
          TextSecondary(text2)
        }
      }

      LibraryItemPopupButton(model = model)
    }
  }
}