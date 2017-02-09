package remix.myplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.IOException;
import java.util.Random;

import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.appwidgets.AppWidgetBig;
import remix.myplayer.appwidgets.AppWidgetMedium;
import remix.myplayer.appwidgets.AppWidgetSmall;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.db.PlayLists;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.listener.LockScreenListener;
import remix.myplayer.listener.ShakeDector;
import remix.myplayer.model.MP3Item;
import remix.myplayer.observer.DBObserver;
import remix.myplayer.observer.MediaStoreObserver;
import remix.myplayer.receiver.HeadsetPlugReceiver;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.activity.FolderActivity;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;


/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放Service
 * 歌曲的播放 控制
 * 回调相关activity的界面更新
 * 通知栏的控制
 */
public class MusicService extends BaseService implements Playback {
    private final static String TAG = "MusicService";
    private static MusicService mInstance;
    /** 是否第一次启动*/
    private static boolean mFirstFlag = true;

    /** 是否正在设置mediapplayer的datasource */
    private static boolean mIsIniting = false;

    /** 播放模式 */
    private static int mPlayModel = Constants.PLAY_LOOP;

    /** 当前是否正在播放 */
    private static Boolean mIsplay = false;

    /** 当前播放的索引 */
    private static int mCurrentIndex = 0;
    /** 当前正在播放的歌曲id */
    private static int mCurrentId = -1;
    /** 当前正在播放的mp3 */
    private static MP3Item mCurrentInfo = null;

    /** 下一首歌曲的索引 */
    private static int mNextIndex = 0;
    /** 下一首播放歌曲的id */
    private static int mNextId = -1;
    /** 下一首播放的mp3 */
    private static MP3Item mNextInfo = null;

    /** MediaExtractor 获得码率等信息 */
    private static MediaExtractor mMediaExtractor;

    /** MediaPlayer 负责歌曲的播放等 */
    private static MediaPlayer mMediaPlayer;

    /** 桌面部件 */
    private AppWidgetMedium mAppWidgetMedium;
    private AppWidgetSmall mAppWidgetSmall;
    private AppWidgetBig mAppWidgetBig;

    /** AudiaoManager */
    private AudioManager mAudioManager;

    /** 播放控制的Receiver */
    private ControlReceiver mControlRecevier;

    /** 监测耳机拔出的Receiver*/
    private HeadsetPlugReceiver mHeadSetReceiver;

    /** 接收桌面部件 */
    private WidgetReceiver mWidgetReceiver;

    /** 监听AudioFocus的改变 */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;

    /** MediaSession */
    private MediaSessionCompat mMediaSession = null;

    /** 当前是否获得AudioFocus */
    private boolean mAudioFouus = false;

    /** 计时器*/
    private TimerUpdater mTimerUpdater;

    /** 定时关闭剩余时间*/
    private long mMillisUntilFinish;

