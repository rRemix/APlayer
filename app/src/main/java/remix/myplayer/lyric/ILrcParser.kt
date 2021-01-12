package remix.myplayer.lyric

import remix.myplayer.lyric.bean.LrcRow
import java.io.BufferedReader

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/28 09:48
 */
interface ILrcParser {
  fun saveLrcRows(lrcRows: List<LrcRow>?, cacheKey: String?, searchKey: String?)
  fun getLrcRows(bufferedReader: BufferedReader?, needCache: Boolean, cacheKey: String?,
                 searchKey: String?): List<LrcRow>
}