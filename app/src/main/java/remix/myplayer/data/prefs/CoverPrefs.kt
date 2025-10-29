package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoverPrefsEntryPoint {
  fun coverPrefs(): CoverPrefs
}

@Singleton
class CoverPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, name = "Cover") {

  fun putCover(key: String, value: String) {
    sp.edit { putString(key, value) }
  }

  fun getCover(key: String, default: String = ""): String {
    return sp.getString(key, default) ?: default
  }

  fun clearAll() {
    clear()
  }
}