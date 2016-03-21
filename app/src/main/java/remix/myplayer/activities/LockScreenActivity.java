package remix.myplayer.activities;

import android.animation.ObjectAnimator;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;

import java.util.ArrayList;
import java.util.Calendar;

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
public class LockScreenActivity extends BaseActivity implements MusicService.Callback{
    private WindowManager mWmManager;
    private final static String TAG = "LockScreenActivity";
    public static LockScreenActivity mInstance;
    private MP3Info mInfo;
    private TextView mSong;
    private TextView mArtist;
    private String[] mDayWeekStrs = new String[]{"一","二","三","四","五","六","天"};
    private ImageButton mPrevButton;
    private ImageButton mNextBUtton;
    private ImageButton mPlayButton;
    private ImageButton mLoveButton;
    private RelativeLayout mContainer;
    private View mView;
    private static boolean mIsRunning = false;
    private SimpleDraweeView mSimpleImage;
    private ImageView mImageBackground;
    private Postprocessor mProcessor;
    private ImageRequest mImageRequest;
    private Bitmap mNewBitMap;
    private int mWidth;
    private int mHeight;
    private boolean mIsLove = false;
    private static boolean mIsPlay = false;
    private Handler mBlurHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            mContainer.setBackground(new BitmapDrawable(getResources(), mNewBitMap));
            mImageBackground.setImageBitmap(mNewBitMap);
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

        //初始化界面
        mImageBackground = (ImageView)findViewById(R.id.lockscreen_background);
        mSimpleImage = (SimpleDraweeView)findViewById(R.id.lockscreen_image);
        mSong = (TextView)findViewById(R.id.lockscreen_song);
        mArtist = (TextView)findViewById(R.id.lockscreen_artist);
        mContainer = (RelativeLayout)findViewById(R.id.lockscreen_container);

        mProcessor = new BasePostprocessor() {
            @Override
            public String getName() {
                return "Postprocessor";
            }
            @Override
            public void process(Bitmap bitmap) {
                double scale = 1.0 * bitmap.getWidth() / bitmap.getHeight();
                if(scale > 0.6){
//                    bitmap = ImageCrop(bitmap);
                }
            }
        };
        mView = getWindow().getDecorView();
        mView.setBackgroundColor(getResources().getColor(R.color.transparent));
    }


    private float mScrollX1;
    private float mScrollX2;
    private float mDistance;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Log.d(TAG,"scollX:" + mView.getScrollX() + "\r\n");

        ObjectAnimator animator = null;
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
//                    try {
//                        animator = ObjectAnimator.ofInt(mView,"ScrollX",(int)-mDistance);
//                        animator.setDuration(20);
//                        animator.start();
//                    } catch (Exception e){
//                        e.printStackTrace();
//                    }
                }
                Log.d(TAG,"distance:" + mDistance + "\r\n");
                break;
            case MotionEvent.ACTION_UP:
                //判断当前位置是否超过整个屏幕宽度的0.25
                //超过则finish;没有则移动回初始状态
                if(-mView.getScrollX() > mWidth * 0.25)
                    finish();
                else {
//                    mView.scrollTo(0, 0);
                    animator = ObjectAnimator.ofInt(mView,"ScrollX",0);
                    animator.setDuration(20);
                    animator.start();
                }
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
        mInfo = MP3info;
        if(!mIsRunning )
            return;
        if(AudioHolderActivity.mOperation == Constants.PLAYORPAUSE){
            mPlayButton.setBackground(getResources().getDrawable(MusicService.getIsplay() ? R.drawable.wy_lock_btn_pause : R.drawable.wy_lock_btn_play));
            return;
        }
        //标题
        mSong.setText(mInfo.getDisplayname());
        mArtist.setText(mInfo.getArtist());
        //收藏
        mIsLove = false;
        ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get("我的收藏");
        for(PlayListItem item : list){
            if(item.getId() == mInfo.getId()){
                mIsLove = true;
            }
        }
        mLoveButton.setImageResource(mIsLove ? R.drawable.wy_lock_btn_loved : R.drawable.wy_lock_btn_love);

        mPlayButton.setBackground(getResources().getDrawable(MusicService.getIsplay() ? R.drawable.wy_lock_btn_pause : R.drawable.wy_lock_btn_play));
        //背景
//        mImageRequest = ImageRequestBuilder.newBuilderWithSource(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()))
//                .setPostprocessor(mProcessor)
//                .build();
//
//        PipelineDraweeController controller = (PipelineDraweeController)
//                Fresco.newDraweeControllerBuilder()
//                        .setImageRequest(mImageRequest)
//                        .setOldController(mSimpleImage.getController())
//                        // other setters as you need
//                        .build();
//        mSimpleImage.setController(controller);
        mSimpleImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),mInfo.getAlbumId()));

        new BlurThread().start();
    }


    @Override
    public int getType() {
        return Constants.LOCKSCREENACTIIVITY;
    }

    public void onLove(View v){
        if(mInfo == null)
            return;
        if(!mIsLove){
            XmlUtil.addSong("我的收藏",mInfo.getDisplayname(),(int)mInfo.getId(),(int)mInfo.getAlbumId());
        } else {
            XmlUtil.deleteSong("我的收藏",new PlayListItem(mInfo.getDisplayname(),(int)mInfo.getId(),(int)mInfo.getAlbumId()));
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

                Bitmap bkg = DBUtil.CheckBitmapBySongId((int) mInfo.getId(), false);
                if (bkg == null)
                    bkg = BitmapFactory.decodeResource(getResources(), R.drawable.no_art_normal);

                mNewBitMap = Bitmap.createBitmap((int) (mWidth / widthscaleFactor), (int) (mHeight / heightscaleFactor), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mNewBitMap);
//                canvas.translate(-mContainer.getLeft() / scaleFactor, -mContainer.getTop() / scaleFactor);
//                canvas.scale(scaleFactor,  scaleFactor);
                Paint paint = new Paint();
                paint.setFlags(Paint.FILTER_BITMAP_FLAG);
                paint.setAlpha((int) (255 * 0.3));
                canvas.drawBitmap(bkg, 0, 0, paint);
                mNewBitMap = CommonUtil.doBlur(mNewBitMap, (int) radius, true);
            }

            Log.d(TAG,"mill: " + (System.currentTimeMillis() - start));
            mBlurHandler.sendEmptyMessage(Constants.UPDATE_BG);
        }
    }

    //更新时间线程
    class TimeThread extends Thread{
        @Override
        public void run() {
            while(mIsRunning){
                Calendar c = Calendar.getInstance();
                int hour = c.get(Calendar.HOUR_OF_DAY);
                int minute = c.get(Calendar.MINUTE);
                int sec = c.get(Calendar.SECOND);
                Message msg = new Message();
                msg.arg1 = hour;
                msg.arg2 = minute;
                msg.what = sec;
                try {
                    sleep(1000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }

}
