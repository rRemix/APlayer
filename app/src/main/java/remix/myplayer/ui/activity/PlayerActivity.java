package remix.myplayer.ui.activity;

import static remix.myplayer.misc.ExtKt.isPortraitOrientation;
import static remix.myplayer.service.MusicService.EXTRA_CONTROL;
import static remix.myplayer.theme.ThemeStore.getAccentColor;
import static remix.myplayer.theme.ThemeStore.getPlayerNextSongBgColor;
import static remix.myplayer.theme.ThemeStore.getPlayerProgressColor;
import static remix.myplayer.theme.ThemeStore.isLightTheme;
import static remix.myplayer.util.Constants.PLAY_LOOP;
import static remix.myplayer.util.Constants.PLAY_REPEAT;
import static remix.myplayer.util.Constants.PLAY_SHUFFLE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.menu.AudioPopupListener;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.GradientDrawableMaker;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.ui.adapter.PagerAdapter;
import remix.myplayer.ui.dialog.FileChooserDialog;
import remix.myplayer.ui.dialog.PlayQueueDialog;
import remix.myplayer.ui.fragment.CoverFragment;
import remix.myplayer.ui.fragment.LyricFragment;
import remix.myplayer.ui.fragment.RecordFragment;
import remix.myplayer.ui.widget.AudioViewPager;
import remix.myplayer.ui.widget.playpause.PlayPauseView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class PlayerActivity extends BaseMusicActivity implements FileChooserDialog.FileCallback {

  private static final String TAG = "PlayerActivity";
//  public static final String EXTRA_SHOW_ANIMATION = "ShowAnimation";
//  public static final String EXTRA_FROM_NOTIFY = "FromNotify";
//  public static final String EXTRA_FROM_ACTIVITY = "FromActivity";
//  public static final String EXTRA_ANIM_URL = "AnimUrl";
//  public static final String EXTRA_RECT = "Rect";

  //上次选中的Fragment
  private int mPrevPosition = 0;
  //第一次启动的标志变量
  private boolean mFirstStart = true;
  //是否正在拖动进度条
  public boolean mIsDragSeekBarFromUser = false;

  //顶部信息
  @BindView(R.id.top_title)
  TextView mTopTitle;
  @BindView(R.id.top_detail)
  TextView mTopDetail;
  //隐藏按钮
  @BindView(R.id.top_hide)
  ImageButton mTopHide;
  //选项按钮
  @BindView(R.id.top_more)
  ImageButton mTopMore;
  //播放控制
  @BindView(R.id.playbar_play_pause)
  PlayPauseView mPlayPauseView;
  @BindView(R.id.playbar_prev)
  ImageButton mPlayBarPrev;
  @BindView(R.id.playbar_next)
  ImageButton mPlayBarNext;
  @BindView(R.id.playbar_model)
  ImageButton mPlayModel;
  @BindView(R.id.playbar_playinglist)
  ImageButton mPlayQueue;
  //已播放时间和剩余播放时间
  @BindView(R.id.text_hasplay)
  TextView mHasPlay;
  @BindView(R.id.text_remain)
  TextView mRemainPlay;
  //进度条
  @BindView(R.id.seekbar)
  SeekBar mProgressSeekBar;
  //背景
  @BindView(R.id.audio_holder_container)
  ViewGroup mContainer;
  @BindView(R.id.holder_pager)
  AudioViewPager mPager;
  //下一首歌曲
  @BindView(R.id.next_song)
  TextView mNextSong;
  @BindView(R.id.volume_down)
  ImageButton mVolumeDown;
  @BindView(R.id.volume_up)
  ImageButton mVolumeUp;
  @BindView(R.id.volume_seekbar)
  SeekBar mVolumeSeekbar;
  @BindView(R.id.volume_container)
  View mVolumeContainer;
  //歌词控件
  private LrcView mLrcView;
  //高亮与非高亮指示器
  private Drawable mHighLightIndicator;
  private Drawable mNormalIndicator;
  private ArrayList<ImageView> mIndicators;

  //当前播放的歌曲
  private Song mInfo;
  //当前是否播放
  private boolean mIsPlay;
  //当前播放时间
  private int mCurrentTime;
  //当前歌曲总时长
  private int mDuration;

  //需要高斯模糊的高度与宽度
  public int mWidth;
  public int mHeight;

  //Fragment
  private LyricFragment mLyricFragment;
  private CoverFragment mCoverFragment;

  /**
   * 下拉关闭
   */
  private float mEventY1;
  private float mEventY2;
  private float mEventX1;
  private float mEventX2;

  /**
   * 更新Handler
   */
  private MsgHandler mHandler;

  /**
   * 更新封面与背景的Handler
   */
  private Uri mUri;
  private static final int UPDATE_BG = 1;
  private static final int UPDATE_TIME_ONLY = 2;
  private static final int UPDATE_TIME_ALL = 3;
  private Bitmap mRawBitMap;
  private Palette.Swatch mSwatch;
  private AudioManager mAudioManager;

  //底部显示控制
  private int mBottomConfig;
  public static final int BOTTOM_SHOW_NEXT = 0;
  public static final int BOTTOM_SHOW_VOLUME = 1;
  public static final int BOTTOM_SHOW_BOTH = 2;
  public static final int BOTTOM_SHOW_NONE = 3;

  private static final int FRAGMENT_COUNT = 2;

  private static final int DELAY_SHOW_NEXT_SONG = 3000;
  private Runnable mVolumeRunnable = () -> {
    mNextSong.startAnimation(makeAnimation(mNextSong, true));
    mVolumeContainer.startAnimation(makeAnimation(mVolumeContainer, false));
  };


  @Override
  protected void setUpTheme() {
//    if (ThemeStore.isLightTheme()) {
//      super.setUpTheme();
//    } else {
//      setTheme(R.style.AudioHolderStyle_Night);
//    }
    final int superThemeRes = ThemeStore.getThemeRes();
    int themeRes;
    switch (superThemeRes) {
      case R.style.Theme_APlayer:
        themeRes = R.style.PlayerActivityStyle;
        break;
      case R.style.Theme_APlayer_Black:
        themeRes = R.style.PlayerActivityStyle_Black;
        break;
      case R.style.Theme_APlayer_Dark:
        themeRes = R.style.PlayerActivityStyle_Dark;
        break;
      default:
        themeRes = R.style.PlayerActivityStyle;
    }
    setTheme(themeRes);
  }

  @Override
  protected void setNavigationBarColor() {
    super.setNavigationBarColor();
    //导航栏变色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && SPUtil
        .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.COLOR_NAVIGATION, false)) {
      final int navigationColor = ThemeStore.getBackgroundColorMain(this);
      getWindow().setNavigationBarColor(navigationColor);
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor));
    }
  }

  @Override
  protected void setStatusBarMode() {
    StatusBarUtil.setStatusBarMode(this, ThemeStore.getBackgroundColorMain(this));
  }

  @Override
  protected void setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucent(this, ThemeStore.getBackgroundColorMain(this));
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);
    ButterKnife.bind(this);

    mHandler = new MsgHandler(this);

