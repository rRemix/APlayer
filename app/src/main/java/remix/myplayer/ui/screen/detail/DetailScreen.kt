package remix.myplayer.ui.screen.detail

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.flow.first
import remix.myplayer.R
import remix.myplayer.data.model.audio.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.playbackSourceKey
import remix.myplayer.service.Command
import remix.myplayer.service.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.ui.nav.DetailScreenRoute
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.openPlayingScreen
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.highLightText
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.defaultAppBarActions
import remix.myplayer.ui.widget.library.AlbumCoverCollage
import remix.myplayer.ui.widget.library.CoverImage
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.ui.widget.library.rememberAlbumCoverPainters
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ext.verticalScrollbar
import remix.myplayer.viewmodel.DetailState
import remix.myplayer.viewmodel.DetailViewModel
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
  model: APlayerModel,
  openFromLibrary: Boolean = false,
  vm: DetailViewModel = hiltViewModel()
) {
  // Room supplies the latest playlist name and membership, including changes made in other screens.
  val detailModel = if (model is PlayList) {
    val playLists by libraryViewModel.playLists.collectAsStateWithLifecycle()
    playLists.firstOrNull { it.id == model.id } ?: model
  } else {
    model
  }
  val detailState by vm.state.collectAsStateWithLifecycle()
  val coverPainters = rememberAlbumCoverPainters(detailState.coverSongs, detailModel, DetailCoverSize)
  val mainVM = mainViewModel
  val selection by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val playbackState by playbackViewModel.playbackUiState.collectAsStateWithLifecycle()
  val selecting = selection.isShowInDetail()
  val selectedIds = remember(selection) { selection.selectedModels(MultiSelectState.Where.Detail) }
  val nav = LocalNavController.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val listState = rememberLazyListState()
  val headerCollapse by remember(listState) {
    derivedStateOf {
      if (listState.firstVisibleItemIndex > 0) {
        1f
      } else {
        val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "header" }
        if (header == null || header.size == 0) {
          0f
        } else {
          // Use the measured height so stacked headers and larger fonts collapse consistently.
          ((listState.layoutInfo.viewportStartOffset - header.offset).toFloat() / header.size)
            .coerceIn(0f, 1f)
        }
      }
    }
  }
  val albumRowState = rememberLazyListState()
  val entry = LocalLifecycleOwner.current as? NavBackStackEntry
  val settingState by settingViewModel.settingsState.collectAsStateWithLifecycle()
  val sourceOpenAction = settingState.play.sourceOpenAction
  val resumeDialog = rememberDialogState(false)

  LaunchedEffect(entry, openFromLibrary) {
    if (!openFromLibrary || entry == null ||
      entry.savedStateHandle.get<Boolean>(SOURCE_OPEN_HANDLED) == true
    ) return@LaunchedEffect
    // Merely previewing this destination must not start playback or open a dialog.
    entry.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
    if (nav.currentBackStackEntry != entry) return@LaunchedEffect
    entry.savedStateHandle[SOURCE_OPEN_HANDLED] = true

    when (sourceOpenAction) {
      SettingPrefs.SOURCE_OPEN_RESUME -> MusicServiceRemote.resumeFrom(model)
      SettingPrefs.SOURCE_OPEN_ASK -> {
        if ((!MusicServiceRemote.isPlayingFrom(model) || !playbackState.isPlaying) &&
          MusicServiceRemote.hasResumableRecord(model)
        ) {
          // Do not show the confirmation over a screen that is still transitioning away.
          entry.lifecycle.currentStateFlow.first { it == Lifecycle.State.RESUMED }
          if (nav.currentBackStackEntry == entry) resumeDialog.show()
        }
      }
    }
  }

  fun exitDetail() {
    if (entry != null && (nav.currentBackStackEntry != entry ||
        entry.lifecycle.currentState != Lifecycle.State.RESUMED)) return
    nav.popBackStack()
  }

  NormalDialog(
    dialogState = resumeDialog,
    titleRes = R.string.resume_playback_title,
    contentRes = R.string.resume_playback_message,
    positiveRes = R.string.resume_playback_confirm,
    onPositive = { MusicServiceRemote.resumeFrom(model) },
    negativeRes = R.string.resume_playback_browse,
  )

  DetailRefreshEffect(detailModel, vm)
  BackHandler(selecting) { mainVM.closeMultiSelect() }

  fun playAll(shuffle: Boolean, index: Int = 0) {
    if (detailState.songs.isEmpty()) return
    val intent = if (shuffle) {
      MusicUtil.makeCmdIntent(Command.SKIP_TO_NEXT, true)
    } else {
      MusicUtil.makeCmdIntent(Command.PLAY_AT).putExtra(MusicService.EXTRA_POSITION, index)
    }
    setPlayQueue(detailState.songs, intent, detailModel)
  }

  Scaffold(
    contentWindowInsets = WindowInsets.systemBars,
    containerColor = LocalTheme.current.mainBackground,
    topBar = {
      AnimatedContent(
        targetState = selecting,
        transitionSpec = {
          if (targetState) {
            slideInVertically() togetherWith slideOutVertically { height -> height / 2 }
          } else {
            slideInVertically { height -> height } togetherWith slideOutVertically()
          }
        },
        label = "DetailTopBar",
      ) { isMultiSelect ->
        if (isMultiSelect) {
          MultiSelectBar(state = selection, scrollBehavior = null, parent = detailModel)
        } else {
          CommonAppBar(
            title = detailModel.detailTitle(),
            onBack = { exitDetail() },
            titleModifier = Modifier.graphicsLayer {
              // Read scroll state in the graphics layer, without recomposing the song list.
              alpha = ((headerCollapse - 0.35f) / 0.5f).coerceIn(0f, 1f)
            },
            actions = {
              defaultAppBarActions.forEach { action ->
                IconButton(onClick = action.action) {
                  Icon(painterResource(action.icon), contentDescription = action.contentDescription)
                }
              }
            },
          )
        }
      }
    },
  ) { padding ->
    Column(Modifier.fillMaxSize().padding(padding)) {
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 16.dp),
        modifier = Modifier.weight(1f).verticalScrollbar(listState),
      ) {
        item(key = "header", contentType = "header") {
          DetailHeader(
            detailModel,
            detailState,
            coverPainters,
            modifier = Modifier.graphicsLayer {
              alpha = (1f - headerCollapse / 0.8f).coerceIn(0f, 1f)
            },
          )
        }
        stickyHeader(key = "play", contentType = "actions") {
          // Cover scrolling content, including the gap and the outlined button's background.
          Box(
            Modifier.fillMaxWidth()
              .background(LocalTheme.current.mainBackground)
              .padding(vertical = 8.dp),
          ) {
            DetailPlayActions(
              enabled = detailState.songs.isNotEmpty() && !selecting,
              onPlay = { playAll(false) },
              onShuffle = { playAll(true) },
            )
          }
        }
        if (detailModel is Artist) {
          if (detailState.albums.isNotEmpty()) {
            item(key = "albums_title", contentType = "section") {
              DetailSectionTitle(stringResource(R.string.tab_album), detailState.albums.size)
            }
            item(key = "albums", contentType = "albums") {
              LazyRow(
                state = albumRowState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
              ) {
                items(detailState.albums, key = { it.albumId }) { album ->
                  ArtistAlbumCard(
                    album, modifier = Modifier.width(144.dp), enabled = !selecting,
                    onClick = { nav.navigate(DetailScreenRoute(album = album)) },
                  )
                }
              }
            }
          }
        }
        item(key = "songs_title", contentType = "section") {
          DetailSectionTitle(stringResource(R.string.tab_song), detailState.songs.size) {
            if (!selecting && detailState.songs.isNotEmpty()) {
              CompositionLocalProvider(LocalContentColor provides LocalTheme.current.textPrimary) {
                DetailPopupButton(detailModel) { vm.refresh(detailModel) }
              }
            }
          }
        }
        if (detailState.songs.isEmpty() || detailState.failed) {
          item(key = "status", contentType = "status") {
            DetailStatus(detailState, onRetry = { vm.refresh(detailModel) })
          }
        }
        itemsIndexed(
          detailState.songs,
          key = { index, song -> "${index}_${song.songIdentity}" },
          contentType = { _, _ -> "song" },
        ) { index, song ->
          val playing = playbackState.song.songIdentity == song.songIdentity
          ListSong(
            modifier = Modifier.heightIn(min = 64.dp),
            song = song,
            modelParent = detailModel,
            subtitle = detailSongSubtitle(detailModel, song),
            selected = selectedIds.contains(song.getKey()),
            playing = playing,
            popupEnabled = !selecting,
            onClickSong = {
              if (selecting) {
                mainVM.updateMultiSelectModel(song)
              } else if (playbackState.isPlaying && playing &&
                (detailModel.playbackSourceKey() == null || MusicServiceRemote.isPlayingFrom(detailModel))
              ) {
                nav.openPlayingScreen()
              } else {
                playAll(false, index)
              }
            },
            onLongClickSong = {
              mainVM.showMultiSelect(context, MultiSelectState.Where.Detail, song)
            },
          )
        }
      }
      BottomBar()
    }
  }
}

