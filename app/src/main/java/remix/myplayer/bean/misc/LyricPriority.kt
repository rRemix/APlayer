package remix.myplayer.bean.misc

import remix.myplayer.App
import remix.myplayer.R
import java.util.*

enum class LyricPriority(val priority: Int, val desc: String) {
  DEF(0, App.context.getString(R.string.default_lyric_priority)),
  IGNORE(1, App.context.getString(R.string.ignore_lrc)),
  EMBEDDED(2, App.context.getString(R.string.embedded_lyric)),
  LOCAL(3, App.context.getString(R.string.local)),
  KUGOU(4, App.context.getString(R.string.kugou)),
  NETEASE(5, App.context.getString(R.string.netease)),
  QQ(6, App.context.getString(R.string.qq)),
  MANUAL(7, App.context.getString(R.string.select_lrc));

  override fun toString(): String {
    return desc
  }

  companion object {
    @JvmStatic
    fun toLyricPrioritys(list: List<CharSequence>): List<LyricPriority> {
      val prioritys = ArrayList<LyricPriority>()
      list.forEach {
        prioritys.add(toLyricPriority(it))
      }
      return prioritys
    }

    @JvmStatic
    fun toLyricPriority(desc: CharSequence): LyricPriority {
      return when (desc) {
        DEF.desc -> DEF
        IGNORE.desc -> IGNORE
        NETEASE.desc -> NETEASE
        KUGOU.desc -> KUGOU
        QQ.desc -> QQ
        LOCAL.desc -> LOCAL
        EMBEDDED.desc -> EMBEDDED
        MANUAL.desc -> MANUAL
        else -> DEF
      }
    }
  }


}
