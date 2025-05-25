package remix.myplayer.compose.ui.widget.app

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.theme.Theme
import remix.myplayer.ui.activity.SongChooseActivity
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.RxUtil.applySingleScheduler
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

  val context = LocalActivity.current
  AnimatedVisibility(showFb,
    modifier = Modifier.padding(end = 38.dp, bottom = 80.dp),
    enter = scaleIn() + fadeIn(),
    exit = scaleOut() + fadeOut()) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(color = LocalTheme.current.secondary, shape = CircleShape)
        .clickableWithoutRipple {
          if (MultipleChoice.isActiveSomeWhere || context == null) {
            return@clickableWithoutRipple
          }

          DatabaseRepository.getInstance()
            .getAllPlaylist()
            .compose<List<PlayList>>(applySingleScheduler<List<PlayList>>())
            .subscribe { playLists ->
              Theme.getBaseDialog(context)
                .title(R.string.new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .inputRange(1, 25)
                .input("", context.getString(R.string.local_list) + playLists.size) { _, input ->
                  if (!TextUtils.isEmpty(input)) {
                    DatabaseRepository.getInstance()
                      .insertPlayList(input.toString())
                      .compose(applySingleScheduler())
                      .subscribe({ id ->
                        //跳转到添加歌曲界面
                        SongChooseActivity.start(context, id, input.toString())
                      }, { throwable ->
                        ToastUtil
                          .show(context, R.string.create_playlist_fail, throwable.toString())
                      })
                  }
                }
                .show()
            }
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