@Composable
private fun DetailRefreshEffect(model: APlayerModel, vm: DetailViewModel) {
  val activity = LocalActivity.current as? BaseMusicActivity
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  DisposableEffect(activity, lifecycle, vm, model) {
    // Load during the enter transition, rather than waiting until it finishes at RESUMED.
    vm.refresh(model)
    var refreshOnResume = false

    fun refreshIfResumed() {
      if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
        vm.refresh(model)
      } else {
        // Includes changes received during the initial transition, after loading started.
        refreshOnResume = true
      }
    }

    val listener = object : MusicEventCallback {
      override fun onMediaStoreChanged() = refreshIfResumed()
      override fun onPermissionChanged(has: Boolean) = refreshIfResumed()
      override fun onTagChanged(oldSong: Song?, newSong: Song) = refreshIfResumed()
      // Playlist changes arrive through Room and restart this effect with the latest model.
      override fun onPlayListChanged(name: String) = Unit
      override fun onServiceConnected(service: MusicService) = Unit
      override fun onServiceDisConnected() = Unit
    }
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_PAUSE -> refreshOnResume = true
        Lifecycle.Event.ON_RESUME -> {
          if (refreshOnResume) {
            refreshOnResume = false
            vm.refresh(model)
          }
        }
        else -> Unit
      }
    }
    activity?.addMusicServiceEventListener(listener)
    // The initial/replayed ON_RESUME does not repeat the load started above.
    lifecycle.addObserver(observer)
    onDispose {
      activity?.removeMusicServiceEventListener(listener)
      lifecycle.removeObserver(observer)
    }
  }
}

