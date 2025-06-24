package remix.myplayer.compose.prefs

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import remix.myplayer.bean.misc.LyricOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "Lyric") {
  val lyricOrderList: List<LyricOrder>
    get() = try {
      Gson().fromJson(lyricOrder, object : TypeToken<List<LyricOrder>>() {}.type)
    } catch (ignore: Exception){
      defaultLyricOrderList
    }

  var lyricOrder by PrefsDelegate(sp, KEY_LYRIC_ORDER, "")

  companion object{
    private val defaultLyricOrderList = LyricOrder.entries

    const val KEY_LYRIC_FONT_SIZE: String = "key_lyric_font_size"
    const val KEY_LYRIC_LOCAL_TIP_SHOWN: String = "Key_lyric_local_tip_shown"
    const val KEY_LYRIC_ORDER = "key_lyric_order"
  }
}