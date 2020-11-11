package remix.myplayer.bean.misc

import remix.myplayer.App
import remix.myplayer.R
import java.util.*

enum class LyricPriority(val priority: Int, val desc: String) {
  DEF(0, App.getContext().getString(R.string.default_lyric_priority)),
  IGNORE(1, App.getContext().getString(R.string.ignore_lrc)),
  KUGOU(2, App.getContext().getString(R.string.kugou)),
  NETEASE(3, App.getContext().getString(R.string.netease)),
  QQ(4, App.getContext().getString(R.string.qq)),
  LOCAL(5, App.getContext().getString(R.string.local)),
  EMBEDED(6, App.getContext().getString(R.string.embedded_lyric)),
  MANUAL(7, App.getContext().getString(R.string.select_lrc));

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
        EMBEDED.desc -> EMBEDED
        MANUAL.desc -> MANUAL
        else -> DEF
      }
    }
  }


}
