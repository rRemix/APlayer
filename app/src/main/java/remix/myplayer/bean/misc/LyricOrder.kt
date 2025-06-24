package remix.myplayer.bean.misc

import remix.myplayer.R

enum class LyricOrder {
  Def,
  Ignore,
  Embedded,
  Local,
  Kugou,
  Netease,
  Qq,
  Manual;

  val stringRes: Int
    get() = when (this) {
      Def -> R.string.default_lyric_priority
      Ignore -> R.string.ignore_lrc
      Embedded -> R.string.embedded_lyric
      Local -> R.string.local
      Kugou -> R.string.kugou
      Netease -> R.string.netease
      Qq -> R.string.qq
      Manual -> R.string.select_lrc
    }
}