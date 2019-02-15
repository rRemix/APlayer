//package remix.myplayer.lyric;
//
//import io.reactivex.disposables.Disposable;
//import java.lang.ref.WeakReference;
//import java.util.List;
//import remix.myplayer.App;
//import remix.myplayer.R;
//import remix.myplayer.bean.mp3.Song;
//import remix.myplayer.lyric.bean.LrcRow;
//import remix.myplayer.lyric.bean.LyricRowWrapper;
//import remix.myplayer.service.MusicService;
//import remix.myplayer.util.SPUtil;
//import timber.log.Timber;
//
//public abstract class UpdateLyricThread extends Thread {
//
//  public static final String TAG = UpdateLyricThread.class.getSimpleName();
//  public static final LrcRow LYRIC_EMPTY_ROW = new LrcRow("", 0, "");
//  public static final LrcRow LYRIC_NO_ROW = new LrcRow("", 0,
//      App.getContext().getString(R.string.no_lrc));
//  public static final LrcRow LYRIC_SEARCHING_ROW = new LrcRow("", 0,
//      App.getContext().getString(R.string.searching));
//  public static final int LRC_INTERVAL = 400;
//
//  private volatile List<LrcRow> mLrcRows;
//  private Disposable mDisposable;
//  private WeakReference<MusicService> mReference;
//  private Song mSong;
//  private Status mStatus = Status.SEARCHING;
//  private int mOffset = 0;
//  private LyricSearcher mLyricSearcher = new LyricSearcher();
//
//  public UpdateLyricThread(MusicService service) {
//    mReference = new WeakReference<>(service);
//    setSongAndGetLyricRows(mReference.get().getCurrentSong());
//  }
//
//  private void updateLrcRows() {
//    if (mSong == null) {
//      mStatus = Status.NO;
//      mLrcRows = null;
//      return;
//    }
//    final int id = mSong.getId();
//    if (mDisposable != null && !mDisposable.isDisposed()) {
//      mDisposable.dispose();
//    }
//    mDisposable = mLyricSearcher.setSong(mSong)
//        .getLyricObservable()
//        .doOnSubscribe(disposable -> mStatus = Status.SEARCHING)
//        .subscribe(lrcRows -> {
//          if (id == mSong.getId()) {
//            mStatus = Status.NORMAL;
//            mOffset = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, id + "", 0);
//            mLrcRows = lrcRows;
//          }
//        }, throwable -> {
//          Timber.tag(TAG).v(throwable);
//          if (id == mSong.getId()) {
//            mStatus = Status.ERROR;
//            mLrcRows = null;
//          }
//        });
//  }
//
//  public void setSongAndGetLyricRows(Song song) {
//    mSong = song;
//    updateLrcRows();
//  }
//
//  public Status getStatus() {
//    return mStatus;
//  }
//
//  public void quitImmediately() {
//    interrupt();
//  }
//
//  @Override
//  public void interrupt() {
//    super.interrupt();
//    Timber.tag(TAG).v("interrupt");
//    if (mDisposable != null && !mDisposable.isDisposed()) {
//      mDisposable.dispose();
//    }
//    mReference = null;
//  }
//
//  protected LyricRowWrapper findCurrentLyric() {
//    final MusicService service = mReference != null ? mReference.get() : null;
//    if (service == null) {
//      return null;
//    }
//    LyricRowWrapper wrapper = new LyricRowWrapper();
//    wrapper.setStatus(mStatus);
//
//    if (mStatus == Status.SEARCHING) {
//      return wrapper;
//    }
//
//    if (mStatus == Status.ERROR || mStatus == Status.NO) {
//      Timber.tag(TAG).v("当前没有歌词");
//      return wrapper;
//    }
//    final Song song = service.getCurrentSong();
//    final int progress = service.getProgress() + mOffset;
//    if (mLrcRows == null || mLrcRows.isEmpty()) {
//      return wrapper;
//    }
//    for (int i = mLrcRows.size() - 1; i >= 0; i--) {
//      LrcRow lrcRow = mLrcRows.get(i);
//      int interval = progress - lrcRow.getTime();
//      if (i == 0 && interval < 0) {
//        //未开始歌唱前显示歌曲信息
//        wrapper.setLineOne(new LrcRow("", 0, song.getTitle()));
//        wrapper.setLineTwo(new LrcRow("", 0, song.getArtist() + " - " + song.getAlbum()));
//        return wrapper;
//      } else if (progress >= lrcRow.getTime()) {
//        if (lrcRow.hasTranslate()) {
//          wrapper.setLineOne(new LrcRow(lrcRow));
//          wrapper.getLineOne().setContent(lrcRow.getContent());
//          wrapper.setLineTwo(new LrcRow(lrcRow));
//          wrapper.getLineTwo().setContent(lrcRow.getTranslate());
//        } else {
//          wrapper.setLineOne(lrcRow);
//          wrapper.setLineTwo(new LrcRow(i + 1 < mLrcRows.size() ? mLrcRows.get(i + 1) : LYRIC_EMPTY_ROW));
//        }
//        return wrapper;
//      }
//    }
//    return wrapper;
//  }
//
//
//  public enum Status {
//    NO, SEARCHING, ERROR, NORMAL
//  }
//}
