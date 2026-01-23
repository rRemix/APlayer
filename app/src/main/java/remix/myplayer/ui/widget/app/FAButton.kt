package remix.myplayer.ui.widget.app

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.ext.clickableWithoutRipple

@SuppressLint("CheckResult")
@Composable
fun FAButton(show: Boolean, onClick: () -> Unit) {
  AnimatedVisibility(
    show,
    modifier = Modifier.padding(end = 38.dp, bottom = 80.dp),
    enter = scaleIn() + fadeIn(),
    exit = scaleOut() + fadeOut()
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(color = LocalTheme.current.secondary, shape = CircleShape)
        .clickableWithoutRipple {
          onClick()
        },
      contentAlignment = Alignment.Center
    ) {
      Icon(
        painterResource(R.drawable.icon_playlist_add),
        contentDescription = "FB",
        tint = Color.White
      )
    }
  }
}