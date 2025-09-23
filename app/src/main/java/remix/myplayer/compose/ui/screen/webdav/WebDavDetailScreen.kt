package remix.myplayer.compose.ui.screen.webdav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.dialog.runWithLoading
import remix.myplayer.compose.ui.screen.BackPressHandler
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.icon
import remix.myplayer.compose.ui.widget.app.BottomBar
import remix.myplayer.compose.ui.widget.common.AppBarAction
import remix.myplayer.compose.ui.widget.common.CommonAppBar
import remix.myplayer.compose.ui.widget.common.PopupButton
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.compose.viewmodel.webDavViewModel
import remix.myplayer.db.room.model.WebDav
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.misc.isAudio
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util
import java.util.concurrent.TimeUnit

@Composable
fun WebDavDetailScreen(webDav: WebDav) {
  val nav = LocalNavController.current
  val webDavVM = webDavViewModel
  val musicVM = musicViewModel
  val settingVM = settingViewModel

  val davResources by webDavVM.webDavResources.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  var url by rememberSaveable {
    mutableStateOf(webDav.lastUrl)
  }
  var refreshTrigger by remember {
    mutableIntStateOf(0)
  }

  val sardine = remember {
    OkHttpSardine(
      OkHttpClient.Builder()
        .connectTimeout(20L, TimeUnit.SECONDS)
        .readTimeout(20L, TimeUnit.SECONDS)
        .writeTimeout(20L, TimeUnit.SECONDS)
        .build()
    ).apply {
      setCredentials(webDav.account, webDav.pwd)
    }
  }

  fun handleBack() {
    if (webDav.server == webDav.lastUrl) { // 根路径
      nav.popBackStack()
      return
    }

    var newUrl = webDav.lastUrl.removeSuffix("/")
    newUrl = newUrl.substring(0, newUrl.lastIndexOf('/'))
    url = newUrl
  }

  BackPressHandler {
    handleBack()
  }

  Scaffold(
    topBar = {
      CommonAppBar(
        title = webDav.alias,
        onBack = {
          handleBack()
        },
        actions = listOf(AppBarAction(R.drawable.ic_close_white_24dp, "WebDetailClose") {
          nav.popBackStack()
        })
      )
    },
    containerColor = LocalTheme.current.mainBackground
  ) { contentPadding ->
    Column(modifier = Modifier.padding(contentPadding)) {
      LazyColumn(modifier = Modifier.weight(1f)) {
        items(davResources, key = { it.path }) { resource ->
          WebDavDetailItem(
            resource,
            onClick = {
              if (resource.isDirectory) {
                // 进入下级目录
                url = webDav.base().plus(resource.path)
              } else {
                // 过滤列表内所有音乐并设置为播放列表
                if (davResources.isEmpty()) {
                  return@WebDavDetailItem
                }
                var select: Song.Remote? = null
                val remotes = davResources
                  .filter { it.isAudio() }
                  .map {
                    val remote = Song.Remote(
                      title = it.name.substringBeforeLast('.'),
                      data = webDav.base().plus(it.path),
                      size = it.contentLength,
                      dateModified = it.creation?.time ?: 0,
                      account = webDav.account,
                      pwd = webDav.pwd
                    )
                    if (it == resource) {
                      select = remote
                    }
                    remote
                  }
                MusicServiceRemote.setPlayQueue(
                  remotes,
                  MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                    .putExtra(MusicService.EXTRA_POSITION, remotes.indexOfFirst {
                      it.data == select?.data
                    })
                )
              }
            },
            onMenuClick = {
              val song = Song.Remote(
                title = resource.name.substringBeforeLast('.'),
                data = webDav.base().plus(resource.path),
                size = resource.contentLength,
                dateModified = resource.creation?.time ?: 0,
                account = webDav.account,
                pwd = webDav.pwd
              )
              when (it) {
                R.string.add_to_next_song -> {
                  Util.sendLocalBroadcast(
                    MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
                      .putExtra(MusicService.EXTRA_SONG, song)
                  )
                }

                R.string.add_to_play_queue -> {
                  musicVM.insertToQueue(listOf(song))
                }

                R.string.song_detail -> {
                  scope.runWithLoading {
                    withContext(Dispatchers.IO) {
                      MusicService.retrieveRemoteSong(song, song)
                    }
                    settingVM.showSongDetailDialog(song)
                  }
                }

                R.string.delete -> {
                  scope.runWithLoading {
                    withContext(Dispatchers.IO) {
                      sardine.delete(webDav.base().plus(resource.path))
                    }
                    refreshTrigger++
                  }
                }
              }
            })
        }
      }
      BottomBar()
    }
  }

  LaunchedEffect(url, refreshTrigger) {
    webDavVM.loadDavResources(sardine, url) {
      webDav.lastUrl = url
      webDavVM.updateLastUrl(webDav, url)
    }
  }
}

@Composable
private fun WebDavDetailItem(
  davResource: DavResource,
  onClick: () -> Unit,
  onMenuClick: (Int) -> Unit
) {
  val theme = LocalTheme.current

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .clickWithRipple(false) {
        onClick()
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    val isAudio = davResource.isAudio()
    val icon = if (davResource.isDirectory) {
      R.drawable.ic_folder_24dp
    } else if (isAudio) {
      R.drawable.ic_audio_file_24dp
    } else {
      R.drawable.ic_lab_profile_24dp
    }

    Icon(
      modifier = Modifier
        .padding(start = 12.dp),
      painter = painterResource(icon),
      contentDescription = "IconWebDavDetailItem",
      tint = theme.icon()
    )

    Column(
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .weight(1f),
      horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(davResource.name)
      Spacer(Modifier.height(4.dp))
      TextSecondary(davResource.path)
    }

    val list = arrayListOf(R.string.delete)
    if (davResource.isAudio()) {
      list.addAll(
        0,
        listOf(
          R.string.add_to_next_song,
          R.string.add_to_play_queue,
          R.string.song_detail,
        )
      )
    }
    PopupButton(list, contentDescription = "WebDavDetailPopupButton", onMenuClick = onMenuClick)
  }
}