package remix.myplayer.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Rect;
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
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.bean.misc.AnimationUrl;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnTagEditListener;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.menu.AudioPopupListener;
import remix.myplayer.misc.tageditor.TagReceiver;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.UriRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
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
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class PlayerActivity extends BaseMusicActivity implements UpdateHelper.Callback,
        FileChooserDialog.FileCallback, OnTagEditListener {
    private static final String TAG = "PlayerActivity";
    //上次选中的Fragment
    private int mPrevPosition = 1;
    //第一次启动的标志变量
    private boolean mFistStart = true;
    //是否正在拖动进度条
    public boolean mIsDragSeekBarFromUser = false;

    //入场动画封面
    SimpleDraweeView mAnimCover;
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
    //Viewpager
    private PagerAdapter mAdapter;
    private ArrayList<ImageView> mDotList;

    //当前播放的歌曲
    private Song mInfo;
    //当前是否播放
    private boolean mIsPlay;
    //当前播放时间
    private int mCurrentTime;
    //当前歌曲总时长
    private int mDuration;

    //需要高斯模糊的高度与宽度
    public static int mWidth;
    public static int mHeight;
    //是否从通知栏启动
    private boolean mFromNotify = false;
    //是否从Activity启动
    private boolean mFromActivity = false;
    //是否需要更新
    private boolean mNeedUpdateUI = true;

    //动画图片信息
    private AnimationUrl mAnimUrl;

    //Fragment
    private LyricFragment mLyricFragment;
    private CoverFragment mCoverFragment;
    private RecordFragment mRecordFragment;

    private static final String SCALE_WIDTH = "SCALE_WIDTH";
    private static final String SCALE_HEIGHT = "SCALE_HEIGHT";
    private static final String TRANSITION_X = "TRANSITION_X";
    private static final String TRANSITION_Y = "TRANSITION_Y";
    /**
     * 存储图片缩放比例和位移距离
     */
    private static Bundle mScaleBundle = new Bundle();
    private static Bundle mTransitionBundle = new Bundle();
    /**
     * 上一个界面图片的宽度和高度
     */
    private static int mOriginWidth;
    private static int mOriginHeight;
    /**
     * 上一个界面图片的位置信息
     */
    private static Rect mOriginRect;
    /**
     * 终点View的位置信息
     */
    private static Rect mDestRect = new Rect();
    /**
     * 动画参数
     */
    private static final SpringConfig COVER_IN_SPRING_CONFIG = SpringConfig.fromOrigamiTensionAndFriction(30, 7);
    private static final SpringConfig COVER_OUT_SPRING_CONFIG = SpringConfig.fromOrigamiTensionAndFriction(35, 7);

    /**
     * 下拉关闭
     */
    private boolean mIsBacking = false;
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
    private static final int UPDATE_BG = 101;
    private static final int UPDATE_TIME_ONLY = 102;
    private static final int UPDATE_TIME_ALL = 103;
    private Bitmap mRawBitMap;

    private Palette.Swatch mSwatch;
    private AudioManager mAudioManager;

    //底部显示控制
    private int mBottomConfig;

    private static final int DELAY_SHOW_NEXT_SONG = 3000;
    private Runnable mVolumeRunnable = () -> {
        mNextSong.startAnimation(makeAnimation(mNextSong, true));
        mVolumeContainer.startAnimation(makeAnimation(mVolumeContainer, false));
    };

    private TagReceiver mTagReceiver;

    @Override
    protected void setUpTheme() {
        if (ThemeStore.isDay())
            super.setUpTheme();
        else {
            setTheme(R.style.AudioHolderStyle_Night);
        }
    }

    @Override
    protected void setStatusBar() {
        if (ThemeStore.isDay()) {
            //获得miui版本
            String miui = "";
            int miuiVersion = 0;
            if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi")) {
                try {
                    Class<?> c = Class.forName("android.os.SystemProperties");
                    Method get = c.getMethod("get", String.class, String.class);
                    miui = (String) (get.invoke(c, "ro.miui.ui.version.name", "unknown"));
                    if (!TextUtils.isEmpty(miui) && miui.length() >= 2 && TextUtils.isDigitsOnly(miui.substring(1, miui.length()))) {
                        miuiVersion = Integer.valueOf(miui.substring(1, miui.length()));
                    }
                } catch (Exception e) {
                    LogUtil.d(TAG, e.toString());
                }
            }
            if (Build.MANUFACTURER.equalsIgnoreCase("Meizu")) {
                StatusBarUtil.setTransparent(this);
                StatusBarUtil.MeizuStatusbar.setStatusBarDarkIcon(this, true);
            } else if (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi") && miuiVersion >= 6 && miuiVersion < 9) {
                StatusBarUtil.setTransparent(this);
                StatusBarUtil.XiaomiStatusbar.setStatusBarDarkMode(true, this);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                StatusBarUtil.setTransparent(this);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                StatusBarUtil.setColorNoTranslucent(this, ColorUtil.getColor(R.color.statusbar_gray_color));
            }
        } else {
            StatusBarUtil.setTransparent(this);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFromNotify = getIntent().getBooleanExtra("Notify", false);
        mFromActivity = getIntent().getBooleanExtra("FromActivity", false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);

        mHandler = new MsgHandler(this);
        mTagReceiver = new TagReceiver(this);
        registerReceiver(mTagReceiver, new IntentFilter(Constants.TAG_EDIT));
//        mHandler.postDelayed(() -> sendBroadcast(new Intent(Constants.TAG_EDIT)
//                .putExtra("newSong",new Song())),2000);

        mInfo = MusicServiceRemote.getCurrentSong();
        mAnimUrl = getIntent().getParcelableExtra("AnimUrl");
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        setUpBottom();
        setUpSize();
        setUpTop();
        setUpGuide();
        setUpViewPager();
        setUpSeekBar();
        setUpViewColor();

        //设置失败加载的图片和缩放类型
        mAnimCover = new SimpleDraweeView(this);
        mAnimCover.getHierarchy().setActualImageScaleType(ScalingUtils.ScaleType.CENTER_CROP);
        mAnimCover.getHierarchy().setFailureImage(ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night, ScalingUtils.ScaleType.CENTER_CROP);
        mContainer.addView(mAnimCover);

        //设置封面
        if (mInfo != null) {
            if (mAnimUrl != null && !TextUtils.isEmpty(mAnimUrl.getUrl()) && mAnimUrl.getAlbumId() == mInfo.getAlbumId()) {
                mAnimCover.setImageURI(mAnimUrl.getUrl());
            } else {
                new LibraryUriRequest(mAnimCover,
                        getSearchRequestWithAlbumType(mInfo),
                        new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();
            }
        }

        //恢复位置信息
        if (savedInstanceState != null && savedInstanceState.getParcelable("Rect") != null) {
            mOriginRect = savedInstanceState.getParcelable("Rect");
        }
    }


    /**
     * 计算图片缩放比例，以及位移距离
     */
    private void getMoveInfo(Rect destRect) {
        // 计算图片缩放比例，并存储在 bundle 中
        mScaleBundle.putFloat(SCALE_WIDTH, (float) destRect.width() / mOriginWidth);
        mScaleBundle.putFloat(SCALE_HEIGHT, (float) destRect.height() / mOriginHeight);

        // 计算位移距离，并将数据存储到 bundle 中
        mTransitionBundle.putFloat(TRANSITION_X, destRect.left - mOriginRect.left);
        mTransitionBundle.putFloat(TRANSITION_Y, destRect.top - mOriginRect.top);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //只有从Activity启动，才使用动画
        if (!mFromNotify && mFromActivity) {
            overridePendingTransition(0, 0);
            mFromActivity = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        if(mFistStart)
//            UpdateUI(MusicService.getCurrentMP3(), MusicService.isPlaying());
        if (mNeedUpdateUI) {
            UpdateUI(MusicServiceRemote.getCurrentSong(), MusicServiceRemote.isPlaying());
            mNeedUpdateUI = false;
        }
        //更新进度条
        new ProgressThread().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsForeground = false;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        UpdateUI(MusicServiceRemote.getCurrentSong(), MusicServiceRemote.isPlaying());
    }

    @Override
    public void onServiceDisConnected() {
        super.onServiceDisConnected();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("Rect", mOriginRect);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mOriginRect == null && savedInstanceState != null && savedInstanceState.getParcelable("Rect") != null)
            mOriginRect = savedInstanceState.getParcelable("Rect");
    }

    @Override
    public void onBackPressed() {
        if (mFromNotify) {
            finish();
            overridePendingTransition(0, R.anim.audio_out);
            return;
        }
        if (mPager.getCurrentItem() == 1) {
            if (mIsBacking || mAnimCover == null) {
                return;
            }
            mIsBacking = true;

            //更新动画控件封面 保证退场动画的封面与fragment中封面一致
            ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(mUri == null ? Uri.EMPTY : mUri);
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequestBuilder.build())
                    .setOldController(mAnimCover.getController())
//                    .setControllerListener(new ControllerListener<ImageInfo>() {
//                        @Override
//                        public void onSubmit(String id, Object callerContext) {
//
//                        }
//
//                        @Override
//                        public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
//                            playBackAnimation();
//                        }
//
//                        @Override
//                        public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
//
//                        }
//
//                        @Override
//                        public void onIntermediateImageFailed(String id, Throwable throwable) {
//
//                        }
//
//                        @Override
//                        public void onFailure(String id, Throwable throwable) {
//                            playBackAnimation();
//                        }
//
//                        @Override
//                        public void onRelease(String id) {
//
//                        }
//                    })
                    .build();
            mAnimCover.setController(controller);
            mAnimCover.setVisibility(View.VISIBLE);
            playBackAnimation();
        } else {
            finish();
            overridePendingTransition(0, R.anim.audio_out);
        }

    }

    private void playBackAnimation() {
        final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
        final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
        final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
        final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;
        Spring coverSpring = SpringSystem.create().createSpring();
        coverSpring.setSpringConfig(COVER_OUT_SPRING_CONFIG);
        coverSpring.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
                if (mAnimCover == null)
                    return;
                final double currentVal = spring.getCurrentValue();
                mAnimCover.setTranslationX((float) (transitionX * currentVal));
                mAnimCover.setTranslationY((float) (transitionY * currentVal));
                mAnimCover.setScaleX((float) (1 + scaleX * currentVal));
                mAnimCover.setScaleY((float) (1 + scaleY * currentVal));
            }

            @Override
            public void onSpringActivate(Spring spring) {
                //隐藏fragment中的image
                mCoverFragment.hideImage();
            }

            @Override
            public void onSpringAtRest(Spring spring) {
                finish();
                overridePendingTransition(0, 0);
            }
        });
        coverSpring.setOvershootClampingEnabled(true);
        coverSpring.setCurrentValue(1);
        coverSpring.setEndValue(0);
    }

    /**
     * 上一首 下一首 播放、暂停
     *
     * @param v
     */
    @OnClick({R.id.playbar_next, R.id.playbar_prev, R.id.playbar_play_container})
    public void onCtrlClick(View v) {
        Intent intent = new Intent(MusicService.ACTION_CMD);
        switch (v.getId()) {
            case R.id.playbar_prev:
                intent.putExtra("Control", Command.PREV);
                break;
            case R.id.playbar_next:
                intent.putExtra("Control", Command.NEXT);
                break;
            case R.id.playbar_play_container:
                intent.putExtra("Control", Command.TOGGLE);
                break;
        }
        sendBroadcast(intent);
    }

    /**
     * 播放模式 播放列表 关闭 隐藏
     *
     * @param v
     */
    @OnClick({R.id.playbar_model, R.id.playbar_playinglist, R.id.top_hide, R.id.top_more})
    public void onOtherClick(View v) {
        switch (v.getId()) {
            //设置播放模式
            case R.id.playbar_model:
                int currentModel = MusicServiceRemote.getPlayModel();
                currentModel = (currentModel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP : ++currentModel);
                MusicServiceRemote.setPlayModel(currentModel);
                Theme.TintDrawable(mPlayModel, currentModel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                        currentModel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                                R.drawable.play_btn_loop_one, ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_6c6a6c : R.color.gray_6b6b6b));

                String msg = currentModel == Constants.PLAY_LOOP ? getString(R.string.model_normal) :
                        currentModel == Constants.PLAY_SHUFFLE ? getString(R.string.model_random) : getString(R.string.model_repeat);
                //刷新下一首
                if (currentModel != Constants.PLAY_SHUFFLE) {
                    mNextSong.setText(getString(R.string.next_song, MusicServiceRemote.getNextSong().getTitle()));
                }
                ToastUtil.show(this, msg);
                break;
            //打开正在播放列表
            case R.id.playbar_playinglist:
                Intent intent = new Intent(this, PlayQueueDialog.class);
                startActivity(intent);
                break;
            //关闭
            case R.id.top_hide:
                onBackPressed();
                break;
            //弹出窗口
            case R.id.top_more:
                @SuppressLint("RestrictedApi")
                Context wrapper = new ContextThemeWrapper(this, Theme.getPopupMenuStyle());
                final PopupMenu popupMenu = new PopupMenu(wrapper, v, Gravity.TOP);
                popupMenu.getMenuInflater().inflate(R.menu.menu_audio_item, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new AudioPopupListener(this, mInfo));
                popupMenu.show();
                break;
        }
    }

    @OnClick({R.id.volume_down, R.id.volume_up, R.id.next_song})
    void onVolumeClick(View view) {
        switch (view.getId()) {
            case R.id.volume_down:
                if (mAudioManager != null) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_PLAY_SOUND);
                }
                break;
            case R.id.volume_up:
                if (mAudioManager != null) {
                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_PLAY_SOUND);
                }
                break;
            case R.id.next_song:
