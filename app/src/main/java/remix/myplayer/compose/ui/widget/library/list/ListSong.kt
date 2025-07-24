package remix.myplayer.compose.ui.widget.library.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.highLightText
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.ui.widget.popup.SongPopupButton

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ListSong(
  modifier: Modifier = Modifier,
  song: Song,
  modelParent: APlayerModel,
  selected: Boolean,
  playing: Boolean,
  onClickSong: () -> Unit,
  onLongClickSong: () -> Unit,
  num: Int? = null,
) {
  val theme = LocalTheme.current

  ConstraintLayout(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = theme.ripple),
        onClick = { onClickSong() },
        onLongClick = { onLongClickSong() }
      )
      .background(if (selected) theme.select else theme.mainBackground)
  ) {
    val (indicator, count, cover, popButton, column) = createRefs()

    if (playing) {
      Box(
        modifier = Modifier
          .width(4.dp)
          .fillMaxHeight()
          .padding(vertical = 8.dp)
          .background(theme.highLightText())
          .constrainAs(indicator) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            start.linkTo(parent.start)
          }
      )
    }

    if (num != null) {
      TextPrimary(
        num.toString(),
        modifier = Modifier
          .width(28.dp)
          .constrainAs(count) {
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
          })
    }

    GlideCover(
      model = song,
      modifier = Modifier
        .size(40.dp)
        .constrainAs(cover) {
          start.linkTo(count.end, 12.dp, goneMargin = 16.dp)
          top.linkTo(parent.top)
          bottom.linkTo(parent.bottom)
        })

    Column(
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.Start,
      modifier = Modifier.constrainAs(column) {
        start.linkTo(cover.end, 16.dp)
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
        end.linkTo(popButton.start, 8.dp)
        width = Dimension.fillToConstraints
      }
    ) {
      TextPrimary(song.showName)
      Spacer(modifier = Modifier.height(4.dp))
      TextSecondary(String.format("%s-%s", song.artist, song.album))
    }

    SongPopupButton(
      modifier = Modifier.constrainAs(popButton) {
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
        end.linkTo(parent.end)
      },
      song = song,
      parent = modelParent
    )
  }
}