package remix.myplayer.lyric.bean

import remix.myplayer.lyric.LyricFetcher

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/10 13:37
 */
data class LyricRowWrapper(var lineOne: LrcRow = LrcRow.LYRIC_EMPTY_ROW,
                           var lineTwo: LrcRow = LrcRow.LYRIC_EMPTY_ROW,
                           var status: LyricFetcher.Status = LyricFetcher.Status.NO) {
  override fun toString(): String {
    return "LyricRowWrapper{" +
        "LineOne=" + lineOne +
        ", LineTwo=" + lineTwo +
        '}'
  }

  companion object {
    val LYRIC_WRAPPER_NO = LyricRowWrapper(LrcRow.LYRIC_NO_ROW, LrcRow.LYRIC_EMPTY_ROW)
    val LYRIC_WRAPPER_SEARCHING = LyricRowWrapper(LrcRow.LYRIC_SEARCHING_ROW, LrcRow.LYRIC_EMPTY_ROW)
  }
}