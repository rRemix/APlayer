package remix.myplayer.activities;

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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.fragments.CoverFragment;
import remix.myplayer.fragments.InformantionFragment;
import remix.myplayer.fragments.LrcFragment;
import remix.myplayer.fragments.RecordFragment;
import remix.myplayer.services.MusicService;

import remix.myplayer.ui.PlayListPopupWindow;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/1.
 */
public class AudioHolderActivity extends AppCompatActivity implements MusicService.Callback{
    public static AudioHolderActivity mInstance;
    public static String mFLAG = "CHILD";
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
    private ViewPager mPager;
    private TextView mHasPlay;
    private TextView mRemainPlay;
    private SeekBar mSeekBar;
    private ImageButton mPlayBarPrev;
    private ImageButton mPlayBarPlay;
    private ImageButton mPlayBarNext;
    private ImageButton mModelLoop;
    private ImageButton mModelShuffle;
    private ImageButton mSearch;
    private ImageButton mFavorite;
    private ImageButton mPlayList;
    private MP3Info mInfo;
    private PagerAdapter mAdapter;
    private MusicService mService;
    private Bundle mBundle;
    private ArrayList<ImageView> mGuideList;
    private int mCurrent;
    private int mDuration;
    private LinearLayout mContainer;
    private ImageView mGuide1;
    private ImageView mGuide2;
    private ImageView mGuide3;
    private TextView mNextText = null;
    public static int mWidth;
    public static int mHeight;
    private Timer mBlurTimer = new Timer();
    private Handler mBlurHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == Utility.UPDATE_BG) {
                //对专辑图片进行高斯模糊，并将其设置为背景
                float radius = 25;
                float scaleFactor = 12;
                if (mWidth > 0 && mHeight > 0 ) {
                    Bitmap bkg = Utility.CheckBitmapBySongId((int) mInfo.getId(),false);
                    if (bkg == null)
                        bkg = BitmapFactory.decodeResource(getResources(), R.drawable.bg_lockscreen_default);
                    Bitmap overlay = Bitmap.createBitmap((int) (mWidth / scaleFactor), (int) (mHeight / scaleFactor), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(overlay);
                    canvas.translate(-mContainer.getLeft() / scaleFactor, -mContainer.getTop() / scaleFactor);
//                canvas.scale(1 / scaleFactor, 1 / scaleFactor);
                    Paint paint = new Paint();
                    paint.setFlags(Paint.FILTER_BITMAP_FLAG);
                    paint.setAlpha((int)(255 * 0.4));
                    canvas.drawBitmap(bkg, 0, 0, paint);
                    overlay = Utility.doBlur(overlay, (int) radius, true);

                    //获得当前背景
//                    Bitmap origin = mContainer.getDrawingCache();
//                    AlphaAnimation in = new AlphaAnimation(1.0f,0.6f);
//                    in.setInterpolator(new LinearInterpolator());
//                    in.setDuration(5000);
//                    in.setFillAfter(true);
//                    mContainer.startAnimation(in);

                    mContainer.setBackground(new BitmapDrawable(getResources(), overlay));
//                    AlphaAnimation out = new AlphaAnimation(0.6f,1.0f);
//                    out.setInterpolator(new LinearInterpolator());
//                    out.setDuration(5000);
//                    out.setFillAfter(true);
//                    mContainer.startAnimation(out);

                }
            }
        }
    };
    private ServiceConnection mConnecting = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.PlayerBinder)service).getService();
            mService.addCallback(AudioHolderActivity.this,1);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mWidth = metrics.widthPixels ;
        mHeight = (int)(metrics.heightPixels * 7 / 10.5);

        RelativeLayout layout = (RelativeLayout)findViewById(R.id.blur_container);

        mInstance = this;
        setContentView(R.layout.audio_holder);
//        mInfo = (MP3Info)getIntent().getExtras().getSerializable("MP3Info");
        mInfo = MusicService.getCurrentMP3();
        mIsPlay = getIntent().getBooleanExtra("Isplay",false);
        Intent intent = new Intent(AudioHolderActivity.this,MusicService.class);
        bindService(intent, mConnecting, Context.BIND_AUTO_CREATE);
        mContainer = (LinearLayout)findViewById(R.id.audio_holder_container);
        mBlurHandler.sendEmptyMessage(Utility.UPDATE_BG);
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
        //初始化下一首歌曲
        initNextSong();
        //初始化三个控制按钮
        initButton();
        //初始化底部四个按钮
        initBottomButton();
    }

    public void initNextSong() {
        if(mNextText == null)
            mNextText = (TextView)findViewById(R.id.next_text);
        MP3Info next = MusicService.getNextMP3();
        if(next != null)
            mNextText.setText("下一首：" + next.getDisplayname());
    }


    private void initBottomButton()
    {
//        mPlayList = (ImageButton)findViewById(R.id.bottom_playlist);
//        mPlayList.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(AudioHolderActivity.this, PlayListPopupWindow.class));
//            }
//        });
        mModelLoop = (ImageButton)findViewById(R.id.play_model_loop);
        mModelShuffle = (ImageButton)findViewById(R.id.play_model_shuffle);
        mModelLoop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MusicService.getPlayModel() == Utility.PLAY_LOOP)
                    return;
                MusicService.setPlayModel(Utility.PLAY_LOOP);
                mModelLoop.setImageResource(R.drawable.play_btn_loop_prs);
                mModelShuffle.setImageResource(R.drawable.play_btn_shuffle);
                Toast.makeText(AudioHolderActivity.this,"顺序播放", Toast.LENGTH_SHORT).show();
            }
        });
        mModelShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MusicService.getPlayModel() == Utility.PLAY_SHUFFLE)
                    return;
                MusicService.setPlayModel(Utility.PLAY_SHUFFLE);
                mModelShuffle.setImageResource(R.drawable.play_btn_shuffle_prs);
                mModelLoop.setImageResource(R.drawable.play_btn_loop);
                Toast.makeText(AudioHolderActivity.this,"随机播放", Toast.LENGTH_SHORT).show();
            }
        });

