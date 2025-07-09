package remix.myplayer.compose.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.util.SPUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdatePrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "Update") {

  var ignoreForever by PrefsDelegate(sp, SPUtil.UPDATE_KEY.IGNORE_FOREVER, false)
}