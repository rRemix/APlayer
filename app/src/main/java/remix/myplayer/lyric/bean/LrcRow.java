package remix.myplayer.lyric.bean;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import remix.myplayer.App;
import remix.myplayer.R;

/**
 * 每行歌词的实体类，实现了Comparable接口，方便List<LrcRow>的sort排序
 *
 * @author Ligang  2014/8/19
 */
public class LrcRow implements Comparable<LrcRow> {

  /**
   * 开始时间 为00:10:00
   ***/
  private String mTimeStr;
  /**
   * 开始时间 毫秒数  00:10:00  为10000
   **/
  private int mTime;
  /**
   * 歌词内容
   **/
  private String mContent;
  /**
   * 歌词内容翻译
   */
  private String mTranslate;
  /**
   * 该行歌词显示的总时间
   **/
  private long mTotalTime;
  /**
   * 该行歌词内容所占的高度
   */
  private int mContentHeight;
  /**
   * 该行歌词翻译所占的高度
   */
  private int mTranslateHeight;
  /**
   * 该句歌词所占的总共高度
   */
  private int mTotalHeight;

  public void setTotalHeight(int height) {
    this.mTotalHeight = height;
  }

  public int getTotalHeight() {
    return mTotalHeight;
  }

  public void setContentHeight(int height) {
    this.mContentHeight = height;
  }

  public int getContentHeight() {
    return mContentHeight;
  }

  public void setTranslateHeight(int height) {
    this.mTranslateHeight = height;
  }

  public int getTranslateHeight() {
    return mTranslateHeight;
  }

  public long getTotalTime() {
    return mTotalTime;
  }

  public void setTotalTime(int totalTime) {
    this.mTotalTime = totalTime;
  }

  public String getTimeStr() {
    return mTimeStr;
  }

  public void setTimeStr(String timeStr) {
    this.mTimeStr = timeStr;
  }

  public int getTime() {
    return mTime;
  }

  public void setTime(int time) {
    this.mTime = time;
  }

  public String getContent() {
    return mContent;
  }

  public void setContent(String content) {
    this.mContent = content;
  }

  public String getTranslate() {
    return mTranslate;
  }

  public void setTranslate(String translate) {
    mTranslate = translate;
  }

  public boolean hasTranslate() {
    return !TextUtils.isEmpty(mTranslate);
  }

  public LrcRow() {
  }

  public LrcRow(LrcRow lrcRow) {
    mTimeStr = lrcRow.getTimeStr();
    mTime = lrcRow.getTime();
    mTotalTime = lrcRow.getTotalTime();
    mContent = lrcRow.getContent();
    mTranslate = lrcRow.getTranslate();
  }

  public LrcRow(String timeStr, int time, String content) {
    super();
    mTimeStr = timeStr;
    mTime = time;
    if (TextUtils.isEmpty(content)) {
      mContent = "";
      mTranslate = "";
      return;
    }
    String[] mulitiContent = content.split("\t");
    mContent = mulitiContent[0];
    if (mulitiContent.length > 1) {
      mTranslate = mulitiContent[1];
    }
  }

  /**
   * 将歌词文件中的某一行 解析成一个List<LrcRow> 因为一行中可能包含了多个LrcRow对象 比如  [03:33.02][00:36.37]当鸽子不再象征和平  ，就包含了2个对象
   */
  public static List<LrcRow> createRows(String lrcLine, int offset) {
    if (!lrcLine.startsWith("[") || !lrcLine.contains("]")) {
      return null;
    }
    //最后一个"]"
    int lastIndexOfRightBracket = lrcLine.lastIndexOf("]");
    //歌词内容
    String content = lrcLine.substring(lastIndexOfRightBracket + 1, lrcLine.length());
    //截取出歌词时间，并将"[" 和"]" 替换为"-"   [offset:0]

    // -03:33.02--00:36.37-
    String times = lrcLine.substring(0, lastIndexOfRightBracket + 1).replace("[", "-")
        .replace("]", "-");
    String[] timesArray = times.split("-");
    List<LrcRow> lrcRows = new ArrayList<>();
    for (String tem : timesArray) {
      //保留空白行
      if (TextUtils.isEmpty(tem.trim()) /**|| TextUtils.isEmpty(content)*/) {
        continue;
      }
      try {
        LrcRow lrcRow = new LrcRow(tem, formatTime(tem) - offset, content);
        lrcRows.add(lrcRow);
      } catch (Exception e) {
      }
    }
    return lrcRows;
  }

  /****
   * 把歌词时间转换为毫秒值  如 将00:10.00  转为10000
   * @param timeStr
   * @return
   */
  private static int formatTime(String timeStr) {
    timeStr = timeStr.replace('.', ':');
    String[] times = timeStr.split(":");
    
    if (times.length > 2) {
      return Integer.parseInt(times[0]) * 60 * 1000
          + Integer.parseInt(times[1]) * 1000
          + Integer.parseInt(times[2]);
    }
    return Integer.parseInt(times[0]) * 60 * 1000
        + Integer.parseInt(times[1]) * 1000;
  }

  @Override
  public int compareTo(@NonNull LrcRow anotherLrcRow) {
    return this.mTime - anotherLrcRow.mTime;
  }

//	@Override
//	public String toString() {
//		return "LrcRow [mTimeStr=" + mTimeStr + ", mTime=" + mTime + ", mTotalTime=" + mTotalTime +", mContent="
//				+ mContent + "]";
//	}

  @Override
  public String toString() {
    return "[" + mTimeStr + "] " + mContent;
  }

  public static int getOffset() {
    return 0;
  }

  public static LrcRow LYRIC_EMPTY_ROW = new LrcRow("", 0, "");
  public static LrcRow LYRIC_NO_ROW = new LrcRow("", 0,
      App.getContext().getString(R.string.no_lrc));
  public static LrcRow LYRIC_SEARCHING_ROW = new LrcRow("", 0,
      App.getContext().getString(R.string.searching));
}
