package remix.myplayer.bean.misc

import remix.myplayer.App
import remix.myplayer.R

enum class LyricPriority(val priority: Int, val desc: String) {
    DEF(0, App.getContext().getString(R.string.default_lyric_priority)),
    IGNORE(1, App.getContext().getString(R.string.ignore_lrc)),
    NETEASE(2, App.getContext().getString(R.string.netease)),
    KUGOU(3, App.getContext().getString(R.string.kugou)),
    LOCAL(4, App.getContext().getString(R.string.local)),
    EMBEDED(5, App.getContext().getString(R.string.embedded_lyric)),
    MANUAL(6, App.getContext().getString(R.string.select_lrc))

}
