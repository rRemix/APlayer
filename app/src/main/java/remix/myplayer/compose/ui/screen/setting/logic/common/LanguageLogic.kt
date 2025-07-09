package remix.myplayer.compose.ui.screen.setting.logic.common

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.compose.activity.ComposeActivity
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.ItemsCallbackSingleChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.helper.LanguageHelper

private val itemRes = listOf(
  R.string.auto,
  R.string.zh_simple,
  R.string.zh_traditional,
  R.string.english,
  R.string.japanese,
)
@Composable
fun LanguageLogic() {
  val context = LocalContext.current
  val setting = activityViewModel<LibraryViewModel>().settingPrefs

  val select by remember {
    mutableIntStateOf(setting.language)
  }
  val state = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.select_language),
    stringResource(R.string.select_language_tips)
  ) {
    state.show()
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.select_language,
    positiveRes = null,
    negativeRes = null,
    itemRes = itemRes,
    itemsCallbackSingleChoice = ItemsCallbackSingleChoice(select) {
      if (select == it) {
        return@ItemsCallbackSingleChoice
      }
      LanguageHelper.saveSelectLanguage(context, it)

      val intent = Intent(context, ComposeActivity::class.java)
      intent.action = Intent.ACTION_MAIN
      intent.addCategory(Intent.CATEGORY_LAUNCHER)
      intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(intent)
    }
  )


}