package remix.myplayer.activities;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.ViewPager;

import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.fragments.CoverFragment;
import remix.myplayer.fragments.LrcFragment;
import remix.myplayer.fragments.RecordFragment;
import remix.myplayer.services.MusicService;

import remix.myplayer.ui.AudioViewPager;
import remix.myplayer.ui.PlayingListPopupWindow;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2015/12/1.
 */
public class AudioHolderActivity extends AppCompatActivity implements MusicService.Callback{
    public static AudioHolderActivity mInstance = null;
    public static String mFLAG = "CHILD";
    //记录操作是下一首还是上一首
    public static int mOperation = -1;
    //是否正在运行
    public static boolean mIsRun;
    //当前上次选中的Fragment
    private int mPrevPosition = 1;
    //是否播放的标志变量
    public static boolean mIsPlay = false;
    //第一次启动的标志变量
    private static boolean mFlag = true;
    //用于更新进度条的定时器
    private Timer mTimer;
    //拖动进度条与更新进度条的互斥量
    private boolean misChanging = false;
    //标记拖动进度条之前是否在播放
    private boolean mPrevIsPlay;
    private TextView mTopTitle;
    private TextView mTopDetail;
    private ImageButton mHide;
    private AudioViewPager mPager;
    private TextView mHasPlay;
    private TextView mRemainPlay;
    private SeekBar mSeekBar;
    private ImageButton mPlayBarPrev;
    private ImageButton mPlayBarPlay;
    private ImageButton mPlayBarNext;
    private ImageButton mPlayModel;
    private ImageButton mPlayingList;
    private MP3Info mInfo;
    private PagerAdapter mAdapter;
    private MusicService mService;
    private Bundle mBundle;
    private ArrayList<ImageView> mGuideList;
    private int mCurrent;
    private int mDuration;
    private LinearLayout mContainer;
    private TextView mNextText = null;
    public static int mWidth;
    public static int mHeight;
    private AlphaAnimation mAnimIn;
    private AlphaAnimation mAnimOut;
    private Bitmap mNewBitMap;
    private boolean mFromNotify = false;
    private MusicService.PlayerReceiver mMusicReceiver;
    private Handler mBlurHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == Constants.UPDATE_BG) {
                //对专辑图片进行高斯模糊，并将其设置为背景
                float radius = 25;
                float scaleFactor = 12;
                if (mWidth > 0 && mHeight > 0 ) {
                    if(mInfo == null) return;
                    Bitmap bkg = DBUtil.CheckBitmapBySongId((int) mInfo.getId(),false);
                    if (bkg == null)
                        bkg = BitmapFactory.decodeResource(getResources(), R.drawable.bg_lockscreen_default);
                    mNewBitMap = Bitmap.createBitmap((int) (mWidth / scaleFactor), (int) (mHeight / scaleFactor), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(mNewBitMap);
//                    canvas.translate(-mContainer.getLeft() / scaleFactor, -mContainer.getTop() / scaleFactor);
//                    canvas.scale(1 / scaleFactor, 1 / scaleFactor);
                    Paint paint = new Paint();
                    paint.setFlags(Paint.FILTER_BITMAP_FLAG);
                    paint.setAlpha((int)(255 * 0.3));
                    canvas.drawBitmap(bkg, 0, 0, paint);
                    mNewBitMap = CommonUtil.doBlur(mNewBitMap, (int) radius, true);
                    //获得当前背景
//                    mContainer.startAnimation(mAnimOut);
                    mContainer.setBackground(new BitmapDrawable(getResources(), mNewBitMap));

                }
            }
        }
    };


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.audio_holder);

        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        //如果是从通知栏启动,关闭通知栏
        if(mFromNotify){
            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(0);
        }

        mInfo = MusicService.getCurrentMP3();
        mIsPlay = MusicService.getIsplay();
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
        //初始化三个控制按钮
        initButton();
        //初始化底部四个按钮
        initBottomButton();
    }

    private void initAnim() {
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mWidth = (int)(metrics.widthPixels  * 0.95);
        mHeight = (int)(metrics.heightPixels * 7 / 9.5);
//        mHeight = metrics.heightPixels;
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
                if(mNewBitMap != null){
                    mContainer.setBackground(new BitmapDrawable(getResources(), mNewBitMap));
                    mContainer.startAnimation(mAnimIn);
                }
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        mContainer = (LinearLayout)findViewById(R.id.audio_holder_container);
        mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
    }




    private void initBottomButton()
    {
        mPlayModel = (ImageButton)findViewById(R.id.playbar_model);
        mPlayModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentmodel = MusicService.getPlayModel();
                currentmodel = (currentmodel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP : ++currentmodel);
                MusicService.setPlayModel(currentmodel);
                mPlayModel.setBackground(getResources().getDrawable(currentmodel == Constants.PLAY_LOOP ? R.drawable.bg_btn_holder_playmodel_normal :
                                        currentmodel == Constants.PLAY_SHUFFLE ? R.drawable.bg_btn_holder_playmodel_shuffle :
                                        R.drawable.bg_btn_holder_playmodel_repeat,getTheme()));
                String msg = currentmodel == Constants.PLAY_LOOP ? "顺序播放" : currentmodel == Constants.PLAY_SHUFFLE ? "随机播放" : "单曲播放";
                Toast.makeText(AudioHolderActivity.this,msg,Toast.LENGTH_SHORT).show();
            }
        });
        mPlayingList = (ImageButton)findViewById(R.id.playbar_playinglist);
        mPlayingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AudioHolderActivity.this,PlayingListPopupWindow.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ProgeressThread thread = new ProgeressThread();
        thread.start();
        mIsRun = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRun = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    class ProgeressThread extends Thread {
        @Override
        public void run() {
            while (mIsRun && mInfo != null) {
                if (MusicService.getIsplay()) {
                    mCurrent = MusicService.getCurrentTime();
                    mHandler.sendEmptyMessage(Constants.UPDATE_TIME_ALL);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public  Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            mHasPlay.setText(CommonUtil.getTime(mCurrent));
            mRemainPlay.setText(CommonUtil.getTime(mDuration - mCurrent));
            if(msg.what == Constants.UPDATE_TIME_ALL)
                mSeekBar.setProgress(mCurrent);
        }
    };

    private void initTopButton()
    {
        mHide = (ImageButton)findViewById(R.id.top_hide);
        mHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void initGuide()
    {
        mGuideList = new ArrayList<>();
        mGuideList.add((ImageView) findViewById(R.id.guide_01));
        mGuideList.add((ImageView) findViewById(R.id.guide_02));
        mGuideList.add((ImageView) findViewById(R.id.guide_03));
    }

    private void initButton()
    {
        mPlayBarPrev = (ImageButton)findViewById(R.id.playbar_prev);
        mPlayBarPlay = (ImageButton)findViewById(R.id.playbar_play);
        mPlayBarNext = (ImageButton)findViewById(R.id.playbar_next);
        UpdatePlayButton(mIsPlay);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Constants.CTL_ACTION);
                switch (v.getId())
                {
                    case R.id.playbar_prev:
                        intent.putExtra("Control", Constants.PREV);
                        mOperation = Constants.PREV;
                        break;
                    case R.id.playbar_next:
                        mOperation = Constants.NEXT;
                        intent.putExtra("Control", Constants.NEXT);
                        break;
                    case R.id.playbar_play:
                        mOperation = Constants.PLAY;
                        intent.putExtra("Control", Constants.PLAY);
                        break;
                }
                sendBroadcast(intent);
            }
        };
        mPlayBarPrev.setOnClickListener(listener);
        mPlayBarPlay.setOnClickListener(listener);
        mPlayBarNext.setOnClickListener(listener);
    }

    private void initSeekBar()
    {
        if(mInfo == null)
            return;
        mDuration = (int)mInfo.getDuration();
        mCurrent = MusicService.getCurrentTime();
        //初始化已播放时间与剩余时间
        mHasPlay = (TextView)findViewById(R.id.text_hasplay);
        mRemainPlay = (TextView)findViewById(R.id.text_remain);
        mHasPlay.setText(CommonUtil.getTime(mCurrent));
        mRemainPlay.setText(CommonUtil.getTime(mDuration - mCurrent));

        mSeekBar = (SeekBar)findViewById(R.id.seekbar);
        mSeekBar.setMax((int) mInfo.getDuration());
        mSeekBar.setProgress(MusicService.getCurrentTime());
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser)
                    mHandler.sendEmptyMessage(Constants.UPDATE_TIME_ONLY);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                misChanging = true;
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MusicService.setProgress(seekBar.getProgress());
                seekBar.setProgress(seekBar.getProgress());
                misChanging = false;
            }
        });
    }

    //更新顶部信息
    public void UpdateTopStatus(MP3Info mp3Info)
    {
        if(mp3Info == null)
            return;
        String title = mp3Info.getDisplayname() == null ? "" : mp3Info.getDisplayname();
        String artist =  mp3Info.getArtist() == null ? "" : mp3Info.getArtist();
        String album =  mp3Info.getAlbum() == null ? "" : mp3Info.getAlbum();

        if(title.equals(""))
            mTopTitle.setText("未知歌曲");
        else
            mTopTitle.setText(title);
        if(artist.equals(""))
            mTopDetail.setText(mp3Info.getAlbum());
        else if(album.equals(""))
            mTopDetail.setText(mp3Info.getArtist());
        else
            mTopDetail.setText(mp3Info.getArtist() + "-" + mp3Info.getAlbum());
    }
    public void UpdatePlayButton(boolean isPlay)
    {
        if(isPlay)
            mPlayBarPlay.setImageResource(R.drawable.play_btn_stop);
        else
            mPlayBarPlay.setImageResource(R.drawable.play_btn_paly);
    }
    private void initTop()
    {
        //初始化顶部信息
        mTopTitle = (TextView)findViewById(R.id.top_title);
        mTopDetail = (TextView)findViewById(R.id.top_detail);
        UpdateTopStatus(mInfo);
    }
    private void initRecordFragment()
    {
        RecordFragment fragment = new RecordFragment();
        mAdapter.AddFragment(fragment);
    }
    private void initCoverFragment()
    {
        CoverFragment fragment = new CoverFragment();
        fragment.setArguments(mBundle);
        mAdapter.AddFragment(fragment);
    }
    private void initLrcFragment()
    {
        LrcFragment fragment = new LrcFragment();
        fragment.setArguments(mBundle);
        mAdapter.AddFragment(fragment);
    }

    private void initPager()
    {
        //初始化Viewpager
        mPager = (AudioViewPager)findViewById(R.id.holder_pager);
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
        if(!mFromNotify)
            overridePendingTransition(R.anim.slide_bottom_in, 0);
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }
    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        mInfo= MP3info;
        mIsPlay = isplay;

        if(mOperation != Constants.PLAY && mIsRun) {
            //更新顶部信息
            UpdateTopStatus(mInfo);
            //更新按钮状态
            UpdatePlayButton(isplay);
            //更新歌词
            ((LrcFragment) mAdapter.getItem(2)).UpdateLrc(mInfo);
            //更新专辑封面
            ((CoverFragment) mAdapter.getItem(1)).UpdateCover(DBUtil.CheckBitmapBySongId((int)MP3info.getId(),false));
            //更新进度条
            mCurrent = MusicService.getCurrentTime();
            mDuration = (int) mInfo.getDuration();
            mSeekBar.setMax(mDuration);
            mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);

        }
        else if(mIsRun)
            //更新按钮状态
            UpdatePlayButton(isplay);

    }

    @Override
    public int getType() {
        return 1;
    }

}
