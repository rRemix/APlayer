package remix.myplayer.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
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
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.blur.StackBlurManager;
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

public class LockScreenActivity extends BaseActivity implements UpdateHelper.Callback{
    private static final String TAG = "LockScreenActivity";
    private static final int IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),210);
    //当前播放的歌曲信息
    private Song mInfo;
    //歌曲与艺术家
    @BindView(R.id.lockscreen_song)
    TextView mSong;
    @BindView(R.id.lockscreen_artist)
    TextView mArtist;
    //下一首歌曲
    @BindView(R.id.lockscreen_next_song)
    TextView mNextSong;
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
    //是否正在运行
    private static boolean mIsRunning = false;
    //高斯模糊后的bitmap
    private Bitmap mNewBitMap;
    //高斯模糊之前的bitmap
    private Bitmap mRawBitMap;
    private int mWidth;
    private int mHeight;
    //歌曲名字体颜色
    @ColorInt
    private int mSongColor = Color.WHITE;
    //艺术家字体颜色
    @ColorInt
    private int mArtistColor = Color.WHITE;
    //是否正在播放
    private static boolean mIsPlay = false;
    private Disposable mDisposable;


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

        if((mInfo = MusicService.getCurrentMP3()) == null)
            return;

        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        mWidth = metric.widthPixels;
        mHeight = metric.heightPixels;

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

        findView(R.id.lockscreen_arrow_container).startAnimation(AnimationUtils.loadAnimation(this,R.anim.arrow_left_to_right));

    }

    //前后两次触摸的X
    private float mScrollX1;
    private float mScrollX2;
    //一次移动的距离
    private float mDistance;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mScrollX1 = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                mScrollX2 = event.getX();
                mDistance = mScrollX2 - mScrollX1;
                mScrollX1 = mScrollX2;
                //如果往右或者是往左没有超过最左边,移动View
                if(mDistance > 0 || ((mView.getScrollX() + (-mDistance)) < 0)) {
                    mView.scrollBy((int) -mDistance, 0);
                }
                LogUtil.d(TAG,"distance:" + mDistance + "\r\n");
                break;
            case MotionEvent.ACTION_UP:
                //判断当前位置是否超过整个屏幕宽度的0.25
                //超过则finish;没有则移动回初始状态
                if(-mView.getScrollX() > mWidth * 0.25)
                    finish();
                else
                    mView.scrollTo(0,0);
                mDistance = mScrollX1 = 0;
                break;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(0,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.cover_right_out);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(LockScreenActivity.class.getSimpleName());
        super.onResume();
        mIsRunning = true;
        UpdateUI(mInfo,mIsPlay);
    }

    public void onPause() {
        MobclickAgent.onPageEnd(LockScreenActivity.class.getSimpleName());
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mDisposable != null)
            mDisposable.dispose();
        if(mRawBitMap != null && !mRawBitMap.isRecycled())
            mRawBitMap.recycle();
        if(mNewBitMap != null && !mNewBitMap.isRecycled())
            mNewBitMap.recycle();
    }

    @Override
    public void UpdateUI(Song Song, boolean isplay) {
        mInfo = Song;
        mIsPlay = isplay;
        if(!mIsRunning){
//            ToastUtil.show(mContext,R.string.error);
            return;
        }
        if(mInfo == null){
//            ToastUtil.show(mContext,R.string.error);
            return;
        }

        //更新播放按钮
        if(mPlayButton != null){
            mPlayButton.setImageResource(MusicService.isPlay() ? R.drawable.lock_btn_pause : R.drawable.lock_btn_play);
        }
        //标题
        if(mSong != null) {
            mSong.setText(mInfo.getTitle());
        }
        //艺术家
        if(mArtist != null) {
            mArtist.setText(mInfo.getArtist());
        }
        //封面
        if(mSimpleImage != null) {
            new LibraryUriRequest(mSimpleImage,
                    getSearchRequestWithAlbumType(mInfo),
                    new RequestConfig.Builder(IMAGE_SIZE,IMAGE_SIZE).build()).load();
        }
        //下一首
        if(mNextSong != null && MusicService.getNextMP3() != null)
            mNextSong.setText(getString(R.string.next_song,MusicService.getNextMP3().getTitle()));

        if(mInfo == null)
            return;

        final int size = DensityUtil.dip2px(mContext,100);
        new ImageUriRequest<Palette.Swatch>(new RequestConfig.Builder(size,size).build()){
            @Override
            public void onError(String errMsg) {
//                ToastUtil.show(mContext,errMsg);
            }

            @Override
            public void onSuccess(Palette.Swatch result) {
                if(result == null)
                    return;
                mSongColor = result.getBodyTextColor();
                mArtistColor = result.getTitleTextColor();

                mImageBackground.setImageBitmap(mNewBitMap);
                mSong.setTextColor(mSongColor);
                mArtist.setTextColor(mArtistColor);
                mNextSong.setTextColor(mSongColor);
                mNextSong.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, Color.TRANSPARENT,0,2,mSongColor,0,0,1));
            }

            @Override
            public void load() {
                getThumbBitmapObservable(ImageUriUtil.getSearchRequestWithAlbumType(mInfo))
                        .compose(RxUtil.applySchedulerToIO())
                        .flatMap(bitmap -> Observable.create((ObservableOnSubscribe<Palette.Swatch>) e -> {
                            mRawBitMap = bitmap;
                            if(mRawBitMap == null)
                                mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.album_empty_bg_night);
                            processBitmap(e);
                        }))
                        .onErrorResumeNext(Observable.create(e -> {
                            mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.album_empty_bg_night);
                            processBitmap(e);
                        }))
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(disposable -> mDisposable = disposable)
                        .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
            }

            private void processBitmap(ObservableEmitter<Palette.Swatch> e) {
                StackBlurManager stackBlurManager = new StackBlurManager(mRawBitMap);
                mNewBitMap = stackBlurManager.processNatively(40);
                Palette palette = Palette.from(mRawBitMap).generate();
                Palette.Swatch swatch = palette.getMutedSwatch();//柔和 暗色
                if(swatch == null)
                    swatch = new Palette.Swatch(Color.GRAY,100);
                e.onNext(swatch);
                e.onComplete();
            }
        }.load();

    }

}
