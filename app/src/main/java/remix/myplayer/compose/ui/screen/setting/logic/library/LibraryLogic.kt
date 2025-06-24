package remix.myplayer.compose.ui.screen.setting.logic.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackMultiChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.SettingViewModel

@Composable
fun LibraryLogic() {
  val vm: SettingViewModel = activityViewModel<SettingViewModel>()

  val state = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.library_category),
    stringResource(R.string.configure_library_category)
  ) {
    state.show()
  }

  val libraries by vm.allLibraries.collectAsStateWithLifecycle()
  val currentLibrary by vm.currentLibrary.collectAsStateWithLifecycle()
  val selectedIndicates = remember {
    mutableStateSetOf(*libraries.map { it.order }.toTypedArray())
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.library_category,
    itemRes = Library.allLibraries.map { it.stringRes },
    onPositive = {
      val newLibraries = selectedIndicates.toSortedSet().map { Library(it) }
      if (libraries == newLibraries) {
        return@NormalDialog
      }

      vm.setAllLibraries(newLibraries)

      if (!newLibraries.contains(currentLibrary)) {
        vm.changeLibrary(newLibraries[0])
      }
    },
    itemsCallbackMultiChoice = ItemsCallbackMultiChoice(selectedIndicates) { index, selected ->
      if (selected) {
        selectedIndicates.add(index)
      } else {
        selectedIndicates.remove(index)
      }
    }
  )
}