@Composable
private fun DetailHeader(
  model: APlayerModel,
  state: DetailState,
  coverPainters: List<Painter>,
  modifier: Modifier = Modifier
) {
  val theme = LocalTheme.current
  BoxWithConstraints(modifier.padding(16.dp)) {
    val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
    val cover: @Composable () -> Unit = {
      val coverModifier = Modifier.size(DetailCoverSize).clip(RoundedCornerShape(4.dp))
      AlbumCoverCollage(
        covers = coverPainters,
        modifier = coverModifier,
      )
    }
    val info: @Composable () -> Unit = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(model.detailTitle(), style = MaterialTheme.typography.headlineSmall, color = theme.textPrimary)
        if (model is Album) {
          Text(model.artist, style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary)
        }
        if (state.loading && state.songs.isEmpty()) {
          Text(stringResource(R.string.loading), color = theme.textSecondary)
        } else {
          Text(
            if (model is Artist) {
              stringResource(
                R.string.artist_library_summary,
                pluralStringResource(R.plurals.artist_album_count, state.albums.size, state.albums.size),
                pluralStringResource(R.plurals.song_num, state.songs.size, state.songs.size),
              )
            } else {
              pluralStringResource(R.plurals.song_num, state.songs.size, state.songs.size)
            },
            style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary,
          )
          Text(
            stringResource(R.string.detail_total_duration, DateUtils.formatElapsedTime(state.duration / 1000)),
            style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary,
          )
        }
      }
    }
    if (stacked) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { cover(); info() }
    } else {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        cover()
        Box(Modifier.weight(1f)) { info() }
      }
    }
  }
}

