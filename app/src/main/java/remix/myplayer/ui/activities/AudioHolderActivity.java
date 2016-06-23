package remix.myplayer.ui.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
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
import android.widget.Toast;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.fragments.CoverFragment;
import remix.myplayer.fragments.LrcFragment;
import remix.myplayer.fragments.RecordFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.listeners.CtrlButtonListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.AudioViewPager;
import remix.myplayer.ui.customviews.LrcView;
import remix.myplayer.ui.dialog.PlayingListDialog;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.Global;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放界面
 */
public class AudioHolderActivity extends BaseAppCompatActivity implements MusicService.Callback{
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
    //拖动进度条与更新进度条的互斥量
    public static boolean mIsDragSeekBar = false;
    //顶部信息
    @ViewInject(R.id.top_title)
    private TextView mTopTitle;
    @ViewInject(R.id.top_detail)
    private TextView mTopDetail;
    //隐藏按钮
    @ViewInject(R.id.top_hide)
    private ImageButton mHide;
    //播放控制
    @ViewInject(R.id.playbar_prev)
    private ImageButton mPlayBarPrev;
    @ViewInject(R.id.playbar_play)
    private ImageButton mPlayBarPlay;
    @ViewInject(R.id.playbar_next)
    private ImageButton mPlayBarNext;
    @ViewInject(R.id.playbar_model)
    private ImageButton mPlayModel;
    @ViewInject(R.id.playbar_playinglist)
    private ImageButton mPlayingList;
    //已播放时间和剩余播放时间
    @ViewInject(R.id.text_hasplay)
    private TextView mHasPlay;
    @ViewInject(R.id.text_remain)
    private TextView mRemainPlay;
    //进度条
    @ViewInject(R.id.seekbar)
    private SeekBar mSeekBar;
    //背景
    @ViewInject(R.id.audio_holder_container)
    private FrameLayout mContainer;
    @ViewInject(R.id.holder_pager)
    private AudioViewPager mPager;


    //Viewpager
    private PagerAdapter mAdapter;

    private Bundle mBundle;
    private ArrayList<ImageView> mGuideList;

