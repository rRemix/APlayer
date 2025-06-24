package remix.myplayer.compose.ui.widget.app

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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.nav.RouteSongChoose
import remix.myplayer.compose.ui.dialog.InputDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.ToastUtil

@SuppressLint("CheckResult")
@Composable
fun FAButton(pagerState: PagerState, libraries: List<Library>) {
  val showFb by remember {
    derivedStateOf {
      pagerState.currentPage == libraries.indexOfFirst {
        it.tag == Library.TAG_PLAYLIST
      }
    }
  }

  val navController = LocalNavController.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val vm = activityViewModel<LibraryViewModel>()
  var text by remember {
    mutableStateOf("")
  }
  val dialogState = rememberDialogState(false)

  InputDialog(
    dialogState = dialogState,
    title = stringResource(R.string.new_playlist),
    positive = stringResource(R.string.create),
    text = text,
    onDismissRequest = {
      text = ""
    },
    onValueChange = {
      text = it
    }
  ) {
    scope.launch {
      try {
        val id = vm.insertPlayList(it)
        if (id > 0) {
          navController.navigate("$RouteSongChoose/${id}/$it")
        }
      } catch (e: Exception) {
        ToastUtil.show(context, R.string.create_playlist_fail, e)
      }
    }
  }

  AnimatedVisibility(
    showFb,
    modifier = Modifier.padding(end = 38.dp, bottom = 80.dp),
    enter = scaleIn() + fadeIn(),
    exit = scaleOut() + fadeOut()
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(color = LocalTheme.current.secondary, shape = CircleShape)
        .clickableWithoutRipple {
          if (MultipleChoice.isActiveSomeWhere) {
            return@clickableWithoutRipple
          }

          text = "${context.getString(R.string.local_list)}${vm.playLists.value.size}"
          dialogState.show()
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