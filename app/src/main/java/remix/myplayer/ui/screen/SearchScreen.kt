@file:OptIn(ExperimentalMaterial3Api::class)

package remix.myplayer.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.prefs.SettingPrefs.Companion.SEARCH_PLAYBACK_ADD_TO_QUEUE
import remix.myplayer.data.prefs.SettingPrefs.Companion.SEARCH_PLAYBACK_PLAY
import remix.myplayer.data.prefs.SettingPrefs.Companion.SEARCH_PLAYBACK_PLAY_ALL
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.library.list.ListSong
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.MultiSelectState
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
  val libraryVM = libraryViewModel
  val mainVM = mainViewModel
  val playbackVM = playbackViewModel
  val settingVM = settingViewModel

  val playbackState by playbackVM.playbackUiState.collectAsStateWithLifecycle()
  val settingsState by settingVM.settingsState.collectAsStateWithLifecycle()
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val librarySongs by libraryVM.songs.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  var songs by remember {
    mutableStateOf(emptyList<Song>())
  }
  var isLoading by remember { mutableStateOf(false) }
  val context = LocalContext.current

  val showMultiSelect = multiSelectState.isShowInSearch()
  val popupEnabled = !showMultiSelect

  BackHandler(showMultiSelect) {
    mainVM.closeMultiSelect()
  }

  var searchKey by rememberSaveable {
    mutableStateOf("")
  }

  Scaffold(
    modifier = Modifier.imePadding(),
    contentWindowInsets = WindowInsets.systemBars,
    containerColor = LocalTheme.current.mainBackground,
    topBar = {
      val textFieldState = rememberTextFieldState()
      AnimatedContent(
        targetState = showMultiSelect,
        transitionSpec = {
          if (targetState) {
            slideInVertically() togetherWith slideOutVertically { height -> height / 2 }
          } else {
            slideInVertically { height -> height } togetherWith slideOutVertically()
          }
        }
      ) { isMultiSelect ->
        if (!isMultiSelect) {
          SongSearchBar(
            textFieldState = textFieldState,
            playbackAction = settingsState.play.searchPlaybackAction,
            onPlaybackActionSelected = settingVM::setSearchPlaybackAction,
            onSearch = { searchKey = it },
          )
        } else {
          MultiSelectBar(
            state = multiSelectState,
            scrollBehavior = null,
            onSelectAll = {
              mainVM.updateMultiSelectModelsAll(songs)
            }
          )
        }
      }
    }
  ) { contentPadding ->
    Box(
      modifier = Modifier
        .padding(contentPadding)
        .consumeWindowInsets(contentPadding)
        .fillMaxSize(),
      contentAlignment = Alignment.TopCenter
    ) {
      if (isLoading) {
        LinearProgressIndicator(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter),
          color = LocalTheme.current.primary
        )
      } else if (songs.isEmpty()) {
        TextSecondary(
          modifier = Modifier.padding(top = 64.dp),
          text = stringResource(R.string.no_search_result), fontSize = 16.sp
        )
      } else {
        val selectedIds by remember {
          derivedStateOf {
            multiSelectState.selectedModels(MultiSelectState.Where.Search)
          }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
          itemsIndexed(songs, key = { _, song ->
            song.id
          }) { pos, song ->
            val selected = selectedIds.contains(song.getKey())
            val isPlayingSong = playbackState.song.id == song.id

            ListSong(
              modifier = Modifier.height(64.dp),
              song = song,
              modelParent = song,
              selected = selected,
              playing = isPlayingSong,
              popupEnabled = popupEnabled,
              onClickSong = {
                if (songs.isEmpty()) {
                  return@ListSong
                }

                if (multiSelectState.where == MultiSelectState.Where.Search) {
                  mainVM.updateMultiSelectModel(song)
                  return@ListSong
                }

                when (settingsState.play.searchPlaybackAction) {
                  SEARCH_PLAYBACK_PLAY_ALL -> setPlayQueue(
                    songs, MusicUtil.makeCmdIntent(Command.PLAY_AT)
                      .putExtra(MusicService.EXTRA_POSITION, pos)
                  )
                  SEARCH_PLAYBACK_ADD_TO_QUEUE -> playbackVM.insertToQueue(listOf(song))
                  else -> Util.sendLocalBroadcast(
                    MusicUtil.makeCmdIntent(Command.PLAY_TEMP)
                      .putExtra(MusicService.EXTRA_SONG, song)
                  )
                }
              },
              onLongClickSong = {
                mainVM.showMultiSelect(context, MultiSelectState.Where.Search, song)
              })
          }
        }
      }

    }
  }

  LaunchedEffect(searchKey, librarySongs) {
    if (searchKey.isEmpty()) {
      songs = emptyList()
      isLoading = false
      return@LaunchedEffect
    }
    isLoading = true
    delay(300)
    val result = withContext(Dispatchers.IO) {
      libraryVM.searchSong(searchKey)
    }
    songs = result
    isLoading = false
  }
}

