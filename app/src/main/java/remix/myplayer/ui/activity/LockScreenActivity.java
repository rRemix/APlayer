package remix.myplayer.ui.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.enrique.stackblur.StackBlurManager;
import com.facebook.drawee.view.SimpleDraweeView;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.listener.CtrlButtonListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.util.Constants;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2016/3/9.
 */

/**
 * 锁屏界面
 * 实际为将手机解锁并对Activity进行处理，使其看起来像锁屏界面
 */

public class LockScreenActivity extends BaseActivity implements MusicService.Callback{
    private final static String TAG = "LockScreenActivity";
    public static LockScreenActivity mInstance;
    //当前播放的歌曲信息
    private MP3Item mInfo;
    //底部滑动提示图片容器
    @BindView(R.id.lockscreen_arrow_container)
    LinearLayout mArrowContainer;
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
    private Palette.Swatch mSwatch;
    private Handler mBlurHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //设置背景
            mImageBackground.setImageBitmap(mNewBitMap);
            //变化字体颜色
            if(mSong != null && mArtist != null ){
                mSong.setTextColor(mSongColor);
                mArtist.setTextColor(mArtistColor);
                mNextSong.setTextColor(mSongColor);
                mNextSong.setBackground(Theme.getShape(GradientDrawable.RECTANGLE, Color.TRANSPARENT,0,2,mSongColor,0,0,1));
            }

        }
    };

    @Override
    protected void setUpTheme() {
    }

    @Override
    protected void setStatusBar() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);
        ButterKnife.bind(this);

        mInstance = this;
        if((mInfo = MusicService.getCurrentMP3()) == null)
            return;
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        mWidth = metric.widthPixels;
        mHeight = metric.heightPixels;
        MusicService.addCallback(this);
        //解锁屏幕并全屏
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attr.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        attr.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        //初始化按钮
        CtrlButtonListener listener = new CtrlButtonListener(this);
        mPrevButton.setOnClickListener(listener);
        mNextButton.setOnClickListener(listener);
        mPlayButton.setOnClickListener(listener);

        //初始化控件
        mImageBackground.setAlpha(0.75f);
        mArrowContainer.startAnimation(AnimationUtils.loadAnimation(this,R.anim.arrow_left_to_right));
        mView = getWindow().getDecorView();
        mView.setBackgroundColor(getResources().getColor(R.color.transparent));

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
        if(mRawBitMap != null && !mRawBitMap.isRecycled())
            mRawBitMap.recycle();
        if(mNewBitMap != null && !mNewBitMap.isRecycled())
            mNewBitMap.recycle();
    }

    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
        mInfo = MP3Item;
        if(mInfo == null){
            ToastUtil.show(this,"mp3Info:null", Toast.LENGTH_LONG);
        }
        if(!mIsRunning ) {
            ToastUtil.show(this,"isRunning:false", Toast.LENGTH_LONG);
            return;
        }
        //更新播放按钮
        if(mPlayButton != null){
            mPlayButton.setImageResource(MusicService.getIsplay() ? R.drawable.lock_btn_pause : R.drawable.lock_btn_play);
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
            MediaStoreUtil.setImageUrl(mSimpleImage,mInfo.getAlbumId());
        }
        //下一首
        if(mNextSong != null && MusicService.getNextMP3() != null)
            mNextSong.setText("下一首：" + MusicService.getNextMP3().getTitle());

        new BlurThread().start();
    }


    @Override
    public int getType() {
        return Constants.LOCKSCREENACTIIVITY;
    }


    @Override
    public void onBackPressed() {
    }

    //高斯模糊线程
    class BlurThread extends Thread{
        @Override
        public void run() {
            if (mInfo == null)
                return;
            mRawBitMap = MediaStoreUtil.getAlbumBitmap(mInfo.getAlbumId(),false);
            if(mRawBitMap == null)
                mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.album_empty_bg_night);

            StackBlurManager mStackBlurManager = new StackBlurManager(mRawBitMap);
            mStackBlurManager.process(40);
            mNewBitMap = mStackBlurManager.returnBlurredImage();

            Palette.from(mRawBitMap).generate(new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    mSwatch = palette.getDarkMutedSwatch();//柔和 暗色
                    if(mSwatch == null)
                        mSwatch = new Palette.Swatch(Color.GRAY,100);
//                    mSongColor = ColorUtil.shiftColor(mSwatch.getRgb(),1.2f);
//                    mArtistColor = ColorUtil.shiftColor(mSwatch.getRgb(),1.1f);
                    mSongColor = mSwatch.getBodyTextColor();
                    mArtistColor = mSwatch.getTitleTextColor();
                    mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
                }
            });

        }
    }

}
