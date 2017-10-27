package remix.myplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.R;
import remix.myplayer.appwidgets.AppWidgetBig;
import remix.myplayer.appwidgets.AppWidgetMedium;
import remix.myplayer.appwidgets.AppWidgetSmall;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.db.PlayLists;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.listener.LockScreenListener;
import remix.myplayer.listener.ShakeDetector;
import remix.myplayer.lyric.LrcRow;
import remix.myplayer.lyric.SearchLRC;
import remix.myplayer.model.FloatLrcContent;
import remix.myplayer.model.mp3.PlayListSong;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.observer.DBObserver;
import remix.myplayer.observer.MediaStoreObserver;
import remix.myplayer.receiver.HeadsetPlugReceiver;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.ui.customview.floatwidget.FloatLrcView;
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
import remix.myplayer.util.floatpermission.FloatWindowManager;


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
    private static boolean mIsInitialized = false;

    /** 播放模式 */
    private static int mPlayModel = Constants.PLAY_LOOP;

    /** 当前是否正在播放 */
    private static Boolean mIsplay = false;

    /** 当前播放的索引 */
    private static int mCurrentIndex = 0;
    /** 当前正在播放的歌曲id */
    private static int mCurrentId = -1;
    /** 当前正在播放的mp3 */
    private static Song mCurrentInfo = null;

    /** 下一首歌曲的索引 */
    private static int mNextIndex = 0;
    /** 下一首播放歌曲的id */
    private static int mNextId = -1;
    /** 下一首播放的mp3 */
    private static Song mNextInfo = null;

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

    /** 事件*/
    private MusicEventReceiver mMusicEventReceiver;

    /** 监测耳机拔出的Receiver*/
    private HeadsetPlugReceiver mHeadSetReceiver;

    /** 接收桌面部件 */
    private WidgetReceiver mWidgetReceiver;

    /** 监听AudioFocus的改变 */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;

    /** MediaSession */
    private MediaSessionCompat mMediaSession;

    /** 当前是否获得AudioFocus */
    private boolean mAudioFouus = false;

    /** 计时器*/
    private TimerUpdater mTimerUpdater;

    /** 定时关闭剩余时间*/
    private long mMillisUntilFinish;

    /** 更新相关Activity的Handler */
    private UpdateUIHandler mUpdateUIHandler;
    /**电源锁*/
    private PowerManager.WakeLock mWakeLock;
    /** 通知栏*/
    private Notify mNotify;
    /** 当前控制命令*/
    private int mControl;
    /** WindowManager 控制悬浮窗*/
    private WindowManager mWindowManager;
    /** 是否显示桌面歌词*/
    private boolean mShowFloatLrc = false;
    /** 桌面歌词控件*/
    private FloatLrcView mFloatLrcView;
    /** 当前歌词*/
    private List<LrcRow> mLrcList = new ArrayList<>();
    /** 已经生成过的随机数 用于随机播放模式*/
    private List<Integer> mRandomList = new ArrayList<>();
    /** service是否停止运行*/
    private boolean mIsServiceStop = false;
    /** handlerThread*/
    private HandlerThread mPlaybackThread;
    private LrcHandler mPlaybackHandler;

    private MediaStoreObserver mMediaStoreObserver;
    private DBObserver mPlayListObserver;
    private DBObserver mPlayListSongObserver;
    private static Context mContext;

    public static final String APLAYER_PACKAGE_NAME = "remix.myplayer";
    public static final String ACTION_MEDIA_CHANGE = APLAYER_PACKAGE_NAME + ".media.change";
    public static final String ACTION_PERMISSION_CHANGE = APLAYER_PACKAGE_NAME + ".permission.change";
    public static final String ACTION_PLAYLIST_CHANGE = APLAYER_PACKAGE_NAME + ".playlist.change";

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
        stopSelf();
        System.exit(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceStop = true;
        unInit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIsServiceStop = false;
        if(mControlRecevier == null){
            mControlRecevier = new ControlReceiver();
            registerReceiver(mControlRecevier,new IntentFilter(Constants.CTL_ACTION));
        }
        if(intent != null && intent.getExtras() != null && intent.getExtras().getBoolean("FromWidget",false)){
            mControlRecevier.onReceive(mContext,intent);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mInstance = this;
        init();
    }

    private void init() {
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        Global.setHeadsetOn(mAudioManager.isWiredHeadsetOn());

        mPlaybackThread = new HandlerThread("IO");
        mPlaybackThread.start();
        mPlaybackHandler = new LrcHandler(this, mPlaybackThread.getLooper());

        mUpdateUIHandler = new UpdateUIHandler(this);

        //电源锁
        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getClass().getSimpleName());
        mWakeLock.setReferenceCounted(false);
        //通知栏
        mNotify = new Notify();
        //监听audiofocus
        mAudioFocusListener = new AudioFocusChangeListener();

        //桌面部件
        mAppWidgetBig = new AppWidgetBig();
        mAppWidgetMedium = new AppWidgetMedium();
        mAppWidgetSmall = new AppWidgetSmall();
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);

        //初始化Receiver
        mMusicEventReceiver = new MusicEventReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MEDIA_CHANGE);
        intentFilter.addAction(ACTION_PERMISSION_CHANGE);
        intentFilter.addAction(ACTION_PLAYLIST_CHANGE);
        registerReceiver(mMusicEventReceiver,intentFilter);
        mControlRecevier = new ControlReceiver();
        registerReceiver(mControlRecevier,new IntentFilter(Constants.CTL_ACTION));
        mHeadSetReceiver = new HeadsetPlugReceiver();
        IntentFilter noisyFileter = new IntentFilter();
        noisyFileter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        noisyFileter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadSetReceiver,noisyFileter);
        mWidgetReceiver = new WidgetReceiver();
        registerReceiver(mWidgetReceiver,new IntentFilter(Constants.WIDGET_UPDATE));

        //监听数据库变化
        mMediaStoreObserver = new MediaStoreObserver(mUpdateUIHandler);
        mPlayListObserver = new DBObserver(mUpdateUIHandler);
        mPlayListSongObserver = new DBObserver(mUpdateUIHandler);
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,true, mMediaStoreObserver);
        getContentResolver().registerContentObserver(PlayLists.CONTENT_URI,true, mPlayListObserver);
        getContentResolver().registerContentObserver(PlayListSongs.CONTENT_URI,true,mPlayListSongObserver);

        setUpMediaPlayer();
        setUpMediaSession();

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
     * 初始化mediasession
     */
    private void setUpMediaSession() {
        //初始化MediaSesson 用于监听线控操作
        mMediaSession = new MediaSessionCompat(getApplicationContext(),"APlayer");
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new SessionCallBack());
        mMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        mMediaSession.setActive(true);

    }

    /**
     * 初始化Mediaplayer
     */
    private void setUpMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setOnCompletionListener(mp -> {
            if(mCloseAfter){
                sendBroadcast(new Intent(Constants.EXIT));
            } else {
                if(mPlayModel == Constants.PLAY_REPEATONE){
                    prepare(mCurrentInfo.getUrl());
                } else {
                    playNextOrPrev(true);
                }
                Global.setOperation(Constants.NEXT);
                acquireWakeLock();
            }
        });
        mMediaPlayer.setOnPreparedListener(mp -> {
            if(mFirstFlag){
                mFirstFlag = false;
                return;
            }
            play();
        });

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            try {
                mIsInitialized = false;
                if(mMediaPlayer != null)
                    mMediaPlayer.release();
                setUpMediaPlayer();
                if(mContext != null){
                    ToastUtil.show(mContext,R.string.mediaplayer_error,what,extra);
                }
                return true;
            } catch (Exception e){
                CommonUtil.uploadException("Error In OnError MusicEventCallback",e.toString());
            }
            return false;
        });
    }

    /**
     * 初始化mediaplayer
     * @param item
     * @param pos
     */
    public void initDataSource(Song item, int pos){
        if(item == null)
            return;
        //初始化当前播放歌曲
        mCurrentInfo = item;
        mCurrentId = mCurrentInfo.getId();
        mCurrentIndex = pos;
        try {
            if(mMediaPlayer == null) {
                setUpMediaPlayer();
            }
            prepare(mCurrentInfo.getUrl());
//            mMediaPlayer.setDataSource(mCurrentInfo.getUrl());
//            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            CommonUtil.uploadException("initDataSource",e);
        }
        //桌面歌词
        updateFloatLrc();
        //初始化下一首歌曲
//        updateNextSong();
        if(mPlayModel == Constants.PLAY_SHUFFLE){
            makeShuffleList(mCurrentId);
        }
        //查找上次退出时保存的下一首歌曲是否还存在
        //如果不存在，重新设置下一首歌曲
        mNextId = SPUtil.getValue(mContext,"Setting","NextSongId",-1);
        if(mNextId == -1){
            mNextIndex = mCurrentIndex;
            updateNextSong();
        } else {
            mNextIndex = mPlayModel == Constants.PLAY_SHUFFLE ?  Global.PlayQueue.indexOf(mNextId) : mRandomList.indexOf(mNextId);
            mNextInfo = MediaStoreUtil.getMP3InfoById(mNextId);
            if(mNextInfo != null)
                return;
            updateNextSong();
        }
    }

    private void unInit(){
        if(mMediaPlayer != null) {
            if(isPlay())
                pause(false);
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if(mMediaExtractor != null)
            mMediaExtractor.release();

        mUpdateUIHandler.removeCallbacksAndMessages(null);
        mShowFloatLrc = false;

        if (Build.VERSION.SDK_INT >= 18) {
            mPlaybackThread.quitSafely();
        } else {
            mPlaybackThread.quit();
        }

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSession.setActive(false);
        mMediaSession.release();
        mNotify.cancel();
        removeFloatLrc();

        CommonUtil.unregisterReceiver(this,mControlRecevier);
        CommonUtil.unregisterReceiver(this,mHeadSetReceiver);
        CommonUtil.unregisterReceiver(this,mWidgetReceiver);
        CommonUtil.unregisterReceiver(this,mMusicEventReceiver);

        releaseWakeLock();
        getContentResolver().unregisterContentObserver(mMediaStoreObserver);
        getContentResolver().unregisterContentObserver(mPlayListObserver);
        getContentResolver().unregisterContentObserver(mPlayListSongObserver);
        ShakeDetector.getInstance(mContext).stopListen();
        LockScreenListener.getInstance(mContext).stopListen();
    }

    /**
     * 播放下一首
     */
    @Override
    public void playNext() {
        playNextOrPrev(true);
    }

    /**
     * 播放上一首
     */
    @Override
    public void playPrevious() {
        playNextOrPrev(false);
    }

    /**
     * 开始播放
     */
    @Override
    public void play() {
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


    /**
     * 根据当前播放状态暂停或者继续播放
     */
    @Override
    public void toggle() {
        if(mMediaPlayer.isPlaying()) {
            pause(false);
        } else {
            play();
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause(boolean updateMediasessionOnly) {
        mIsplay = false;
        mMediaPlayer.pause();
        //记录最后一次播放的时间
        mLastPlayedTime = System.currentTimeMillis();
        if(updateMediasessionOnly)
            updateMediaSession(Global.Operation);
        else
            update(Global.Operation);
    }

    /**
     * 播放选中的歌曲
     * 比如在全部歌曲或者专辑详情里面选中某一首歌曲
     * @param position 播放位置
     */
    @Override
    public void playSelectSong(int position){
        if((mCurrentIndex = position) == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1)) {
            CommonUtil.uploadException("playSelectSong","position:" + position + (" playQueueSize:" + Global.PlayQueue != null ? Global.PlayQueue.size() : "null"));
            ToastUtil.show(mContext,R.string.illegal_arg);
            return;
        }

        mCurrentId = Global.PlayQueue.get(mCurrentIndex);
        mCurrentInfo = MediaStoreUtil.getMP3InfoById(mCurrentId);

        mNextIndex = mCurrentIndex;
        mNextId = mCurrentId;

        if(mCurrentInfo == null) {
            ToastUtil.show(mContext,R.string.song_lose_effect);
            return;
        }
//        mIsplay = true;
        prepare(mCurrentInfo.getUrl());
        updateNextSong();
    }

    public class WidgetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getStringExtra("WidgetName");
            int[] appIds = intent.getIntArrayExtra("WidgetIds");
            switch (str){
                case "BigWidget":
                    if(mAppWidgetBig != null)
                        mAppWidgetBig.updateWidget(context,appIds,true);
                    break;
                case "MediumWidget":
                    if(mAppWidgetMedium != null)
                        mAppWidgetMedium.updateWidget(context,appIds,true);
                    break;
                case "SmallWidget":
                    if(mAppWidgetSmall != null)
                        mAppWidgetSmall.updateWidget(context,appIds,true);
                    break;
            }
        }
    }

    public class MusicEventReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            handleMusicEvent(intent);
        }
    }

    private void handleMusicEvent(Intent intent) {
        if(intent == null)
            return;
        String action = intent.getAction();
        List<MusicEventHelper.MusicEventCallback> callbacks = MusicEventHelper.getCallbacks();
        if(callbacks == null)
            return;
        if(ACTION_MEDIA_CHANGE.equals(action)){
            for(MusicEventHelper.MusicEventCallback callback : callbacks){
                callback.onMediaStoreChanged();
            }
        } else if(ACTION_PERMISSION_CHANGE.equals(action)){
            for(MusicEventHelper.MusicEventCallback callback : callbacks){
                callback.onPermissionChanged(intent.getBooleanExtra("permission",false));
            }
        } else if(ACTION_PLAYLIST_CHANGE.equals(action)){
            for(MusicEventHelper.MusicEventCallback callback : callbacks){
                callback.onPlayListChanged();
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
            if(intent == null || intent.getExtras() == null)
                return;
            int control = intent.getIntExtra("Control",-1);
            //保存控制命令,用于播放界面判断动画
            Global.setOperation(control);
            mControl = control;
            //先判断是否是关闭通知栏
            if(intent.getExtras().getBoolean("Close")){
                Global.setNotifyShowing(false);
                pause(false);
                mNotify.cancel();
                return;
            }

            if(control == Constants.PLAYSELECTEDSONG || control == Constants.PREV || control == Constants.NEXT
                    || control == Constants.TOGGLE || control == Constants.PAUSE || control == Constants.START){
                if(Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
//                    ToastUtil.show(mContext,R.string.list_is_empty);
//                    return;
                    //列表为空，尝试读取
                    Global.PlayQueueID = SPUtil.getValue(mContext,"Setting","PlayQueueID",-1);
                    Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                }
//                if(CommonUtil.isFastDoubleClick()) {
//                    return;
//                }
            }

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
//                    mIsplay = !mIsplay;
                    toggle();
                    break;
                //暂停
                case Constants.PAUSE:
                    pause(false);
                    break;
                //继续播放
                case Constants.START:
                    play();
                    break;
                //改变播放模式
                case Constants.CHANGE_MODEL:
                    mPlayModel = (mPlayModel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP : ++mPlayModel);
                    setPlayModel(mPlayModel);
                    break;
                //取消或者添加收藏
                case Constants.LOVE:
                    int exist = PlayListUtil.isLove(mCurrentId);
                    if(exist == PlayListUtil.EXIST){
                        PlayListUtil.deleteSong(mCurrentId,Global.MyLoveID);
                    } else if (exist == PlayListUtil.NONEXIST){
                        PlayListUtil.addSong(new PlayListSong(mCurrentInfo.getId(), Global.MyLoveID,Constants.MYLOVE));
                    }
                    updateAppwidget();
                    break;
                //桌面歌词
                case Constants.TOGGLE_FLOAT_LRC:
                    boolean open = intent.getBooleanExtra("FloatLrc",false);
                    if(mShowFloatLrc != open){
                        mShowFloatLrc = open;
                        if(mShowFloatLrc){
                            updateFloatLrc();
                        } else {
                            closeFloatLrc();
                        }
                    }
                    break;
                case Constants.TOGGLE_MEDIASESSION:
                    switch (SPUtil.getValue(mContext,"Setting","LockScreenOn",Constants.APLAYER_LOCKSCREEN)){
                        case Constants.APLAYER_LOCKSCREEN:
                        case Constants.CLOSE_LOCKSCREEN:
                            cleanMetaData();
                            break;
                        case Constants.SYSTEM_LOCKSCREEN:
                            updateMediaSession(Constants.NEXT);
                            break;
                    }
                    break;
                //临时播放一首歌曲
                case Constants.PLAY_TEMP:
                    Song temp = (Song) intent.getSerializableExtra("Song");
                    if(temp != null){
                        mCurrentInfo = temp;
                        prepare(mCurrentInfo.getUrl());
                    }
                    break;
                default:break;
            }
        }
    }

    /**
     * 清除锁屏显示的内容
     */
    private void cleanMetaData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSession.setMetadata(new MediaMetadataCompat.Builder().build());
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackState.STATE_NONE,0,1f).build());
        }
    }

    /**
     * 更新
     * @param control
     */
    private void update(int control){
        if(control == Constants.PLAYSELECTEDSONG || control == Constants.PREV || control == Constants.NEXT
                || control == Constants.TOGGLE || control == Constants.PAUSE || control == Constants.START
                || control == Constants.PLAY_TEMP) {
            //更新ui
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
            //更新通知栏
            mNotify.notify(mContext);
            //更新桌面歌词播放按钮
            if(mFloatLrcView != null)
                mFloatLrcView.setPlayIcon(MusicService.isPlay());
            updateMediaSession(control);
        }
    }

    /**
     * 更新锁屏
     * @param control
     */
    private void updateMediaSession(int control) {
        if(SPUtil.getValue(mContext,"Setting","LockScreenOn",Constants.APLAYER_LOCKSCREEN) != Constants.SYSTEM_LOCKSCREEN ||  mCurrentInfo == null)
            return;
//        mMediaSession.setActive(true);
        int playState = mIsplay
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        if(control == Constants.TOGGLE || control == Constants.PAUSE || control == Constants.START){
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState,getProgress(),1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build());
        } else {
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState,getProgress(),1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_PLAY_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build());


            final String uri = MediaStoreUtil.getImageUrl(mCurrentInfo.getAlbumId(),Constants.URL_ALBUM);
            ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(!TextUtils.isEmpty(uri) ? Uri.parse(uri) : Uri.EMPTY)
                        .build();
            DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);
            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onNewResultImpl(Bitmap bitmap) {
                    mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,mCurrentInfo.getAlbum())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,mCurrentInfo.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,mCurrentInfo.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,mCurrentInfo.getDisplayname())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mCurrentInfo.getDuration())
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,mCurrentIndex)
                            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,Global.PlayQueue != null ? Global.PlayQueue.size() : 0)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,copy(bitmap))
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrentInfo.getTitle())
                            .build());
                }

                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,mCurrentInfo.getAlbum())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,mCurrentInfo.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,mCurrentInfo.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,mCurrentInfo.getDisplayname())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mCurrentInfo.getDuration())
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,mCurrentIndex)
                            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,Global.PlayQueue != null ? Global.PlayQueue.size() : 0)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,null)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrentInfo.getTitle())
                            .build());
                }
            }, CallerThreadExecutor.getInstance());

        }
    }


    /**
     * 准备播放
     * @param path 播放歌曲的路径
     */
    private void prepare(final String path) {
        try {
            if(TextUtils.isEmpty(path)){
                ToastUtil.show(mContext,getString(R.string.path_empty));
                return;
            }
            mAudioFouus =  mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(!mAudioFouus) {
                ToastUtil.show(mContext,getString(R.string.cant_request_audio_focus));
                return;
            }
            if(isPlay()){
                pause(true);
            }
            mIsInitialized = false;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepareAsync();
//            mIsplay = true;
            mIsInitialized = true;
        } catch (Exception e){
            ToastUtil.show(mContext,getString(R.string.play_failed) + e.toString());
            mIsInitialized = false;
        }
    }

    /**
     * 得到一个随机数
     * @return
     */
    private int getShuffle(){
        if(!mRandomList.contains(mCurrentId))
            mRandomList.add(mCurrentId);
        //先判断是否已经随机循环完播放队列并且将当前播放的歌曲添加到RandomList
        if (mRandomList.size() == Global.PlayQueue.size() || mRandomList.size() == 0) {
            mRandomList.clear();
        }
        //从未用到过的歌曲id中取出一个
        //并将取出的索引添加到mRandomList 标志这首歌曲已经被随机播放过
        int nextId = 0;
        List<Integer> tempList = (List<Integer>) Global.PlayQueue.clone();
        if (tempList != null && tempList.size() > 0) {
            tempList.removeAll(mRandomList);
            nextId = tempList.get(new Random().nextInt(tempList.size()));
        }
        return nextId;
    }

    /**
     * 根据当前播放模式，切换到上一首或者下一首
     * @param isNext 是否是播放下一首
     */
    public void playNextOrPrev(boolean isNext){
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
            ToastUtil.show(mContext,getString(R.string.list_is_empty));
            return;
        }
        if(isNext){
            //如果是点击下一首 播放预先设置好的下一首歌曲
            mCurrentId = mNextId;
            mCurrentIndex = mNextIndex;
            mCurrentInfo = new Song(mNextInfo);
        } else {
            //如果点击上一首
            if ((--mCurrentIndex) < 0)
                mCurrentIndex = Global.PlayQueue.size() - 1;
            if(mCurrentIndex  == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1))
                return;

            mCurrentId = mPlayModel == Constants.PLAY_SHUFFLE ? mRandomList.get(mCurrentIndex) : Global.PlayQueue.get(mCurrentIndex);

            mCurrentInfo = MediaStoreUtil.getMP3InfoById(mCurrentId);
            mNextIndex = mCurrentIndex;
            mNextId = mCurrentId;
        }
        updateNextSong();
        if(mCurrentInfo == null) {
            ToastUtil.show(mContext,R.string.song_lose_effect);
            return;
        }
        mIsplay = true;
        prepare(mCurrentInfo.getUrl());

    }

    /**
     * 更新下一首歌曲
     */
    public void updateNextSong(){
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0){
            ToastUtil.show(mContext,R.string.list_is_empty);
            return;
        }