//                mLyric.setVisibility(View.GONE);
//                mVolumeContainer.setVisibility(View.VISIBLE);
                if (mBottomConfig == 2) {
                    mNextSong.startAnimation(makeAnimation(mNextSong, false));
                    mVolumeContainer.startAnimation(makeAnimation(mVolumeContainer, true));
                    mHandler.removeCallbacks(mVolumeRunnable);
                    mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
                }
                break;
        }
        if (view.getId() != R.id.next_song) {
            final int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            final int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mVolumeSeekbar.setProgress((int) (current * 1.0 / max * 100));
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
    private void setUpGuide() {
        mDotList = new ArrayList<>();
        mDotList.add(findView(R.id.guide_01));
        mDotList.add(findView(R.id.guide_02));
        mDotList.add(findView(R.id.guide_03));
        int width = DensityUtil.dip2px(this, 8);
        int height = DensityUtil.dip2px(this, 2);
        mHighLightIndicator = Theme.getShape(GradientDrawable.RECTANGLE, ThemeStore.getAccentColor(), width, height);
        mNormalIndicator = Theme.getShape(GradientDrawable.RECTANGLE, ColorUtil.adjustAlpha(ThemeStore.getAccentColor(), 0.3f), width, height);
        for (int i = 0; i < mDotList.size(); i++) {
            mDotList.get(i).setImageDrawable(mNormalIndicator);
        }
    }

    /**
     * 初始化seekbar
     */
    private void setUpSeekBar() {
//        RelativeLayout seekbarContainer = findViewById(R.id.seekbar_container);
//        mProgressSeekBar = new SeekBar(mContext);
//        mProgressSeekBar.setProgressDrawable(getResources().getDrawable(R.drawable.bg_progress));
//        mProgressSeekBar.setPadding(DensityUtil.dip2px(mContext,5),0,DensityUtil.dip2px(mContext,5),0);
//        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,10)));
//
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext,2));
//        lp.setMargins(DensityUtil.dip2px(mContext,10),0,DensityUtil.dip2px(mContext,10),0);
//
//        lp.addRule(RelativeLayout.LEFT_OF,R.id.text_remain);
//        lp.addRule(RelativeLayout.RIGHT_OF,R.id.text_hasplay);
//        lp.addRule(RelativeLayout.CENTER_VERTICAL);
//        seekbarContainer.addView(mProgressSeekBar,lp);
        if (mInfo == null)
            return;
        //初始化已播放时间与剩余时间
        mDuration = (int) mInfo.getDuration();
        final int temp = MusicServiceRemote.getProgress();
        mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;

        if (mDuration > 0 && mDuration - mCurrentTime > 0) {
            mHasPlay.setText(Util.getTime(mCurrentTime));
            mRemainPlay.setText(Util.getTime(mDuration - mCurrentTime));
        }

        //初始化seekbar
        if (mDuration > 0 && mDuration < Integer.MAX_VALUE)
            mProgressSeekBar.setMax(mDuration);
        else
            mProgressSeekBar.setMax(1000);

        if (mCurrentTime > 0 && mCurrentTime < mDuration)
            mProgressSeekBar.setProgress(mCurrentTime);
        else
            mProgressSeekBar.setProgress(0);

        mProgressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHandler.sendEmptyMessage(UPDATE_TIME_ONLY);
                mCurrentTime = progress;
                if (mLrcView != null)
                    mLrcView.seekTo(progress, true, fromUser);
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

        final int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekbar.setProgress((int) (current * 1.0 / max * 100));
        mVolumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mBottomConfig == 2) {
                    mHandler.removeCallbacks(mVolumeRunnable);
                    mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
                }
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        (int) (seekBar.getProgress() / 100f * max),
                        AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if (mBottomConfig == 2) {
            mHandler.postDelayed(mVolumeRunnable, DELAY_SHOW_NEXT_SONG);
        }
    }

    /**
     * 更新顶部歌曲信息
     *
     * @param song
     */
    public void updateTopStatus(Song song) {
        if (song == null)
            return;
        String title = song.getTitle() == null ? "" : song.getTitle();
        String artist = song.getArtist() == null ? "" : song.getArtist();
        String album = song.getAlbum() == null ? "" : song.getAlbum();

        if (title.equals(""))
            mTopTitle.setText(getString(R.string.unknown_song));
        else
            mTopTitle.setText(title);
        if (artist.equals(""))
            mTopDetail.setText(song.getAlbum());
        else if (album.equals(""))
            mTopDetail.setText(song.getArtist());
        else
            mTopDetail.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
    }

    /**
     * 更新播放、暂停按钮
     *
     * @param isPlay
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
    private void setUpViewPager() {
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        //activity重启后复用以前的fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getFragments() != null && fragmentManager.getFragments().size() > 0) {
            for (Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof RecordFragment) {
                    mRecordFragment = (RecordFragment) fragment;
                } else if (fragment instanceof CoverFragment) {
                    mCoverFragment = (CoverFragment) fragment;
                } else if (fragment instanceof LyricFragment)
                    mLyricFragment = (LyricFragment) fragment;
            }
        }
        Bundle bundle = new Bundle();
        bundle.putInt("Width", mWidth);
        bundle.putParcelable("Song", mInfo);

        //初始化所有fragment
        if (mRecordFragment == null)
            mRecordFragment = new RecordFragment();
        mAdapter.addFragment(mRecordFragment);
        if (mCoverFragment == null)
            mCoverFragment = new CoverFragment();
        mCoverFragment.setOnFirstLoadFinishListener(() -> mAnimCover.setVisibility(View.INVISIBLE));

        mCoverFragment.setInflateFinishListener(view -> {
            //从通知栏启动只设置位置信息并隐藏
            //不用启动动画
            if (mFromNotify) {
                mCoverFragment.showImage();
                //隐藏动画用的封面并设置位置信息
                mAnimCover.setVisibility(View.GONE);
                return;
            }

            if (mOriginRect == null || mOriginRect.width() <= 0 || mOriginRect.height() <= 0) {
                //获取传入的界面信息
                mOriginRect = getIntent().getParcelableExtra("Rect");
            }

            if (mOriginRect == null) {
                return;
            }
            // 获取上一个界面中，图片的宽度和高度
            mOriginWidth = mOriginRect.width();
            mOriginHeight = mOriginRect.height();

            // 设置 view 的位置，使其和上一个界面中图片的位置重合
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(mOriginWidth, mOriginHeight);
            params.setMargins(mOriginRect.left, mOriginRect.top - StatusBarUtil.getStatusBarHeight(mContext), mOriginRect.right, mOriginRect.bottom);
            mAnimCover.setLayoutParams(params);

            //获得终点控件的位置信息
            view.getGlobalVisibleRect(mDestRect);
            // 计算图片缩放比例和位移距离
            getMoveInfo(mDestRect);

            mAnimCover.setPivotX(0);
            mAnimCover.setPivotY(0);

            final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
            final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
            final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
            final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;

            final Spring spring = SpringSystem.create().createSpring();
            spring.setSpringConfig(COVER_IN_SPRING_CONFIG);
            spring.addListener(new SimpleSpringListener() {
                @Override
                public void onSpringUpdate(Spring spring) {
                    if (mAnimCover == null)
                        return;
                    final double currentVal = spring.getCurrentValue();
                    mAnimCover.setTranslationX((float) (transitionX * currentVal));
                    mAnimCover.setTranslationY((float) (transitionY * currentVal));
                    mAnimCover.setScaleX((float) (1 + scaleX * currentVal));
                    mAnimCover.setScaleY((float) (1 + scaleY * currentVal));
                }

                @Override
                public void onSpringAtRest(Spring spring) {
                    //入场动画结束时显示fragment中的封面
                    mCoverFragment.showImage();
//                    mHandler.postDelayed(() -> {
//                        //隐藏动画用的封面
//                        mAnimCover.setVisibility(View.INVISIBLE);
//                    },24);

                }

                @Override
                public void onSpringActivate(Spring spring) {
                    overridePendingTransition(0, 0);
                }
            });
            spring.setOvershootClampingEnabled(true);
            spring.setCurrentValue(0);
            spring.setEndValue(1);

        });
        mCoverFragment.setArguments(bundle);

        mAdapter.addFragment(mCoverFragment);

        if (mLyricFragment == null)
            mLyricFragment = new LyricFragment();
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
            mLrcView.setHighLightColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_highlight_day : R.color.lrc_highlight_night));
            mLrcView.setOtherColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_normal_day : R.color.lrc_normal_night));
            mLrcView.setTimeLineColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_normal_day : R.color.lrc_normal_night));
        });
        mLyricFragment.setArguments(bundle);
        mAdapter.addFragment(mLyricFragment);
        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(mAdapter.getCount() - 1);

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
                LogUtil.d("PlayerAction", "ThresHoldX: " + THRESHOLD_X + " DistanceX: " + Math.abs(mEventX1 - mEventX2));
                LogUtil.d("PlayerAction", "ThresHoldY: " + THRESHOLD_Y + " DistanceY: " + (mEventY2 - mEventY1));
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
                mDotList.get(mPrevPosition).setImageDrawable(mNormalIndicator);
                mDotList.get(position).setImageDrawable(mHighLightIndicator);
                mPrevPosition = position;
                if (position == 0)
                    mPager.setIntercept(true);
                else
                    mPager.setIntercept(false);
                //歌词界面常亮
                if (position == 2 && SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SCREEN_ALWAYS_ON, false)) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mPager.setCurrentItem(1);
    }

    //更新界面
    @Override
    public void UpdateUI(Song song, boolean isplay) {
        mInfo = song;
        //两种情况下更新ui
        //一是activity在前台  二是activity暂停后有更新的动作，当activity重新回到前台后更新ui
        if (!mIsForeground || mInfo == null) {
            mNeedUpdateUI = true;
            return;
        }
        //当操作不为播放或者暂停且正在运行时，更新所有控件
        if ((Global.getOperation() != Command.TOGGLE || mNeedUpdateUI)) {
            //更新顶部信息
            updateTopStatus(mInfo);
            //更新歌词
            if (!mFistStart) {
                mLyricFragment.updateLrc(mInfo);
            }
            //更新进度条
            int temp = MusicServiceRemote.getProgress();
            mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
            mDuration = (int) mInfo.getDuration();
            mProgressSeekBar.setMax(mDuration);
            //更新下一首歌曲
            mNextSong.setText(getString(R.string.next_song, MusicServiceRemote.getNextSong().getTitle()));
            updateBg();
            requestCover(Global.getOperation() != Command.TOGGLE && !mFistStart);
        }
        //更新按钮状态
        if(mIsPlay != isplay)
            updatePlayButton(isplay);
    }

    //更新进度条线程
    private class ProgressThread extends Thread {
        @Override
        public void run() {
            while (mIsForeground) {
                if (!MusicServiceRemote.isPlaying())
                    continue;
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

    /**
     * 初始化底部区域
     */
    private void setUpBottom() {
        mBottomConfig = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, 2);
        if (mBottomConfig == 0) {//仅显示下一首
            mVolumeContainer.setVisibility(View.GONE);
            mNextSong.setVisibility(View.VISIBLE);
        } else if (mBottomConfig == 1) {//仅显示音量控制
            mVolumeContainer.setVisibility(View.VISIBLE);
            mNextSong.setVisibility(View.GONE);
        } else if(mBottomConfig == 3){//关闭
            View volumeLayout = findViewById(R.id.layout_player_volume);
            volumeLayout.setVisibility(View.INVISIBLE);
//            LinearLayout.LayoutParams volumelLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0);
//            volumelLp.weight = 0;
//            volumeLayout.setLayoutParams(volumelLp);

//            View controlLayout = findViewById(R.id.layout_player_control);
//            LinearLayout.LayoutParams controlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0);
//            controlLp.weight = 2f;
//            controlLayout.setLayoutParams(controlLp);
        }
    }

    /**
     * 根据主题颜色修改按钮颜色
     */
    private void setUpViewColor() {
        int accentColor = ThemeStore.getAccentColor();
        int tintColor = ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_6c6a6c : R.color.gray_6b6b6b);

        setProgressDrawable(mProgressSeekBar, accentColor);
        setProgressDrawable(mVolumeSeekbar, accentColor);
        //修改thumb
        int inset = DensityUtil.dip2px(mContext, 6);
        mProgressSeekBar.setThumb(new InsetDrawable(Theme.TintDrawable(Theme.getShape(GradientDrawable.RECTANGLE, accentColor, DensityUtil.dip2px(this, 2), DensityUtil.dip2px(this, 6)), accentColor),
                inset, inset, inset, inset));
        mVolumeSeekbar.setThumb(new InsetDrawable(Theme.TintDrawable(Theme.getShape(GradientDrawable.RECTANGLE, accentColor, DensityUtil.dip2px(this, 2), DensityUtil.dip2px(this, 6)), accentColor),
                inset, inset, inset, inset));
