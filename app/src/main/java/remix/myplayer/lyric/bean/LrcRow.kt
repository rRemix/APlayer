package remix.myplayer.lyric.bean

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import remix.myplayer.App
import remix.myplayer.R
import java.util.*

/**
 * 每行歌词的实体类，实现了Comparable接口，方便List<LrcRow>的sort排序
 *
 * @author Ligang  2014/8/19
</LrcRow> */
class LrcRow : Comparable<LrcRow> {
  @SerializedName("mTimeStr")
  var timeStr: String = ""

  /**
   * 开始时间 毫秒数  00:10:00  为10000
   */
  @SerializedName("mTime")
  var time = 0

  /**
   * 歌词内容
   */
  @SerializedName("mContent")
  var content: String = ""

  /**
   * 歌词内容翻译
   */
  @SerializedName("mTranslate")
  var translate: String = ""

  /**
   * 该行歌词显示的总时间
   */
  @SerializedName("mTotalTime")
  var totalTime: Long = 0
    private set

  /**
   * 该行歌词内容所占的高度
   */
  @SerializedName("mContentHeight")
  var contentHeight = 0

  /**
   * 该行歌词翻译所占的高度
   */
  @SerializedName("mTranslateHeight")
  var translateHeight = 0

  /**
   * 该句歌词所占的总共高度
   */
  @SerializedName("mTotalHeight")
  var totalHeight = 0

  fun setTotalTime(totalTime: Int) {
    this.totalTime = totalTime.toLong()
  }

  fun hasTranslate(): Boolean {
    return !TextUtils.isEmpty(translate)
  }

  constructor() {}
  constructor(lrcRow: LrcRow) {
    timeStr = lrcRow.timeStr
    time = lrcRow.time
    totalTime = lrcRow.totalTime
    content = lrcRow.content
    translate = lrcRow.translate
  }

  constructor(timeStr: String, time: Int, content: String) : super() {
    this.timeStr = timeStr
    this.time = time
    if (TextUtils.isEmpty(content)) {
      this.content = ""
      translate = ""
      return
    }
    val mulitiContent = content.split("\t".toRegex()).toTypedArray()
    this.content = mulitiContent[0]
    if (mulitiContent.size > 1) {
      translate = mulitiContent[1]
    }
  }

  override fun compareTo(row: LrcRow): Int {
    return time - row.time
  }

  //	@Override
  //	public String toString() {
  //		return "LrcRow [mTimeStr=" + mTimeStr + ", mTime=" + mTime + ", mTotalTime=" + mTotalTime +", mContent="
  //				+ mContent + "]";
  //	}
  override fun toString(): String {
    return "[$timeStr] $content"
  }

  companion object {
    /**
     * 将歌词文件中的某一行 解析成一个List<LrcRow> 因为一行中可能包含了多个LrcRow对象 比如  [03:33.02][00:36.37]当鸽子不再象征和平  ，就包含了2个对象
    </LrcRow> */
    fun createRows(lrcLine: String, offset: Int): List<LrcRow>? {
      if (!lrcLine.startsWith("[") || !lrcLine.contains("]")) {
        return null
      }
      //最后一个"]"
      val lastIndexOfRightBracket = lrcLine.lastIndexOf("]")
      //歌词内容
      val content = lrcLine.substring(lastIndexOfRightBracket + 1, lrcLine.length)
      //截取出歌词时间，并将"[" 和"]" 替换为"-"   [offset:0]

      // -03:33.02--00:36.37-
      val times = lrcLine.substring(0, lastIndexOfRightBracket + 1).replace("[", "-")
          .replace("]", "-")
      val timesArray = times.split("-".toRegex()).toTypedArray()
      val lrcRows: MutableList<LrcRow> = ArrayList()
      for (tem in timesArray) {
        //保留空白行
        if (TextUtils.isEmpty(tem.trim { it <= ' ' })
        /**|| TextUtils.isEmpty(content) */
        ) {
          continue
        }
        try {
          val lrcRow = LrcRow(tem, formatTime(tem) - offset, content)
          lrcRows.add(lrcRow)
        } catch (e: Exception) {
        }
      }
      return lrcRows
    }

    /****
     * 把歌词时间转换为毫秒值  如 将00:10.00  转为10000
     * @param str
     * @return
     */
    private fun formatTime(str: String): Int {
      val timeStr = str.replace('.', ':')
      val times = timeStr.split(":".toRegex()).toTypedArray()
      return if (times.size > 2) {
        times[0].toInt() * 60 * 1000 + times[1].toInt() * 1000 + times[2].toInt()
      } else times[0].toInt() * 60 * 1000 + times[1].toInt() * 1000
    }

    val offset: Int
      get() = 0

    @JvmField
    var LYRIC_EMPTY_ROW = LrcRow("", 0, "")

    @JvmField
    var LYRIC_NO_ROW = LrcRow("", 0,
        App.getContext().getString(R.string.no_lrc))

    @JvmField
    var LYRIC_SEARCHING_ROW = LrcRow("", 0,
        App.getContext().getString(R.string.searching))
  }
}