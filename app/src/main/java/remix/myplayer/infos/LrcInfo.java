package remix.myplayer.infos;

/**
 * Created by Remix on 2015/12/8.
 */

/**
 * 歌词信息
 */
public class LrcInfo {
    private String mSentence;
    private int mStartTime;
    private int mEndTime;
    private int mDuration;

    public LrcInfo(String mSentence, int mStartTime, int mEndTime) {
        this.mSentence = mSentence;
        this.mStartTime = mStartTime;
        this.mEndTime = mEndTime;
    }

    public LrcInfo(String mSentence, int mStartTime) {
        this.mSentence = mSentence;
        this.mStartTime = mStartTime;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int mDuration) {
        this.mDuration = mDuration;
    }

    public boolean isInTime(long time) {
        return time >= mStartTime && time <= mEndTime;
    }
    public String getSentence() {
        return mSentence;
    }

    public void setSentence(String mSentence) {
        this.mSentence = mSentence;
    }

    public int getStartTime() {
        return mStartTime;
    }

    public void setStartTime(int mStartTime) {
        this.mStartTime = mStartTime;
    }

    public int getEndTime() {
        return mEndTime;
    }

    public void setEndTime(int mEndTime) {
        this.mEndTime = mEndTime;
    }
}
