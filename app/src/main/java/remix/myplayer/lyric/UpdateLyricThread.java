package remix.myplayer.lyric;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.lyric.bean.LrcRowWrapper;
import remix.myplayer.util.LogUtil;

import static remix.myplayer.helper.MusicServiceRemote.getCurrentSong;
import static remix.myplayer.helper.MusicServiceRemote.getProgress;


public abstract class UpdateLyricThread extends Thread {
    public static final LrcRow EMPTY_ROW = new LrcRow("",0,"");
    public static final LrcRow NO_ROW = new LrcRow("",0, App.getContext().getString(R.string.no_lrc));
    public static final LrcRow SEARCHING_ROW = new LrcRow("",0,App.getContext().getString(R.string.searching));
    public static final int LRC_INTERVAL = 400;

    private volatile List<LrcRow> mLrcRows;
    private Song mSong;
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private Status mStatus = Status.SEARCHING;

    public UpdateLyricThread(){
        setSongAndGetLyricRows(MusicServiceRemote.getCurrentSong());
    }

    public UpdateLyricThread(Song song){
        setSongAndGetLyricRows(song);
    }

    private void updateLrcRows() {
        if(mSong == null){
            mStatus = Status.NO;
            mLrcRows = null;
            return;
        }
        final int id = mSong.getId();
        mDisposable.add(new SearchLrc(mSong).getLyric()
                .doOnSubscribe(disposable -> mStatus = Status.SEARCHING)
                .subscribe(lrcRows -> {
                    if (id == mSong.getId()) {
                        mStatus = Status.NORMAL;
                        mLrcRows = lrcRows;
                    }
                }, throwable -> {
                    LogUtil.e(throwable);
                    if (id == mSong.getId()) {
                        mStatus = Status.ERROR;
                        mLrcRows = null;
                    }
                }));
    }

    public void setSongAndGetLyricRows(Song song){
        mSong = song;
        updateLrcRows();
    }

    public Status getStatus() {
        return mStatus;
    }

    public void quitImmediately(){
        interrupt();
    }

    @Override
    public void interrupt() {
        mDisposable.dispose();
        super.interrupt();
    }

    protected LrcRowWrapper findCurrentLyric() {
        LrcRowWrapper wrapper = new LrcRowWrapper();
        wrapper.setStatus(mStatus);
        if(mStatus == Status.SEARCHING){
            return wrapper;
        }
        Song song = getCurrentSong();
        if(mStatus == Status.ERROR || mStatus == Status.NO){
            LogUtil.d("DesktopLrc","当前歌词 -- findCurrentLyricError");
            return wrapper;
        }
        int progress = getProgress();
        LogUtil.d("DesktopLrc","当前歌词 -- Progress: " + getProgress());
        for(int i = mLrcRows.size() - 1;i >= 0 ;i--){
            LrcRow lrcRow = mLrcRows.get(i);
            int interval = progress - lrcRow.getTime();
            if(i == 0 && interval < 0){
                //未开始歌唱前显示歌曲信息
                wrapper.setLineOne(new LrcRow("",0, song.getTitle()));
                wrapper.setLineTwo(new LrcRow("",0, song.getArtist() + " - " + song.getAlbum()));
                return wrapper;
            }
            else if(progress >= lrcRow.getTime()){
                if(lrcRow.hasTranslate()){
                    wrapper.setLineOne(new LrcRow(lrcRow));
                    wrapper.getLineOne().setContent(lrcRow.getContent());
                    wrapper.setLineTwo(new LrcRow(lrcRow));
                    wrapper.getLineTwo().setContent(lrcRow.getTranslate());
                } else {
                    wrapper.setLineOne(lrcRow);
                    wrapper.setLineTwo(new LrcRow(i + 1 < mLrcRows.size() ? mLrcRows.get(i + 1) : EMPTY_ROW));
                }
                return wrapper;
            }
        }
        return wrapper;
    }

    public enum Status{
        NO,SEARCHING,ERROR,NORMAL
    }
}
