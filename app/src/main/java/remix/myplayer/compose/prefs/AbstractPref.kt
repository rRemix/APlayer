package remix.myplayer.compose.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

abstract class AbstractPref(context: Context, name: String? = null) {
  private val name by lazy {
    name ?: this.javaClass.simpleName
  }
  val sp: SharedPreferences by lazy {
    context.getSharedPreferences(this.name, Context.MODE_PRIVATE)
  }

  fun clear() {
    sp.edit { clear() }
  }
}