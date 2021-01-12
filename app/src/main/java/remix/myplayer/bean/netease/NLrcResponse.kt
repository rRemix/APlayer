package remix.myplayer.bean.netease

data class NLrcResponse(val lrc: LrcActualData? = null,
                        val klyric: LrcActualData? = null,
                        val tlyric: LrcActualData? = null) {

  data class LrcActualData(val version: Int,
                           val lyric: String? = null) {
  }
}