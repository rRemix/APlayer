package remix.myplayer.lyric

import android.os.Environment
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import remix.myplayer.App
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.misc.cache.DiskCache
import java.io.BufferedReader
import java.io.File

/**
 * @ClassName
 * @Description 解析歌词实现类
 * @Author Xiaoborui
 * @Date 2016/10/28 09:50
 */

class DefaultLrcParser : ILrcParser {
  override fun saveLrcRows(lrcRows: List<LrcRow>?, cacheKey: String, searchKey: String) {
    if (lrcRows == null || lrcRows.isEmpty())
      return

    //缓存
    DiskCache.getLrcDiskCache()?.apply {
      edit(cacheKey)?.apply {
        newOutputStream(0)?.use { outStream ->
          outStream.write(Gson().toJson(lrcRows, object : TypeToken<List<LrcRow>>() {}.type).toByteArray())
        }
      }?.commit()
    }?.flush()

    //保存歌词原始文件
    if (TextUtils.isEmpty(searchKey) || Environment.MEDIA_MOUNTED != Environment.getExternalStorageState())
      return

    File(Environment.getExternalStorageDirectory(), "Android/data/"
        + App.getContext().packageName + "/lyric")
        .run {
          //目录
          if (exists() || mkdirs())
            File(this, searchKey.replace("/".toRegex(), "") + ".lrc")
          else null
        }?.run {
          //文件不存在或者成功重新创建
          if (!exists() || (delete() && createNewFile()))
            this
          else null
        }?.run {
          lrcRows.forEach { lrcRow ->
            val strBuilder = StringBuilder(128)
            strBuilder.append("[")
            strBuilder.append(lrcRow.timeStr)
            strBuilder.append("]")
            strBuilder.append(lrcRow.content).append("\n")
            if (lrcRow.hasTranslate()) {
              strBuilder.append("[")
              strBuilder.append(lrcRow.timeStr)
              strBuilder.append("]")
              strBuilder.append(lrcRow.translate).append("\n")
            }
            appendText(strBuilder.toString())
          }
        }
  }

  override fun getLrcRows(bufferedReader: BufferedReader?, needCache: Boolean, cacheKey: String, searchKey: String): List<LrcRow>? {

    //解析歌词
    val lrcRows = ArrayList<LrcRow>()
    val allLine = ArrayList<String>()
    var offset = 0
    if (bufferedReader == null)
      return lrcRows
    bufferedReader.useLines { allLines ->
      allLines.forEach { eachLine ->
        allLine.add(eachLine)
        //读取offset标签
        if (eachLine.startsWith("[offset:") && eachLine.endsWith("]")) {
          val offsetInString = eachLine.substring(eachLine.lastIndexOf(":") + 1, eachLine.length - 1)
          if (offsetInString.isNotEmpty() && TextUtils.isDigitsOnly(offsetInString)) {
            offset = Integer.valueOf(offsetInString)
          }
        }
      }
    }

    if (allLine.size == 0)
      return lrcRows

    for (temp in allLine) {
      //解析每一行歌词
      val rows = LrcRow.createRows(temp, offset)
      if (rows != null && rows.size > 0)
        lrcRows.addAll(rows)
    }

    lrcRows.sort()
    //合并翻译
    val combineLrcRows = ArrayList<LrcRow>()
    var index = 0
    while (index < lrcRows.size) {
      // 判断下一句歌词和当前歌词的时间是否一致，一致则认为下一句是当前歌词的翻译
      val currentRow = lrcRows[index]
      val nextRow = lrcRows.getOrNull(index + 1)
      if (currentRow.time == nextRow?.time && !currentRow.content.isNullOrBlank()) { // 带翻译的歌词
        val tmp = LrcRow()
        tmp.content = currentRow.content
        tmp.time = currentRow.time
        tmp.timeStr = currentRow.timeStr
        tmp.translate = nextRow.content
        combineLrcRows.add(tmp)
        index++
      } else { // 普通歌词
        combineLrcRows.add(currentRow)
      }
      index++
    }

    lrcRows.clear()
    lrcRows.addAll(combineLrcRows)


    if (lrcRows.size == 0)
      return lrcRows
    lrcRows.sort()
    //每行歌词的时间
    for (i in 0 until lrcRows.size - 1) {
      lrcRows[i].setTotalTime(lrcRows[i + 1].time - lrcRows[i].time)
    }
    lrcRows[lrcRows.size - 1].setTotalTime(5000)
    //缓存
    if (needCache) {
      saveLrcRows(lrcRows, cacheKey, searchKey)
    }

    return lrcRows
  }

  companion object {
    const val TAG = "DefaultLrcParser"
    const val THRESHOLD_PROPORTION = 0.3
  }
}
