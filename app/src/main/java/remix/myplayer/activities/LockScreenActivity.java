package remix.myplayer.activities;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.CtrlButtonListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2016/3/9.
 */
public class LockScreenActivity extends Activity implements MusicService.Callback{
    private WindowManager mWmManager;
    private final static String TAG = "LockScreenActivity";
    private MP3Info mInfo;
    private TextView mTime;
    private TextView mDate;
    private TextView mSong;
    private TextView mArtist;
    private String[] mDayWeekStrs = new String[]{"一","二","三","四","五","六","天"};
    private ImageButton mPrevButton;
    private ImageButton mNextBUtton;
    private ImageButton mPlayButton;
    private ImageButton mLoveButton;
    private FrameLayout mContainer;
    private View mView;
    private static boolean mIsRunning = false;
    private SimpleDraweeView mImage;
    private int mWidth;
    private GestureDetector mDetector;
    private Postprocessor mProcessor;
    private ImageRequest mImageRequest;
    private Handler mTimeHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mTime.setText(msg.arg1 + ":" + (msg.arg2 < 10 ? "0" + msg.arg2 : msg.arg2));
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);

        if((mInfo = MusicService.getCurrentMP3()) == null)
            return;
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        mWidth = metric.widthPixels;

        MusicService.addCallback(this);
        //解锁屏幕并全屏
        WindowManager.LayoutParams attr = getWindow().getAttributes();
        attr.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        attr.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
        attr.flags |= WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;

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
        mImage = (SimpleDraweeView)findViewById(R.id.lockscreen_image);
        mTime = (TextView)findViewById(R.id.lockscreen_time);
        mDate = (TextView)findViewById(R.id.lockscreen_date);
        mSong = (TextView)findViewById(R.id.lockscreen_song);
        mArtist = (TextView)findViewById(R.id.lockscreen_artist);
        mContainer = (FrameLayout)findViewById(R.id.lockscreen_container);
        mDetector = new GestureDetector(this,new SimGesDetectorListener());

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
    }


    private float mScrollX1;
    private float mScrollX2;
    private float mDistance;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        Log.d(TAG,"scollX:" + mView.getScrollX() + "\r\n");

        switch (action){
            case MotionEvent.ACTION_DOWN:
                mScrollX1 = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                mScrollX2 = event.getX();
                mDistance = mScrollX2 - mScrollX1;
                mScrollX1 = mScrollX2;
                //如果往右或者是往左没有超过最左边,移动View
                if(mDistance > 0 || ((mView.getScrollX() + (-mDistance)) < 0))
                    mView.scrollBy((int)-mDistance,0);
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
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        UpdateUI(mInfo,MusicService.getIsplay());
        new TimeThread().start();
    }


    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
        mInfo = MP3info;
        if(!mIsRunning )
            return;
        //日期
        Calendar c = Calendar.getInstance();
        int day_week = c.get(Calendar.DAY_OF_WEEK);
        int month = c.get(Calendar.MONTH);
        int day_month = c.get(Calendar.DAY_OF_MONTH);
        mDate.setText(month + "月" + day_month + "日" + "  星期" + mDayWeekStrs[day_week]);
        //标题
        mSong.setText(mInfo.getDisplayname());
        mArtist.setText(mInfo.getArtist());
        mPlayButton.setBackground(getResources().getDrawable(MusicService.getIsplay() ? R.drawable.wy_lock_btn_pause : R.drawable.wy_lock_btn_play));
        //背景

        mImageRequest = ImageRequestBuilder.newBuilderWithSource(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()))
                .setPostprocessor(mProcessor)
                .build();

        PipelineDraweeController controller = (PipelineDraweeController)
                Fresco.newDraweeControllerBuilder()
                        .setImageRequest(mImageRequest)
                        .setOldController(mImage.getController())
                        // other setters as you need
                        .build();
        mImage.setController(controller);
//        mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mInfo.getAlbumId()));
    }

    /**
     * 按正方形裁切图片
     */
    public static Bitmap ImageCrop(Bitmap bitmap) {
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int wh = w > h ? h : w;// 裁切后所取的正方形区域边长
        int retX = w > h ? (w - h) / 2 : 0;//基于原图，取正方形左上角x坐标
        int retY = w > h ? 0 : (h - w) / 2;
        return Bitmap.createBitmap(bitmap, retX, retY, wh, wh, null, false);
    }

    @Override
    public int getType() {
        return Constants.LOCKSCREENACTIIVITY;
    }

    public void onLove(View v){

    }


    class SimGesDetectorListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            try {
                Log.d(TAG,"distanceX:" + distanceX);
                Log.d(TAG,"X:" + mView.getX());
                Log.d(TAG,"scollX:" + mView.getScrollX() + "\n");


                //判断滑动后的位置是否比最左边还远
                if((mView.getScrollX() + distanceX) > 0)
                    return super.onScroll(e1, e2, distanceX, distanceY);
                //当前位置划过三分之一finish
                if(-mView.getScrollX() > mWidth * 0.3)
                    finish();

                mView.scrollBy((int)distanceX,0);

            }
            catch (Exception e){
                e.printStackTrace();
            }

            return super.onScroll(e1, e2, distanceX, distanceY);
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
                mTimeHandler.sendMessage(msg);
                try {
                    sleep(1000);
                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }
    }
}