//    mShowAnimation = getIntent().getBooleanExtra(EXTRA_SHOW_ANIMATION,false);
    mInfo = MusicServiceRemote.getCurrentSong();
    //动画图片信息
//    AnimationUrl animUrl = getIntent().getParcelableExtra(EXTRA_ANIM_URL);
    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    setUpBottom();
    setUpSize();
    setUpTop();
    setUpFragments();
    setUpIndicator();
    setUpSeekBar();
    setUpViewColor();

//    //设置失败加载的图片和缩放类型
//    mAnimationCover = new SimpleDraweeView(this);
//    mAnimationCover.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
//    mAnimationCover.getHierarchy().setFailureImage(
//        isLightTheme() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night,
//        ScalingUtils.ScaleType.CENTER_CROP);
//    mContainer.addView(mAnimationCover);
//
//    //设置封面
//    if (mInfo != null) {
//      if (animUrl != null && !TextUtils.isEmpty(animUrl.getUrl())
//          && animUrl.getAlbumId() == mInfo.getAlbumId()) {
//        mAnimationCover.setImageURI(animUrl.getUrl());
//      } else {
//        new LibraryUriRequest(mAnimationCover,
//            getSearchRequestWithAlbumType(mInfo),
//            new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
//      }
//    }

  }

//  /**
//   * 计算图片缩放比例，以及位移距离
//   */
//  private void getMoveInfo(Rect destRect) {
//    // 计算图片缩放比例，并存储在 bundle 中
//    mScaleBundle.putFloat(SCALE_WIDTH, (float) destRect.width() / mOriginWidth);
//    mScaleBundle.putFloat(SCALE_HEIGHT, (float) destRect.height() / mOriginHeight);
//
//    // 计算位移距离，并将数据存储到 bundle 中
//    mTransitionBundle.putFloat(TRANSITION_X, destRect.left - mOriginRect.left);
//    mTransitionBundle.putFloat(TRANSITION_Y, destRect.top - mOriginRect.top);
//  }


  @Override
  public void onResume() {
    super.onResume();
    if (isPortraitOrientation(this)) {
      mPager.setCurrentItem(0);
    }
    //更新进度条
    new ProgressThread().start();
  }

  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
    onMetaChanged();
    onPlayStateChange();
  }

