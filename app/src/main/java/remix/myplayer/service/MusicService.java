package remix.myplayer.service;

import android.Manifest;
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
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.R;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
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
import remix.myplayer.misc.floatpermission.FloatWindowManager;
import remix.myplayer.model.FloatLrcContent;
import remix.myplayer.model.mp3.PlayListSong;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.observer.DBObserver;
import remix.myplayer.observer.MediaStoreObserver;
import remix.myplayer.receiver.HeadsetPlugReceiver;
import remix.myplayer.service.notification.Notify;
import remix.myplayer.service.notification.NotifyImpl;
import remix.myplayer.service.notification.NotifyImpl24;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.customview.floatwidget.FloatLrcView;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
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
public class MusicService extends BaseService implements Playback,MusicEventHelper.MusicEventCallback{
    private final static String TAG = "MusicService";
    private static MusicService mInstance;
    /** 是否第一次准备完成*/
    private boolean mFirstPrepared = true;

    /** 是否正在设置mediapplayer的datasource */
    private static boolean mIsInitialized = false;

    /** 数据是否加载完成*/
    private boolean mLoadFinished = false;

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
    private MediaPlayer mMediaPlayer;

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
    private boolean mAudioFocus = false;

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
    private PlaybackHandler mPlaybackHandler;

    /** shortcut*/
    private DynamicShortcutManager mShortcutManager;

    private MediaStoreObserver mMediaStoreObserver;
    private DBObserver mPlayListObserver;
    private DBObserver mPlayListSongObserver;
    private Context mContext;

    protected boolean mHasPermission = false;

    public static final String APLAYER_PACKAGE_NAME = "remix.myplayer";
    public static final String ACTION_MEDIA_CHANGE = APLAYER_PACKAGE_NAME + ".media.change";
    public static final String ACTION_PERMISSION_CHANGE = APLAYER_PACKAGE_NAME + ".permission.change";
    public static final String ACTION_PLAYLIST_CHANGE = APLAYER_PACKAGE_NAME + ".playlist.change";
    public static final String ACTION_APPWIDGET_OPERATE = APLAYER_PACKAGE_NAME + "appwidget.operate";
    public static final String ACTION_SHORTCUT_SHUFFLE = APLAYER_PACKAGE_NAME + ".shortcut.shuffle";
    public static final String ACTION_SHORTCUT_MYLOVE = APLAYER_PACKAGE_NAME + ".shortcut.my_love";
    public static final String ACTION_SHORTCUT_LASTADDED = APLAYER_PACKAGE_NAME + "shortcut.last_added";
    public static final String ACTION_SHORTCUT_CONTINUE_PLAY = APLAYER_PACKAGE_NAME + "shortcut.continue_play";
    public static final String ACTION_LOAD_FINISH = APLAYER_PACKAGE_NAME + "load.finish";

    public MusicService(){}
    public MusicService(Context context) {
        mContext = context;
    }

    public synchronized static MusicService getInstance(){
        return mInstance;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        unInit();
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
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG,"onCreate");
        mContext = this;
        mInstance = this;
        init();
    }

    @Override
    public int onStartCommand(Intent commandIntent, int flags, int startId) {
        LogUtil.d(TAG,"onStartCommand");
        mIsServiceStop = false;

        if(!mLoadFinished && (mHasPermission = CommonUtil.hasPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {
            //读取数据
//            mNotify.updateForLoading();
            loadDataSync();
//            mNotify.cancelLoadingNotify();
        }

        String action = commandIntent.getAction();
        if(TextUtils.isEmpty(action)) {
            return START_NOT_STICKY;
        }
        mPlaybackHandler.postDelayed(() -> handleStartCommandIntent(commandIntent, action),200);
        return START_NOT_STICKY;
    }

    private void init() {
        MusicEventHelper.addCallback(this);

        mShortcutManager = new DynamicShortcutManager(mContext);

        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        Global.setHeadsetOn(mAudioManager.isWiredHeadsetOn());

        mPlaybackThread = new HandlerThread("IO");
        mPlaybackThread.start();
        mPlaybackHandler = new PlaybackHandler(this, mPlaybackThread.getLooper());

        mUpdateUIHandler = new UpdateUIHandler(this);

        //电源锁
        mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,getClass().getSimpleName());
        mWakeLock.setReferenceCounted(false);
        //通知栏
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N & !SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,false)){
            mNotify = new NotifyImpl24(this);
        } else {
            mNotify = new NotifyImpl(this);
        }

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
            LogUtil.d(TAG,"准备完成:" + mFirstPrepared);
            if(mFirstPrepared){
                mFirstPrepared = false;
                return;
            }
            LogUtil.d(TAG,"开始播放");
            play();
        });

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            try {
                mIsInitialized = false;
                if(mMediaPlayer != null)
                    mMediaPlayer.release();
                setUpMediaPlayer();
                ToastUtil.show(mContext,R.string.mediaplayer_error,what,extra);
                return true;
            } catch (Exception e){

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
        LogUtil.d(TAG,"当前歌曲:" + item.getTitle());
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
            mUpdateUIHandler.post(() -> ToastUtil.show(mContext,e.toString()));
        }
        //桌面歌词
