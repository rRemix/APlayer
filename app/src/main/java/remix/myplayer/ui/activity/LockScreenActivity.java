package remix.myplayer.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.lyric.UpdateLyricThread;
import remix.myplayer.lyric.bean.LyricRowWrapper;
import remix.myplayer.misc.menu.CtrlButtonListener;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.ui.blur.StackBlurManager;
import remix.myplayer.ui.widget.VerticalScrollTextView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.StatusBarUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2016/3/9.
 */

/**
 * 锁屏界面
 * 实际为将手机解锁并对Activity进行处理，使其看起来像锁屏界面
 */

public class LockScreenActivity extends BaseMusicActivity {
    private static final String TAG = "LockScreenActivity";
    private static final Bitmap DEFAULT_BITMAP = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.album_empty_bg_night);
    private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 210);
    private static final int BLUR_SIZE = DensityUtil.dip2px(App.getContext(), 100);
    private static final RequestConfig CONFIG = new RequestConfig.Builder(BLUR_SIZE, BLUR_SIZE).build();
    //歌曲与艺术家
    @BindView(R.id.lockscreen_song)
    TextView mSong;
    @BindView(R.id.lockscreen_artist)
    TextView mArtist;
    //歌词
    @BindView(R.id.lockscreen_lyric)
    VerticalScrollTextView mLyric;
    //控制按钮
    @BindView(R.id.lockscreen_prev)
    ImageButton mPrevButton;
    @BindView(R.id.lockscreen_next)
    ImageButton mNextButton;
    @BindView(R.id.lockscreen_play)
    ImageButton mPlayButton;
    @BindView(R.id.lockscreen_image)
    SimpleDraweeView mSimpleImage;
    //背景
    @BindView(R.id.lockscreen_background)
    ImageView mImageBackground;

    //DecorView, 跟随手指滑动
    private View mView;
    //高斯模糊后的bitmap
    private Bitmap mNewBitMap;
    //高斯模糊之前的bitmap
    private Bitmap mRawBitMap;
    private int mWidth;

    //是否正在播放
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private volatile LyricRowWrapper mCurLyric;
    private UpdateLyricThread mUpdateLyricThread;


    @Override
    protected void setUpTheme() {
    }

    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);
        ButterKnife.bind(this);

        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        mWidth = metric.widthPixels;

        //解锁屏幕
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        attr.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

        //初始化按钮
        CtrlButtonListener listener = new CtrlButtonListener(this);
        mPrevButton.setOnClickListener(listener);
        mNextButton.setOnClickListener(listener);
        mPlayButton.setOnClickListener(listener);

        //初始化控件
        mImageBackground.setAlpha(0.75f);
        mView = getWindow().getDecorView();
        mView.setBackgroundColor(Color.TRANSPARENT);

        findViewById(R.id.lockscreen_arrow_container).startAnimation(AnimationUtils.loadAnimation(this, R.anim.arrow_left_to_right));

    }

    //前后两次触摸的X
    private float mScrollX1;
    private float mScrollX2;
    //一次移动的距离
    private float mDistance;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mView == null)
            return true;
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrollX1 = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                mScrollX2 = event.getX();
                mDistance = mScrollX2 - mScrollX1;
                mScrollX1 = mScrollX2;
                //如果往右或者是往左没有超过最左边,移动View
                if (mDistance > 0 || ((mView.getScrollX() + (-mDistance)) < 0)) {
                    mView.scrollBy((int) -mDistance, 0);
                }
                LogUtil.d(TAG, "distance:" + mDistance + "\r\n");
                break;
            case MotionEvent.ACTION_UP:
                //判断当前位置是否超过整个屏幕宽度的0.25
                //超过则finish;没有则移动回初始状态
                if (-mView.getScrollX() > mWidth * 0.25)
                    finish();
                else
                    mView.scrollTo(0, 0);
                mDistance = mScrollX1 = 0;
                break;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(0, 0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.cover_right_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUpdateLyricThread != null) {
            mUpdateLyricThread.interrupt();
            mUpdateLyricThread = null;
        }
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        if (mRawBitMap != null && !mRawBitMap.isRecycled())
            mRawBitMap.recycle();
        if (mNewBitMap != null && !mNewBitMap.isRecycled())
            mNewBitMap.recycle();
    }

    @Override
    public void onServiceConnected(@NonNull MusicService service) {
        super.onServiceConnected(service);
        if (mUpdateLyricThread == null) {
            mUpdateLyricThread = new UpdateLockScreenLyricThread(this, service);
            mUpdateLyricThread.start();
        }
        onMetaChanged();
        onPlayStateChange();
    }


    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        final Song song = MusicServiceRemote.getCurrentSong();
        //歌词
        if (mUpdateLyricThread != null)
            mUpdateLyricThread.setSongAndGetLyricRows(song);
        //标题
        if (mSong != null) {
            mSong.setText(song.getTitle());
        }
        //艺术家
        if (mArtist != null) {
            mArtist.setText(song.getArtist());
        }
        //封面
        if (mSimpleImage != null) {
            new LibraryUriRequest(mSimpleImage,
                    getSearchRequestWithAlbumType(song),
                    new RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load();
        }


        new ImageUriRequest<Palette>(CONFIG) {
            @Override
            public void onError(String errMsg) {
//                ToastUtil.show(mContext,errMsg);
            }

            @Override
            public void onSuccess(Palette result) {
                setResult(result);
            }

            @Override
            public Disposable load() {
                final Disposable disposable = getThumbBitmapObservable(ImageUriUtil.getSearchRequestWithAlbumType(song))
                        .compose(RxUtil.applySchedulerToIO())
                        .flatMap(bitmap -> Observable.create((ObservableOnSubscribe<Palette>) e -> {
                            if (bitmap == null) {
                                processBitmap(e, DEFAULT_BITMAP);
                            } else {
                                processBitmap(e, bitmap);
                            }
                        }))
                        .onErrorResumeNext(Observable.create(e -> processBitmap(e, DEFAULT_BITMAP)))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
                mDisposable.add(disposable);
                return disposable;
            }
        }.load();
    }

    @Override
    public void onPlayStateChange() {
        super.onPlayStateChange();
        //更新播放按钮
        if (mPlayButton != null) {
            mPlayButton.setImageResource(MusicServiceRemote.isPlaying() ? R.drawable.lock_btn_pause : R.drawable.lock_btn_play);
        }
    }

    private void setResult(Palette result) {
        if (result == null)
            return;
        mImageBackground.setImageBitmap(mNewBitMap);

        Palette.Swatch swatch = ColorUtil.getSwatch(result);
        mSong.setTextColor(swatch.getBodyTextColor());
        mArtist.setTextColor(swatch.getTitleTextColor());
        mLyric.setTextColor(swatch.getBodyTextColor());
    }

    private void processBitmap(ObservableEmitter<Palette> e, Bitmap raw) {
        if (isFinishing()) {
            e.onComplete();
            return;
        }
        mRawBitMap = MusicService.copy(raw);
        if (mRawBitMap == null || mRawBitMap.isRecycled()) {
            e.onComplete();
            return;
        }
        StackBlurManager stackBlurManager = new StackBlurManager(mRawBitMap);
        mNewBitMap = stackBlurManager.processNatively(40);
        Palette palette = Palette.from(mRawBitMap).generate();
        e.onNext(palette);
        e.onComplete();
    }

    private void setCurrentLyric(LyricRowWrapper wrapper) {
        runOnUiThread(() -> {
            mCurLyric = wrapper;
            if (mCurLyric == null) {
                mLyric.setTextWithAnimation(R.string.no_lrc);
            } else if (mCurLyric.getStatus() == UpdateLyricThread.Status.SEARCHING) {
                mLyric.setText("");
            } else if (mCurLyric.getStatus() == UpdateLyricThread.Status.ERROR ||
                    mCurLyric.getStatus() == UpdateLyricThread.Status.NO) {
                mLyric.setTextWithAnimation(R.string.no_lrc);
            } else {
                mLyric.setTextWithAnimation(String.format("%s\n%s", mCurLyric.getLineOne().getContent(), mCurLyric.getLineTwo().getContent()));
            }
        });
    }

    private static class UpdateLockScreenLyricThread extends UpdateLyricThread {
        private final WeakReference<LockScreenActivity> mRef;

        private UpdateLockScreenLyricThread(LockScreenActivity activity, MusicService service) {
            super(service);
            mRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(LRC_INTERVAL);
                } catch (InterruptedException e) {
                    return;
                }
                final LockScreenActivity activity = mRef.get();
                if (activity != null)
                    activity.setCurrentLyric(findCurrentLyric());
            }
        }
    }