@Composable
private fun SongSearchBar(
  textFieldState: TextFieldState,
  playbackAction: Int,
  onPlaybackActionSelected: (Int) -> Unit,
  onSearch: (String) -> Unit,
) {
  val theme = LocalTheme.current

  CommonAppBar(null, true) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 64.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      val query = textFieldState.text.toString()
      val interactionSource = remember { MutableInteractionSource() }
      val fontSize = 16.sp

      BasicTextField(
        value = query,
        onValueChange = {
          textFieldState.edit { replace(0, length, it) }
          onSearch(textFieldState.text.toString())
        },
        modifier = Modifier.weight(1f).height(56.dp),
        singleLine = true,
        textStyle = TextStyle(fontSize = fontSize),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
          onSearch(query)
        }),
        interactionSource = interactionSource,
        decorationBox =
          @Composable { innerTextField ->
            TextFieldDefaults.DecorationBox(
              value = query,
              innerTextField = innerTextField,
              enabled = true,
              singleLine = true,
              visualTransformation = VisualTransformation.None,
              interactionSource = interactionSource,
              placeholder = {
                TextPrimary(stringResource(R.string.search_hint), fontSize = fontSize)
              },
              trailingIcon = {
                if (query.isNotEmpty()) {
                  IconButton(onClick = {
                    textFieldState.clearText()
                    onSearch("")
                  }) {
                    Icon(
                      modifier = Modifier.size(20.dp),
                      imageVector = Icons.Filled.Clear,
                      tint = theme.textPrimary,
                      contentDescription = "SearchClear"
                    )
                  }
                }
              },
              shape = SearchBarDefaults.inputFieldShape,
              colors = TextFieldDefaults.colors().copy(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                cursorColor = theme.primary,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedIndicatorColor = theme.primary,
                unfocusedIndicatorColor = theme.primary
              ),
              contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(),
              container = {},
            )
          }
      )
      SearchPlaybackActionButton(playbackAction, onPlaybackActionSelected)
    }
  }
}

@Composable
private fun SearchPlaybackActionButton(
  action: Int,
  onActionSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val title = stringResource(R.string.search_playback_action)
  val theme = LocalTheme.current
  val options = remember {
    listOf(
      Triple(SEARCH_PLAYBACK_PLAY, R.string.play, R.string.search_play_description),
      Triple(SEARCH_PLAYBACK_PLAY_ALL, R.string.play_all, R.string.search_play_all_description),
      Triple(
        SEARCH_PLAYBACK_ADD_TO_QUEUE,
        R.string.add_to_play_queue,
        R.string.search_add_to_queue_description,
      ),
    )
  }

  Box(modifier) {
    TooltipBox(
      positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
      tooltip = { PlainTooltip { Text(title) } },
      state = rememberTooltipState(),
    ) {
      IconButton(onClick = { expanded = true }) {
        Icon(
          painter = painterResource(R.drawable.ic_tune_24dp),
          contentDescription = title,
          tint = LocalContentColor.current,
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.widthIn(min = 256.dp, max = 320.dp),
      containerColor = theme.dialogBackground,
    ) {
      TextSecondary(title, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

      options.forEach { (option, labelRes, descriptionRes) ->
        DropdownMenuItem(
          modifier = Modifier.semantics { selected = action == option },
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          text = {
            Column {
              TextPrimary(stringResource(labelRes), maxLine = 2)
              TextSecondary(
                stringResource(descriptionRes),
                modifier = Modifier.padding(top = 4.dp),
                maxLine = 3,
              )
            }
          },
          trailingIcon = {
            if (action == option) {
              Icon(Icons.Filled.Check, contentDescription = null, tint = theme.primary)
            }
          },
          onClick = {
            onActionSelected(option)
            expanded = false
          },
        )
      }
    }
  }
}