//  @Override
//  protected void onSaveInstanceState(Bundle outState) {
//    super.onSaveInstanceState(outState);
//    outState.putParcelable(EXTRA_RECT, mOriginRect);
//    //activity重启后就不用动画了
//    outState.putBoolean(EXTRA_SHOW_ANIMATION, false);
//  }
//
//  @Override
//  protected void onRestoreInstanceState(Bundle savedInstanceState) {
//    super.onRestoreInstanceState(savedInstanceState);
//    if (savedInstanceState != null) {
//      if (mOriginRect == null && savedInstanceState.getParcelable(EXTRA_RECT) != null) {
//        mOriginRect = savedInstanceState.getParcelable(EXTRA_RECT);
//      }
//      mShowAnimation = savedInstanceState.getBoolean(EXTRA_SHOW_ANIMATION,false);
//    }
//  }

  @Override
  protected void onStart() {
    super.onStart();
    //只有从Activity启动，才使用动画
//    if (mShowAnimation) {
//      overridePendingTransition(0, 0);
//    }
    overridePendingTransition(R.anim.audio_in, 0);
  }


  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, R.anim.audio_out);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
//    finish();
//    overridePendingTransition(0, R.anim.audio_out);
//    //从通知栏打开或者横屏直接退出
//    if (!mShowAnimation) {
//      finish();
//      overridePendingTransition(0, R.anim.audio_out);
//      return;
//    }
//    if (mPager.getCurrentItem() == 1) {
//      if (mIsBacking || mAnimationCover == null) {
//        return;
//      }
//      mIsBacking = true;
//
//      //更新动画控件封面 保证退场动画的封面与fragment中封面一致
//      ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder
//          .newBuilderWithSource(mUri == null ? Uri.EMPTY : mUri);
//      DraweeController controller = Fresco.newDraweeControllerBuilder()
//          .setImageRequest(imageRequestBuilder.build())
//          .setOldController(mAnimationCover.getController())
////                    .setControllerListener(new ControllerListener<ImageInfo>() {
////                        @Override
////                        public void onSubmit(String id, Object callerContext) {
////
////                        }
////
////                        @Override
////                        public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
////                            playBackAnimation();
////                        }
////
////                        @Override
////                        public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
////
////                        }
////
////                        @Override
////                        public void onIntermediateImageFailed(String id, Throwable throwable) {
////
////                        }
////
////                        @Override
////                        public void onFailure(String id, Throwable throwable) {
////                            playBackAnimation();
////                        }
////
////                        @Override
////                        public void onRelease(String id) {
////
////                        }
////                    })
//          .build();
//      mAnimationCover.setController(controller);
//      mAnimationCover.setVisibility(View.VISIBLE);
//      playBackAnimation();
//    } else {
//      finish();
//      overridePendingTransition(0, R.anim.audio_out);
//    }

  }

//  private void playBackAnimation() {
//    final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
//    final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
//    final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
//    final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;
//    Spring coverSpring = SpringSystem.create().createSpring();
//    coverSpring.setSpringConfig(COVER_OUT_SPRING_CONFIG);
//    coverSpring.addListener(new SimpleSpringListener() {
//      @Override
//      public void onSpringUpdate(Spring spring) {
//        if (mAnimationCover == null) {
//          return;
//        }
//        final double currentVal = spring.getCurrentValue();
//        mAnimationCover.setTranslationX((float) (transitionX * currentVal));
//        mAnimationCover.setTranslationY((float) (transitionY * currentVal));
//        mAnimationCover.setScaleX((float) (1 + scaleX * currentVal));
//        mAnimationCover.setScaleY((float) (1 + scaleY * currentVal));
//      }
//
//      @Override
//      public void onSpringActivate(Spring spring) {
//        //隐藏fragment中的image
//        mCoverFragment.hideImage();
//      }
//
//      @Override
//      public void onSpringAtRest(Spring spring) {
//        finish();
//        overridePendingTransition(0, 0);
//      }
//    });
//    coverSpring.setOvershootClampingEnabled(true);
//    coverSpring.setCurrentValue(1);
//    coverSpring.setEndValue(0);
//  }

  /**
   * 上一首 下一首 播放、暂停
   */
  @OnClick({R.id.playbar_next, R.id.playbar_prev, R.id.playbar_play_container})
  public void onCtrlClick(View v) {
    Intent intent = new Intent(MusicService.ACTION_CMD);
    switch (v.getId()) {
      case R.id.playbar_prev:
        intent.putExtra(EXTRA_CONTROL, Command.PREV);
        break;
      case R.id.playbar_next:
        intent.putExtra(EXTRA_CONTROL, Command.NEXT);
        break;
      case R.id.playbar_play_container:
        intent.putExtra(EXTRA_CONTROL, Command.TOGGLE);
        break;
    }
    sendLocalBroadcast(intent);
  }

  /**
   * 播放模式 播放列表 关闭 隐藏
   */
  @OnClick({R.id.playbar_model, R.id.playbar_playinglist, R.id.top_hide, R.id.top_more})
  public void onOtherClick(View v) {
    switch (v.getId()) {
      //设置播放模式
      case R.id.playbar_model:
        int currentModel = MusicServiceRemote.getPlayModel();
        currentModel = (currentModel == PLAY_REPEAT ? PLAY_LOOP : ++currentModel);
        MusicServiceRemote.setPlayModel(currentModel);
        mPlayModel.setImageDrawable(Theme.tintDrawable(currentModel == PLAY_LOOP ? R.drawable.play_btn_loop :
                currentModel == PLAY_SHUFFLE ? R.drawable.play_btn_shuffle : R.drawable.play_btn_loop_one,
            ThemeStore.getPlayerBtnColor()));

        String msg = currentModel == PLAY_LOOP ? getString(R.string.model_normal)
            : currentModel == PLAY_SHUFFLE ? getString(R.string.model_random)
                : getString(R.string.model_repeat);
        //刷新下一首
        if (currentModel != PLAY_SHUFFLE) {
          mNextSong
              .setText(getString(R.string.next_song, MusicServiceRemote.getNextSong().getTitle()));
        }
        ToastUtil.show(this, msg);
        break;
      //打开正在播放列表
      case R.id.playbar_playinglist:
        PlayQueueDialog.newInstance()
            .show(getSupportFragmentManager(), PlayQueueDialog.class.getSimpleName());
        break;
      //关闭
      case R.id.top_hide:
        onBackPressed();
        break;
      //弹出窗口
      case R.id.top_more:
        @SuppressLint("RestrictedApi") final PopupMenu popupMenu = new PopupMenu(mContext, v, Gravity.TOP);
        popupMenu.getMenuInflater().inflate(R.menu.menu_audio_item, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new AudioPopupListener(this, mInfo));
        popupMenu.show();
        break;
    }
  }

  @SuppressLint("CheckResult")
  @OnClick({R.id.volume_down, R.id.volume_up, R.id.next_song})
  void onVolumeClick(View view) {
    switch (view.getId()) {
      case R.id.volume_down:
        Completable
            .fromAction(() -> {
              if (mAudioManager != null) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_PLAY_SOUND);
              }
            })
            .subscribeOn(Schedulers.io())
            .subscribe();

        break;
      case R.id.volume_up:
        Completable
            .fromAction(() -> {
              if (mAudioManager != null) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_PLAY_SOUND);
              }
            })
            .subscribeOn(Schedulers.io())
            .subscribe();

        break;
      case R.id.next_song:
//                mLyric.setVisibility(View.GONE);
//                mVolumeContainer.setVisibility(View.VISIBLE);
        if (mBottomConfig == BOTTOM_SHOW_BOTH) {
          mNextSong.startAnimation(makeAnimation(mNextSong, false));
          mVolumeContainer.startAnimation(makeAnimation(mVolumeContainer, true));
          mHandler.removeCallbacks(mVolumeRunnable);
          mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
        }
        break;
    }
    if (view.getId() != R.id.next_song) {
      Single.zip(Single.fromCallable(() -> mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),
          Single.fromCallable(() -> mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)),
          (max, current) -> new long[]{max, current})
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(longs -> mVolumeSeekbar.setProgress((int) (longs[1] * 1.0 / longs[1] * 100)));
    }
  }

  private AlphaAnimation makeAnimation(View view, boolean show) {
    AlphaAnimation alphaAnimation = new AlphaAnimation(show ? 0 : 1, show ? 1 : 0);
    alphaAnimation.setDuration(300);
    alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {
        view.setVisibility(View.VISIBLE);
      }

      @Override
      public void onAnimationEnd(Animation animation) {
        view.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {

      }
    });
    return alphaAnimation;
  }

  /**
   * 获得屏幕大小
   */
  private void setUpSize() {
    WindowManager wm = getWindowManager();
    Display display = wm.getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    mWidth = metrics.widthPixels;
    mHeight = metrics.heightPixels;
  }

  /**
   * 初始化三个dot
   */
  private void setUpIndicator() {
    int width = DensityUtil.dip2px(this, 8);
    int height = DensityUtil.dip2px(this, 2);

    mHighLightIndicator = new GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(getAccentColor())
        .make();

    mNormalIndicator = new GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(getAccentColor())
        .alpha(0.3f)
        .make();

    mIndicators = new ArrayList<>();
    mIndicators.add(findViewById(R.id.guide_01));
    mIndicators.add(findViewById(R.id.guide_02));
    mIndicators.get(0).setImageDrawable(mHighLightIndicator);
    mIndicators.get(1).setImageDrawable(mNormalIndicator);
  }

  /**
   * 初始化seekbar
   */
  @SuppressLint("CheckResult")
  private void setUpSeekBar() {
    if (mInfo == null) {
      return;
    }
    //初始化已播放时间与剩余时间
    mDuration = (int) mInfo.getDuration();
    final int temp = MusicServiceRemote.getProgress();
    mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;

    if (mDuration > 0 && mDuration - mCurrentTime > 0) {
      mHasPlay.setText(Util.getTime(mCurrentTime));
      mRemainPlay.setText(Util.getTime(mDuration - mCurrentTime));
    }

    //初始化seekbar
    if (mDuration > 0 && mDuration < Integer.MAX_VALUE) {
      mProgressSeekBar.setMax(mDuration);
    } else {
      mProgressSeekBar.setMax(1000);
    }

    if (mCurrentTime > 0 && mCurrentTime < mDuration) {
      mProgressSeekBar.setProgress(mCurrentTime);
    } else {
      mProgressSeekBar.setProgress(0);
    }

    mProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          updateProgressText(progress);
        }
        mHandler.sendEmptyMessage(UPDATE_TIME_ONLY);
        mCurrentTime = progress;
        if (mLrcView != null) {
          mLrcView.seekTo(progress, true, fromUser);
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        mIsDragSeekBarFromUser = true;
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        //没有播放拖动进度条无效
//                if(!mIsPlay){
//                    seekBar.setProgress(0);
//                }
        MusicServiceRemote.setProgress(seekBar.getProgress());
        mIsDragSeekBarFromUser = false;
      }
    });

    //音量的Seekbar
    Single.zip(Single.fromCallable(() -> mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)),
        Single.fromCallable(() -> mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)),
        (max, current) -> new int[]{max, current})
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(ints -> {
          final int current = ints[1];
          final int max = ints[0];
          mVolumeSeekbar.setProgress((int) (current * 1.0 / max * 100));
          mVolumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
              if (mBottomConfig == BOTTOM_SHOW_BOTH) {
                mHandler.removeCallbacks(mVolumeRunnable);
                mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
              }
              if (fromUser) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    (int) (seekBar.getProgress() / 100f * max),
                    AudioManager.FLAG_PLAY_SOUND);
              }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
          });
        });

    if (mBottomConfig == BOTTOM_SHOW_BOTH) {
      mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
    }
  }

  /**
   * 更新顶部歌曲信息
   */
  public void updateTopStatus(Song song) {
    if (song == null) {
      return;
    }
    String title = song.getTitle() == null ? "" : song.getTitle();
    String artist = song.getArtist() == null ? "" : song.getArtist();
    String album = song.getAlbum() == null ? "" : song.getAlbum();

    if (title.equals("")) {
      mTopTitle.setText(getString(R.string.unknown_song));
    } else {
      mTopTitle.setText(title);
    }
    if (artist.equals("")) {
      mTopDetail.setText(song.getAlbum());
    } else if (album.equals("")) {
      mTopDetail.setText(song.getArtist());
    } else {
      mTopDetail.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
    }
  }

  /**
   * 更新播放、暂停按钮
   */
  public void updatePlayButton(final boolean isPlay) {
    mIsPlay = isPlay;
    mPlayPauseView.updateState(isPlay, true);
  }

  /**
   * 初始化顶部信息
   */
  private void setUpTop() {
    updateTopStatus(mInfo);
  }

  /**
   * 初始化viewpager
   */
  @SuppressLint("ClickableViewAccessibility")
  private void setUpFragments() {
    final FragmentManager fragmentManager = getSupportFragmentManager();

    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    fragmentManager.executePendingTransactions();
    final List<Fragment> fragments = fragmentManager.getFragments();
    if (fragments != null) {
      for (Fragment fragment : fragments) {
        if (fragment instanceof LyricFragment ||
            fragment instanceof CoverFragment ||
            fragment instanceof RecordFragment) {
          fragmentManager.beginTransaction().remove(fragment).commitNow();
        }
      }
    }

    mCoverFragment = new CoverFragment();
    setUpCoverFragment();
    mLyricFragment = new LyricFragment();
    setUpLyricFragment();

    if (isPortraitOrientation(this)) {
//      mRecordFragment = new RecordFragment();

      //Viewpager
      PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
//      adapter.addFragment(mRecordFragment);
      adapter.addFragment(mCoverFragment);
      adapter.addFragment(mLyricFragment);

      mPager.setAdapter(adapter);
      mPager.setOffscreenPageLimit(adapter.getCount() - 1);
      mPager.setCurrentItem(0);

      final int THRESHOLD_Y = DensityUtil.dip2px(mContext, 40);
      final int THRESHOLD_X = DensityUtil.dip2px(mContext, 60);
      //下滑关闭
      mPager.setOnTouchListener((v, event) -> {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
          mEventX1 = event.getX();
          mEventY1 = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
          mEventX2 = event.getX();
          mEventY2 = event.getY();
          if (mEventY2 - mEventY1 > THRESHOLD_Y && Math.abs(mEventX1 - mEventX2) < THRESHOLD_X) {
            onBackPressed();
          }
        }
        return false;
      });
      mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
          mIndicators.get(mPrevPosition).setImageDrawable(mNormalIndicator);
          mIndicators.get(position).setImageDrawable(mHighLightIndicator);
          mPrevPosition = position;
          //歌词界面常亮
          if (position == 1 && SPUtil
              .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
      });
    } else {
      //歌词界面常亮
      if (SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON,
          false)) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
      }
      mCoverFragment = new CoverFragment();
      setUpCoverFragment();
      mLyricFragment = new LyricFragment();
      setUpLyricFragment();
      fragmentManager
          .beginTransaction()
          .replace(R.id.container_cover, mCoverFragment)
          .replace(R.id.container_lyric, mLyricFragment)
          .commit();
    }

  }

  private void setUpLyricFragment() {
    mLyricFragment.setOnInflateFinishListener(view -> {
      mLrcView = (LrcView) view;
      mLrcView.setOnLrcClickListener(new LrcView.OnLrcClickListener() {
        @Override
        public void onClick() {
        }

        @Override
        public void onLongClick() {
        }
      });
      mLrcView.setOnSeekToListener(progress -> {
        if (progress > 0 && progress < MusicServiceRemote.getDuration()) {
          MusicServiceRemote.setProgress(progress);
          mCurrentTime = progress;
          mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
        }
      });
      mLrcView.setHighLightColor(ThemeStore.getTextColorPrimary());
      mLrcView.setOtherColor(ThemeStore.getTextColorSecondary());
      mLrcView.setTimeLineColor(ThemeStore.getTextColorSecondary());
    });
  }

  private void setUpCoverFragment() {
//    mCoverFragment.setOnFirstLoadFinishListener(() -> mAnimationCover.setVisibility(View.INVISIBLE));
//    mCoverFragment.setInflateFinishListener(view -> {
//      //不启动动画 直接显示
//      if (!mShowAnimation) {
//        mCoverFragment.showImage();
//        //隐藏动画用的封面并设置位置信息
//        mAnimationCover.setVisibility(View.GONE);
//        return;
//      }
//
//      if (mOriginRect == null || mOriginRect.width() <= 0 || mOriginRect.height() <= 0) {
//        //获取传入的界面信息
//        mOriginRect = getIntent().getParcelableExtra(EXTRA_RECT);
//      }
//
//      if (mOriginRect == null) {
//        return;
//      }
//      // 获取上一个界面中，图片的宽度和高度
//      mOriginWidth = mOriginRect.width();
//      mOriginHeight = mOriginRect.height();
//
//      // 设置 view 的位置，使其和上一个界面中图片的位置重合
//      FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mOriginWidth, mOriginHeight);
//      params.setMargins(mOriginRect.left,
//          mOriginRect.top - StatusBarUtil.getStatusBarHeight(mContext), mOriginRect.right,
//          mOriginRect.bottom);
//      mAnimationCover.setLayoutParams(params);
//
//      //获得终点控件的位置信息
//      view.getGlobalVisibleRect(mDestRect);
//      // 计算图片缩放比例和位移距离
//      getMoveInfo(mDestRect);
//
//      mAnimationCover.setPivotX(0);
//      mAnimationCover.setPivotY(0);
//
//      final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
//      final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
//      final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
//      final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;
//
//      final Spring spring = SpringSystem.create().createSpring();
//      spring.setSpringConfig(COVER_IN_SPRING_CONFIG);
//      spring.addListener(new SimpleSpringListener() {
//        @Override
//        public void onSpringUpdate(Spring spring) {
//          if (mAnimationCover == null) {
//            return;
//          }
//          final double currentVal = spring.getCurrentValue();
//          mAnimationCover.setTranslationX((float) (transitionX * currentVal));
//          mAnimationCover.setTranslationY((float) (transitionY * currentVal));
//          mAnimationCover.setScaleX((float) (1 + scaleX * currentVal));
//          mAnimationCover.setScaleY((float) (1 + scaleY * currentVal));
//        }
//
//        @Override
//        public void onSpringAtRest(Spring spring) {
//          //入场动画结束时显示fragment中的封面
//          mCoverFragment.showImage();
////                    mHandler.postDelayed(() -> {
////                        //隐藏动画用的封面
////                        mAnimationCover.setVisibility(View.INVISIBLE);
////                    },24);
//
//        }
//
//        @Override
//        public void onSpringActivate(Spring spring) {
//          overridePendingTransition(0, 0);
//        }
//      });
//      spring.setOvershootClampingEnabled(true);
//      spring.setCurrentValue(0);
//      spring.setEndValue(1);
//
//    });
  }

  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();

    final Song newSong = MusicServiceRemote.getCurrentSong();
    updateTopStatus(newSong);
    mLyricFragment.updateLrc(newSong);
    mInfo = newSong;
    requestCover(false);
  }

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    mInfo = MusicServiceRemote.getCurrentSong();
    //两种情况下更新ui
    //一是activity在前台  二是activity暂停后有更新的动作，当activity重新回到前台后更新ui