//    private static class ImageUriRequestImpl extends ImageUriRequest<Palette.Swatch> {
//        private final WeakReference<LockScreenActivity> mRef;
//        private final Song mSong;
//
//        ImageUriRequestImpl(LockScreenActivity activity, Song song) {
//            mRef = new WeakReference<>(activity);
//            mSong = song;
//        }
//
//        @Override
//        public void onError(String errMsg) {
//
//        }
//
//        @Override
//        public void onSuccess(Palette.Swatch result) {
//            if (mRef.get() != null && !mRef.get().isFinishing())
//                mRef.get().setResult(result);
//        }
//
//        @SuppressLint("CheckResult")
//        @Override
//        public void load() {
//            final LockScreenActivity activity = mRef.get();
//            if (activity == null || activity.isFinishing())
//                return;
//            UriRequest request = ImageUriUtil.getSearchRequestWithAlbumType(mSong);
//            getThumbBitmapObservable(request)
//                    .compose(RxUtil.applySchedulerToIO())
//                    .flatMap(bitmap -> Observable.create((ObservableOnSubscribe<Palette.Swatch>) e -> {
//                        if (bitmap == null) {
//                            activity.processBitmap(e, DEFAULT_BITMAP);
//                        } else {
//                            activity.processBitmap(e, bitmap);
//                        }
//                    }))
//                    .onErrorResumeNext(Observable.create(e -> activity.processBitmap(e, DEFAULT_BITMAP)))
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
//        }
//    }

}
