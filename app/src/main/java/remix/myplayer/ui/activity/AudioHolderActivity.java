package remix.myplayer.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.PagerAdapter;
import remix.myplayer.fragment.CoverFragment;
import remix.myplayer.fragment.LrcFragment;
import remix.myplayer.fragment.RecordFragment;
import remix.myplayer.listener.AudioPopupListener;
import remix.myplayer.lrc.onSeekListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.customview.AudioViewPager;
import remix.myplayer.lrc.LrcView;
import remix.myplayer.ui.dialog.PlayQueueDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class AudioHolderActivity extends BaseActivity implements MusicService.Callback{
    private static final String TAG = "AudioHolderActivity";
    public static AudioHolderActivity mInstance = null;
    //是否正在运行
    public static boolean mIsRunning;
    //上次选中的Fragment
    private int mPrevPosition = 1;
    //是否播放的标志变量
    public static boolean mIsPlay = false;
    //第一次启动的标志变量
    private boolean mFistStart = true;
    //是否正在拖动进度条
    public static boolean mIsDragSeekBar = false;
    private Palette.Swatch mSwatch = null;
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
    @BindView(R.id.playbar_prev)
    ImageButton mPlayBarPrev;
    @BindView(R.id.playbar_play)
    ImageButton mPlayBarPlay;
    @BindView(R.id.playbar_next)
    ImageButton mPlayBarNext;
    @BindView(R.id.playbar_model)
    ImageButton mPlayModel;
    @BindView(R.id.playbar_playinglist)
    ImageButton mPlayingList;
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

    //歌词控件
    private LrcView mLrcView;
    //背景渐变色
    @ColorInt
    private int mColorFrom;
    @ColorInt
    private int mColorTo;
    @ColorInt
    private int mColorDraken;
    @ColorInt
    private int mColorDark;

    //Viewpager
    private PagerAdapter mAdapter;
    private Bundle mBundle;
    private ArrayList<ImageView> mGuideList;

    //当前播放的歌曲
    private MP3Item mInfo;
    //当前播放时间
    private int mCurrentTime;
    //当前歌曲总时长
    private int mDuration;

    //需要高斯模糊的高度与宽度
    public static int mWidth;
    public static int mHeight;
    //高斯模糊之前的bitmap
    private Bitmap mRawBitMap;
    //背景消失与现实的动画
    private AlphaAnimation mAnimIn;
    private AlphaAnimation mAnimOut;
    //是否从通知栏启动
    private boolean mFromNotify = false;
    //是否从MainActivity启动
    private boolean mFromMainActivity = false;
    //是否是后退按钮
    private boolean mFromBack = false;
    //标题颜色
    public static int mHColor = Color.BLACK;
    //内容颜色
    public static int mLColor = Color.GRAY;
    //更新背景与专辑封面的Handler
    private Handler mBlurHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.UPDATE_BG) {
                long start = System.currentTimeMillis();
                //第一次更新不启用动画
                if(!mFistStart)
                    mContainer.startAnimation(mAnimOut);
                else
                    changeColor();
                LogUtil.d(TAG,"duration:" + (System.currentTimeMillis() - start));
                //更新专辑封面
                ((CoverFragment) mAdapter.getItem(1)).UpdateCover(mInfo,!mFistStart);
                if(mFistStart)
                    mFistStart = false;

            }
        }
    };
    //更新进度条的Handler
    public  Handler mProgressHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //如果当前正在播放，参数合法且用户没有在拖动进度条，更新进度条与时间
            if(mHasPlay != null
                    && mRemainPlay != null
                    && mCurrentTime > 0
                    && (mDuration - mCurrentTime) > 0){
                mHasPlay.setText(CommonUtil.getTime(mCurrentTime));
                mRemainPlay.setText(CommonUtil.getTime(mDuration - mCurrentTime));
            }
            if(msg.what == Constants.UPDATE_TIME_ALL && mSeekBar != null && !mIsDragSeekBar)
                mSeekBar.setProgress(mCurrentTime);
        }
    };


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    @Override
    protected void setUpTheme() {
        super.setUpTheme();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_holder);
        ButterKnife.bind(this);
        mInstance = this;

        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        mFromMainActivity =  getIntent().getBooleanExtra("FromMainActivity",false);
        //获是否正在播放和正在播放的歌曲
        mInfo = MusicService.getCurrentMP3();
        mIsPlay = MusicService.getIsplay();

        MusicService.addCallback(this);
        //初始化动画相关
        initAnim();
        //初始化顶部信息
        initTop();
        //初始化三个指示标志
        initGuide();
        //初始化ViewPager
        initPager();
        //初始化seekbar以及播放时间
        initSeekBar();
        //初始化控制按钮
        initControlButton();
    }

    /**
     * 上一首 下一首 播放、暂停
     * @param v
     */
    @OnClick({R.id.playbar_next,R.id.playbar_prev,R.id.playbar_play})
    public void onCtrlClick(View v){
        Intent intent = new Intent(Constants.CTL_ACTION);
        switch (v.getId()) {
            case R.id.playbar_prev:
                intent.putExtra("Control", Constants.PREV);
                break;
            case R.id.playbar_next:
                intent.putExtra("Control", Constants.NEXT);
                break;
            case R.id.playbar_play:
                intent.putExtra("Control", Constants.PLAYORPAUSE);
                if(mSwatch != null)
                    Theme.TintDrawable(mPlayBarPlay,!mIsPlay ? R.drawable.play_btn_play : R.drawable.play_btn_stop,mColorDraken);

                break;
        }
        MobclickAgent.onEvent(this,v.getId() == R.id.playbar_prev ? "Prev" : v.getId() == R.id.playbar_next ? "Next" : "Play");
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
                MusicService.setPlayModel(currentmodel);
                Theme.TintDrawable(mPlayModel,currentmodel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                        currentmodel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                                R.drawable.play_btn_loop_one,mColorDraken);

                String msg = currentmodel == Constants.PLAY_LOOP ? getString(R.string.model_normal) :
                        currentmodel == Constants.PLAY_SHUFFLE ? getString(R.string.model_random) : getString(R.string.model_repeat);
                ToastUtil.show(AudioHolderActivity.this,msg);
                break;
            //打开正在播放列表
            case R.id.playbar_playinglist:
                MobclickAgent.onEvent(this,"PlayingList");
                startActivity(new Intent(AudioHolderActivity.this,PlayQueueDialog.class));
                break;
            //关闭
            case R.id.top_hide:
                mFromBack = true;
                finish();
                break;
            //弹出窗口
            case R.id.top_more:
                Context wrapper = new ContextThemeWrapper(AudioHolderActivity.this,R.style.PopupMenuDayStyle);
                final PopupMenu popupMenu = new PopupMenu(wrapper,v, Gravity.TOP);
                popupMenu.getMenuInflater().inflate(R.menu.audio_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new AudioPopupListener(AudioHolderActivity.this,mInfo));
                popupMenu.show();
                break;
        }
    }

    private void initAnim() {
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;
        mAnimIn = (AlphaAnimation)AnimationUtils.loadAnimation(this,R.anim.audio_bg_in);
        mAnimIn.setFillAfter(true);
        mAnimOut = (AlphaAnimation)AnimationUtils.loadAnimation(this,R.anim.audio_bg_out);
        mAnimOut.setFillAfter(true);
        mAnimOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                changeColor();
                mContainer.startAnimation(mAnimIn);
//                if(mNewBitMap != null){
//                    mContainer.setBackground(new BitmapDrawable(getResources(), mNewBitMap));
//                    mContainer.startAnimation(mAnimIn);
//                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        new SwatchThread().start();
    }


    private void initControlButton() {
        //初始化播放模式
        int playmodel = SPUtil.getValue(this,"Setting", "PlayModel",Constants.PLAY_LOOP);
        mPlayModel.setImageDrawable(getResources().getDrawable(playmodel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                playmodel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                        R.drawable.play_btn_loop_one));
    }

    @Override
    public void onResume() {
        super.onResume();
        //更新界面
        if(MusicService.getCurrentMP3().getId() != mInfo.getId()) {
            UpdateUI(MusicService.getCurrentMP3(), MusicService.getIsplay());
        }
        mIsRunning = true;
        //更新进度条
        new ProgeressThread().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRawBitMap != null && !mRawBitMap.isRecycled())
            mRawBitMap.recycle();
    }

    private void initGuide() {
        mGuideList = new ArrayList<>();
        mGuideList.add((ImageView) findView(R.id.guide_01));
        mGuideList.add((ImageView) findViewById(R.id.guide_02));
        mGuideList.add((ImageView) findViewById(R.id.guide_03));
    }

    private void initSeekBar() {
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
                if(fromUser)
                    mProgressHandler.sendEmptyMessage(Constants.UPDATE_TIME_ONLY);
                if(mLrcView != null)
                    mLrcView.seekTo(progress,fromUser);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragSeekBar = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //没有播放拖动进度条无效
                if(!mIsPlay){
                    seekBar.setProgress(0);
                }
                MusicService.setProgress(seekBar.getProgress());
                mIsDragSeekBar = false;
            }
        });
    }

    /**
     *
     */
    public void setMP3Item(MP3Item mp3Item){
        if(mp3Item != null)
            mInfo = mp3Item;
    }

    /**
     * 更新顶部歌曲信息
     * @param mp3Item
     */
    public void UpdateTopStatus(MP3Item mp3Item) {
        if(mp3Item == null)
            return;
        String title = mp3Item.getTitle() == null ? "" : mp3Item.getTitle();
        String artist =  mp3Item.getArtist() == null ? "" : mp3Item.getArtist();
        String album =  mp3Item.getAlbum() == null ? "" : mp3Item.getAlbum();

        if(title.equals(""))
            mTopTitle.setText(getString(R.string.unknow_song));
        else
            mTopTitle.setText(title);
        if(artist.equals(""))
            mTopDetail.setText(mp3Item.getAlbum());
        else if(album.equals(""))
            mTopDetail.setText(mp3Item.getArtist());
        else
            mTopDetail.setText(mp3Item.getArtist() + "-" + mp3Item.getAlbum());
    }

    public void UpdatePlayButton(boolean isPlay) {
        if(mPlayBarPlay != null)
            Theme.TintDrawable(mPlayBarPlay,!isPlay ? R.drawable.play_btn_play : R.drawable.play_btn_stop,mColorDraken);
    }

    private void initTop() {
        //初始化顶部信息
        UpdateTopStatus(mInfo);
    }

    private void initRecordFragment() {
        RecordFragment fragment = new RecordFragment();
        mAdapter.AddFragment(fragment);
    }

    private void initCoverFragment() {
        CoverFragment fragment = new CoverFragment();
        fragment.setArguments(mBundle);
        mAdapter.AddFragment(fragment);
    }

    private void initLrcFragment() {
        LrcFragment fragment = new LrcFragment();
        fragment.setOnFindListener(new LrcFragment.OnLrcViewFindListener() {
            @Override
            public void onLrcViewFind(LrcView lrcView) {
                mLrcView = lrcView;
                mLrcView.setOnSeekListener(new onSeekListener() {
                    @Override
                    public void onLrcSeek(int newProgress) {
                        if(newProgress > 0 && newProgress < MusicService.getDuration()){
                            MusicService.setProgress(newProgress);
                            mCurrentTime = newProgress;
                            mProgressHandler.sendEmptyMessage(Constants.UPDATE_TIME_ALL);
                        }
                    }
                });
            }
        });
        fragment.setArguments(mBundle);
        mAdapter.AddFragment(fragment);
    }

    private void initPager() {
        //初始化Viewpager
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mBundle = new Bundle();
        mBundle.putSerializable("MP3Item", mInfo);
        initRecordFragment();
        initCoverFragment();
        initLrcFragment();
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if(mLrcView != null){
                    mLrcView.setViewPagerScroll(true);
                }
            }
            @Override
            public void onPageSelected(int position) {
                mGuideList.get(mPrevPosition).setImageResource(R.drawable.play_icon_unselected_dot);
                mGuideList.get(position).setImageResource(R.drawable.play_icon_selected_dot);
                mPrevPosition = position;
                if(position == 0)
                    mPager.setIntercept(true);
                else
                    mPager.setIntercept(false);
                if(mLrcView != null)
                    mLrcView.setViewPagerScroll(false);
            }
            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mPager.setCurrentItem(1);
    }
    @Override
    protected void onStart() {
        super.onStart();
        //只有从Mactivity启动，才使用动画
        if(!mFromNotify && mFromMainActivity) {
            overridePendingTransition(R.anim.slide_bottom_in, 0);
            mFromMainActivity = false;
        }
    }

    @Override
    public void onBackPressed() {
        mFromBack = true;
        super.onBackPressed();
    }

    @Override
    public void finish() {
        super.finish();
        //只有后退到MainActivity,才使用动画
        if(mFromBack) {
            overridePendingTransition(0, R.anim.slide_bottom_out);
            mFromBack = false;
        }
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay){
        mInfo = MP3Item;
        mIsPlay = isplay;

        //当操作不为播放或者暂停且正在运行时，更新界面
        if(Global.getOperation() != Constants.PLAYORPAUSE && mInfo != null ) {
            //更新顶部信息
            UpdateTopStatus(mInfo);
            //更新歌词
            ((LrcFragment) mAdapter.getItem(2)).UpdateLrc(mInfo);

            //更新进度条
            int temp = MusicService.getProgress();
            mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
            mDuration = (int) mInfo.getDuration();
            mSeekBar.setMax(mDuration);

            new SwatchThread().start();
        }
        //操作为播放选中歌曲时不更新背景
        //只更新按钮状态
        else{
            //更新按钮状态
            UpdatePlayButton(isplay);
        }

    }

    @Override
    public int getType() {
        return Constants.AUDIOHOLDERACTIVITY;
    }

    //更新进度条线程
    class ProgeressThread extends Thread {
        @Override
        public void run() {
            while (mIsRunning) {
                int temp = MusicService.getProgress();
                if (MusicService.getIsplay() && temp > 0 && temp < mDuration) {
                    mCurrentTime = temp;
                    mProgressHandler.sendEmptyMessage(Constants.UPDATE_TIME_ALL);
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
     * 修改所有控件颜色
     */
    private void changeColor(){
        //修改颜色
        if(mTopDetail != null && mTopTitle != null && mRawBitMap != null){
            //修改背景颜色
            mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{mColorFrom, mColorTo}));

            //修改顶部字体颜色
            mTopTitle.setTextColor(mColorDraken);
            mTopDetail.setTextColor(mColorDark);

            //锁屏界面字体颜色
            if(mLrcView != null){
                mLrcView.setHightLightColor(ColorUtil.adjustAlpha(mSwatch.getRgb(),0.9f));
                mLrcView.setNormalColor(ColorUtil.adjustAlpha(mSwatch.getRgb(),0.4f));
                mLrcView.setHorizontalColor(ColorUtil.adjustAlpha(mSwatch.getRgb(),0.4f));
            }
//            mHColor =  mSwatch.getTitleTextColor();
//            mLColor = mSwatch.getBodyTextColor();

            LayerDrawable layerDrawable =  (LayerDrawable) mSeekBar.getProgressDrawable();
            //修改track颜色
            ((GradientDrawable)layerDrawable.getDrawable(0)).setColor(ColorUtil.adjustAlpha(mSwatch.getRgb(),0.3f));
            //修改progress颜色
            (layerDrawable.getDrawable(1)).setColorFilter(mColorDraken, PorterDuff.Mode.SRC_IN);
            mSeekBar.setProgressDrawable(layerDrawable);

            //修改thumb颜色
            // Drawable drawable = mSeekBar.getThumb();
            Drawable drawable = getResources().getDrawable(R.drawable.thumb);
            Theme.TintDrawable(drawable,mColorDraken);
            mSeekBar.setThumb(drawable);

            //修改顶部按钮颜色
            Theme.TintDrawable(mTopHide,R.drawable.play_btn_back,mColorDraken);
            Theme.TintDrawable(mTopMore,R.drawable.list_icn_more,mColorDraken);

            //修改控制按钮颜色
            Theme.TintDrawable(mPlayBarNext,R.drawable.play_btn_next,mColorDraken);
            Theme.TintDrawable(mPlayBarPrev,R.drawable.play_btn_pre,mColorDraken);

            int currentmodel = MusicService.getPlayModel();
            Theme.TintDrawable(mPlayModel,currentmodel == Constants.PLAY_LOOP ? R.drawable.play_btn_loop :
                    currentmodel == Constants.PLAY_SHUFFLE ? R.drawable.play_btn_shuffle :
                            R.drawable.play_btn_loop_one,mColorDraken);
            Theme.TintDrawable(mPlayBarPlay,!mIsPlay ? R.drawable.play_btn_play : R.drawable.play_btn_stop,mColorDraken);
            Theme.TintDrawable(mPlayingList,R.drawable.play_btn_normal_list,mColorDraken);
        }
    }


    class SwatchThread extends Thread{
        @Override
        public void run() {
            if(mInfo != null){
                mRawBitMap = MediaStoreUtil.getAlbumBitmapBySongId(mInfo.getId(),false);
                if(mRawBitMap == null)
                    mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.no_art_normal);

                /** start*/
                Palette palette = new Palette.Builder(mRawBitMap).generate();
                mSwatch = palette.getMutedSwatch();
                if(mSwatch == null)
                    mSwatch = new Palette.Swatch(Color.GRAY,100);
                mColorFrom = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.4f);
                mColorTo = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.05f);
//                mColorDraken = ColorUtil.shiftColor(mSwatch.getRgb(),0.8f);
//                mColorDark = mSwatch.getRgb();
                mColorDraken = mSwatch.getRgb();
                mColorDark = ColorUtil.adjustAlpha(mSwatch.getRgb(),0.8f);
                //锁屏界面字体颜色
                mHColor =  mSwatch.getTitleTextColor();
                mLColor = mSwatch.getBodyTextColor();
                /** end */
                mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
            }
        }
    }

}