//        updateFloatLrc();
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
        MusicEventHelper.removeCallback(this);
        mLoadFinished = false;
        mIsInitialized = false;
        mShortcutManager.updateContinueShortcut();
        mNotify.cancelPlayingNotify();
        closeAudioEffectSession();
        removeFloatLrc();
        updateAppwidget();
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

    private void closeAudioEffectSession() {
        final Intent audioEffectsIntent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
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
        mAudioFocus = mAudioManager.requestAudioFocus(
                mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if(!mAudioFocus)
            return;
        mIsplay = true; //更新所有界面
        update(Global.getOperation());
        mMediaPlayer.start();

        mPlaybackHandler.post(() -> {
            //保存当前播放和下一首播放的歌曲的id
            SPUtil.putValue(mContext,"Setting","LastSongId", mCurrentId);
            SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
        });

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

    @Override
    public void onMediaStoreChanged() {

    }

    @Override
    public void onPermissionChanged(boolean has) {
        if(has != mHasPermission && has){
            mHasPermission = true;
            loadAsync();
        }
    }

    @Override
    public void onPlayListChanged() {
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

    private void handleStartCommandIntent(Intent commandIntent, String action) {
        mFirstPrepared = false;
        switch (action){
            case ACTION_APPWIDGET_OPERATE:
                Intent appwidgetIntent = new Intent(Constants.CTL_ACTION);
                appwidgetIntent.putExtra("Control",commandIntent.getIntExtra("Control",-1));
                sendBroadcast(appwidgetIntent);
                break;
            case ACTION_SHORTCUT_CONTINUE_PLAY:
                Intent continueIntent = new Intent(Constants.CTL_ACTION);
                continueIntent.putExtra("Control",Constants.TOGGLE);
                sendBroadcast(continueIntent);
                break;
            case ACTION_SHORTCUT_SHUFFLE:
                if(mPlayModel != Constants.PLAY_SHUFFLE){
                    setPlayModel(Constants.PLAY_SHUFFLE);
                }
                Intent shuffleIntent = new Intent(Constants.CTL_ACTION);
                shuffleIntent.putExtra("Control", Constants.NEXT);
                sendBroadcast(shuffleIntent);
                break;
            case ACTION_SHORTCUT_MYLOVE:
                List<Integer> myLoveIds = PlayListUtil.getIDList(Global.MyLoveID);
                if(myLoveIds == null || myLoveIds.size() == 0) {
                    ToastUtil.show(mContext, R.string.list_is_empty);
                    return;
                }
                Intent myloveIntent = new Intent(Constants.CTL_ACTION);
                myloveIntent.putExtra("Control",Constants.PLAYSELECTEDSONG);
                myloveIntent.putExtra("Position",0);
                Global.setPlayQueue(myLoveIds,mContext,myloveIntent);
                break;
            case ACTION_SHORTCUT_LASTADDED:
                List<Song> songs = MediaStoreUtil.getLastAddedSong();
                List<Integer> lastAddIds = new ArrayList<>();
                if(songs == null || songs.size() == 0) {
                    ToastUtil.show(mContext,R.string.list_is_empty);
                    return;
                }
                for(Song song : songs){
                    lastAddIds.add(song.getId());
                }
                Intent lastedIntent = new Intent(Constants.CTL_ACTION);
                lastedIntent.putExtra("Control", Constants.PLAYSELECTEDSONG);
                lastedIntent.putExtra("Position",0);

                Global.setPlayQueue(lastAddIds,mContext,lastedIntent);
                break;
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
                mNotify.cancelPlayingNotify();
                if(mFloatLrcView != null)
                    mFloatLrcView.cancelNotify();
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
                //切换通知栏样式
                case Constants.TOGGLE_NOTIFY:
                    mNotify.cancelPlayingNotify();
                    boolean classic = intent.getBooleanExtra(SPUtil.SPKEY.NOTIFTY_STYLE_CLASS,false);
                    if(classic){
                        mNotify = new NotifyImpl(MusicService.this);
                    } else {
                        mNotify = new NotifyImpl24(MusicService.this);
                    }
                    if(Global.isNotifyShowing())
                        mNotify.updateForPlaying();
                    break;
                //解锁通知栏
                case Constants.UNLOCK_DESTOP_LYRIC:
                    if(mFloatLrcView != null)
                        mFloatLrcView.saveLock(false, true);
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
            mNotify.updateForPlaying();
            //更新桌面歌词播放按钮
            if(mFloatLrcView != null)
                mFloatLrcView.setPlayIcon(MusicService.isPlay());
            updateMediaSession(control);
            mShortcutManager.updateContinueShortcut();
        }
    }

    public MediaSessionCompat getMediaSession(){
        return mMediaSession;
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
            LogUtil.d(TAG,"准备播放");
            if(TextUtils.isEmpty(path)){
                mUpdateUIHandler.post(() -> ToastUtil.show(mContext,getString(R.string.path_empty)));
                return;
            }
            mAudioFocus = mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(!mAudioFocus) {
                mUpdateUIHandler.post(() -> ToastUtil.show(mContext,getString(R.string.cant_request_audio_focus)));
                return;
            }
            if(isPlay()){
                pause(true);
            }
            LogUtil.d("initDataSource","prepare");
            mIsInitialized = false;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepareAsync();
//            mIsplay = true;
            mIsInitialized = true;
        } catch (Exception e){
            mUpdateUIHandler.post(() -> ToastUtil.show(mContext,getString(R.string.play_failed) + e.toString()));
            mIsInitialized = false;
        }
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
        LogUtil.d(TAG,"播放下一首");
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
        return mInstance.mMediaPlayer;
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
        if(getMediaPlayer() != null)
            getMediaPlayer().seekTo(current);
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
        if(getMediaPlayer() != null && mIsInitialized)
            return getMediaPlayer().getCurrentPosition();
        return 0;
    }

    public static long getDuration(){
        if(getMediaPlayer() != null && mIsInitialized){
            return getMediaPlayer().getDuration();
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
    private void loadAsync() {
        new Thread(){
            @Override
            public void run() {
                loadDataSync();
            }
        }.start();

    }

    private void loadDataSync(){
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
            //播放模式
            mPlayModel = SPUtil.getValue(mContext,"Setting", "PlayModel",Constants.PLAY_LOOP);
            Global.PlayQueueID = SPUtil.getValue(mContext,"Setting","PlayQueueID",-1);
            Global.MyLoveID = SPUtil.getValue(mContext,"Setting","MyLoveID",-1);
            Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
            Global.PlayList = PlayListUtil.getAllPlayListInfo();
            mShowFloatLrc = SPUtil.getValue(mContext,"Setting","FloatLrc",false);

            //摇一摇
            if(SPUtil.getValue(mContext,"Setting","Shake",false)){
                ShakeDetector.getInstance(mContext).beginListen();
            }
            //锁屏
            LockScreenListener.getInstance(mContext).beginListen();
        }
        initLastSong();
        mLoadFinished = true;
        sendBroadcast(new Intent(ACTION_LOAD_FINISH));
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
     * 更新桌面歌词
     */
    private void updateFloatLrc() {
        mPlaybackHandler.post(() -> {
            synchronized (MusicService.class){
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
                        mUpdateUIHandler.removeMessages(Constants.CREATE_FLOAT_LRC);
                        mUpdateUIHandler.sendEmptyMessageDelayed(Constants.CREATE_FLOAT_LRC,LRC_THRESHOLD);
                    } else {
                        if(mLrcList == null || mLrcList.size() == 0 || mFloatLrcView == null) {
                            mCurrentLrc.Line1 = new LrcRow("",0,getResources().getString(R.string.no_lrc));
                            mCurrentLrc.Line2 = EMPTY_ROW;
                            mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT).sendToTarget();
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
                                mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT).sendToTarget();
                                LogUtil.d("DesktopLrc","sendToTarget");
                            } else if(Math.abs(temp) < LRC_THRESHOLD){
                                mCurrentLrc.Line1 = mLrcList.get(i);
                                mCurrentLrc.Line2 = (i + 1 < mLrcList.size() ? mLrcList.get(i + 1) : EMPTY_ROW);
                                mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT).sendToTarget();
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            param.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            param.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
//        if(SPUtil.getValue(mContext,"Setting", SPUtil.SPKEY.FLOAT_LRC_LOCK,false)){
//            param.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
//        } else {
//            param.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
//        }
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
            mFloatLrcView.cancelNotify();
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
                mAudioFocus = true;
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
                mAudioFocus = false;
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

    private static class PlaybackHandler extends Handler{
        private final WeakReference<MusicService> mRef;
        PlaybackHandler(MusicService service, Looper looper){
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