    //当前播放的歌曲
    private MP3Info mInfo;
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
                Log.d(TAG,"duration:" + (System.currentTimeMillis() - start));
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
    public int getLayoutId() {
        return R.layout.activity_audio_holder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        mFromMainActivity =  getIntent().getBooleanExtra("FromMainActivity",false);
        //获是否正在播放和正在播放的歌曲
        mInfo = MusicService.getCurrentMP3();
        mIsPlay = MusicService.getIsplay();
        try {
            MusicService.addCallback(this);
            //初始化动画相关
            initAnim();
            //初始化顶部信息
            initTop();
            //初始化顶部两个按钮
            initTopButton();
            //初始化三个指示标志
            initGuide();
            //初始化ViewPager
            initPager();
            //初始化seekbar以及播放时间
            initSeekBar();
            //初始化控制按钮
            initControlButton();

        } catch (Exception e){
            e.printStackTrace();
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

        new BitmapThread().start();
    }


    private void initControlButton() {
        //前进，播放，后退
        UpdatePlayButton(mIsPlay);
        CtrlButtonListener mListener = new CtrlButtonListener(getApplicationContext());
        mPlayBarPrev.setOnClickListener(mListener);
        mPlayBarPlay.setOnClickListener(mListener);
        mPlayBarNext.setOnClickListener(mListener);

        //初始化播放模式
        int playmodel = SharedPrefsUtil.getValue(this,"setting", "PlayModel",Constants.PLAY_LOOP);
        mPlayModel.setImageDrawable(getResources().getDrawable(playmodel == Constants.PLAY_LOOP ? R.drawable.bg_btn_holder_playmodel_normal :
                playmodel == Constants.PLAY_SHUFFLE ? R.drawable.bg_btn_holder_playmodel_shuffle :
                        R.drawable.bg_btn_holder_playmodel_repeat));

        mPlayModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentmodel = MusicService.getPlayModel();
                currentmodel = (currentmodel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP : ++currentmodel);
                MusicService.setPlayModel(currentmodel);
                mPlayModel.setImageDrawable(getResources().getDrawable(currentmodel == Constants.PLAY_LOOP ? R.drawable.bg_btn_holder_playmodel_normal :
                        currentmodel == Constants.PLAY_SHUFFLE ? R.drawable.bg_btn_holder_playmodel_shuffle :
                                R.drawable.bg_btn_holder_playmodel_repeat));
                String msg = currentmodel == Constants.PLAY_LOOP ? getString(R.string.model_normal) :
                        currentmodel == Constants.PLAY_SHUFFLE ? getString(R.string.model_random) : getString(R.string.model_repeat);
                Toast.makeText(AudioHolderActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });


        mPlayingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AudioHolderActivity.this,PlayingListDialog.class));
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        //更新界面
        if(MusicService.getCurrentMP3() != mInfo)
            UpdateUI(MusicService.getCurrentMP3(),MusicService.getIsplay());
        mIsRunning = true;
        //更新进度条
        new ProgeressThread().start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void initTopButton(){
        mHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFromBack = true;
                finish();
            }
        });
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
        final int temp = MusicService.getCurrentTime();
        mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
        Log.d(TAG,"Duration:" + mDuration + "  CurrentTime:" + mCurrentTime);

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
                if(LrcView.mInstance != null)
                    LrcView.mInstance.seekTo(progress,fromUser);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsDragSeekBar = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MusicService.setProgress(seekBar.getProgress());
                seekBar.setProgress(seekBar.getProgress());
                mIsDragSeekBar = false;
            }
        });
    }



    //更新顶部信息
    public void UpdateTopStatus(MP3Info mp3Info) {
        if(mp3Info == null)
            return;
        String title = mp3Info.getDisplayname() == null ? "" : mp3Info.getDisplayname();
        String artist =  mp3Info.getArtist() == null ? "" : mp3Info.getArtist();
        String album =  mp3Info.getAlbum() == null ? "" : mp3Info.getAlbum();

        if(title.equals(""))
            mTopTitle.setText(getString(R.string.unknow_song));
        else
            mTopTitle.setText(title);
        if(artist.equals(""))
            mTopDetail.setText(mp3Info.getAlbum());
        else if(album.equals(""))
            mTopDetail.setText(mp3Info.getArtist());
        else
            mTopDetail.setText(mp3Info.getArtist() + "-" + mp3Info.getAlbum());
    }

    public void UpdatePlayButton(boolean isPlay) {
        if(isPlay)
            mPlayBarPlay.setImageResource(R.drawable.bg_btn_holder_stop);
        else
            mPlayBarPlay.setImageResource(R.drawable.bg_btn_holder_play);
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
        fragment.setArguments(mBundle);
        mAdapter.AddFragment(fragment);
    }

    private void initPager() {
        //初始化Viewpager
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mBundle = new Bundle();
        mBundle.putSerializable("MP3Info", mInfo);
        initRecordFragment();
        initCoverFragment();
        initLrcFragment();
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
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
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        mInfo = MP3info;
        mIsPlay = isplay;

        //当操作不为播放或者暂停且正在运行时，更新界面
        if(Global.getOperation() != Constants.PLAYORPAUSE && mInfo != null ) {
            try {
                //更新顶部信息
                UpdateTopStatus(mInfo);
                //更新按钮状态
                UpdatePlayButton(isplay);
                //更新歌词
                ((LrcFragment) mAdapter.getItem(2)).UpdateLrc(mInfo);

                //更新进度条
                int temp = MusicService.getCurrentTime();
                mCurrentTime = temp > 0 && temp < mDuration ? temp : 0;
                mDuration = (int) mInfo.getDuration();
                mSeekBar.setMax(mDuration);
                //操作为播放选中歌曲时不更新背景
//                if(mOperation != Constants.PLAYSELECTEDSONG)
                    new BitmapThread().start();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        //只更新按钮状态
        else if(mIsRunning)
            //更新按钮状态
            UpdatePlayButton(isplay);

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
                int temp = MusicService.getCurrentTime();
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

    private void changeColor(){
        //修改颜色
        if(mTopDetail != null && mTopTitle != null && mRawBitMap != null){
            Palette.from(mRawBitMap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    Palette.Swatch f = palette.getLightMutedSwatch();//柔和 亮色

                    if(f != null){
                        //修改顶部字体颜色
                        mTopTitle.setTextColor(f.getBodyTextColor());
                        mTopDetail.setTextColor(f.getTitleTextColor());
                        //修改背景颜色
                        mContainer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{f.getRgb(), Color.WHITE}));

                        mHColor =  f.getTitleTextColor();
                        mLColor = f.getBodyTextColor();
                        //修改进度条背景颜色
                        LayerDrawable layerDrawable =  (LayerDrawable) mSeekBar.getProgressDrawable();
                        ((GradientDrawable)layerDrawable.getDrawable(0)).setColor(f.getRgb());
                        mSeekBar.setProgressDrawable(layerDrawable);

                    }

                }
            });
        }
    }

    //高斯模糊线程
    class BitmapThread extends Thread{
        @Override
        public void run() {
            if(mInfo != null){
                mRawBitMap = DBUtil.CheckBitmapBySongId((int) mInfo.getId(),false);
                if(mRawBitMap == null)
                    mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.no_art_normal);
                mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
            }
        }
    }

}