//        mModel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int PlayModel = MusicService.getPlayModel();
//                mModel.setImageResource(PlayModel == Utility.PLAY_NORMAL ? R.drawable.bg_btn_holder_playmodel_shuffle : R.drawable.bg_btn_holder_playmodel_normal);
//                MusicService.setPlayModel(PlayModel == Utility.PLAY_NORMAL ? Utility.PLAY_SHUFFLE : Utility.PLAY_NORMAL);
//                Toast.makeText(AudioHolderActivity.this,PlayModel == Utility.PLAY_NORMAL ? "随机播放" : "顺序播放",Toast.LENGTH_SHORT).show();
//            }
//        });
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
        unbindService(mConnecting);
    }

    class ProgeressThread extends Thread {
        @Override
        public void run() {
            while (mIsRun && mInfo != null) {
                if (MusicService.getIsplay()) {
                    mCurrent = MusicService.getCurrentTime();
                    mHandler.sendEmptyMessage(Utility.UPDATE_TIME);
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
            if(msg.what == Utility.UPDATE_TIME)
            {
                mSeekBar.setProgress(mCurrent);
                mHasPlay.setText(Utility.getTime(mCurrent));
                mRemainPlay.setText(Utility.getTime(mDuration - mCurrent));
            }
        }
    };

    private void initTopButton()
    {
        mHide = (ImageButton)findViewById(R.id.top_hide);
//        mTopSetting = (ImageButton)findViewById(R.id.top_setting);
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
//        CtrlButtonListener listener = new CtrlButtonListener(getApplicationContext());
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Utility.CTL_ACTION);
                switch (v.getId())
                {
                    case R.id.playbar_prev:
                        intent.putExtra("Control", Utility.PREV);
                        break;
                    case R.id.playbar_next:
                        intent.putExtra("Control", Utility.NEXT);
                        break;
                    case R.id.playbar_play:
                        intent.putExtra("Control", Utility.PLAY);
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
        mHasPlay.setText(Utility.getTime(mCurrent));
        mRemainPlay.setText(Utility.getTime(mDuration - mCurrent));

        mSeekBar = (SeekBar)findViewById(R.id.seekbar);
        mSeekBar.setMax((int) mInfo.getDuration());
        mSeekBar.setProgress(MusicService.getCurrentTime());

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && (Math.abs(mCurrent - progress) > 300)) {
                    mSeekBar.setProgress(progress);
                    MusicService.setProgress(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                misChanging = true;
                //记录下是否播放，如果正在播放则暂停
                mPrevIsPlay = MusicService.getIsplay();
                if (mPrevIsPlay) {
                    Intent intent = new Intent(Utility.CTL_ACTION);
                    intent.putExtra("Control", Utility.PAUSE);
                    sendBroadcast(intent);
                }
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                misChanging = false;
                //如果之前正在播放，则继续播放
                if (mPrevIsPlay) {
                    Intent intent = new Intent(Utility.CTL_ACTION);
                    intent.putExtra("Control", Utility.CONTINUE);
                    sendBroadcast(intent);
                }
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
    private void initInformationFragment()
    {
//        InformantionFragment fragment = new InformantionFragment();
//        fragment.setArguments(mBundle);
//        mAdapter.AddFragment(fragment);
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
        mPager = (ViewPager)findViewById(R.id.holder_pager);
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mBundle = new Bundle();
        mBundle.putSerializable("MP3Info", mInfo);
        initInformationFragment();
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
        overridePendingTransition(R.anim.slide_bottom_in, 0);
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_bottom_out);
    }
    @Override
    public void getCurrentInfo(MP3Info MP3info,boolean isplay){
        if(mInfo.getId() != MP3info.getId())
            mBlurHandler.sendEmptyMessage(Utility.UPDATE_BG);
        mInfo= MP3info;
        mIsPlay = isplay;
        if(mIsRun) {
            //更新详情页信息
//            ((InformantionFragment) mAdapter.getItem(0)).UpdateInformation(mInfo);
            //更新顶部信息
            UpdateTopStatus(mInfo);
            //更新按钮状态
            UpdatePlayButton(isplay);
            //更新歌词
            ((LrcFragment) mAdapter.getItem(2)).UpdateLrc(mInfo);
            //更新专辑封面
            ((CoverFragment) mAdapter.getItem(1)).UpdateCover(Utility.CheckBitmapBySongId((int)MP3info.getId(),false));
            //更新进度条
            mCurrent = MusicService.getCurrentTime();
            mDuration = (int) mInfo.getDuration();
            mSeekBar.setMax(mDuration);
            mBlurHandler.sendEmptyMessage(Utility.UPDATE_BG);
            //更新下一首歌曲
            initNextSong();
        }
    }


}
