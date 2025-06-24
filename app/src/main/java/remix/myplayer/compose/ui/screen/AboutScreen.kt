package remix.myplayer.compose.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.CommonAppBar
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.misc.AppInfo

@Composable
fun AboutScreen() {
  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.about), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    Box(
      modifier = Modifier
        .padding(contentPadding)
        .fillMaxSize(),
    ) {
      Image(
        painter = painterResource(id = R.drawable.image_aboutus),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .padding(top = 8.dp)
          .align(Alignment.TopCenter)
      )

      val state = rememberDialogState()
      NormalDialog(
        dialogState = state,
        content = AppInfo.prettyPrinted,
        positive = stringResource(R.string.close),
        negative = null
      )
      TextSecondary(
        "v${BuildConfig.VERSION_NAME}",
        fontSize = 15.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .combinedClickable(interactionSource = null, indication = null, onClick = {

          }, onLongClick = {
            state.show()
          })
          .wrapContentHeight(align = Alignment.CenterVertically)
          .align(Alignment.BottomCenter)
      )
    }
  }
}