package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppUpdatePrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "Update") {

  var ignoreForever by PrefsDelegate(sp, IGNORE_FOREVER, false)

  fun setIgnoreVersion(versionCode: Int, ignored: Boolean = true) {
    sp.edit { putBoolean(versionCode.toString(), ignored) }
  }
  fun isVersionIgnored(versionCode: Int) =
    sp.getBoolean(versionCode.toString(), false)

  companion object {

    private const val IGNORE_FOREVER: String = "ignore_forever"
  }
}