    /** 更新相关Activity的Handler */
    private Handler mUpdateUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.UPDATE_UI) {
                UpdateHelper.update(mCurrentInfo,mIsplay);
            }
            if(mAppWidgetMedium != null)
                mAppWidgetMedium.updateWidget(mContext);
            if(mAppWidgetSmall != null){
                mAppWidgetSmall.updateWidget(mContext);
            }
        }
    };

    /**电源锁*/
    private PowerManager.WakeLock mWakeLock;
    /** 通知栏*/
    private Notify mNotify;

    private MediaStoreObserver mMediaStoreObserver;
    private DBObserver mPlayListObserver;
    private DBObserver mPlayListSongObserver;
    private static Context mContext;

    public MusicService(){}
    public MusicService(Context context) {
        mContext = context;
    }

    public synchronized static MusicService getInstance(){
        if(mInstance == null)
            mInstance = new MusicService();
        return mInstance;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if(mNotify != null)
            mNotify.cancel();
        ShakeDector.getInstance(mContext).stopListen();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unInit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if(mControlRecevier == null){
//            mControlRecevier = new ControlReceiver();
//            registerReceiver(mControlRecevier,new IntentFilter(Constants.CTL_ACTION));
//        }
//        if(intent.getExtras() != null && intent.getExtras().getBoolean("FromWidget",false)){
//            mControlRecevier.onReceive(null,intent);
//        }
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mInstance = this;
        init();
    }

    private void init() {
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        Global.setHeadsetOn(mAudioManager.isWiredHeadsetOn());
        //电源锁
        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getClass().getSimpleName());
        mWakeLock.setReferenceCounted(false);
        //通知栏
        mNotify = new Notify();
        //监听audiofocus
        mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            //记录焦点变化之前是否在播放;
            private boolean mNeedContinue = false;
            @Override
            public void onAudioFocusChange(int focusChange) {
                //获得audiofocus
                if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
                    mAudioFouus = true;
                    if(mMediaPlayer == null)
                        init();
                    else if(mNeedContinue){
                        play();
                        mNeedContinue = false;
                        Global.setOperation(Constants.TOGGLE);
                    }
                    mMediaPlayer.setVolume(1.0f,1.0f);
                }

                //暂停播放
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
                    mNeedContinue = mIsplay;
                    if(mIsplay && mMediaPlayer != null){
                        Global.setOperation(Constants.TOGGLE);
                        pause();
                    }
                }

                //失去audiofocus 暂停播放
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                    mAudioFouus = false;
                    if(mIsplay && mMediaPlayer != null) {
                        Global.setOperation(Constants.TOGGLE);
                        pause();
                    }
                }
                //通知更新ui
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
                sendBroadcast(new Intent(Constants.NOTIFY));
            }
        };
        //播放模式
        mPlayModel = SPUtil.getValue(this,"Setting", "PlayModel",Constants.PLAY_LOOP);

        //桌面部件
        mAppWidgetMedium = new AppWidgetMedium();
        mAppWidgetSmall = new AppWidgetSmall();

        //初始化Receiver
        mControlRecevier = new ControlReceiver();
        registerReceiver(mControlRecevier,new IntentFilter(Constants.CTL_ACTION));
        mHeadSetReceiver = new HeadsetPlugReceiver();
        registerReceiver(mHeadSetReceiver,new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        mWidgetReceiver = new WidgetReceiver();
        registerReceiver(mWidgetReceiver,new IntentFilter(Constants.WIDGET_UPDATE));

        //监听媒体库变化
        Handler updateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //更新adapter
                if(msg.what == Constants.UPDATE_ADAPTER){
                    if(ChildHolderActivity.getInstance() != null ){
                        ChildHolderActivity.getInstance().updateList();
                    }
                    if (FolderActivity.getInstance() != null ) {
                        FolderActivity.getInstance().updateList();
                    }
                } else if(msg.what == Constants.UPDATE_CHILDHOLDER_ADAPTER){
                    if(ChildHolderActivity.getInstance() != null ){
                        ChildHolderActivity.getInstance().updateList();
                    }
                }
            }
        };
        //监听数据库变化
        mMediaStoreObserver = new MediaStoreObserver(updateHandler);
        mPlayListObserver = new DBObserver(updateHandler);
        mPlayListSongObserver = new DBObserver(updateHandler);
        //监听删除
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,true, mMediaStoreObserver);
        getContentResolver().registerContentObserver(PlayLists.CONTENT_URI,true, mPlayListObserver);
        getContentResolver().registerContentObserver(PlayListSongs.CONTENT_URI,true,mPlayListSongObserver);

        //初始化MediaSesson 用于监听线控操作
        mMediaSession = new MediaSessionCompat(getApplicationContext(),"session");
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setCallback(new SessionCallBack());
        mMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        mMediaSession.setActive(true);

        //初始化Mediaplayer
        if(mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if(mPlayModel == Constants.PLAY_REPEATONE){
                    prepareAndPlay(mCurrentInfo.getUrl());
                } else {
                    playNextOrPrev(true, true);
                }
                Global.setOperation(Constants.NEXT);
                //更新通知栏
                sendBroadcast(new Intent(Constants.NOTIFY));
                acquireWakeLock();
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if(mFirstFlag){
                    mFirstFlag = false;
                    return;
                }
                play();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
//                CommonUtil.uploadException("MediaPlayerError",new Exception("what:" + what + " extra:" + extra));
                LogUtil.e("AppWidget", "what = " + what + " extar = " + extra);
                return true;
            }
        });
        //初始化MediaExtractor
        mMediaExtractor = new MediaExtractor();

        //初始化音效设置
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
        if(!CommonUtil.isIntentAvailable(this,i)){
            EQActivity.Init();
        }
        //读取数据
        loadData();
    }

    /**
     * 初始化mediaplayer
     * @param item
     * @param pos
     */
    public static void initDataSource(MP3Item item,int pos){
        LogUtil.e("AppWidget","initDataSource:" + item);
        if(item == null)
            return;
        //初始化当前播放歌曲
        mCurrentInfo = item;
        mCurrentId = mCurrentInfo.getId();
        mCurrentIndex = pos;
        try {
            if(mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
            }
            mMediaPlayer.setDataSource(mCurrentInfo.getUrl());
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            LogUtil.e("AppWidget","initDataSourceError:" + e);
            mFirstFlag = false;
            e.printStackTrace();
        }
        //初始化下一首歌曲
        mNextId = SPUtil.getValue(APlayerApplication.getContext(),"Setting","NextSongId",-1);
        if(mNextId == -1){
            mNextIndex = mCurrentIndex;
            updateNextSong();
        } else {
            //查找上次退出时保存的下一首歌曲是否还存在
            //如果不存在，重新设置下一首歌曲
            mNextIndex = Global.PlayQueue.indexOf(mNextId);
            mNextInfo = MediaStoreUtil.getMP3InfoById(mNextId);
            if(mNextInfo != null)
                return;
            mNextIndex = mCurrentIndex + 1;
            updateNextSong();
        }

    }

    private void unInit(){
        if(mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if(mMediaExtractor != null)
            mMediaExtractor.release();

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSession.release();
        unregisterReceiver(mControlRecevier);
        unregisterReceiver(mHeadSetReceiver);
        unregisterReceiver(mWidgetReceiver);
        releaseWakeLock();
        getContentResolver().unregisterContentObserver(mMediaStoreObserver);
        getContentResolver().unregisterContentObserver(mPlayListObserver);
        getContentResolver().unregisterContentObserver(mPlayListSongObserver);
        //停止锁屏和摇一摇监听
//        if(SPUtil.getValue(mContext,"Setting","LockScreenOn",false))
        LockScreenListener.getInstance(mContext).stopListen();
//        if(SPUtil.getValue(mContext,"Setting","Shake",false))
        ShakeDector.getInstance(mContext).stopListen();

        mNotify.cancel();
    }

    /**
     * 播放下一首
     */
    @Override
    public void playNext() {
        playNextOrPrev(true,true);
    }

    /**
     * 播放上一首
     */
    @Override
    public void playPrevious() {
        playNextOrPrev(false, true);
    }

    /**
     * 开始播放
     */
    @Override
    public void play() {
        new Thread(){
            @Override
            public void run(){
                mAudioFouus = mAudioManager.requestAudioFocus(
                        mAudioFocusListener,
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                if(!mAudioFouus)
                    return;
                mIsplay = true;
                mMediaPlayer.start();
                //更新所有界面
                update(Global.getOperation());
                //保存当前播放的下一首播放的歌曲的id
                SPUtil.putValue(mContext,"Setting","LastSongId", mCurrentId);
                SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
            }
        }.start();
    }


    /**
     * 根据当前播放状态暂停或者继续播放
     */
    @Override
    public void toggle() {
        if(mMediaPlayer.isPlaying()) {
            pause();
        }
        else {
            LogUtil.e("AppWidget","currentInfo:" + mCurrentInfo);
            if(mCurrentInfo == null)
                return;
            LogUtil.e("AppWidget","play");
            play();
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause() {
        mIsplay = false;
        mMediaPlayer.pause();
        //记录最后一次播放的时间
        mLastPlayedTime = System.currentTimeMillis();
        //更新所有界面
        update(Global.getOperation());
    }

    /**
     * 播放选中的歌曲
     * 比如在全部歌曲或者专辑详情里面选中某一首歌曲
     * @param position 播放位置
     */
    @Override
    public void playSelectSong(int position){
        if((mCurrentIndex = position) == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1))
            return;

        mCurrentId = Global.PlayQueue.get(mCurrentIndex);
        mCurrentInfo = MediaStoreUtil.getMP3InfoById(mCurrentId);

        mNextIndex = mCurrentIndex;
        mNextId = mCurrentId;

        if(mCurrentInfo == null) {
            ToastUtil.show(mContext,R.string.song_lose_effect);
            return;
        }
        mIsplay = true;
        prepareAndPlay(mCurrentInfo.getUrl());
        updateNextSong();
    }

    public class WidgetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getStringExtra("WidgetName");
            switch (str){
                case "BigWidget":
                    if(mAppWidgetBig != null)
                        mAppWidgetBig.updateWidget(context);
                    break;
                case "MediumWidget":
                    if(mAppWidgetMedium != null)
                        mAppWidgetMedium.updateWidget(context);
                    break;
                case "SmallWidget":
                    if(mAppWidgetSmall != null)
                        mAppWidgetSmall.updateWidget(context);
                    break;
            }
        }
    }

    /**
     * 接受控制命令
     * 包括暂停、播放、上下首、播放模式
     */
    public class ControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.e("AppWidget","onReceive");
            if(intent.getExtras() == null)
                return;
            int control = intent.getIntExtra("Control",-1);
            //保存控制命令,用于播放界面判断动画
            Global.setOperation(control);
            //先判断是否是关闭通知栏
            if(intent.getExtras().getBoolean("Close")){
//                NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
//                manager.cancel(0);
                Global.setNotifyShowing(false);
                pause();
                //更新ui
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
                mNotify.cancel();
                return;
            }

            LogUtil.e("AppWidget","control:" + control);
            if(control == Constants.PLAYSELECTEDSONG || control == Constants.PREV || control == Constants.NEXT
                    || control == Constants.TOGGLE || control == Constants.PAUSE || control == Constants.START){
                if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
                    return;
                if(CommonUtil.isFastDoubleClick()) {
                    return;
                }
            }
            LogUtil.e("AppWidget","control---:" + control);

            switch (control) {
                //播放选中的歌曲
                case Constants.PLAYSELECTEDSONG:
                    playSelectSong(intent.getIntExtra("Position", -1));
                    break;
                //播放上一首
                case Constants.PREV:
                    playPrevious();
                    break;
                //播放下一首
                case Constants.NEXT:
                    playNext();
                    break;
                //暂停或者继续播放
                case Constants.TOGGLE:
                    LogUtil.e("AppWidget","playQueue:" + (Global.PlayQueue != null));
                    if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
                        return;
                    mIsplay = !mIsplay;
                    toggle();
                    break;
                //暂停
                case Constants.PAUSE:
                    pause();
                    break;
                //继续播放
                case Constants.START:
                    play();
                    break;
                //顺序播放
                case Constants.PLAY_LOOP:
                    mPlayModel = Constants.PLAY_LOOP;
                    break;
                //随机播放
                case Constants.PLAY_SHUFFLE:
                    mPlayModel = Constants.PLAY_SHUFFLE;
                    break;
                //单曲循环
                case Constants.PLAY_REPEATONE:
                    mPlayModel = Constants.PLAY_REPEATONE;
                default:break;
            }
        }
    }

    /**
     * 更新
     * @param control
     */
    private void update(int control){
        if(control != Constants.PLAY_LOOP &&
                control != Constants.PLAY_SHUFFLE &&
                control != Constants.PLAY_REPEATONE) {
            //更新ui
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
            //更新通知栏
            mNotify.notify(mContext);
        }
    }

    /**
     * 准备播放
     * @param path 播放歌曲的路径
     */
    private void prepareAndPlay(final String path) {
        new Thread(){
            @Override
            public void run() {
                try {
                    mAudioFouus =  mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN) ==
                            AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                    LogUtil.e("AppWidget","hasAudioFocus:" + mAudioFouus + " path:" + path + " mediaplayer:" + mMediaPlayer);
                    if(!mAudioFouus)
                        return;
                    mIsIniting = true;
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(path);

                    mMediaPlayer.prepareAsync();
//                    mFirstFlag = false;
                    mIsplay = true;
                    mIsIniting = false;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * 根据当前播放列表的长度，得到一个随机数
     * @return
     */
    private static int getShuffle(){
        if(Global.PlayQueue.size() == 1)
            return 0;
        return new Random().nextInt(Global.PlayQueue.size() - 1);
    }

    /**
     * 根据当前播放模式，切换到上一首或者下一首
     * @param isNext 是否是播放下一首
     * @param needPlay 是否需要播放
     */
    public void playNextOrPrev(boolean isNext, boolean needPlay){
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
            return;

        if(isNext){
            //如果是点击下一首 播放预先设置好的下一首歌曲
            mCurrentId = mNextId;
            mCurrentIndex = mNextIndex;
            mCurrentInfo = new MP3Item(mNextInfo);
        } else {
            //如果点击上一首 根据播放模式查找上一首歌曲 再重新设置当前歌曲为上一首
            if ((--mCurrentIndex) < 0)
                mCurrentIndex = Global.PlayQueue.size() - 1;
            if(mCurrentIndex  == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1))
                return;
            mCurrentId = Global.PlayQueue.get(mCurrentIndex);
            mCurrentInfo = MediaStoreUtil.getMP3InfoById(mCurrentId);
            mNextIndex = mCurrentIndex;
            mNextId = mCurrentId;
        }

        if(mCurrentInfo == null) {
            ToastUtil.show(mContext,R.string.song_lose_effect);
            return;
        }
        mIsplay = true;
        if(needPlay)
            prepareAndPlay(mCurrentInfo.getUrl());
        updateNextSong();
    }

    /**
     * 更新下一首歌曲
     */
    public static void updateNextSong(){
        if(mPlayModel == Constants.PLAY_LOOP || mPlayModel == Constants.PLAY_REPEATONE){
            if ((++mNextIndex) > Global.PlayQueue.size() - 1)
                mNextIndex = 0;
        } else{
            mNextIndex = getShuffle();
        }
        if(mNextIndex <= Global.PlayQueue.size()){
            mNextId = Global.PlayQueue.get(mNextIndex);
        }
        mNextInfo = MediaStoreUtil.getMP3InfoById(mNextId);
    }

    /**
     * 获得MediaPlayer
     * @return
     */
    public static MediaPlayer getMediaPlayer(){
        return mMediaPlayer;
    }

    /**
     * 获得播放模式
     * @return
     */
    public static int getPlayModel() {
        return mPlayModel;
    }

    /**
     * 设置播放模式并更新下一首歌曲
     * @param playModel
     */
    public static void setPlayModel(int playModel) {
        mPlayModel = playModel;
        SPUtil.putValue(mContext,"Setting", "PlayModel",mPlayModel);
        //保存正在播放和下一首歌曲
        SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
        SPUtil.putValue(mContext,"Setting","LastSongId",mCurrentId);
//        if(!CommonUtil.isFastDoubleClick()){
//            mPlayModel = playModel;
//            SPUtil.putValue(mContext,"Setting", "PlayModel",mPlayModel);
//            //保存正在播放和下一首歌曲
//            SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
//            SPUtil.putValue(mContext,"Setting","LastSongId",mCurrentId);
//        }
    }

    /**
     * 获得是否正在播放
     * @return
     */
    public static boolean isPlay() {
        return mIsplay;
    }

    /**
     * 设置MediaPlayer播放进度
     * @param current
     */
    public static void setProgress(int current) {
        if(mMediaPlayer != null)
            mMediaPlayer.seekTo(current);
    }

    /**
     * 返回当前播放歌曲
     * @return
     */
    public static MP3Item getCurrentMP3() {
        return mCurrentInfo;
    }

    /**
     * 返回下一首播放歌曲
     * @return
     */
    public static MP3Item getNextMP3(){
        return mNextInfo;
    }

    public static MediaFormat getMediaFormat(){
        if(mCurrentInfo == null)
            return null;
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mCurrentInfo.getUrl());// the adresss location of the sound on sdcard.
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return mMediaExtractor.getTrackFormat(0);
    }

    /**
     * 获得歌曲码率信息
     * @return type 0:码率 1:采样率 2:格式
     */
    public static String getRateInfo(int type){
        MediaFormat mf = getMediaFormat();
        if(mf == null)
            return "";
        switch (type){
            case Constants.BIT_RATE:
                if(mf.containsKey(MediaFormat.KEY_BIT_RATE)){
                    return mf.getInteger(MediaFormat.KEY_BIT_RATE) / 1024 + "";
                } else {
                    long durationUs = mf.containsKey(MediaFormat.KEY_DURATION) ? mf.getLong(MediaFormat.KEY_DURATION) : mCurrentInfo.getDuration();
                    return mCurrentInfo.getSize() * 8 / (durationUs / 1024) + "";
                }
            case Constants.SAMPLE_RATE:
                return mf.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                        mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) + "":
                        "";
            case Constants.MIME:
                return mf.containsKey(MediaFormat.KEY_MIME) ?
                        mf.getString(MediaFormat.KEY_MIME) + "":
                        "";
            default:return "";
        }
    }

    /**
     * 获得当前播放进度
     * @return
     */
    public static int getProgress() {
        if(mMediaPlayer != null && !mIsIniting)
            return mMediaPlayer.getCurrentPosition();
        return 0;
    }

    public static long getDuration(){
        if(mMediaPlayer != null && !mIsIniting){
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    /**
     * 获得当前播放索引
     * @return
     */
    public static int getCurrentPos() {
        return mCurrentIndex;
    }

    /**
     * 设置当前索引
     * @param pos
     */
    public static void setCurrentPos(int pos) {
        mCurrentIndex = pos;
    }

    /**
     * 读取歌曲id列表与播放队列
     */
    private void loadData() {
        new Thread(){
            @Override
            public void run() {
                final boolean isFirst = SPUtil.getValue(getApplicationContext(), "Setting", "First", true);
                SPUtil.putValue(mContext,"Setting","First",false);
                //读取sd卡歌曲id
                Global.AllSongList = MediaStoreUtil.getAllSongsIdWithFolder();
                //第一次启动软件
                if(isFirst){
                    try {
                        //默认全部歌曲为播放队列
                        Global.PlayQueueID = PlayListUtil.addPlayList(Constants.PLAY_QUEUE);
                        Global.setPlayQueue(Global.AllSongList);
                        SPUtil.putValue(mContext,"Setting","PlayQueueID",Global.PlayQueueID);
                        //添加我的收藏列表
                        Global.MyLoveID = PlayListUtil.addPlayList(getString(R.string.my_favorite));
                        SPUtil.putValue(mContext,"Setting","MyLoveID",Global.MyLoveID);
                        //保存默认主题设置
                        SPUtil.putValue(mContext,"Setting","ThemeMode", ThemeStore.DAY);
                        SPUtil.putValue(mContext,"Setting","ThemeColor",ThemeStore.THEME_BLUE);
                    } catch (Exception e){
                        CommonUtil.uploadException("新建我的收藏列表错误:" + Global.PlayQueueID,e);
                    }
                }else {
                    Global.PlayQueueID = SPUtil.getValue(mContext,"Setting","PlayQueueID",-1);
                    Global.MyLoveID = SPUtil.getValue(mContext,"Setting","MyLoveID",-1);
                    Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                    Global.PlayList = PlayListUtil.getAllPlayListInfo();
                    Global.RecentlyID = SPUtil.getValue(mContext,"Setting","RecentlyID",-1);
                }
                initLastSong();

                //保存所有目录名字包含lyric的目录
//                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
//                    CommonUtil.getLyricDir(Environment.getExternalStorageDirectory());
//                }
            }
        }.start();

    }

    /**
     * 初始化上一次退出时时正在播放的歌曲
     * @return
     */
    private void initLastSong() {
        LogUtil.e("AppWidget","initLastSong");
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
            return ;
        //读取上次退出时正在播放的歌曲的id
        int lastId = SPUtil.getValue(mContext,"Setting","LastSongId",-1);
        //上次退出时正在播放的歌曲是否还存在
        boolean isLastSongExist = false;
        //上次退出时正在播放的歌曲的pos
        int pos = 0;
        //查找上次退出时的歌曲是否还存在
        if(lastId != -1){
            for(int i = 0; i < Global.PlayQueue.size(); i++){
                if(lastId == Global.PlayQueue.get(i)){
                    isLastSongExist = true;
                    pos = i;
                    break;
                }
            }
        }

        MP3Item item;
        //上次退出时保存的正在播放的歌曲未失效
        if(isLastSongExist && (item = MediaStoreUtil.getMP3InfoById(lastId)) != null) {
            MusicService.initDataSource(item,pos);
        }else {
            //重新找到一个歌曲id
            int id =  Global.PlayQueue.get(0);
            for(int i = 0; i < Global.PlayQueue.size() ; i++){
                id = Global.PlayQueue.get(i);
                if (id != lastId)
                    break;
            }
            item = MediaStoreUtil.getMP3InfoById(id);
            SPUtil.putValue(mContext,"Setting","LastSongId",id);
            MusicService.initDataSource(item,0);
        }
    }

    /**
     * 开始或者停止计时
     * @param start
     * @param duration
     */
    public void toggleTimer(boolean start,long duration){
        if(start){
            if(duration <= 0)
                return;
            mTimerUpdater = new TimerUpdater(duration,1000);
            mTimerUpdater.start();
        } else {
            if(mTimerUpdater != null){
                mTimerUpdater.cancel();
                mTimerUpdater = null;
            }
        }
    }

    /**
     * 剩余的计时时间
     * @return
     */
    public long getMillUntilFinish(){
        synchronized (TimerUpdater.class){
            return mMillisUntilFinish;
        }
    }

    /**
     * 定时关闭计时器
     */
    public class TimerUpdater extends CountDownTimer{
        public TimerUpdater(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            synchronized (TimerUpdater.class){
                mMillisUntilFinish = millisUntilFinished;
            }
        }

        @Override
        public void onFinish() {
            //时间到后发送关闭程序的广播
            sendBroadcast(new Intent(Constants.EXIT));
        }
    }

    private void releaseWakeLock(){
        if(mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();
    }

    private void acquireWakeLock(){
        if(mWakeLock != null)
            mWakeLock.acquire(30000L);
    }

    /**
     * 记录在一秒中线控按下的次数
     */
    private static int mCount = 0;
    /**
     * 接受线控事件
     * 根据线控按下次数,做出相应操作
     */
    public class SessionCallBack extends MediaSessionCompat.Callback{
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Intent intent_ctl = null;
            KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(event == null) return  true;

            //过滤按下事件
            boolean isActionUp = (event.getAction() == KeyEvent.ACTION_UP);
            if(!isActionUp) {
                return true;
            }
            int keyCode = event.getKeyCode();
            if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                intent_ctl = new Intent(Constants.CTL_ACTION);
                int arg = keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ? Constants.TOGGLE :
                        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT : Constants.PREV;
                intent_ctl.putExtra("Control", arg);
                getApplicationContext().sendBroadcast(intent_ctl);
                return true;
            }

            //如果是第一次按下，开启一条线程去判断用户操作
            if(mCount == 0){
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            sleep(800);
                            int arg = -1;
                            arg = mCount == 1 ? Constants.TOGGLE : mCount == 2 ? Constants.NEXT : Constants.PREV;
                            mCount = 0;
                            Intent intent = new Intent(Constants.CTL_ACTION);
                            intent.putExtra("Control", arg);
                            getApplicationContext().sendBroadcast(intent);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
            mCount++;
            return true;
        }

    }

    /**
     * @return 最近是否在播放
     */
    private boolean recentlyPlayed() {
        return isPlay() || System.currentTimeMillis() - mLastPlayedTime < IDLE_DELAY;
    }

    /**
     * 通知栏相关
     */
    public static int NOTIFICATION_ID = 1;
    private static final int IDLE_DELAY = 5 * 60 * 1000;
    private long mLastPlayedTime;
    private int mNotifyMode = NOTIFY_MODE_NONE;
    private long mNotificationPostTime = 0;
    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;
    class Notify {
        private RemoteViews mRemoteView;
        private RemoteViews mRemoteBigView;
        private boolean mIsplay = false;
        private Notification mNotification;
        private NotificationManager mNotificationManager;

        private void notify(final Context context) {
            mRemoteBigView = new RemoteViews(context.getPackageName(),  R.layout.notification_big);
            mRemoteView = new RemoteViews(context.getPackageName(),R.layout.notification);
            mIsplay = MusicService.isPlay();

            if(!Global.isNotifyShowing() && !mIsplay)
                return;

            if((MusicService.getCurrentMP3() != null)) {
                boolean isSystemColor = SPUtil.getValue(context,"Setting","IsSystemColor",true);

                MP3Item temp = MusicService.getCurrentMP3();
                //设置歌手，歌曲名
                mRemoteBigView.setTextViewText(R.id.notify_song, temp.getTitle());
                mRemoteBigView.setTextViewText(R.id.notify_artist_album, temp.getArtist() + " - " + temp.getAlbum());

                mRemoteView.setTextViewText(R.id.notify_song,temp.getTitle());
                mRemoteView.setTextViewText(R.id.notify_artist_album,temp.getArtist() + " - " + temp.getAlbum());

                //设置了黑色背景
                if(!isSystemColor){
                    mRemoteBigView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary));
                    mRemoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary));
                    //背景
                    mRemoteBigView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black);
                    mRemoteBigView.setViewVisibility(R.id.notify_bg, View.VISIBLE);
                    mRemoteView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black);
                    mRemoteView.setViewVisibility(R.id.notify_bg,View.VISIBLE);
                }
                //设置播放按钮
                if(!mIsplay){
                    mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
                    mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
                }else{
                    mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
                    mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
                }
                //设置封面
                int size = DensityUtil.dip2px(context,120);
                ImageRequest imageRequest =
                        ImageRequestBuilder.newBuilderWithSource(Uri.parse(MediaStoreUtil.getImageUrl(temp.getAlbumId(),Constants.URL_ALBUM)))
                                .setResizeOptions(new ResizeOptions(size,size))
                                .setProgressiveRenderingEnabled(true)
                                .build();
                DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);


                dataSource.subscribe(new BaseBitmapDataSubscriber() {
                    @Override
                    protected void onNewResultImpl(Bitmap bitmap) {
                        Bitmap result = Bitmap.createBitmap(bitmap);
                        if(result != null) {
                            mRemoteBigView.setImageViewBitmap(R.id.notify_image, result);
                            mRemoteView.setImageViewBitmap(R.id.notify_image,result);
                        } else {
                            mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                            mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                        }
                        pushNotify(context);
                    }
                    @Override
                    protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                        mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                        pushNotify(context);
                    }
                }, CallerThreadExecutor.getInstance());
            }
        }

        private void pushNotify(Context context) {
            buildAction(context);
            buildNotitication(context);
            if(mNotificationManager == null)
                mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

            final int newNotifyMode;
            if (isPlay()) {
                newNotifyMode = NOTIFY_MODE_FOREGROUND;
            } else if (recentlyPlayed()) {
                newNotifyMode = NOTIFY_MODE_BACKGROUND;
            } else {
                newNotifyMode = NOTIFY_MODE_NONE;
            }
            int notificationId = NOTIFICATION_ID;
            if (mNotifyMode != newNotifyMode) {
                if (mNotifyMode == NOTIFY_MODE_FOREGROUND) {
                    stopForeground(newNotifyMode == NOTIFY_MODE_NONE);
                } else if (newNotifyMode == NOTIFY_MODE_NONE) {
                    mNotificationManager.cancel(notificationId);
                }
            }
            if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
                startForeground(notificationId, mNotification);
            } else if (newNotifyMode == NOTIFY_MODE_BACKGROUND) {
                mNotificationManager.notify(notificationId, mNotification);
            }

            mNotifyMode = newNotifyMode;
            Global.setNotifyShowing(true);
        }

        private void buildNotitication(Context context) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
            if(mNotification == null){
                mBuilder.setContent(mRemoteView)
                        .setCustomBigContentView(mRemoteBigView)
                        .setContentText("")
                        .setContentTitle("")
                        .setWhen(System.currentTimeMillis())
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setOngoing(mIsplay)
                        .setSmallIcon(R.drawable.notifbar_icon);
                //点击通知栏打开播放界面
                //后退回到主界面
                Intent result = new Intent(context,PlayerActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addParentStack(PlayerActivity.class);
                stackBuilder.addNextIntent(result);
                stackBuilder.editIntentAt(1).putExtra("Notify", true);
                stackBuilder.editIntentAt(0).putExtra("Notify", true);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                mNotification = mBuilder.build();
            } else {
                mNotification.bigContentView = mRemoteBigView;
                mNotification.contentView = mRemoteView;
            }
        }

        private void buildAction(Context context) {
            //添加Action
            Intent actionIntent = new Intent(Constants.CTL_ACTION);
            actionIntent.putExtra("FromNotify", true);
            //播放或者暂停
            actionIntent.putExtra("Control", Constants.TOGGLE);
            PendingIntent playIntent = PendingIntent.getBroadcast(context, 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteBigView.setOnClickPendingIntent(R.id.notify_play, playIntent);
            mRemoteView.setOnClickPendingIntent(R.id.notify_play,playIntent);
            //下一首
            actionIntent.putExtra("Control", Constants.NEXT);
            PendingIntent nextIntent = PendingIntent.getBroadcast(context,2,actionIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
            mRemoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
            //上一首
            actionIntent.putExtra("Control", Constants.PREV);
            PendingIntent prevIntent = PendingIntent.getBroadcast(context, 3, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteBigView.setOnClickPendingIntent(R.id.notify_prev,prevIntent);

            //关闭通知栏
            actionIntent.putExtra("Close", true);
            PendingIntent closeIntent = PendingIntent.getBroadcast(context, 4, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent);
            mRemoteView.setOnClickPendingIntent(R.id.notify_close,closeIntent);
        }

        public void cancel(){
            stopForeground(true);
            mNotificationManager.cancelAll();
            mNotifyMode = NOTIFY_MODE_NONE;
        }
    }
}
