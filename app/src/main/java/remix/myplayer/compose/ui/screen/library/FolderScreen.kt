package remix.myplayer.compose.ui.screen.library

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.compose.nav.DetailScreenRoute
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.icon
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.popup.LibraryItemPopupButton
import remix.myplayer.compose.viewmodel.MultiSelectState
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.mainViewModel

@Composable
fun FolderScreen() {
  val libraryVM = libraryViewModel
  val folders by libraryVM.folders.collectAsStateWithLifecycle()
  val nav = LocalNavController.current

  val mainVM = mainViewModel
  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val selectedFolders by remember {
    derivedStateOf {
      multiSelectState.selectedModels(MultiSelectState.Where.Folder)
    }
  }

  LazyColumn(modifier = Modifier.fillMaxHeight()) {
    itemsIndexed(folders, key = { _, folder ->
      folder.path
    }) { pos, folder ->
      FolderItem(
        folder,
        selected = selectedFolders.contains(folder.getKey()),
        onClick = {
          if (multiSelectState.where == MultiSelectState.Where.Folder) {
            mainVM.updateMultiSelectModel(folder)
            return@FolderItem
          }

          nav.navigate(DetailScreenRoute(folder = folder))
        },
        onLongClick = {
          mainVM.showMultiSelect(context, MultiSelectState.Where.Folder, folder)
        }
      )
    }
  }
}

@Composable
fun FolderItem(folder: Folder, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
  val theme = LocalTheme.current

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(color = theme.ripple),
        onClick = onClick,
        onLongClick = onLongClick
      )
      .background(if (selected) theme.select else theme.mainBackground)
  ) {
    Icon(
      painter = painterResource(id = R.drawable.icon_folder),
      contentDescription = "Folder Icon",
      tint = theme.icon(),
      modifier = Modifier
        .padding(15.dp)
        .align(Alignment.CenterStart)
    )

    LibraryItemPopupButton(modifier = Modifier.align(Alignment.CenterEnd), model = folder)

    Text(
      text = pluralStringResource(R.plurals.song_num, folder.count, folder.count),
      fontSize = 12.sp,
      color = Color.Black,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .padding(end = 78.dp)
    )

    Column(
      modifier = Modifier
        .fillMaxHeight()
        .fillMaxWidth(0.6f)
        .padding(
          start = 15.dp + 24.dp + 15.dp,
        ),
      verticalArrangement = Arrangement.Center
    ) {
      TextPrimary(text = folder.name ?: "", fontSize = 12.sp)
      TextSecondary(text = folder.path, fontSize = 10.sp)
    }

  }
}

@Preview(showBackground = true)
@Composable
fun FolderItemPreview() {
  APlayerTheme {
    FolderItem(
      Folder("Folder", 10, "/sdcard/Music"),
      selected = true,
      onClick = {},
      onLongClick = {})
  }
}
