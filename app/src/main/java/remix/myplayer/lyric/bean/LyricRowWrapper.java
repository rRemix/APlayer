package remix.myplayer.lyric.bean;

import static remix.myplayer.lyric.bean.LrcRow.LYRIC_EMPTY_ROW;
import static remix.myplayer.lyric.bean.LrcRow.LYRIC_NO_ROW;
import static remix.myplayer.lyric.bean.LrcRow.LYRIC_SEARCHING_ROW;

import remix.myplayer.lyric.LyricFetcher;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/10 13:37
 */

public class LyricRowWrapper {
  public static final LyricRowWrapper LYRIC_WRAPPER_NO = new LyricRowWrapper(LYRIC_NO_ROW, LYRIC_EMPTY_ROW);
  public static final LyricRowWrapper LYRIC_WRAPPER_SEARCHING = new LyricRowWrapper(LYRIC_SEARCHING_ROW,LYRIC_EMPTY_ROW);


  public LyricRowWrapper(LrcRow lineOne,LrcRow lineTwo){
    mLineOne = lineOne;
    mLineTwo = lineTwo;
  }

  public LyricRowWrapper(){

  }

  private LrcRow mLineOne = LYRIC_EMPTY_ROW;
  private LrcRow mLineTwo = LYRIC_EMPTY_ROW;

  private LyricFetcher.Status mStatus;

  public LrcRow getLineOne() {
    return mLineOne;
  }

  public void setLineOne(LrcRow lineOne) {
    mLineOne = lineOne;
  }

  public LrcRow getLineTwo() {
    return mLineTwo;
  }

  public void setLineTwo(LrcRow lineTwo) {
    mLineTwo = lineTwo;
  }

  public LyricFetcher.Status getStatus() {
    return mStatus;
  }

  public void setStatus(LyricFetcher.Status status) {
    mStatus = status;
  }

  @Override
  public String toString() {
    return "LyricRowWrapper{" +
        "LineOne=" + mLineOne +
        ", LineTwo=" + mLineTwo +
        '}';
  }
}