//        if(mPlayModel == Constants.PLAY_LOOP || mPlayModel == Constants.PLAY_REPEATONE){
//            if ((++mNextIndex) > Global.PlayQueue.size() - 1)
//                mNextIndex = 0;
//            if(mNextIndex <= Global.PlayQueue.size()){
//                mNextId = Global.PlayQueue.get(mNextIndex);
//            }
//        } else{
//            mNextId = getShuffle();
//            mNextIndex = Global.PlayQueue.indexOf(mNextId);
//        }

        if(mPlayModel == Constants.PLAY_SHUFFLE){
            if(mRandomList.size() == 0){
                makeShuffleList(mCurrentId);
            }
            if ((++mNextIndex) > mRandomList.size() - 1)
                mNextIndex = 0;
            mNextId = mRandomList.get(mNextIndex);
        } else {
            if ((++mNextIndex) > Global.PlayQueue.size() - 1)
                mNextIndex = 0;
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
    public void setPlayModel(int playModel) {
        mPlayModel = playModel;
        updateAppwidget();
        SPUtil.putValue(mContext,"Setting", "PlayModel",mPlayModel);
        //保存正在播放和下一首歌曲
        SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
        SPUtil.putValue(mContext,"Setting","LastSongId",mCurrentId);
        if(mPlayModel == Constants.PLAY_SHUFFLE){
            mRandomList.clear();
            makeShuffleList(mCurrentId);
        }
    }

    /**
     * 生成随机播放列表
     * @param current
     */
    public void makeShuffleList(final int current) {
        if(mRandomList == null)
            mRandomList = new ArrayList<>();
        mRandomList.clear();
        mRandomList.addAll(Global.PlayQueue);
        if (mRandomList.isEmpty())
            return;
        if (current >= 0) {
            mRandomList.remove(Integer.valueOf(current));
            Collections.shuffle(mRandomList);
            mRandomList.add(0,current);
        } else {
            Collections.shuffle(mRandomList);
        }
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
    public static Song getCurrentMP3() {
        return mCurrentInfo;
    }

    /**
     * 返回下一首播放歌曲
     * @return
     */
    public static Song getNextMP3(){
        return mNextInfo;
    }

    public static MediaFormat getMediaFormat(){
        if(mCurrentInfo == null)
            return null;
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mCurrentInfo.getUrl());
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
        if(mMediaPlayer != null && mIsInitialized)
            return mMediaPlayer.getCurrentPosition();
        return 0;
    }

    public static long getDuration(){
        if(mMediaPlayer != null && mIsInitialized){
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
                final boolean isFirst = SPUtil.getValue(mContext, "Setting", "First", true);
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
                        CommonUtil.uploadException("新建列表错误",e);
                    }
                }else {
                    Global.PlayQueueID = SPUtil.getValue(mContext,"Setting","PlayQueueID",-1);
                    Global.MyLoveID = SPUtil.getValue(mContext,"Setting","MyLoveID",-1);
                    Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                    Global.PlayList = PlayListUtil.getAllPlayListInfo();
                    mShowFloatLrc = SPUtil.getValue(mContext,"Setting","FloatLrc",false);
                    //播放模式
                    mPlayModel = SPUtil.getValue(mContext,"Setting", "PlayModel",Constants.PLAY_LOOP);
                    //摇一摇
                    if(SPUtil.getValue(mContext,"Setting","Shake",false)){
                        ShakeDetector.getInstance(mContext).beginListen();
                    }
                    //锁屏
                    LockScreenListener.getInstance(mContext).beginListen();
                }
                initLastSong();
            }
        }.start();

    }

    /**
     * 初始化上一次退出时时正在播放的歌曲
     * @return
     */
    private void initLastSong() {
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

        Song item;
        //上次退出时保存的正在播放的歌曲未失效
        if(isLastSongExist && (item = MediaStoreUtil.getMP3InfoById(lastId)) != null) {
            initDataSource(item,pos);
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
            initDataSource(item,0);
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
    //是否播放完毕
    private boolean mCloseAfter;
    private class TimerUpdater extends CountDownTimer{
        TimerUpdater(long millisInFuture, long countDownInterval) {
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
            //如果当前正在播放 播放完当前歌曲再关闭
            if(mIsplay)
                mCloseAfter = true;
            else
                sendBroadcast(new Intent(Constants.EXIT));
        }
    }

    /**
     * 释放电源锁
     */
    private void releaseWakeLock(){
        if(mWakeLock != null && mWakeLock.isHeld())
            mWakeLock.release();
    }

    /**
     * 获得电源锁
     */
    private void acquireWakeLock(){
        if(mWakeLock != null)
            mWakeLock.acquire(30000L);
    }

    /**
     * @return 最近是否在播放
     */
    private boolean recentlyPlayed() {
        return isPlay() || System.currentTimeMillis() - mLastPlayedTime < IDLE_DELAY;
    }

    /**
     * 更新桌面歌词
     */
    private void updateFloatLrc() {
        mPlaybackHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (MusicService.class){
                    Log.d(TAG,"thread:" + Thread.currentThread().toString());
                    if (checkPermission())
                        return;
                    if(!mShowFloatLrc)
                        return;
                    //根据操作判断是否需要更新歌词
                    int control = Global.Operation;
                    if((control != Constants.TOGGLE && control != Constants.PAUSE && control != Constants.START) || mLrcList == null)
                        mLrcList = new SearchLRC(mCurrentInfo).getLrc("");
                    if(mUpdateFloatLrcThread == null) {
                        mUpdateFloatLrcThread = new UpdateFloatLrcThread();
                        mUpdateFloatLrcThread.start();
                    }
                }
            }
        });
    }

    /**
     * 判断是否有悬浮窗权限
     * 没有权限关闭桌面歌词
     * @return
     */
    private boolean checkPermission() {
        //判断是否有权限
        if(!FloatWindowManager.getInstance().checkPermission(mContext)){
            closeFloatLrc();
            return true;
        }
        return false;
    }

    /**
     * 复制bitmap
     * @param bitmap
     * @return
     */
    public static Bitmap copy(Bitmap bitmap) {
        if(bitmap == null){
            return null;
        }
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        try {
            return bitmap.copy(config, false);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新桌面部件
     */
    private void updateAppwidget() {
        if(mAppWidgetMedium != null)
            mAppWidgetMedium.updateWidget(mContext,null,true);
        if(mAppWidgetSmall != null)
            mAppWidgetSmall.updateWidget(mContext,null,true);
        if(mAppWidgetBig != null)
            mAppWidgetBig.updateWidget(mContext,null,true);
        //暂停停止更新进度条和时间
        if(!isPlay()){
            if(mWidgetTimer != null){
                mWidgetTimer.cancel();
                mWidgetTimer = null;
            }
            if(mWidgetTask != null){
                mWidgetTask.cancel();
                mWidgetTask = null;
            }
        } else {
            //开始播放后持续更新进度条和时间
            if(mWidgetTimer != null)
                return;
            mWidgetTimer = new Timer();
            mWidgetTask = new SeekbarTask();
            mWidgetTimer.schedule(mWidgetTask,1000,1000);
        }
    }

    /**
     * 通知栏
     */
    public static int NOTIFICATION_ID = 1;
    private static final int IDLE_DELAY = 5 * 60 * 1000;
    private long mLastPlayedTime;
    private int mNotifyMode = NOTIFY_MODE_NONE;
    private long mNotificationPostTime = 0;
    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;
    private class Notify {
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

                Song temp = MusicService.getCurrentMP3();
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
                final int size = DensityUtil.dip2px(context,120);
                final String uri = MediaStoreUtil.getImageUrl(temp.getAlbumId(),Constants.URL_ALBUM);
                ImageRequest imageRequest =
                        ImageRequestBuilder.newBuilderWithSource(!TextUtils.isEmpty(uri) ? Uri.parse(uri) : Uri.EMPTY)
                                .setResizeOptions(new ResizeOptions(size,size))
                                .build();
                DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);

                dataSource.subscribe(new BaseBitmapDataSubscriber() {
                    @Override
                    protected void onNewResultImpl(Bitmap bitmap) {
                        try {
                            Bitmap result = copy(bitmap);
                            if(result != null) {
                                mRemoteBigView.setImageViewBitmap(R.id.notify_image, result);
                                mRemoteView.setImageViewBitmap(R.id.notify_image,result);
                            } else {
                                mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                                mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                            }
                        } catch (Exception e){
                            CommonUtil.uploadException("PushNotify Error",e);
                        } finally {
                            pushNotify(context);
                        }
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
            mNotificationManager.notify(notificationId, mNotification);
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
                mBuilder.setCustomBigContentView(mRemoteBigView);
                mBuilder.setCustomContentView(mRemoteView);
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

//                Intent notifyIntent = new Intent(context,PlayerActivity.class);
//                notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                PendingIntent pendingIntent = PendingIntent.getActivity(context,0,notifyIntent,PendingIntent.FLAG_UPDATE_CURRENT);

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
            if(mNotificationManager == null)
                mNotificationManager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancelAll();
            mNotifyMode = NOTIFY_MODE_NONE;
        }
    }

    /** 更新桌面歌词*/
    private static final int LRC_THRESHOLD = 400;
    private static final int LRC_INTERVAL = 600;
//    public String mCurrentLrc;
    private UpdateFloatLrcThread mUpdateFloatLrcThread;
    private FloatLrcContent mCurrentLrc = new FloatLrcContent();
    private LrcRow EMPTY_ROW = new LrcRow("",0,"");
    private class UpdateFloatLrcThread extends Thread{
        @Override
        public void run() {
            while (mShowFloatLrc ){
                try {
                    Thread.sleep(LRC_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //判断权限
                if (checkPermission())
                    return;
                if(mIsServiceStop){
                    mUpdateUIHandler.sendEmptyMessage(Constants.REMOVE_FLOAT_LRC);
                    return;
                }
                //当前应用在前台
                if(CommonUtil.isAppOnForeground(mContext)){
                    if(isFloatLrcShowing())
                        mUpdateUIHandler.sendEmptyMessage(Constants.REMOVE_FLOAT_LRC);
                } else{
                    //当前正在播放
//                    if(!isPlay())
//                        continue;
                    if(!isFloatLrcShowing()) {
                        mUpdateUIHandler.sendEmptyMessage(Constants.CREATE_FLOAT_LRC);
                    } else {
                        Message target = mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT);
                        if(mLrcList == null || mLrcList.size() == 0 || mFloatLrcView == null) {
                            mCurrentLrc.Line1 = new LrcRow("",0,getResources().getString(R.string.no_lrc));
                            mCurrentLrc.Line2 = EMPTY_ROW;
                            target.sendToTarget();
                            continue;
                        }
                        int progress = getProgress();
                        for(int i = 0 ; i < mLrcList.size();i++){
                            LrcRow lrcRow = mLrcList.get(i);
                            int temp = progress - lrcRow.getTime();
                            if(i == 0 && temp < 0){
                                if(mCurrentInfo == null)
                                    break;
                                mCurrentLrc.Line1 = new LrcRow("",0,mCurrentInfo.getTitle());
                                mCurrentLrc.Line2 = new LrcRow("",0,mCurrentInfo.getArtist() + " - " + mCurrentInfo.getAlbum());
                                target.sendToTarget();
                                LogUtil.d("DesktopLrc","sendToTarget");
                            } else if(Math.abs(temp) < LRC_THRESHOLD){
                                mCurrentLrc.Line1 = mLrcList.get(i);
                                mCurrentLrc.Line2 = (i + 1 < mLrcList.size() ? mLrcList.get(i + 1) : EMPTY_ROW);
                                target.sendToTarget();
                                LogUtil.d("DesktopLrc","sendToTarget");
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建桌面歌词悬浮窗
     */
    private boolean mIsFloatLrcInitializing = false;
    private void createFloatLrc(){
        if (checkPermission())
            return;

        if(mIsFloatLrcInitializing)
            return;
        mIsFloatLrcInitializing = true;
        final WindowManager.LayoutParams param = new WindowManager.LayoutParams();
        param.type = WindowManager.LayoutParams.TYPE_PHONE;
//        param.type = WindowManager.LayoutParams.TYPE_PHONE;
        param.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        param.format = PixelFormat.RGBA_8888;
        param.gravity = Gravity.TOP;
        param.width = mContext.getResources().getDisplayMetrics().widthPixels;
        param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        param.x = 0;
        param.y = SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_Y,0);
        if(mFloatLrcView != null){
            mWindowManager.removeView(mFloatLrcView);
            mFloatLrcView = null;
        }
        mFloatLrcView = new FloatLrcView(mContext);
        mWindowManager.addView(mFloatLrcView,param);
        mIsFloatLrcInitializing = false;
        LogUtil.d("DesktopLrc","create desktop lrc");
    }

    /**
     * 移除桌面歌词
     */
    private void removeFloatLrc(){
        if(mFloatLrcView != null){
            mWindowManager.removeView(mFloatLrcView);
            mFloatLrcView = null;
        }
    }

    /**
     * 桌面歌词是否显示
     * @return
     */
    private boolean isFloatLrcShowing(){
        return mFloatLrcView != null;
    }

    /**
     * 关闭桌面歌词
     */
    private void closeFloatLrc() {
        SPUtil.putValue(mContext,"Setting","FloatLrc",false);
        mShowFloatLrc = false;
        mUpdateFloatLrcThread = null;
        if(mLrcList != null)
            mLrcList.clear();
        mUpdateUIHandler.removeMessages(Constants.CREATE_FLOAT_LRC);
        mUpdateUIHandler.sendEmptyMessageDelayed(Constants.REMOVE_FLOAT_LRC,LRC_INTERVAL);
    }

    /** 更新桌面部件进度*/
    private Timer mWidgetTimer;
    private TimerTask mWidgetTask;
    private class SeekbarTask extends TimerTask{
        @Override
        public void run() {
            if(mAppWidgetSmall != null)
                mAppWidgetSmall.updateWidget(mContext,null,false);
            if(mAppWidgetMedium != null)
                mAppWidgetMedium.updateWidget(mContext,null,false);
            if(mAppWidgetBig != null)
                mAppWidgetBig.updateWidget(mContext,null,false);
        }
    }

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
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
                    pause(false);
                }
            }
            //失去audiofocus 暂停播放
            if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                mAudioFouus = false;
                if(mIsplay && mMediaPlayer != null) {
                    Global.setOperation(Constants.TOGGLE);
                    pause(false);
                }
            }
            //通知更新ui
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
        }
    }

    /**
     * 记录在一秒中线控按下的次数
     */
    private static int mHeadSetHookCount = 0;
    private class SessionCallBack extends MediaSessionCompat.Callback{
        private HeadSetRunnable mHeadsetRunnable;
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if(mediaButtonEvent == null)
                return true;
            KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if(event == null)
                return  true;

            boolean isActionUp = (event.getAction() == KeyEvent.ACTION_UP);
            if(!isActionUp) {
                return true;
            }

            Intent intent = new Intent(Constants.CTL_ACTION);
            int keyCode = event.getKeyCode();
            if(keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                intent.putExtra("Control", keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ? Constants.TOGGLE :
                        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT : Constants.PREV);
                mContext.sendBroadcast(intent);
                return true;
            }
            //如果是第一次按下，开启一条线程去判断用户操作
            if(mHeadSetHookCount == 0){
                if(mHeadsetRunnable == null)
                    mHeadsetRunnable = new HeadSetRunnable();
                mUpdateUIHandler.postDelayed(mHeadsetRunnable,800);
            }
            if(keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
                mHeadSetHookCount++;
            return true;
        }

        private class HeadSetRunnable implements Runnable{
            @Override
            public void run() {
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", mHeadSetHookCount == 1 ? Constants.TOGGLE : mHeadSetHookCount == 2 ? Constants.NEXT : Constants.PREV);
                mContext.sendBroadcast(intent);
                mHeadSetHookCount = 0;
            }
        }
    }

    private static class LrcHandler extends Handler{
        private final WeakReference<MusicService> mRef;
        LrcHandler(MusicService service, Looper looper){
            super(looper);
            mRef = new WeakReference<>(service);
        }
    }

    private static class UpdateUIHandler extends Handler{
        private final WeakReference<MusicService> mRef;
        UpdateUIHandler(MusicService service) {
            super();
            mRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if(mRef.get() == null)
                return;
            MusicService musicService = mRef.get();
            switch (msg.what){
                case Constants.UPDATE_UI:
                    musicService.updateAppwidget();
                    musicService.updateFloatLrc();
                    UpdateHelper.update(mCurrentInfo,mIsplay);
                    break;
                case Constants.UPDATE_FLOAT_LRC_CONTENT:
                    if(musicService.mFloatLrcView != null){
                        if(musicService.mCurrentLrc != null)
                            musicService.mFloatLrcView.setText(musicService.mCurrentLrc.Line1,musicService.mCurrentLrc.Line2);
                    }
                    break;
                case Constants.REMOVE_FLOAT_LRC:
                    musicService.removeFloatLrc();
                    break;
                case Constants.CREATE_FLOAT_LRC:
                    musicService.createFloatLrc();
                    break;
                case Constants.UPDATE_ADAPTER:
//                    if(ChildHolderActivity.getInstance() != null ){
//                        ChildHolderActivity.getInstance().updateList();
//                    }
//                    if (FolderActivity.getInstance() != null ) {
//                        FolderActivity.getInstance().updateList();
//                    }
                    musicService.handleMusicEvent(new Intent(ACTION_MEDIA_CHANGE));
                    break;
                case Constants.UPDATE_PLAYLIST:
                    musicService.handleMusicEvent(new Intent(ACTION_PLAYLIST_CHANGE));
                    break;
            }
        }
    }
}