//        if (!mIsForeground) {
//            mNeedUpdateUI = true;
//            return;
//        }
    //当操作不为播放或者暂停且正在运行时，更新所有控件
    final int operation = MusicServiceRemote.getOperation();
    if ((operation != Command.TOGGLE || mFirstStart)) {
      //更新顶部信息
      updateTopStatus(mInfo);
      //更新歌词
      mHandler.postDelayed(() -> mLyricFragment.updateLrc(mInfo), 500);
      //更新进度条
      int temp = MusicServiceRemote.getProgress();
      mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
      mDuration = (int) mInfo.getDuration();
      mProgressSeekBar.setMax(mDuration);
      //更新下一首歌曲
      mNextSong.setText(getString(R.string.next_song, MusicServiceRemote.getNextSong().getTitle()));
      updateBg();
      requestCover(operation != Command.TOGGLE && !mFirstStart);
    }
  }

  @Override
  public void onPlayStateChange() {
    super.onPlayStateChange();
    //更新按钮状态
    final boolean isPlay = MusicServiceRemote.isPlaying();
    if (mIsPlay != isPlay) {
      updatePlayButton(isPlay);
    }
  }

  //更新进度条线程
  private class ProgressThread extends Thread {

    @Override
    public void run() {
      while (mIsForeground) {
        //音量
        if (mVolumeSeekbar.getVisibility() == View.VISIBLE) {
          final int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
          final int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
          runOnUiThread(() -> mVolumeSeekbar.setProgress((int) (current * 1.0 / max * 100)));
        }
        if (!MusicServiceRemote.isPlaying()) {
          continue;
        }
        int progress = MusicServiceRemote.getProgress();
        if (progress > 0 && progress < mDuration) {
          mCurrentTime = progress;
          mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
          try {
            //1000ms时间有点长
            sleep(500);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return super.onKeyDown(keyCode, event);
  }

  /**
   * 初始化底部区域
   */
  private void setUpBottom() {
    mBottomConfig = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, BOTTOM_SHOW_BOTH);
    if (!isPortraitOrientation(this)) {//横屏不显示底部
      mBottomConfig = BOTTOM_SHOW_NONE;
    }
    if (mBottomConfig == BOTTOM_SHOW_NEXT) {//仅显示下一首
      mVolumeContainer.setVisibility(View.GONE);
      mNextSong.setVisibility(View.VISIBLE);
    } else if (mBottomConfig == BOTTOM_SHOW_VOLUME) {//仅显示音量控制
      mVolumeContainer.setVisibility(View.VISIBLE);
      mNextSong.setVisibility(View.GONE);
    } else if (mBottomConfig == BOTTOM_SHOW_NONE) {//关闭
      View volumeLayout = findViewById(R.id.layout_player_volume);
      volumeLayout.setVisibility(View.INVISIBLE);
      LinearLayout.LayoutParams volumelLp = (LinearLayout.LayoutParams) volumeLayout
          .getLayoutParams();
      volumelLp.weight = 0;
      volumeLayout.setLayoutParams(volumelLp);

      View controlLayout = findViewById(R.id.layout_player_control);
      LinearLayout.LayoutParams controlLp = new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, 0);
      controlLp.weight = 2f;
      controlLayout.setLayoutParams(controlLp);
    }
  }

  /**
   * 根据主题颜色修改按钮颜色
   */
  private void setUpViewColor() {
    int accentColor = getAccentColor();
    int tintColor = ThemeStore.getPlayerBtnColor();

    setProgressDrawable(mProgressSeekBar, accentColor);
    setProgressDrawable(mVolumeSeekbar, accentColor);
    //修改thumb
    int inset = DensityUtil.dip2px(mContext, 6);

    final int width = DensityUtil.dip2px(this, 2);
    final int height = DensityUtil.dip2px(this, 6);

    new GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(accentColor)
        .make();

    mProgressSeekBar.setThumb(new InsetDrawable(
        new GradientDrawableMaker()
            .width(width)
            .height(height)
            .color(accentColor)
            .make(),
        inset, inset, inset, inset));
    mVolumeSeekbar.setThumb(new InsetDrawable(new GradientDrawableMaker()
        .width(width)
        .height(height)
        .color(accentColor)
        .make(),
        inset, inset, inset, inset));
//        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(context,10),DensityUtil.dip2px(context,10)));
//        Drawable seekbarBackground = mProgressSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

    //修改控制按钮颜色
    Theme.tintDrawable(mPlayBarNext, R.drawable.play_btn_next, accentColor);
    Theme.tintDrawable(mPlayBarPrev, R.drawable.play_btn_pre, accentColor);

    //歌曲名颜色
    mTopTitle.setTextColor(ThemeStore.getPlayerTitleColor());

    //修改顶部按钮颜色
    Theme.tintDrawable(mTopHide, R.drawable.icon_player_back, tintColor);
    Theme.tintDrawable(mTopMore, R.drawable.icon_player_more, tintColor);
    //播放模式与播放队列
    int playMode = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
        PLAY_LOOP);
    Theme.tintDrawable(mPlayModel, playMode == PLAY_LOOP ? R.drawable.play_btn_loop :
        playMode == PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
            R.drawable.play_btn_loop_one, tintColor);
    Theme.tintDrawable(mPlayQueue, R.drawable.play_btn_normal_list, tintColor);

    //音量控制
    mVolumeDown.getDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
    mVolumeUp.getDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
//        Theme.tintDrawable(mVolumeDown,R.drawable.ic_volume_down_black_24dp,tintColor);
//        Theme.tintDrawable(mVolumeUp,R.drawable.ic_volume_up_black_24dp,tintColor);

    mPlayPauseView.setBackgroundColor(accentColor);
    //下一首背景
    mNextSong.setBackground(new GradientDrawableMaker()
        .color(getPlayerNextSongBgColor())
        .corner(DensityUtil.dip2px(2))
        .width(DensityUtil.dip2px(288))
        .height(DensityUtil.dip2px(38))
        .make());
    mNextSong.setTextColor(ThemeStore.getPlayerNextSongTextColor());

  }

  private void setProgressDrawable(SeekBar seekBar, int accentColor) {
    LayerDrawable progressDrawable = (LayerDrawable) seekBar.getProgressDrawable();
    //修改progress颜色
    ((GradientDrawable) progressDrawable.getDrawable(0)).setColor(getPlayerProgressColor());
    (progressDrawable.getDrawable(1)).setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
    seekBar.setProgressDrawable(progressDrawable);
  }

  //更新背景
  private void updateBg() {
//        if(!mDiscolour)
//            return;
//        Observable.create((ObservableOnSubscribe<Palette.Swatch>) e -> {
//            mRawBitMap = MediaStoreUtil.getAlbumBitmap(mInfo.getAlbumId(),false);
//            if(mRawBitMap == null)
//                mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.album_empty_bg_night);
//            Palette.from(mRawBitMap).generate(palette -> {
//                if(palette == null || palette.getMutedSwatch() == null){
//                    mSwatch = new Palette.Swatch(Color.GRAY,100);
//                } else {
//                    mSwatch = palette.getMutedSwatch();//柔和 暗色
//                }
//                e.onNext(mSwatch);
//            });
//        })
//        .compose(RxUtil.applyScheduler())
//        .subscribe(swatch -> {
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        });

  }

  private void updateCover(boolean withAnimation) {
    mCoverFragment.updateCover(mInfo, mUri, withAnimation);
    mFirstStart = false;
  }

  /**
   * 更新封面
   */
  private void requestCover(boolean withAnimation) {
    //更新封面
    if (mInfo == null) {
      mUri = Uri.parse("res://" + mContext.getPackageName() + "/" + (isLightTheme()
          ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
      updateCover(withAnimation);
    } else {
      new ImageUriRequest<String>() {
        @Override
        public void onError(Throwable throwable) {
          mUri = Uri.EMPTY;
          updateCover(withAnimation);
        }

        @Override
        public void onSuccess(String result) {
          mUri = Uri.parse(result);
          updateCover(withAnimation);
        }

        @Override
        public Disposable load() {
          return getCoverObservable(getSearchRequestWithAlbumType(mInfo))
              .compose(RxUtil.applyScheduler())
              .subscribe(this::onSuccess, this::onError);
        }
      }.load();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    mHandler.remove();
  }

  /**
   * 选择歌词文件
   */
  @Override
  public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
//        //如果之前忽略过该歌曲的歌词，取消忽略
//        Set<String> ignoreLrcId = new HashSet<>(SPUtil.getStringSet(this,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID"));
//        if(ignoreLrcId.size() > 0){
//            for (String id : ignoreLrcId){
//                if((mInfo.getID() + "").equals(id)){
//                    ignoreLrcId.remove(mInfo.getID() + "");
//                    SPUtil.putStringSet(context,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID",ignoreLrcId);
//                }
//            }
//        }
    SPUtil.putValue(mContext, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "",
        SPUtil.LYRIC_KEY.LYRIC_MANUAL);
    mLyricFragment.updateLrc(file.getAbsolutePath());

    sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.CHANGE_LYRIC));
  }

  @Override
  public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {
  }

  private void updateProgressText(int current) {
    if (mHasPlay != null
        && mRemainPlay != null
        && current > 0
        && (mDuration - current) > 0) {
      mHasPlay.setText(Util.getTime(current));
      mRemainPlay.setText(Util.getTime(mDuration - current));
    }
  }

  private void updateProgressByHandler() {
    updateProgressText(mCurrentTime);
  }

  private void updateSeekbarByHandler() {
    mProgressSeekBar.setProgress(mCurrentTime);
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
//        if(msg.what == UPDATE_BG){
//            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
//            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
//        }
    if (msg.what == UPDATE_TIME_ONLY && !mIsDragSeekBarFromUser) {
      updateProgressByHandler();
    }
    if (msg.what == UPDATE_TIME_ALL && !mIsDragSeekBarFromUser) {
      updateProgressByHandler();
      updateSeekbarByHandler();
    }
  }

  public LyricFragment getLyricFragment() {
    return mLyricFragment;
  }

  public void showLyricOffsetView() {
    //todo∂
    if (mPager.getCurrentItem() != 2) {
      mPager.setCurrentItem(2, true);
    }
    if (getLyricFragment() != null) {
      getLyricFragment().showLyricOffsetView();
    }
  }
}
