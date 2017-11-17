package remix.myplayer.ui.activity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
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
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PagerAdapter;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.listener.AudioPopupListener;
import remix.myplayer.lyric.LrcView;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.AudioViewPager;
import remix.myplayer.ui.customview.playpause.PlayPauseView;
import remix.myplayer.ui.dialog.FileChooserDialog;
import remix.myplayer.ui.dialog.PlayQueueDialog;
import remix.myplayer.ui.fragment.CoverFragment;
import remix.myplayer.ui.fragment.LrcFragment;
import remix.myplayer.ui.fragment.RecordFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class PlayerActivity extends BaseActivity implements UpdateHelper.Callback,FileChooserDialog.FileCallback{
    private static final String TAG = "PlayerActivity";
    //是否正在运行
    public static boolean mIsRunning;
    //上次选中的Fragment
    private int mPrevPosition = 1;
    //是否播放的标志变量
    public static boolean mIsPlay = false;
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
    SeekBar mSeekBar;
    //背景
    @BindView(R.id.audio_holder_container)
    FrameLayout mContainer;
    @BindView(R.id.holder_pager)
    AudioViewPager mPager;
    //下一首歌曲
    @BindView(R.id.next_song)
    TextView mNextSong;
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
    //是否开启变色背景
    private boolean mDiscolour = false;

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
     * 下拉关闭
     */
    private boolean mIsBacking = false;
    private float mEventY1;
    private float mEventY2;

    /** 更新Handler */
    private MsgHandler mHandler;

    /** 更新封面与背景的Handler */
    private Uri mUri;
    private static final int UPDATE_COVER = 100;
    private static final int UPDATE_BG = 101;
    private static final int UPDATE_TIME_ONLY = 102;
    private static final int UPDATE_TIME_ALL = 103;
    private Bitmap mRawBitMap;

    private Palette.Swatch mSwatch;

    @Override
    protected void setUpTheme() {
        if(ThemeStore.isDay())
            super.setUpTheme();
        else {
            setTheme(R.style.AudioHolderStyle_Night);
        }
    }

    @Override
    protected void setStatusBar() {
        //是否开启背景变色
        mDiscolour = SPUtil.getValue(this,"Setting","Discolour",false) && ThemeStore.isDay();
        if(ThemeStore.isDay()){
            //获得miui版本
            String miui = "";
            int miuiVersion = 0;
            if(Build.MANUFACTURER.equals("Xiaomi")){
                try {
                    Class<?> c = Class.forName("android.os.SystemProperties");
                    Method get = c.getMethod("get", String.class, String.class );
                    miui = (String)(get.invoke(c, "ro.miui.ui.version.name", "unknown" ));
                    if(!TextUtils.isEmpty(miui) && miui.length() >= 2 && TextUtils.isDigitsOnly(miui.substring(1,miui.length()))){
                        miuiVersion = Integer.valueOf(miui.substring(1,miui.length()));
                    }
                }catch (Exception e){
                    CommonUtil.uploadException("miui版本解析错误",e);
                }
            }
            if(Build.MANUFACTURER.equals("Meizu")){
                StatusBarUtil.MeizuStatusbar.setStatusBarDarkIcon(this,true);
                if(mDiscolour)
                    StatusBarUtil.setTransparent(this);
                else
                    StatusBarUtil.setColorNoTranslucent(this, Color.WHITE);
            } else if (Build.MANUFACTURER.equals("Xiaomi") && miuiVersion >= 6 && miuiVersion < 9){
                StatusBarUtil.XiaomiStatusbar.setStatusBarDarkMode(true,this);
                if(mDiscolour)
                    StatusBarUtil.setTransparent(this);
                else
                    StatusBarUtil.setColorNoTranslucent(this, Color.WHITE);
            }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                StatusBarUtil.setTransparent(this);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }  else {
                if(mDiscolour)
                    StatusBarUtil.setTransparent(this);
                else
                    StatusBarUtil.setColorNoTranslucent(this,ColorUtil.getColor(R.color.statusbar_gray_color));
            }
        } else {
            StatusBarUtil.setTransparent(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        mFromActivity =  getIntent().getBooleanExtra("FromActivity",false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);

        mHandler = new MsgHandler(this);

        //获是否正在播放和正在播放的歌曲
        mInfo = MusicService.getCurrentMP3();
        mIsPlay = MusicService.isPlay();

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
        if(mInfo != null)
            MediaStoreUtil.setImageUrl(mAnimCover,mInfo.getAlbumId());

        //恢复位置信息
        if(savedInstanceState != null && savedInstanceState.getParcelable("Rect") != null){
            mOriginRect = savedInstanceState.getParcelable("Rect");
        }

        getWindow().getDecorView().setOnTouchListener((v,event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                mEventY1 = event.getY();
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                mEventY2 = event.getY();
                if(mEventY2 - mEventY1 > 100){
                    onBackPressed();
                }
            }
            return false;
        });

        if(SPUtil.getValue(this,"Setting","LrcHint",true)){
            SPUtil.putValue(this,"Setting","LrcHint",false);
            new MaterialDialog.Builder(mContext)
                    .content(getString(R.string.lc_operation_hint))
                    .contentColorAttr(R.attr.text_color_primary)
                    .positiveColorAttr(R.attr.text_color_primary)
                    .backgroundColorAttr(R.attr.background_color_3)
                    .show();
        }
    }

    /**
     * 计算图片缩放比例，以及位移距离
     *
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
        //只有从Mactivity启动，才使用动画
        if(!mFromNotify && mFromActivity) {
            overridePendingTransition(0,0);
            mFromActivity = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //更新界面
        mIsRunning = true;
//        UpdateUI(MusicService.getCurrentMP3(), MusicService.isPlay());
        if(mNeedUpdateUI){
            UpdateUI(MusicService.getCurrentMP3(), MusicService.isPlay());
            mNeedUpdateUI = false;
        }
        //更新进度条
        new ProgeressThread().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("Rect",mOriginRect);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(mOriginRect == null && savedInstanceState != null && savedInstanceState.getParcelable("Rect") != null)
            mOriginRect = savedInstanceState.getParcelable("Rect");
    }

    @Override
    public void onBackPressed() {
        if(mFromNotify){
            finish();
            overridePendingTransition(0,R.anim.audio_out);
            return;
        }
        if(mPager.getCurrentItem() == 1){
            if(mIsBacking || mAnimCover == null){
               return;
            }
            //更新动画控件封面 保证退场动画的封面与fragment中封面一致
            mIsBacking = true;
            mAnimCover.setImageURI(mUri);

            final float transitionX = mTransitionBundle.getFloat(TRANSITION_X);
            final float transitionY = mTransitionBundle.getFloat(TRANSITION_Y);
            final float scaleX = mScaleBundle.getFloat(SCALE_WIDTH) - 1;
            final float scaleY = mScaleBundle.getFloat(SCALE_HEIGHT) - 1;
            Spring coverSpring = SpringSystem.create().createSpring();
            coverSpring.addListener(new SimpleSpringListener(){
                @Override
                public void onSpringUpdate(Spring spring) {
                    if(mAnimCover == null)
                        return;
                    final double currentVal = spring.getCurrentValue();
                    mAnimCover.setTranslationX((float) (transitionX * currentVal));
                    mAnimCover.setTranslationY((float) (transitionY * currentVal));
                    mAnimCover.setScaleX((float) (1 + scaleX * currentVal));
                    mAnimCover.setScaleY((float) (1 + scaleY * currentVal));
                }
                @Override
                public void onSpringActivate(Spring spring) {
                    mAnimCover.setVisibility(View.VISIBLE);
                    //隐藏fragment中的image
                    if(mAdapter.getItem(1) instanceof CoverFragment){
                        ((CoverFragment) mAdapter.getItem(1)).hideImage();
                    }

                }
                @Override
                public void onSpringAtRest(Spring spring) {
                    finish();
                    overridePendingTransition(0,0);
                }
            });
            coverSpring.setOvershootClampingEnabled(true);
            coverSpring.setCurrentValue(1);
            coverSpring.setEndValue(0);
        } else {
            finish();
            overridePendingTransition(0,R.anim.audio_out);
        }

    }

    /**
     * 上一首 下一首 播放、暂停
     * @param v
     */
    @OnClick({R.id.playbar_next,R.id.playbar_prev,R.id.playbar_play_container})
    public void onCtrlClick(View v){
        Intent intent = new Intent(Constants.CTL_ACTION);
        switch (v.getId()) {
            case R.id.playbar_prev:
                intent.putExtra("Control", Constants.PREV);
                break;
            case R.id.playbar_next:
                intent.putExtra("Control", Constants.NEXT);
                break;
            case R.id.playbar_play_container:
                intent.putExtra("Control", Constants.TOGGLE);
                break;
        }
        MobclickAgent.onEvent(this,v.getId() == R.id.playbar_play_container ? "Prev" : v.getId() == R.id.playbar_next ? "Next" : "Play");
        sendBroadcast(intent);
    }

    /**
     * 播放模式 播放列表 关闭 隐藏
     * @param v
     */
    @OnClick({R.id.playbar_model,R.id.playbar_playinglist,R.id.top_hide,R.id.top_more})
    public void onOtherClick(View v){
        switch (v.getId()){
            //设置播放模式
            case R.id.playbar_model:
                MobclickAgent.onEvent(this,"PlayModel");
                int currentmodel = MusicService.getPlayModel();
                currentmodel = (currentmodel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP : ++currentmodel);
                MusicService.getInstance().setPlayModel(currentmodel);
                Theme.TintDrawable(mPlayModel,currentmodel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                        currentmodel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                                R.drawable.play_btn_loop_one,ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_6c6a6c : R.color.gray_6b6b6b));

                String msg = currentmodel == Constants.PLAY_LOOP ? getString(R.string.model_normal) :
                        currentmodel == Constants.PLAY_SHUFFLE ? getString(R.string.model_random) : getString(R.string.model_repeat);
                //刷新下一首
                if(currentmodel != Constants.PLAY_SHUFFLE && MusicService.getNextMP3() != null){
                    mNextSong.setText(getString(R.string.next_song,MusicService.getNextMP3().getTitle()));
                }
                ToastUtil.show(this,msg);
                break;
            //打开正在播放列表
            case R.id.playbar_playinglist:
                MobclickAgent.onEvent(this,"PlayingList");
                Intent intent = new Intent(this,PlayQueueDialog.class);
                startActivity(intent);
                break;
            //关闭
            case R.id.top_hide:
                onBackPressed();
                break;
            //弹出窗口
            case R.id.top_more:
                Context wrapper = new ContextThemeWrapper(this,Theme.getPopupMenuStyle());
                final PopupMenu popupMenu = new PopupMenu(wrapper,v, Gravity.TOP);
                popupMenu.getMenuInflater().inflate(R.menu.audio_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new AudioPopupListener(this,mInfo));
                popupMenu.show();
                break;
        }
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
        int width = DensityUtil.dip2px(this,8);
        int height = DensityUtil.dip2px(this,2);
        mHighLightIndicator = Theme.getShape(GradientDrawable.RECTANGLE,ThemeStore.getAccentColor(),width,height);
        mNormalIndicator = Theme.getShape(GradientDrawable.RECTANGLE,ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.3f),width,height);
        for(int i = 0; i < mDotList.size(); i++){
            mDotList.get(i).setImageDrawable(mNormalIndicator);
        }
    }

    /**
     * 初始化seekbar
     */
    private void setUpSeekBar() {
//        RelativeLayout seekbarContainer = findViewById(R.id.seekbar_container);
//        mSeekBar = new SeekBar(mContext);
//        mSeekBar.setProgressDrawable(getResources().getDrawable(R.drawable.bg_progress));
//        mSeekBar.setPadding(DensityUtil.dip2px(mContext,5),0,DensityUtil.dip2px(mContext,5),0);
//        mSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,10)));
//
//        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext,2));
//        lp.setMargins(DensityUtil.dip2px(mContext,10),0,DensityUtil.dip2px(mContext,10),0);
//
//        lp.addRule(RelativeLayout.LEFT_OF,R.id.text_remain);
//        lp.addRule(RelativeLayout.RIGHT_OF,R.id.text_hasplay);
//        lp.addRule(RelativeLayout.CENTER_VERTICAL);
//        seekbarContainer.addView(mSeekBar,lp);
        if(mInfo == null)
            return;
        //初始化已播放时间与剩余时间
        mDuration = (int)mInfo.getDuration();
        final int temp = MusicService.getProgress();
        mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;

        if(mDuration > 0 && mCurrentTime >= 0 && (mDuration - mCurrentTime) > 0){
            mHasPlay.setText(CommonUtil.getTime(mCurrentTime));
            mRemainPlay.setText(CommonUtil.getTime(mDuration - mCurrentTime));
        }

        //初始化seekbar
        if(mDuration > 0 && mDuration < Integer.MAX_VALUE)
            mSeekBar.setMax(mDuration);
        else
            mSeekBar.setMax(1000);

        if(mCurrentTime > 0 && mCurrentTime < mDuration)
            mSeekBar.setProgress(mCurrentTime);
        else
            mSeekBar.setProgress(0);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mHandler.sendEmptyMessage(UPDATE_TIME_ONLY);
                if(mLrcView != null)
                    mLrcView.seekTo(progress,true,fromUser);
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
                MusicService.setProgress(seekBar.getProgress());
                mIsDragSeekBarFromUser = false;
            }
        });
    }

    public void setMP3Item(Song song){
        if(song != null)
            mInfo = song;
    }

    /**
     * 更新顶部歌曲信息
     * @param song
     */
    public void updateTopStatus(Song song) {
        if(song == null)
            return;
        String title = song.getTitle() == null ? "" : song.getTitle();
        String artist =  song.getArtist() == null ? "" : song.getArtist();
        String album =  song.getAlbum() == null ? "" : song.getAlbum();

        if(title.equals(""))
            mTopTitle.setText(getString(R.string.unknow_song));
        else
            mTopTitle.setText(title);
        if(artist.equals(""))
            mTopDetail.setText(song.getAlbum());
        else if(album.equals(""))
            mTopDetail.setText(song.getArtist());
        else
            mTopDetail.setText(song.getArtist() + "-" + song.getAlbum());
    }

    /**
     * 更新播放、暂停按钮
     * @param isPlay
     */
    public void updatePlayButton(final boolean isPlay) {
        mPlayPauseView.updateState(isPlay,true);
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
    private void setUpViewPager(){
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        Bundle bundle = new Bundle();
        bundle.putInt("Width", mWidth);
        bundle.putSerializable("Song", mInfo);
        //初始化所有fragment
        final RecordFragment recordFragment = new RecordFragment();
        mAdapter.AddFragment(recordFragment);
        CoverFragment coverFragment = new CoverFragment();
        coverFragment.setInflateFinishListener(view -> {
            //从通知栏启动只设置位置信息并隐藏
            //不用启动动画
            if (mFromNotify) {
                if (mAdapter.getItem(1) instanceof CoverFragment)
                    ((CoverFragment) mAdapter.getItem(1)).showImage();
                //隐藏动画用的封面并设置位置信息
                mAnimCover.setVisibility(View.GONE);
                return;
            }

            if(mOriginRect == null || mOriginRect.width() <= 0 || mOriginRect.height() <= 0) {
                //获取传入的界面信息
                mOriginRect = getIntent().getParcelableExtra("Rect");
            }

            if(mOriginRect == null) {
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
            spring.addListener(new SimpleSpringListener(){
                @Override
                public void onSpringUpdate(Spring spring) {
                    if(mAnimCover == null)
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
                    if (mAdapter.getItem(1) instanceof CoverFragment) {
                        ((CoverFragment) mAdapter.getItem(1)).showImage();
                    }
                    //隐藏动画用的封面
                    mAnimCover.setVisibility(View.GONE);
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
        coverFragment.setArguments(bundle);

        mAdapter.AddFragment(coverFragment);
        final LrcFragment lrcFragment = new LrcFragment();
        lrcFragment.setOnInflateFinishListener(view -> {
            if (!(view instanceof LrcView))
                return;
            mLrcView = (LrcView) view;
            mLrcView.setOnLrcClickListener(new LrcView.OnLrcClickListener() {
                @Override
                public void onClick() {
                }
                @Override
                public void onLongClick() {
                    new MaterialDialog.Builder(mContext)
                            .items(getString(R.string.ignore_lrc), getString(R.string.select_lrc))
                            .itemsColorAttr(R.attr.text_color_primary)
                            .backgroundColorAttr(R.attr.background_color_3)
                            .itemsCallback((dialog, itemView, position, text) -> {
                                switch (position){
                                    case 0:
                                        //忽略这首歌的歌词
                                        new MaterialDialog.Builder(mContext)
                                                .negativeText(R.string.cancel)
                                                .negativeColorAttr(R.attr.text_color_primary)
                                                .positiveText(R.string.confirm)
                                                .positiveColorAttr(R.attr.text_color_primary)
                                                .title(R.string.confirm_ignore_lrc)
                                                .titleColorAttr(R.attr.text_color_primary)
                                                .backgroundColorAttr(R.attr.background_color_3)
                                                .onPositive((dialog1, which) -> {
                                                    Set<String> ignoreLrcID = SPUtil.getStringSet(mContext, "Setting", "IgnoreLrcID");
                                                    if (mInfo != null && ignoreLrcID != null) {
                                                        ignoreLrcID.add(mInfo.getId() + "");
                                                        SPUtil.putStringSet(mContext, "Setting", "IgnoreLrcID", ignoreLrcID);
                                                        lrcFragment.updateLrc(mInfo);
                                                    }
                                                })
                                                .show();
                                        break;
                                    case 1:
                                        //手动选择歌词
                                        new FileChooserDialog.Builder(PlayerActivity.this)
                                                .extensionsFilter(".lrc")
                                                .show();
                                        break;
                                }
                            })
                            .show();
                }
            });
            mLrcView.setOnSeekToListener(progress -> {
                if (progress > 0 && progress < MusicService.getDuration()) {
                    MusicService.setProgress(progress);
                    mCurrentTime = progress;
                    mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
                }
            });
            mLrcView.setHighLightColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_highlight_day : R.color.lrc_highlight_night));
            mLrcView.setOtherColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_highlight_day : R.color.lrc_highlight_night));
            mLrcView.setTimeLineColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.lrc_normal_day : R.color.lrc_normal_night));
        });
        lrcFragment.setArguments(bundle);
        mAdapter.AddFragment(lrcFragment);

        mPager.setAdapter(mAdapter);
        mPager.setOffscreenPageLimit(mAdapter.getCount() - 1);
        //下滑关闭
        mPager.setOnTouchListener((v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN){
                mEventY1 = event.getY();
            }
            if(event.getAction() == MotionEvent.ACTION_UP){
                mEventY2 = event.getY();
                if(mEventY2 - mEventY1 > 200)
                    onBackPressed();
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
                if (mLrcView != null)
                    mLrcView.setViewPagerScroll(false);
                //歌词界面常亮
                if(position == 2 && SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.SCREEN_ALWAYS_ON,false)){
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (mLrcView != null)
                    mLrcView.setViewPagerScroll(state != ViewPager.SCROLL_STATE_IDLE);
            }
        });
        mPager.setCurrentItem(1);
    }

    //更新界面
    @Override
    public void UpdateUI(Song song, boolean isplay){
        mInfo = song;
        mIsPlay = isplay;
        //两种情况下更新ui
        //一是activity在前台  二是activity暂停后有更新的动作，当activity重新回到前台后更新ui
        if(!mIsRunning || mInfo == null){
            mNeedUpdateUI = true;
            return;
        }
        //当操作不为播放或者暂停且正在运行时，更新所有控件
        if((Global.getOperation() != Constants.TOGGLE || mFistStart)) {
            //更新顶部信息
            updateTopStatus(mInfo);
            //更新歌词
            ((LrcFragment) mAdapter.getItem(2)).updateLrc(mInfo);
            //更新进度条
            int temp = MusicService.getProgress();
            mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
            mDuration = (int) mInfo.getDuration();
            mSeekBar.setMax(mDuration);
            //更新下一首歌曲
            if(MusicService.getNextMP3() != null){
                mNextSong.setText(getString(R.string.next_song,MusicService.getNextMP3().getTitle()));
            }
            new Thread(){
                @Override
                public void run() {
                    updateBg();
                    updateCover();
                }
            }.start();
        }
        //更新按钮状态
        updatePlayButton(isplay);
    }

    //更新进度条线程
    private class ProgeressThread extends Thread {
        @Override
        public void run() {
            while (mIsRunning) {
                if(!MusicService.isPlay())
                    continue;
                int progress = MusicService.getProgress();
                if (progress > 0 && progress < mDuration) {
                    mCurrentTime = progress;
                    mHandler.sendEmptyMessage(UPDATE_TIME_ALL);
                    try {
                        sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 根据主题颜色修改按钮颜色
     */
    private void setUpViewColor() {
        int accentColor = ThemeStore.getAccentColor();
        int tintColor = ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_6c6a6c : R.color.gray_6b6b6b);

        LayerDrawable progressDrawable =  (LayerDrawable) mSeekBar.getProgressDrawable();
        //修改progress颜色
        ((GradientDrawable)progressDrawable.getDrawable(0)).setColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_efeeed : R.color.gray_343438));
        (progressDrawable.getDrawable(1)).setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        mSeekBar.setProgressDrawable(progressDrawable);

        //修改thumb
        int inset = DensityUtil.dip2px(mContext,6);
        mSeekBar.setThumb(new InsetDrawable(Theme.TintDrawable(Theme.getShape(GradientDrawable.RECTANGLE,accentColor, DensityUtil.dip2px(this,2),DensityUtil.dip2px(this,6)),accentColor),
                inset,inset,inset,inset));

//        mSeekBar.setThumb(Theme.getShape(GradientDrawable.OVAL,ThemeStore.getAccentColor(),DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,10)));
//        Drawable seekbarBackground = mSeekBar.getBackground();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && seekbarBackground instanceof RippleDrawable) {
//            ((RippleDrawable)seekbarBackground).setColor(ColorStateList.valueOf( ColorUtil.adjustAlpha(ThemeStore.getAccentColor(),0.2f)));
//        }

        //修改控制按钮颜色
        Theme.TintDrawable(mPlayBarNext,R.drawable.play_btn_next,accentColor);
        Theme.TintDrawable(mPlayBarPrev,R.drawable.play_btn_pre,accentColor);

        //歌曲名颜色
        mTopTitle.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.black_333333 : R.color.white_e5e5e5));

        //修改顶部按钮颜色
        Theme.TintDrawable(mTopHide,R.drawable.icon_player_back,tintColor);
        Theme.TintDrawable(mTopMore,R.drawable.icon_player_more,tintColor);
        //播放模式与播放队列
        int playmodel = SPUtil.getValue(this,"Setting", "PlayModel",Constants.PLAY_LOOP);
        Theme.TintDrawable(mPlayModel,playmodel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                playmodel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                        R.drawable.play_btn_loop_one,tintColor);
        Theme.TintDrawable(mPlayQueue,R.drawable.play_btn_normal_list,tintColor);

        mPlayPauseView.setBackgroundColor(accentColor);
        //下一首背景
        mNextSong.setBackground(Theme.getShape(GradientDrawable.RECTANGLE,ColorUtil.getColor(ThemeStore.isDay() ? R.color.white_fafafa : R.color.gray_343438),
                DensityUtil.dip2px(this,2),0,0,DensityUtil.dip2px(this,288),DensityUtil.dip2px(this,38),1));
        mNextSong.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.gray_a8a8a8 : R.color.white_e5e5e5));
    }


    //更新背景
    private void updateBg() {
        if(!mDiscolour)
            return;
        //更新背景

        mRawBitMap = MediaStoreUtil.getAlbumBitmap(mInfo.getAlbumId(),false);
        if(mRawBitMap == null)
            mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.album_empty_bg_night);

        Palette.from(mRawBitMap).generate(palette -> {
            if(palette == null || palette.getMutedSwatch() == null){
                mSwatch = new Palette.Swatch(Color.GRAY,100);
            } else {
                mSwatch = palette.getMutedSwatch();//柔和 暗色
            }
            mHandler.removeMessages(UPDATE_BG);
            mHandler.sendEmptyMessage(UPDATE_BG);
        });
    }

    /**
     * 更新封面
     */
    private void updateCover() {
        //更新封面
        if(mInfo == null || (mInfo = MusicService.getCurrentMP3()) == null){
            mUri = Uri.parse("res://" + mContext.getPackageName() + "/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
        } else {
            File imgFile = MediaStoreUtil.getImageUrlInCache(mInfo.getAlbumId(), Constants.URL_ALBUM);
            if(imgFile.exists()) {
                mUri = Uri.parse("file:///" +  imgFile);
            } else {
                mUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mInfo.getAlbumId());
            }
        }
        mHandler.removeMessages(UPDATE_COVER);
        mHandler.sendEmptyMessageDelayed(UPDATE_COVER,mFistStart ? 16 : 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.remove();
    }

    /**
     * 选择歌词文件
     * @param dialog
     * @param file
     */
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        //如果之前忽略过该歌曲的歌词，取消忽略
        Set<String> ignoreLrcId = SPUtil.getStringSet(this,"Setting","IgnoreLrcID");
        if(ignoreLrcId != null && ignoreLrcId.size() > 0){
            for (String id : ignoreLrcId){
                if((mInfo.getId() + "").equals(id)){
                    ignoreLrcId.remove(mInfo.getId() + "");
                    SPUtil.putStringSet(this,"Setting","IgnoreLrcID",ignoreLrcId);
                }
            }
        }
        ((LrcFragment) mAdapter.getItem(2)).updateLrc(file.getAbsolutePath());
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {
    }

    private void updateProgressByHandler() {
        if(mHasPlay != null
                && mRemainPlay != null
                && mCurrentTime > 0
                && (mDuration - mCurrentTime) > 0){
            mHasPlay.setText(CommonUtil.getTime(mCurrentTime));
            mRemainPlay.setText(CommonUtil.getTime(mDuration - mCurrentTime));
        }
    }

    private void updateSeekbarByHandler(){
        mSeekBar.setProgress(mCurrentTime);
    }

    @OnHandleMessage
    public void handleInternal(Message msg){
        if(msg.what == UPDATE_COVER){
            ((CoverFragment) mAdapter.getItem(1)).UpdateCover(mInfo,mUri,!mFistStart);
            mFistStart = false;
        }
        if(msg.what == UPDATE_BG){
            int colorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f);
            int colorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{colorFrom, colorTo}));
        }
        if(msg.what == UPDATE_TIME_ONLY && !mIsDragSeekBarFromUser){
            updateProgressByHandler();
        }
        if(msg.what == UPDATE_TIME_ALL && !mIsDragSeekBarFromUser){
            updateProgressByHandler();
            updateSeekbarByHandler();
        }
    }

}