@Composable
private fun DetailPlayActions(enabled: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit) {
  val theme = LocalTheme.current
  val shape = RoundedCornerShape(4.dp)
  Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Button(
      onClick = onPlay, enabled = enabled, shape = shape,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = theme.primary, contentColor = theme.primaryReverse,
        disabledContainerColor = theme.textPrimary.copy(alpha = 0.12f),
        disabledContentColor = theme.textPrimary.copy(alpha = 0.38f),
      ),
    ) {
      Icon(painterResource(R.drawable.ic_play_arrow_black_24dp), contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(6.dp))
      Text(stringResource(R.string.play_all))
    }
    OutlinedButton(
      onClick = onShuffle, enabled = enabled, shape = shape,
      modifier = Modifier.weight(1f).heightIn(min = 48.dp),
      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
      border = BorderStroke(1.dp, theme.highLightText().copy(alpha = if (enabled) 1f else 0.38f)),
      colors = ButtonDefaults.outlinedButtonColors(
        contentColor = theme.highLightText(), disabledContentColor = theme.textPrimary.copy(alpha = 0.38f),
      ),
    ) {
      Icon(painterResource(R.drawable.ic_shuffle_white_24dp), contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(6.dp))
      Text(stringResource(R.string.detail_shuffle))
    }
  }
}

@Composable
private fun DetailSectionTitle(title: String, count: Int, action: @Composable () -> Unit = {}) {
  Row(
    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp).heightIn(min = 48.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = LocalTheme.current.textPrimary)
    Text("  $count", style = MaterialTheme.typography.bodyMedium, color = LocalTheme.current.textSecondary)
    Spacer(Modifier.weight(1f))
    action()
  }
}

@Composable
private fun ArtistAlbumCard(album: Album, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
  Column(
    modifier.clip(RoundedCornerShape(4.dp)).clickable(enabled = enabled, onClick = onClick),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    CoverImage(
      model = album, circle = false,
      modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
    )
    Text(
      album.album, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
      style = MaterialTheme.typography.bodyMedium, color = LocalTheme.current.textPrimary,
    )
  }
}

@Composable
private fun DetailStatus(state: DetailState, onRetry: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (state.loading) {
      CircularProgressIndicator(modifier = Modifier.size(28.dp), color = LocalTheme.current.highLightText())
    }
    Text(
      stringResource(if (state.loading) R.string.loading else if (state.failed) R.string.load_failed else R.string.no_song),
      color = LocalTheme.current.textSecondary,
    )
    if (state.failed && !state.loading) {
      TextButton(onClick = onRetry) { Text(stringResource(R.string.detail_retry), color = LocalTheme.current.highLightText()) }
    }
  }
}

private fun detailSongSubtitle(model: APlayerModel, song: Song): String {
  val name = when (model) {
    is Album -> song.artist
    is Artist -> song.album
    else -> return String.format("%s · %s", song.artist, song.album)
  }
  // DetailViewModel has already read durations on IO when calculating the total duration.
  val duration = song.duration.takeIf { it > 0 }?.let { DateUtils.formatElapsedTime(it / 1000) }
  return listOfNotNull(name.takeIf { it.isNotBlank() }, duration).joinToString(" · ")
}

private fun APlayerModel.detailTitle(): String = when (this) {
  is Artist -> artist
  is Album -> album
  is PlayList -> name
  is Folder -> name?.takeIf { it.isNotBlank() } ?: path
  is Genre -> genre
  else -> error("Unsupported detail model: ${this::class.java.simpleName}")
}

private const val SOURCE_OPEN_HANDLED = "source_open_handled"
private val DetailCoverSize = 112.dp
