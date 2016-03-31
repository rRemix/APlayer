package remix.myplayer.activities;

import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.graphics.Palette;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.listeners.CtrlButtonListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.XmlUtil;

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
    private MP3Info mInfo;
    //歌曲与艺术家
    private TextView mSong;
    private TextView mArtist;
    //控制按钮
    private ImageButton mPrevButton;
    private ImageButton mNextBUtton;
    private ImageButton mPlayButton;
    private ImageButton mLoveButton;
    //DecorView, 跟随手指滑动
    private View mView;
    //是否正在运行
    private static boolean mIsRunning = false;

    private SimpleDraweeView mSimpleImage;
    //背景
    private ImageView mImageBackground;
    //高斯模糊后的bitmap
    private Bitmap mNewBitMap;
    //高斯模糊之前的bitmap
    private Bitmap mRawBitMap;
    private int mWidth;
    private int mHeight;
    //是否添加到收藏列表
    private boolean mIsLove = false;
    //是否正在播放
    private static boolean mIsPlay = false;
    //是否第一次打开
    private boolean mIsFirst = true;
    private Handler mBlurHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            mContainer.setBackground(new BitmapDrawable(getResources(), mNewBitMap));
            //设置背景
            mImageBackground.setImageBitmap(mNewBitMap);

            //变化字体颜色
            if(mSong != null && mArtist != null && mRawBitMap != null){
                Palette.from(mRawBitMap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        Palette.Swatch a = palette.getVibrantSwatch();//有活力
                        Palette.Swatch b = palette.getDarkVibrantSwatch();//有活力 暗色
                        Palette.Swatch c = palette.getLightVibrantSwatch();//有活力 亮色
                        Palette.Swatch d = palette.getMutedSwatch();//柔和
                        Palette.Swatch e = palette.getDarkMutedSwatch();//柔和 暗色
                        Palette.Swatch f = palette.getLightMutedSwatch();//柔和 亮色

                        if(d != null){
                            mSong.setTextColor(d.getBodyTextColor());
                            mArtist.setTextColor(d.getTitleTextColor());
                        }
                    }
                });
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);

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
//        attr.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attr.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        attr.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        //初始化按钮
        mPrevButton = (ImageButton)findViewById(R.id.playbar_prev);
        mNextBUtton = (ImageButton)findViewById(R.id.playbar_next);
        mPlayButton = (ImageButton)findViewById(R.id.playbar_play);
        mLoveButton = (ImageButton)findViewById(R.id.lockscreen_love);

        CtrlButtonListener listener = new CtrlButtonListener(this);
        mPrevButton.setOnClickListener(listener);
        mNextBUtton.setOnClickListener(listener);
        mPlayButton.setOnClickListener(listener);

        //初始化控件
        mImageBackground = (ImageView)findViewById(R.id.lockscreen_background);
        mSimpleImage = (SimpleDraweeView)findViewById(R.id.lockscreen_image);
        mSong = (TextView)findViewById(R.id.lockscreen_song);
        mArtist = (TextView)findViewById(R.id.lockscreen_artist);


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
                Log.d(TAG,"distance:" + mDistance + "\r\n");
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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        UpdateUI(mInfo,mIsPlay);
    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
        Log.d(TAG,this.toString());
        mInfo = MP3info;
        if(!mIsRunning )
            return;
        //只更新播放按钮
        mPlayButton.setBackground(getResources().getDrawable(MusicService.getIsplay() ? R.drawable.wy_lock_btn_pause : R.drawable.wy_lock_btn_play));
        if(AudioHolderActivity.mOperation == Constants.PLAYORPAUSE && mIsFirst){
            mIsFirst = false;
            return;
        }
        //标题
        mSong.setText(mInfo.getDisplayname());
        mArtist.setText(mInfo.getArtist());
        //判断是否收藏
        mIsLove = false;
        try {
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get("我的收藏");
            for(PlayListItem item : list){
                if(item.getId() == mInfo.getId()){
                    mIsLove = true;
                }
            }
        } catch (Exception e){
            Log.d(TAG,"list error:" + e.toString());
            e.printStackTrace();
        }
        mLoveButton.setImageResource(mIsLove ? R.drawable.wy_lock_btn_loved : R.drawable.wy_lock_btn_love);

        mSimpleImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),mInfo.getAlbumId()));

        new BlurThread().start();
    }


    @Override
    public int getType() {
        return Constants.LOCKSCREENACTIIVITY;
    }

    //添加或取消收藏
    public void onLove(View v){
        if(mInfo == null)
            return;
        if(!mIsLove){
            XmlUtil.addSongToPlayList("我的收藏",mInfo.getDisplayname(),(int)mInfo.getId(),(int)mInfo.getAlbumId());
        } else {
            XmlUtil.deleteSongFromPlayList("我的收藏",new PlayListItem(mInfo.getDisplayname(),(int)mInfo.getId(),(int)mInfo.getAlbumId()));
        }
        mIsLove = !mIsLove;
        mLoveButton.setImageResource(mIsLove ? R.drawable.wy_lock_btn_loved : R.drawable.wy_lock_btn_love);
    }


    @Override
    public void onBackPressed() {

    }

    //高斯模糊线程
    class BlurThread extends Thread{
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            if (mWidth > 0 && mHeight > 0 ) {
                if (mInfo == null) return;
                float radius = 40;
                float widthscaleFactor = 3.3f;
                float heightscaleFactor = (float) (widthscaleFactor * (mHeight * 1.0 / mWidth));

                mRawBitMap = DBUtil.CheckBitmapBySongId((int) mInfo.getId(),false);
                if(mRawBitMap == null)
                    mRawBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.artist_empty_bg);
//                Bitmap bkg = DBUtil.CheckBitmapBySongId((int) mInfo.getId(), false);
//                if (bkg == null)
//                    bkg = BitmapFactory.decodeResource(getResources(), R.drawable.no_art_normal);

                mNewBitMap = Bitmap.createBitmap((int) (mWidth / widthscaleFactor), (int) (mHeight / heightscaleFactor), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mNewBitMap);
//                canvas.translate(-mContainer.getLeft() / scaleFactor, -mContainer.getTop() / scaleFactor);
//                canvas.scale(scaleFactor,  scaleFactor);
                Paint paint = new Paint();
                paint.setFlags(Paint.FILTER_BITMAP_FLAG);
                paint.setAlpha((int) (255 * 0.5));
                canvas.drawBitmap(mRawBitMap, 0, 0, paint);
                mNewBitMap = CommonUtil.doBlur(mNewBitMap, (int) radius, true);
            }

//            Log.d(TAG,"mill: " + (System.currentTimeMillis() - start));
            mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
        }
    }

}