//        mProgressSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,10)));
//        Drawable seekbarBackground = mProgressSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

        //修改控制按钮颜色
        Theme.TintDrawable(mPlayBarNext, R.drawable.play_btn_next, accentColor);
        Theme.TintDrawable(mPlayBarPrev, R.drawable.play_btn_pre, accentColor);

        //歌曲名颜色
        mTopTitle.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.black_333333 : R.color.white_e5e5e5));

        //修改顶部按钮颜色
        Theme.TintDrawable(mTopHide, R.drawable.icon_player_back, tintColor);
        Theme.TintDrawable(mTopMore, R.drawable.icon_player_more, tintColor);
        //播放模式与播放队列
        int playMode = SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, Constants.PLAY_LOOP);
        Theme.TintDrawable(mPlayModel, playMode == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                playMode == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                        R.drawable.play_btn_loop_one, tintColor);
        Theme.TintDrawable(mPlayQueue, R.drawable.play_btn_normal_list, tintColor);

        //音量控制
        mVolumeDown.getDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
        mVolumeUp.getDrawable().setColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
//        Theme.TintDrawable(mVolumeDown,R.drawable.ic_volume_down_black_24dp,tintColor);
//        Theme.TintDrawable(mVolumeUp,R.drawable.ic_volume_up_black_24dp,tintColor);

        mPlayPauseView.setBackgroundColor(accentColor);
        //下一首背景
        mNextSong.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, ColorUtil.getColor(ThemeStore.isDay() ? R.color.white_fafafa : R.color.gray_343438),
                DensityUtil.dip2px(this, 2), 0, 0, DensityUtil.dip2px(this, 288), DensityUtil.dip2px(this, 38), 1));
        mNextSong.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_a8a8a8 : R.color.white_e5e5e5));

    }

    private void setProgressDrawable(SeekBar seekBar, int accentColor) {
        LayerDrawable progressDrawable = (LayerDrawable) seekBar.getProgressDrawable();
        //修改progress颜色
        ((GradientDrawable) progressDrawable.getDrawable(0)).setColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_efeeed : R.color.gray_343438));
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
        mFistStart = false;
    }

    /**
     * 更新封面
     */
    private void requestCover(boolean withAnimation) {
        //更新封面
        if (mInfo == null) {
            mUri = Uri.parse("res://" + mContext.getPackageName() + "/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
            updateCover(withAnimation);
        } else {
            new ImageUriRequest<String>() {
                @Override
                public void onError(String errMsg) {
                    mUri = Uri.EMPTY;
                    updateCover(withAnimation);
                }

                @Override
                public void onSuccess(String result) {
                    mUri = Uri.parse(result);
                    updateCover(withAnimation);
                }

                @Override
                public void load() {
                    getCoverObservable(getSearchRequestWithAlbumType(mInfo))
                            .compose(RxUtil.applyScheduler())
                            .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
                }
            }.load();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.remove();
        Util.unregisterReceiver(this, mTagReceiver);
    }

    /**
     * 选择歌词文件
     *
     * @param dialog
     * @param file
     */
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
//        //如果之前忽略过该歌曲的歌词，取消忽略
//        Set<String> ignoreLrcId = new HashSet<>(SPUtil.getStringSet(this,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID"));
//        if(ignoreLrcId.size() > 0){
//            for (String id : ignoreLrcId){
//                if((mInfo.getID() + "").equals(id)){
//                    ignoreLrcId.remove(mInfo.getID() + "");
//                    SPUtil.putStringSet(mContext,SPUtil.SETTING_KEY.NAME,"IgnoreLrcID",ignoreLrcId);
//                }
//            }
//        }
        SPUtil.putValue(mContext, SPUtil.LYRIC_KEY.NAME, mInfo.getId() + "", SPUtil.LYRIC_KEY.LYRIC_MANUAL);
        mLyricFragment.updateLrc(file.getAbsolutePath());
        sendBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control", Command.CHANGE_LYRIC));
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {
    }

    private void updateProgressByHandler() {
        if (mHasPlay != null
                && mRemainPlay != null
                && mCurrentTime > 0
                && (mDuration - mCurrentTime) > 0) {
            mHasPlay.setText(Util.getTime(mCurrentTime));
            mRemainPlay.setText(Util.getTime(mDuration - mCurrentTime));
        }
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

    @Override
    public void onTagEdit(Song newSong) {
        if (newSong != null) {
            updateTopStatus(newSong);
            mLyricFragment.updateLrc(newSong, true);
            Fresco.getImagePipeline().clearCaches();
            final UriRequest request = ImageUriUtil.getSearchRequestWithAlbumType(mInfo);
            SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getLastFMKey());
            SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getNeteaseCacheKey());
            mInfo = newSong;
            requestCover(false);
        }
    }

    public void showLyricOffsetView(){
        if(mPager.getCurrentItem() != 2){
            mPager.setCurrentItem(2,true);
        }
        if(getLyricFragment() != null){
            getLyricFragment().showLyricOffsetView();
        }
    }
}
