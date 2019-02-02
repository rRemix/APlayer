package remix.myplayer.service;

import static remix.myplayer.lyric.UpdateLyricThread.LRC_INTERVAL;
import static remix.myplayer.request.network.RxUtil.applySingleScheduler;
import static remix.myplayer.ui.activity.base.BaseActivity.EXTERNAL_STORAGE_PERMISSIONS;
import static remix.myplayer.ui.activity.base.BaseMusicActivity.EXTRA_PERMISSION;
import static remix.myplayer.ui.activity.base.BaseMusicActivity.EXTRA_PLAYLIST;
import static remix.myplayer.util.Constants.PLAY_LOOP;
import static remix.myplayer.util.Constants.PLAY_REPEAT;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.Util.registerLocalReceiver;
import static remix.myplayer.util.Util.sendLocalBroadcast;
import static remix.myplayer.util.Util.unregisterLocalReceiver;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.tencent.bugly.crashreport.CrashReport;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.jetbrains.annotations.NotNull;
import remix.myplayer.R;
import remix.myplayer.appshortcuts.DynamicShortcutManager;
import remix.myplayer.appwidgets.BaseAppwidget;
import remix.myplayer.appwidgets.big.AppWidgetBig;
import remix.myplayer.appwidgets.medium.AppWidgetMedium;
import remix.myplayer.appwidgets.medium.AppWidgetMediumTransparent;
import remix.myplayer.appwidgets.small.AppWidgetSmall;
import remix.myplayer.appwidgets.small.AppWidgetSmallTransparent;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.db.room.model.PlayQueue;
import remix.myplayer.helper.MusicEventCallback;
import remix.myplayer.helper.ShakeDetector;
import remix.myplayer.helper.SleepTimer;
import remix.myplayer.lyric.UpdateLyricThread;
import remix.myplayer.lyric.bean.LyricRowWrapper;
import remix.myplayer.misc.LogObserver;
import remix.myplayer.misc.Migration;
import remix.myplayer.misc.exception.MusicServiceException;
import remix.myplayer.misc.floatpermission.FloatWindowManager;
import remix.myplayer.misc.observer.MediaStoreObserver;
import remix.myplayer.misc.receiver.HeadsetPlugReceiver;
import remix.myplayer.misc.receiver.MediaButtonReceiver;
import remix.myplayer.request.RemoteUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.notification.Notify;
import remix.myplayer.service.notification.NotifyImpl;
import remix.myplayer.service.notification.NotifyImpl24;
import remix.myplayer.ui.activity.LockScreenActivity;
import remix.myplayer.ui.widget.desktop.DesktopLyricView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;
import timber.log.Timber;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by Remix on 2015/12/1.
 */

/**
 * 播放Service 歌曲的播放 控制 回调相关activity的界面更新 通知栏的控制
 */
@SuppressLint("CheckResult")
public class MusicService extends BaseService implements Playback, MusicEventCallback {

  public static final String TAG_LIFECYCLE = "ServiceLifeCycle";
  public static final String EXTRA_DESKTOP_LYRIC = "DesktopLyric";
  public static final String EXTRA_SONG = "Song";
  public static final String EXTRA_POSITION = "Position";

  //更新桌面部件
  public final static int UPDATE_APPWIDGET = 0x100;
  //更新正在播放歌曲
  public final static int UPDATE_META_DATA = 0x102;
  //更新播放状态
  public final static int UPDATE_PLAY_STATE = 0x103;

  //更新桌面歌词内容
  public static final int UPDATE_DESKTOP_LRC_CONTENT = 0x104;
  //移除桌面歌词
  public static final int REMOVE_DESKTOP_LRC = 0x105;
  //添加桌面歌词
  public static final int CREATE_DESKTOP_LRC = 0x106;


  /**
   * 所有歌曲id
   */
  private List<Integer> mAllSong = new ArrayList<>();
  /**
   * 播放队列id
   */
  private List<Integer> mPlayQueue = new ArrayList<>();
  /**
   * 已经生成过的随机数 用于随机播放模式
   */
  private List<Integer> mRandomQueue = new ArrayList<>();
  /**
   * 是否第一次准备完成
   */
  private boolean mFirstPrepared = true;

  /**
   * 是否正在设置mediapplayer的datasource
   */
  private boolean mIsInitialized = false;

  /**
   * 数据是否加载完成
   */
  private boolean mLoadFinished = false;

  /**
   * 播放模式
   */
  private int mPlayModel = PLAY_LOOP;

  /**
   * 当前是否正在播放
   */
  private Boolean mIsPlay = false;

  /**
   * 当前播放的索引
   */
  private int mCurrentIndex = 0;
  /**
   * 当前正在播放的歌曲id
   */
  private int mCurrentId = -1;
  /**
   * 当前正在播放的mp3
   */
  private Song mCurrentSong = null;

  /**
   * 下一首歌曲的索引
   */
  private int mNextIndex = 0;
  /**
   * 下一首播放歌曲的id
   */
  private int mNextId = -1;
  /**
   * 下一首播放的mp3
   */
  private Song mNextSong = null;

  /**
   * MediaPlayer 负责歌曲的播放等
   */
  private IjkMediaPlayer mMediaPlayer;

  /**
   * 桌面部件
   */
  private Map<String, BaseAppwidget> mAppWidgets = new HashMap<>();

  /**
   * AudioManager
   */
  private AudioManager mAudioManager;

  /**
   * 播放控制的Receiver
   */
  private ControlReceiver mControlReceiver;

  /**
   * 事件
   */
  private MusicEventReceiver mMusicEventReceiver;

  /**
   * 监测耳机拔出的Receiver
   */
  private HeadsetPlugReceiver mHeadSetReceiver;

  /**
   * 接收桌面部件
   */
  private WidgetReceiver mWidgetReceiver;

  /**
   * 监听AudioFocus的改变
   */
  private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;

  /**
   * MediaSession
   */
  private MediaSessionCompat mMediaSession;

  /**
   * 当前是否获得AudioFocus
   */
  private boolean mAudioFocus = false;

  /**
   * 更新相关Activity的Handler
   */
  private UpdateUIHandler mUpdateUIHandler;
  /**
   * 电源锁
   */
  private PowerManager.WakeLock mWakeLock;
  /**
   * 通知栏
   */
  private Notify mNotify;
  /**
   * 当前控制命令
   */
  private int mControl;
  /**
   * WindowManager 控制悬浮窗
   */
  private WindowManager mWindowManager;
  /**
   * 是否显示桌面歌词
   */
  private boolean mShowDesktopLyric = false;
  /**
   * 桌面歌词控件
   */
  private DesktopLyricView mDesktopLyricView;

  /**
   * service是否停止运行
   */
  private boolean mIsServiceStop = true;
  /**
   * handlerThread
   */
  private HandlerThread mPlaybackThread;
  private PlaybackHandler mPlaybackHandler;
  /**
   * 监听锁屏
   */
  private ScreenReceiver mScreenReceiver;
  private boolean mScreenOn = true;

  /**
   * shortcut
   */
  private DynamicShortcutManager mShortcutManager;

  /**
   * 音量控制
   */
  private VolumeController mVolumeController;

  /**
   * 退出时播放的进度
   */
  private int mLastProgress;

  /**
   * 是否开启断点播放
   */
  private boolean mPlayAtBreakPoint;

  /**
   * 操作类型
   */
  private int mOperation = -1;

  /**
   * Binder
   */
  private final IBinder mMusicBinder = new MusicBinder();
  /**
   * 数据库
   */
  private final DatabaseRepository mDBRepository = DatabaseRepository.getInstance();

  private MediaStoreObserver mMediaStoreObserver;
  private MusicService mService;

  protected boolean mHasPermission = false;

  private boolean mAlreadyUnInit;
  private float mSpeed = 1.0f;

  public static final String APLAYER_PACKAGE_NAME = "remix.myplayer";
  //媒体数据库变化
  public static final String MEDIA_STORE_CHANGE = APLAYER_PACKAGE_NAME + ".media_store.change";
  //读写权限变化
  public static final String PERMISSION_CHANGE = APLAYER_PACKAGE_NAME + ".permission.change";
  //播放列表变换
  public static final String PLAYLIST_CHANGE = APLAYER_PACKAGE_NAME + ".playlist.change";
  //播放数据变化
  public static final String META_CHANGE = APLAYER_PACKAGE_NAME + ".meta.change";
  //播放状态变化
  public static final String PLAY_STATE_CHANGE = APLAYER_PACKAGE_NAME + ".play_state.change";

  public static final String EXTRA_CONTROL = "Control";
  public static final String ACTION_APPWIDGET_OPERATE = APLAYER_PACKAGE_NAME + "appwidget.operate";
  public static final String ACTION_SHORTCUT_SHUFFLE = APLAYER_PACKAGE_NAME + ".shortcut.shuffle";
  public static final String ACTION_SHORTCUT_MYLOVE = APLAYER_PACKAGE_NAME + ".shortcut.my_love";
  public static final String ACTION_SHORTCUT_LASTADDED =
      APLAYER_PACKAGE_NAME + "shortcut.last_added";
  public static final String ACTION_SHORTCUT_CONTINUE_PLAY =
      APLAYER_PACKAGE_NAME + "shortcut.continue_play";
  public static final String ACTION_LOAD_FINISH = APLAYER_PACKAGE_NAME + "load.finish";
  public static final String ACTION_CMD = APLAYER_PACKAGE_NAME + ".cmd";
  public static final String ACTION_WIDGET_UPDATE = APLAYER_PACKAGE_NAME + ".widget_update";
  public static final String ACTION_TOGGLE_TIMER = APLAYER_PACKAGE_NAME + ".toggle_timer";

  private static final long MEDIA_SESSION_ACTIONS = PlaybackStateCompat.ACTION_PLAY
      | PlaybackStateCompat.ACTION_PAUSE
      | PlaybackStateCompat.ACTION_PLAY_PAUSE
      | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
      | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
      | PlaybackStateCompat.ACTION_STOP
      | PlaybackStateCompat.ACTION_SEEK_TO;

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);
    Timber.tag(TAG_LIFECYCLE).v("onTaskRemoved");
//        unInit();
//        stopSelf();
//        System.exit(0);
  }

  @Override
  public void onDestroy() {
    Timber.tag(TAG_LIFECYCLE).v("onDestroy");
    super.onDestroy();
    mIsServiceStop = true;
    unInit();
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Timber.tag(TAG_LIFECYCLE).v("onCreate");
    mService = this;
    setUp();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return mMusicBinder;
  }


  public class MusicBinder extends Binder {

    @NonNull
    public MusicService getService() {
      return MusicService.this;
    }
  }

  @SuppressLint("CheckResult")
  @Override
  public int onStartCommand(Intent commandIntent, int flags, int startId) {
    Timber.tag(TAG_LIFECYCLE).v("onStartCommand");
    mIsServiceStop = false;

    Single.create((SingleOnSubscribe<String>) emitter -> {
      if (!mLoadFinished && (mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS))) {
        load();
      }
      String action = commandIntent != null ? commandIntent.getAction() : "";
      if (!TextUtils.isEmpty(action)) {
        emitter.onSuccess(action);
      }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        .subscribe(action -> handleStartCommandIntent(commandIntent, action));

//        if(!mLoadFinished && (mHasPermission = Util.hasPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}))) {
//            loadSync();
//        }
//
//        String action = commandIntent != null ? commandIntent.getAction() : "";
//        if(TextUtils.isEmpty(action)) {
//            return START_STICKY;
//        }
//        mPlaybackHandler.postDelayed(() -> handleStartCommandIntent(commandIntent, action),200);
    return START_NOT_STICKY;
  }

  private void setUp() {
    mShortcutManager = new DynamicShortcutManager(this);
    mVolumeController = new VolumeController(this);
    mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    HeadsetPlugReceiver.setHeadsetOn(mAudioManager.isWiredHeadsetOn());

    mPlaybackThread = new HandlerThread("IO");
    mPlaybackThread.start();
    mPlaybackHandler = new PlaybackHandler(this, mPlaybackThread.getLooper());

    mUpdateUIHandler = new UpdateUIHandler(this);

    //电源锁
    mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
    mWakeLock.setReferenceCounted(false);
    //通知栏
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N & !SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
            false)) {
      mNotify = new NotifyImpl24(this);
    } else {
      mNotify = new NotifyImpl(this);
    }

    //监听audiofocus
    mAudioFocusListener = new AudioFocusChangeListener();

    //桌面部件
    mAppWidgets.put("AppWidgetBig", new AppWidgetBig());
    mAppWidgets.put("AppWidgetMedium", new AppWidgetMedium());
    mAppWidgets.put("AppWidgetMediumTransparent", new AppWidgetMediumTransparent());
    mAppWidgets.put("AppWidgetSmall", new AppWidgetSmall());
    mAppWidgets.put("AppWidgetSmallTransparent", new AppWidgetSmallTransparent());

    mWindowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);

    //初始化Receiver
    mMusicEventReceiver = new MusicEventReceiver();
    IntentFilter eventFilter = new IntentFilter();
    eventFilter.addAction(MEDIA_STORE_CHANGE);
    eventFilter.addAction(PERMISSION_CHANGE);
    eventFilter.addAction(PLAYLIST_CHANGE);
    registerLocalReceiver(mMusicEventReceiver, eventFilter);

    mControlReceiver = new ControlReceiver();
    registerLocalReceiver(mControlReceiver, new IntentFilter(ACTION_CMD));

    mHeadSetReceiver = new HeadsetPlugReceiver();
    IntentFilter noisyFilter = new IntentFilter();
    noisyFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG);
    registerReceiver(mHeadSetReceiver, noisyFilter);

    mWidgetReceiver = new WidgetReceiver();
    registerReceiver(mWidgetReceiver, new IntentFilter(ACTION_WIDGET_UPDATE));

    mScreenReceiver = new ScreenReceiver();
    IntentFilter screenFilter = new IntentFilter();
    screenFilter.addAction(Intent.ACTION_SCREEN_ON);
    screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
    registerReceiver(mScreenReceiver, screenFilter);

    //监听数据库变化
    mMediaStoreObserver = new MediaStoreObserver(this);
    getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true,
        mMediaStoreObserver);

    setUpPlayer();
    setUpSession();

    //初始化音效设置
    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
    if (Util.isIntentAvailable(this, i)) {
      openAudioEffectSession();
    } else {
//            EQActivity.Init();
    }

  }

  /**
   * 初始化mediasession
   */
  private void setUpSession() {
    ComponentName mediaButtonReceiverComponentName = new ComponentName(getApplicationContext(),
        MediaButtonReceiver.class);

    Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
    mediaButtonIntent.setComponent(mediaButtonReceiverComponentName);

    PendingIntent pendingIntent = PendingIntent
        .getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0);

    mMediaSession = new MediaSessionCompat(getApplicationContext(), "APlayer",
        mediaButtonReceiverComponentName, pendingIntent);
    mMediaSession.setCallback(new MediaSessionCompat.Callback() {
      @Override
      public boolean onMediaButtonEvent(Intent event) {
        return MediaButtonReceiver.handleMediaButtonIntent(MusicService.this, event);
      }

      @Override
      public void onSkipToNext() {
        Timber.v("onSkipToNext");
        playNext();
      }

      @Override
      public void onSkipToPrevious() {
        Timber.v("onSkipToPrevious");
        playPrevious();
      }

      @Override
      public void onPlay() {
        Timber.v("onPlay");
        play(true);
      }

      @Override
      public void onPause() {
        Timber.v("onPause");
        pause(false);
      }

      @Override
      public void onStop() {
        stopSelf();
      }

      @Override
      public void onSeekTo(long pos) {
        setProgress(pos);
      }
    });

    mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        | MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
    mMediaSession.setMediaButtonReceiver(pendingIntent);
    mMediaSession.setActive(true);
  }

  /**
   * 初始化Mediaplayer
   */
  private void setUpPlayer() {
    mMediaPlayer = new IjkMediaPlayer();

    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

    mMediaPlayer.setOnCompletionListener(mp -> {
      if (mPlayModel == PLAY_REPEAT) {
        prepare(mCurrentSong.getUrl());
      } else {
        playNextOrPrev(true);
      }
      setOperation(Command.NEXT);
      acquireWakeLock();
    });
    mMediaPlayer.setOnPreparedListener(mp -> {
      Timber.v("准备完成:%s", mFirstPrepared);
      if (mFirstPrepared) {
        mFirstPrepared = false;
        if (mLastProgress > 0) {
          mMediaPlayer.seekTo(mLastProgress);
        }
        return;
      }
      Timber.v("开始播放");
      //记录播放历史
      updatePlayHistory();
      play(false);
    });

    mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
      try {
        mIsInitialized = false;
        if (mMediaPlayer != null) {
          mMediaPlayer.release();
        }
        setUpPlayer();
        ToastUtil.show(mService, R.string.mediaplayer_error, what, extra);
        return true;
      } catch (Exception ignored) {

      }
      return false;
    });
  }


  /**
   * 更新播放历史
   */
  private void updatePlayHistory() {
    mDBRepository.updateHistory(mCurrentSong)
        .compose(applySingleScheduler())
        .subscribe(new LogObserver());

  }

  /**
   * 初始化mediaplayer
   */
  private void setUpDataSource(Song item, int pos) {
    if (item == null) {
      return;
    }
    //初始化当前播放歌曲
    Timber.v("当前歌曲:%s", item.getTitle());
    mCurrentSong = item;
    mCurrentId = mCurrentSong.getId();
    mCurrentIndex = pos;
    if (mMediaPlayer == null) {
      setUpPlayer();
    }
    prepare(mCurrentSong.getUrl(), false);
    //桌面歌词
//        updateDesktopLyric();
    //初始化下一首歌曲
//        updateNextSong();
    if (mPlayModel == Constants.PLAY_SHUFFLE) {
      makeShuffleList(mCurrentId);
    }
    //查找上次退出时保存的下一首歌曲是否还存在
    //如果不存在，重新设置下一首歌曲
    mNextId = SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, -1);
    if (mNextId == -1) {
      mNextIndex = mCurrentIndex;
      updateNextSong();
    } else {
      mNextIndex = mPlayModel != Constants.PLAY_SHUFFLE ? mPlayQueue.indexOf(mNextId)
          : mRandomQueue.indexOf(mNextId);
      mNextSong = MediaStoreUtil.getSongById(mNextId);
      if (mNextSong != null) {
        return;
      }
      updateNextSong();
    }
  }

  private void unInit() {
    if (mAlreadyUnInit) {
      return;
    }
    closeAudioEffectSession();
    if (mMediaPlayer != null) {
      if (isPlaying()) {
        pause(false);
      }
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
    mLoadFinished = false;
    mIsInitialized = false;
    mShortcutManager.updateContinueShortcut(this);

    mNotify.cancelPlayingNotify();

    updateAppwidget();
    removeDesktopLyric();
    if (mUpdateDesktopLyricThread != null) {
      mUpdateDesktopLyricThread.quitImmediately();
    }

    mUpdateUIHandler.removeCallbacksAndMessages(null);
    mShowDesktopLyric = false;

    if (Build.VERSION.SDK_INT >= 18) {
      mPlaybackThread.quitSafely();
    } else {
      mPlaybackThread.quit();
    }

    mAudioManager.abandonAudioFocus(mAudioFocusListener);
    mMediaSession.setActive(false);
    mMediaSession.release();

    unregisterLocalReceiver(mControlReceiver);
    unregisterLocalReceiver(mMusicEventReceiver);
    Util.unregisterReceiver(this, mHeadSetReceiver);
    Util.unregisterReceiver(this, mScreenReceiver);
    Util.unregisterReceiver(this, mWidgetReceiver);

    releaseWakeLock();
    getContentResolver().unregisterContentObserver(mMediaStoreObserver);

    ShakeDetector.getInstance().stopListen();

    mAlreadyUnInit = true;
  }

  private void closeAudioEffectSession() {
    final Intent audioEffectsIntent = new Intent(
        AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
    sendBroadcast(audioEffectsIntent);
  }

  private void openAudioEffectSession() {
    final Intent audioEffectsIntent = new Intent(
        AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
    audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
    sendBroadcast(audioEffectsIntent);
  }

  public void setAllSong(List<Integer> allSong) {
    mAllSong = allSong;
  }

  public List<Integer> getAllSong() {
    return mAllSong;
  }

  public List<Integer> getPlayQueue() {
    return mPlayQueue;
  }

  public DatabaseRepository getRepository() {
    return mDBRepository;
  }

  /**
   * 设置播放队列
   */
  @WorkerThread
  public synchronized void setPlayQueue(final List<Integer> newQueueList) {
    if (newQueueList == null || newQueueList.size() == 0) {
      return;
    }
    if (newQueueList.equals(mPlayQueue)) {
      return;
    }
    mPlayQueue.clear();
    mPlayQueue.addAll(newQueueList);

    mDBRepository.runInTransaction(() -> {
      mDBRepository.clearPlayQueue()
          .concatWith(mDBRepository.insertToPlayQueue(mPlayQueue))
          .subscribe();
    });
  }

  /**
   * 设置播放队列
   */
  @WorkerThread
  public synchronized void setPlayQueue(final List<Integer> newQueueList, final Intent intent) {
    //当前模式是随机播放 或者即将设置为随机播放 都要更新mRandomList
    boolean shuffle = intent.getBooleanExtra("shuffle", false) ||
        SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
            PLAY_LOOP) == Constants.PLAY_SHUFFLE;
    if (newQueueList == null || newQueueList.size() == 0) {
      return;
    }
    //设置的播放队列相等
    boolean equals = newQueueList.equals(mPlayQueue);
    if (!equals) {
      mPlayQueue.clear();
      mPlayQueue.addAll(newQueueList);
    }

    if (shuffle) {
      setPlayModel(Constants.PLAY_SHUFFLE);
      updateNextSong();
    }
    mControlReceiver.onReceive(this, intent);

    if (equals) {
      return;
    }

    mDBRepository.runInTransaction(() -> {
      mDBRepository.clearPlayQueue()
          .concatWith(mDBRepository.insertToPlayQueue(mPlayQueue))
          .subscribe();
    });

  }

  public void setPlay(boolean isPlay) {
    mIsPlay = isPlay;
    mUpdateUIHandler.sendEmptyMessage(UPDATE_PLAY_STATE);
//        sendLocalBroadcast(new Intent(PLAY_STATE_CHANGE));
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
  public void play(boolean fadeIn) {
    mAudioFocus = mAudioManager.requestAudioFocus(
        mAudioFocusListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    if (!mAudioFocus) {
      return;
    }
    setPlay(true);
    //eq可用
    Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
    i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
    if (Util.isIntentAvailable(this, i)) {
      openAudioEffectSession();
    }
    //倍速播放
    setSpeed(mSpeed);
    //更新所有界面
    update(getOperation());
    mMediaPlayer.start();
    if (fadeIn) {
      mVolumeController.fadeIn();
    } else {
      mVolumeController.directTo(1);
    }

    mPlaybackHandler.post(() -> {
      //保存当前播放和下一首播放的歌曲的id
      SPUtil
          .putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, mCurrentId);
      SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, mNextId);
    });
  }


  /**
   * 根据当前播放状态暂停或者继续播放
   */
  @Override
  public void toggle() {
    if (mMediaPlayer.isPlaying()) {
      pause(false);
    } else {
      play(true);
    }
  }

  /**
   * 暂停
   */
  @Override
  public void pause(boolean updateMediaSessionOnly) {
    if (updateMediaSessionOnly) {
      updateMediaSession(getOperation());
    } else {
      if (!isPlaying()) { //如果当前已经暂停了 就不重复操作了 避免已经关闭了通知栏又再次显示
        return;
      }
      setPlay(false);
      update(getOperation());
      mVolumeController.fadeOut();
    }
  }

  /**
   * 播放选中的歌曲 比如在全部歌曲或者专辑详情里面选中某一首歌曲
   *
   * @param position 播放位置
   */
  @Override
  public void playSelectSong(int position) {
//        final List<Integer> playQueue = new ArrayList<>(mPlayQueue);
//        final List<Integer> randomQueue = new ArrayList<>(mRandomQueue);
    if ((mCurrentIndex = position) == -1 || (mCurrentIndex >= mPlayQueue.size())) {
      ToastUtil.show(mService, R.string.illegal_arg);
      return;
    }
    mCurrentId = mPlayQueue.get(mCurrentIndex);
    mCurrentSong = MediaStoreUtil.getSongById(mCurrentId);

    mNextIndex = mCurrentIndex;
    mNextId = mCurrentId;

    try {
      //如果是随机播放 需要调整下RandomQueue
      //保证正常播放队列和随机播放队列中当前歌曲的索引一致
      int index = mRandomQueue.indexOf(mCurrentId);
      if (mPlayModel == Constants.PLAY_SHUFFLE &&
          index != mCurrentIndex &&
          index > 0) {
        Collections.swap(mRandomQueue, mCurrentIndex, index);
      }
    } catch (Exception e) {
      CrashReport.postCatchedException(new MusicServiceException("playSelectSong", e));
    }

    if (mCurrentSong == null) {
      ToastUtil.show(mService, R.string.song_lose_effect);
      return;
    }
//        mIsPlay = true;
    prepare(mCurrentSong.getUrl());
    updateNextSong();
  }

  @Override
  public void onMediaStoreChanged() {

  }

  @Override
  public void onPermissionChanged(boolean has) {
    if (has != mHasPermission && has) {
      mHasPermission = true;
      loadSync();
    }
  }

  @Override
  public void onPlayListChanged(String name) {
    if (name.equals(PlayQueue.TABLE_NAME)) {
      mDBRepository
          .getPlayQueue()
          .compose(applySingleScheduler())
          .subscribe(new Consumer<List<Integer>>() {
            @Override
            public void accept(List<Integer> ids) throws Exception {
              //todo 更新下一首
              mPlayQueue = ids;
            }
          });
    }
  }

  @Override
  public void onMetaChanged() {

  }

  @Override
  public void onPlayStateChange() {

  }

  @Override
  public void onServiceConnected(MusicService service) {

  }

  @Override
  public void onServiceDisConnected() {

  }

  public class WidgetReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//            final int skin = SPUtil.getValue(context,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
//            SPUtil.putValue(context,SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.APP_WIDGET_SKIN,skin == SKIN_WHITE_1F ? SKIN_TRANSPARENT : SKIN_WHITE_1F);

      String name = intent.getStringExtra(BaseAppwidget.EXTRA_WIDGET_NAME);
      int[] appIds = intent.getIntArrayExtra(BaseAppwidget.EXTRA_WIDGET_IDS);
      switch (name) {
        case "AppWidgetBig":
          if (mAppWidgets.get("AppWidgetBig") != null) {
            mAppWidgets.get("AppWidgetBig").updateWidget(mService, appIds, true);
          }
          break;
        case "AppWidgetMedium":
          if (mAppWidgets.get("AppWidgetMedium") != null) {
            mAppWidgets.get("AppWidgetMedium").updateWidget(mService, appIds, true);
          }
          break;
        case "AppWidgetSmall":
          if (mAppWidgets.get("AppWidgetSmall") != null) {
            mAppWidgets.get("AppWidgetSmall").updateWidget(mService, appIds, true);
          }
          break;
        case "AppWidgetMediumTransparent":
          if (mAppWidgets.get("AppWidgetMediumTransparent") != null) {
            mAppWidgets.get("AppWidgetMediumTransparent").updateWidget(mService, appIds, true);
          }
          break;
        case "AppWidgetSmallTransparent":
          if (mAppWidgets.get("AppWidgetSmallTransparent") != null) {
            mAppWidgets.get("AppWidgetSmallTransparent").updateWidget(mService, appIds, true);
          }
          break;
      }
    }
  }

  public class MusicEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      handleMusicEvent(intent);
    }
  }

  private void handleStartCommandIntent(Intent commandIntent, String action) {
    mFirstPrepared = false;
    switch (action) {
      case ACTION_APPWIDGET_OPERATE:
        Intent appwidgetIntent = new Intent(ACTION_CMD);
        int control = commandIntent.getIntExtra("Control", -1);
        if (control == UPDATE_APPWIDGET) {
//                    int skin = SPUtil.getValue(this,SPUtil.SETTING_KEY.SETTING_,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
//                    SPUtil.putValue(this,SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.APP_WIDGET_SKIN,skin == SKIN_WHITE_1F ? SKIN_TRANSPARENT : SKIN_WHITE_1F);
          updateAppwidget();
        } else {
          appwidgetIntent.putExtra("Control", control);
          mControlReceiver.onReceive(this, appwidgetIntent);
        }
        break;
      case ACTION_SHORTCUT_CONTINUE_PLAY:
        Intent continueIntent = new Intent(ACTION_CMD);
        continueIntent.putExtra("Control", Command.TOGGLE);
        mControlReceiver.onReceive(this, continueIntent);
        break;
      case ACTION_SHORTCUT_SHUFFLE:
        if (mPlayModel != Constants.PLAY_SHUFFLE) {
          setPlayModel(Constants.PLAY_SHUFFLE);
        }
        Intent shuffleIntent = new Intent(ACTION_CMD);
        shuffleIntent.putExtra("Control", Command.NEXT);
        mControlReceiver.onReceive(this, shuffleIntent);
        break;
      case ACTION_SHORTCUT_MYLOVE:
        List<Integer> myLoveIds = mDBRepository.getMyLoveList().blockingGet();
        if (myLoveIds == null || myLoveIds.size() == 0) {
          ToastUtil.show(mService, R.string.list_is_empty);
          return;
        }
        Intent myloveIntent = new Intent(ACTION_CMD);
        myloveIntent.putExtra("Control", Command.PLAYSELECTEDSONG);
        myloveIntent.putExtra("Position", 0);
        setPlayQueue(myLoveIds, myloveIntent);
        break;
      case ACTION_SHORTCUT_LASTADDED:
        List<Song> songs = MediaStoreUtil.getLastAddedSong();
        List<Integer> lastAddIds = new ArrayList<>();
        if (songs == null || songs.size() == 0) {
          ToastUtil.show(mService, R.string.list_is_empty);
          return;
        }
        for (Song song : songs) {
          lastAddIds.add(song.getId());
        }
        Intent lastedIntent = new Intent(ACTION_CMD);
        lastedIntent.putExtra("Control", Command.PLAYSELECTEDSONG);
        lastedIntent.putExtra("Position", 0);
        setPlayQueue(lastAddIds, lastedIntent);
        break;
      default:
        if (action.equalsIgnoreCase(ACTION_CMD)) {
          mControlReceiver.onReceive(this, commandIntent);
        }
    }
  }

  private void handleMusicEvent(Intent intent) {
    if (intent == null) {
      return;
    }
    String action = intent.getAction();
    if (MEDIA_STORE_CHANGE.equals(action)) {
      onMediaStoreChanged();
    } else if (PERMISSION_CHANGE.equals(action)) {
      onPermissionChanged(intent.getBooleanExtra(EXTRA_PERMISSION, false));
    } else if (PLAYLIST_CHANGE.equals(action)) {
      onPlayListChanged(intent.getStringExtra(EXTRA_PLAYLIST));
    }
  }

  private void handleMetaChange() {
    if (mCurrentSong == null) {
      return;
    }
    updateAppwidget();
    if (mNeedShowDesktopLyric) {
      mShowDesktopLyric = true;
      mNeedShowDesktopLyric = false;
    }
    updateDesktopLyric(false);
    updateNotification();

    updateMediaSession(getOperation());
    sendLocalBroadcast(new Intent(MusicService.META_CHANGE));
  }

  private void updateNotification() {
    if (mNotify != null) {
      mNotify.updateForPlaying();
    }
  }

  private void handlePlayStateChange() {
    if (mCurrentSong == null) {
      return;
    }
    //更新桌面歌词播放按钮
    if (mDesktopLyricView != null) {
      mDesktopLyricView.setPlayIcon(isPlaying());
    }
    if (mShortcutManager != null) {
      mShortcutManager.updateContinueShortcut(this);
    }
    sendLocalBroadcast(new Intent(MusicService.PLAY_STATE_CHANGE));
  }

  /**
   * 接受控制命令 包括暂停、播放、上下首、改版播放模式等
   */
  public class ControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Timber.v("ControlReceiver: %s", intent);
      if (intent == null || intent.getExtras() == null) {
        return;
      }
      int control = intent.getIntExtra(EXTRA_CONTROL, -1);
      mControl = control;

      if (control == Command.PLAYSELECTEDSONG || control == Command.PREV || control == Command.NEXT
          || control == Command.TOGGLE || control == Command.PAUSE || control == Command.START) {
        //保存控制命令,用于播放界面判断动画
        setOperation(control);
        if (mPlayQueue == null || mPlayQueue.size() == 0) {
          //列表为空，尝试读取
          mDBRepository.getPlayQueue()
              .compose(applySingleScheduler())
              .subscribe(queues -> mPlayQueue = queues);
        }
      }

      switch (control) {
        //关闭通知栏
        case Command.CLOSE_NOTIFY:
          Notify.setNotifyShowing(false);
//          if (mNotify instanceof NotifyImpl24) { //仅仅只是设置标志位
//            return;
//          }
          pause(false);
          if (mUpdateDesktopLyricThread != null) {
            mUpdateDesktopLyricThread.quitByNotification();
          }
          mUpdateUIHandler.postDelayed(() -> mNotify.cancelPlayingNotify(), 300);
          break;
        //播放选中的歌曲
        case Command.PLAYSELECTEDSONG:
          playSelectSong(intent.getIntExtra("Position", -1));
          break;
        //播放上一首
        case Command.PREV:
          playPrevious();
          break;
        //播放下一首
        case Command.NEXT:
          playNext();
          break;
        //暂停或者继续播放
        case Command.TOGGLE:
          toggle();
          break;
        //暂停
        case Command.PAUSE:
          pause(false);
          break;
        //继续播放
        case Command.START:
          play(false);
          break;
        //改变播放模式
        case Command.CHANGE_MODEL:
          mPlayModel = (mPlayModel == PLAY_REPEAT ? PLAY_LOOP
              : ++mPlayModel);
          setPlayModel(mPlayModel);
          break;
        //取消或者添加收藏
        case Command.LOVE:
          mDBRepository.toggleMyLove(mCurrentId)
              .compose(applySingleScheduler())
              .subscribe(aBoolean -> updateAppwidget(), Timber::v);

          break;
        //桌面歌词
        case Command.TOGGLE_DESKTOP_LYRIC:
          boolean open;
          if (intent.hasExtra(EXTRA_DESKTOP_LYRIC)) {
            open = intent.getBooleanExtra(EXTRA_DESKTOP_LYRIC, false);
          } else {
            open = !SPUtil.getValue(mService,
                SPUtil.SETTING_KEY.NAME,
                SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false);
          }
          if (open && !FloatWindowManager.getInstance().checkPermission(mService)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
              Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
              permissionIntent.setData(Uri.parse("package:" + getPackageName()));
              permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              if (Util.isIntentAvailable(mService, permissionIntent)) {
                startActivity(permissionIntent);
              }
            }
            ToastUtil.show(mService, R.string.plz_give_float_permission);
            break;
          }
          SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW,
              open);
          if (mShowDesktopLyric != open) {
            mShowDesktopLyric = open;
            ToastUtil.show(mService,
                mShowDesktopLyric ? R.string.opened_desktop_lrc : R.string.closed_desktop_lrc);
            if (mShowDesktopLyric) {
              updateDesktopLyric(false);
            } else {
              closeDesktopLyric();
            }
          }
          break;
        case Command.TOGGLE_MEDIASESSION:
          switch (SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
              Constants.APLAYER_LOCKSCREEN)) {
            case Constants.APLAYER_LOCKSCREEN:
            case Constants.CLOSE_LOCKSCREEN:
              cleanMetaData();
              break;
            case Constants.SYSTEM_LOCKSCREEN:
              updateMediaSession(Command.NEXT);
              break;
          }
          break;
        //临时播放一首歌曲
        case Command.PLAY_TEMP:
          Song tempSong = intent.getParcelableExtra(EXTRA_SONG);
          if (tempSong != null) {
            mCurrentSong = tempSong;
            setOperation(Command.PLAY_TEMP);
            prepare(mCurrentSong.getUrl());
          }
          break;
        //切换通知栏样式
        case Command.TOGGLE_NOTIFY:
          boolean classic = intent.getBooleanExtra(SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false);
          if (classic) {
            mNotify = new NotifyImpl(MusicService.this);
          } else {
            mNotify = new NotifyImpl24(MusicService.this);
          }
          if (Notify.isNotifyShowing()) {
            updateNotification();
          }
          break;
        //解锁桌面歌词
        case Command.UNLOCK_DESKTOP_LYRIC:
          if (mDesktopLyricView != null) {
            mDesktopLyricView.saveLock(false, true);
          } else {
            SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, false);
          }
          //更新通知栏
          updateNotification();
          break;
        //锁定通知栏
        case Command.LOCK_LYRIC:
          //更新通知栏
          updateNotification();
          break;
        //某一首歌曲添加至下一首播放
        case Command.ADD_TO_NEXT_SONG:
          Song nextSong = intent.getParcelableExtra(EXTRA_SONG);
          if (nextSong == null) {
            return;
          }
          //添加到播放队列
          if (mNextId == nextSong.getId()) {
            ToastUtil.show(mService, R.string.already_add_to_next_song);
            return;
          }
          //根据当前播放模式，添加到队列
          if (mPlayModel == Constants.PLAY_SHUFFLE) {
            if (mRandomQueue.contains(nextSong.getId())) {
              mRandomQueue.remove(Integer.valueOf(nextSong.getId()));
              mRandomQueue.add(mCurrentIndex + 1 < mRandomQueue.size() ? mCurrentIndex + 1 : 0,
                  nextSong.getId());
            } else {
              mRandomQueue.add(mRandomQueue.indexOf(mCurrentId) + 1, nextSong.getId());
            }
          } else {
            if (mPlayQueue.contains(nextSong.getId())) {
              mPlayQueue.remove(Integer.valueOf(nextSong.getId()));
              mPlayQueue.add(mCurrentIndex + 1 < mPlayQueue.size() ? mCurrentIndex + 1 : 0,
                  nextSong.getId());
            } else {
              mPlayQueue.add(mPlayQueue.indexOf(mCurrentId) + 1, nextSong.getId());
            }
          }

          //更新下一首
          mNextIndex = mCurrentIndex;
          updateNextSong();
          //保存到数据库
          if (mPlayModel != Constants.PLAY_SHUFFLE) {
            Single
                .fromCallable(() -> {
                  mDBRepository.runInTransaction(() -> {
                    mDBRepository.clearPlayQueue();
                    mDBRepository.insertToPlayQueue(mPlayQueue);
                  });
                  return true;
                })
                .compose(applySingleScheduler())
                .subscribe(success -> ToastUtil.show(mService, R.string.already_add_to_next_song),
                    throwable -> ToastUtil.show(mService, R.string.play_failed));
          }
          break;
        //改变歌词源
        case Command.CHANGE_LYRIC:
          if (mShowDesktopLyric) {
            updateDesktopLyric(true);
          }
          break;
        //断点播放
        case Command.PLAY_AT_BREAKPOINT:
          mPlayAtBreakPoint = intent.getBooleanExtra(SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false);
          if (!mPlayAtBreakPoint) {
            SPUtil
                .putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS,
                    0);
          }
          break;
        //切换定时器
        case Command.TOGGLE_TIMER:
          final boolean hasDefault = SPUtil
              .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DEFAULT, false);
          if (!hasDefault) {
            ToastUtil.show(mService, getString(R.string.plz_set_default_time));
          }
          final int time = SPUtil
              .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.TIMER_DURATION, -1);
          SleepTimer.toggleTimer(time * 1000);
          break;
        //耳机拔出
        case Command.HEADSET_CHANGE:
          if (isPlaying()) {
            pause(false);
          }
          break;
        default:
          break;
      }
    }
  }

  /**
   * 是否只需要更新播放状态,比如暂停
   */
  private boolean updatePlayStateOnly(int cmd) {
    return cmd == Command.PAUSE || cmd == Command.START || cmd == Command.TOGGLE;
  }

  private boolean updateAllView(int cmd) {
    return cmd == Command.PLAYSELECTEDSONG || cmd == Command.PREV || cmd == Command.NEXT
        || cmd == Command.PLAY_TEMP;
  }

  /**
   * 清除锁屏显示的内容
   */
  private void cleanMetaData() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mMediaSession.setMetadata(new MediaMetadataCompat.Builder().build());
      mMediaSession.setPlaybackState(
          new PlaybackStateCompat.Builder().setState(PlaybackState.STATE_NONE, 0, 1f).build());
    }
  }

  /**
   * 更新
   */
  private void update(int control) {
    mUpdateUIHandler.sendEmptyMessage(UPDATE_META_DATA);
//        if (updateAllView(control)) {
//            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_META_DATA);
//        } else if (!mMediaPlayer.playedOnce()) {
//            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_META_DATA);
//        } else if (updatePlayStateOnly(control)) {
//            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_PLAY_STATE);
//        }
  }

  public MediaSessionCompat getMediaSession() {
    return mMediaSession;
  }

  /**
   * 更新锁屏
   */
  private void updateMediaSession(int control) {
    if (mCurrentSong == null) {
      mMediaSession.setMetadata(null);
      return;
    }
    boolean isSmartisan = Build.MANUFACTURER.equalsIgnoreCase("smartisan");
    if ((!isSmartisan && SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
            Constants.APLAYER_LOCKSCREEN) == Constants.CLOSE_LOCKSCREEN)) {
      mMediaSession.setMetadata(null);
      return;
    }

    MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mCurrentSong.getAlbum())
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mCurrentSong.getArtist())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, mCurrentSong.getArtist())
        .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, mCurrentSong.getDisplayname())
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mCurrentSong.getDuration())
        .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, mCurrentIndex + 1)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mCurrentSong.getTitle());
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      builder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mPlayQueue.size());
    }

    if (updatePlayStateOnly(control)) {
      mMediaSession.setMetadata(builder.build());
    } else {
      new RemoteUriRequest(getSearchRequestWithAlbumType(mCurrentSong),
          new RequestConfig.Builder(400, 400).build()) {
        @Override
        public void onError(String errMsg) {
          setMediaSessionData(null);
        }

        @Override
        public void onSuccess(Bitmap result) {
          setMediaSessionData(result);
        }

        private void setMediaSessionData(Bitmap result) {
          result = copy(result);
          builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, result);
          mMediaSession.setMetadata(builder.build());
        }
      }.load();
    }

    mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
        .setState(mIsPlay ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
            getProgress(), mSpeed)
        .setActions(MEDIA_SESSION_ACTIONS).build());
  }

  /**
   * 准备播放
   *
   * @param path 播放歌曲的路径
   */

  private void prepare(final String path, final boolean requestFocus) {
    Timber.v("准备播放: %s", path);
    if (TextUtils.isEmpty(path)) {
      ToastUtil.show(mService, getString(R.string.path_empty));
      return;
    }
    if (requestFocus) {
      mAudioFocus = mAudioManager
          .requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
              AudioManager.AUDIOFOCUS_GAIN) ==
          AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
      if (!mAudioFocus) {
        ToastUtil.show(mService, getString(R.string.cant_request_audio_focus));
        return;
      }
    }
    if (isPlaying()) {
      pause(true);
    }
    mIsInitialized = false;
//    openAudioEffectSession();

    mPlaybackHandler.post(() -> {
      try {
        mMediaPlayer.reset();
        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
        mMediaPlayer.setDataSource(path);
        mMediaPlayer.prepareAsync();
        mIsInitialized = true;
      } catch (Exception e) {
        ToastUtil.show(mService, getString(R.string.play_failed) + e.toString());
        mIsInitialized = false;
      }
    });
  }

  /**
   * 准备播放
   *
   * @param path 播放歌曲的路径
   */
  private void prepare(final String path) {
    prepare(path, true);
  }

  /**
   * 根据当前播放模式，切换到上一首或者下一首
   *
   * @param isNext 是否是播放下一首
   */
  public void playNextOrPrev(boolean isNext) {
    if (mPlayQueue == null || mPlayQueue.size() == 0) {
      ToastUtil.show(mService, getString(R.string.list_is_empty));
      return;
    }
    Timber.v("播放下一首");
    if (isNext) {
      //如果是点击下一首 播放预先设置好的下一首歌曲
      mCurrentId = mNextId;
      mCurrentIndex = mNextIndex;
      mCurrentSong = new Song(mNextSong);
    } else {
      final List<Integer> queue = new ArrayList<>(mPlayModel == Constants.PLAY_SHUFFLE ?
          mRandomQueue : mPlayQueue);
      //如果点击上一首
      if ((--mCurrentIndex) < 0) {
        mCurrentIndex = queue.size() - 1;
      }
      if (mCurrentIndex == -1 || (mCurrentIndex > queue.size() - 1)) {
        return;
      }
      mCurrentId = queue.get(mCurrentIndex);

      mCurrentSong = MediaStoreUtil.getSongById(mCurrentId);
      mNextIndex = mCurrentIndex;
      mNextId = mCurrentId;
    }
    updateNextSong();
    if (mCurrentSong == null) {
      ToastUtil.show(mService, R.string.song_lose_effect);
      return;
    }
    setPlay(true);
    prepare(mCurrentSong.getUrl());

  }

  /**
   * 更新下一首歌曲
   */
  public void updateNextSong() {
    if (mPlayQueue == null || mPlayQueue.size() == 0) {
      ToastUtil.show(mService, R.string.list_is_empty);
      return;
    }

    if (mPlayModel == Constants.PLAY_SHUFFLE) {
      if (mRandomQueue.size() == 0) {
        makeShuffleList(mCurrentId);
      }
      if ((++mNextIndex) >= mRandomQueue.size()) {
        mNextIndex = 0;
      }
      mNextId = mRandomQueue.get(mNextIndex);
    } else {
      if ((++mNextIndex) >= mPlayQueue.size()) {
        mNextIndex = 0;
      }
      mNextId = mPlayQueue.get(mNextIndex);
    }
    mNextSong = MediaStoreUtil.getSongById(mNextId);
  }

  public IMediaPlayer getMediaPlayer() {
    return mMediaPlayer;
  }

  /**
   * 获得播放模式
   */
  public int getPlayModel() {
    return mPlayModel;
  }

  /**
   * 设置播放模式并更新下一首歌曲
   */
  public void setPlayModel(int playModel) {
    mPlayModel = playModel;
    updateAppwidget();
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, mPlayModel);
    //保存正在播放和下一首歌曲
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID, mNextId);
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, mCurrentId);
    if (mPlayModel == Constants.PLAY_SHUFFLE) {
      mRandomQueue.clear();
      makeShuffleList(mCurrentId);
    }
  }

  /**
   * 生成随机播放列表
   */
  public void makeShuffleList(int current) {
    if (mRandomQueue == null) {
      mRandomQueue = new ArrayList<>();
    }
    mRandomQueue.clear();
    mRandomQueue.addAll(mPlayQueue);
    if (mRandomQueue.isEmpty()) {
      return;
    }
//        if (current >= 0) {
//            boolean removed = mRandomQueue.remove(Integer.valueOf(current));
//            Collections.shuffle(mRandomQueue);
//            if(removed)
//                mRandomQueue.add(0,current);
//        } else {
//            Collections.shuffle(mRandomQueue);
//        }
    Collections.shuffle(mRandomQueue);
  }

  /**
   * 获得是否正在播放
   */
  public boolean isPlaying() {
    return mIsPlay;
  }

  /**
   * 设置MediaPlayer播放进度
   */
  public void setProgress(long current) {
    if (mMediaPlayer != null && mIsInitialized) {
      mMediaPlayer.seekTo(current);
    }
  }

  /**
   * 返回当前播放歌曲
   */
  public Song getCurrentSong() {
    return mCurrentSong != null ? mCurrentSong : Song.EMPTY_SONG;
  }

  public void setCurrentSong(Song song) {
    if (song != null) {
      mCurrentSong = song;
    }
  }

  /**
   * 返回下一首播放歌曲
   */
  public Song getNextSong() {
    return mNextSong;
  }

  /**
   * 获得当前播放进度
   */
  public int getProgress() {
    try {
      if (mMediaPlayer != null && mIsInitialized) {
        return (int) mMediaPlayer.getCurrentPosition();
      }
    } catch (IllegalStateException e) {
      Timber.v("getProgress() %s", e.toString());
    }
    return 0;
  }

  public long getDuration() {
    if (mMediaPlayer != null && mIsInitialized) {
      return mMediaPlayer.getDuration();
    }
    return 0;
  }

  public void setSpeed(float speed) {
    if (mMediaPlayer != null && mIsInitialized) {
      mSpeed = speed;
      mMediaPlayer.setSpeed(mSpeed);
    }
  }


  public void setOperation(int operation) {
    mOperation = operation;
  }

  public int getOperation() {
    return mOperation;
  }

  /**
   * 读取歌曲id列表与播放队列
   */
  private void loadSync() {
    mPlaybackHandler.post(this::load);
  }

  @WorkerThread
  private synchronized void load() {
    //迁移数据
    Migration.migrationDatabase(this);

    final boolean isFirst = SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, true);
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SETTING_KEY.FIRST_LOAD, false);
    //读取sd卡歌曲id
    mAllSong = MediaStoreUtil.getAllSongsId();
    //第一次启动软件
    if (isFirst) {
      //新建我的收藏
      mDBRepository.insertPlayList(DatabaseRepository.getMyLove()).subscribe(new LogObserver() {
        @Override
        public void onSuccess(@NotNull Object value) {
          super.onSuccess(value);
        }

        @Override
        public void onError(@NotNull Throwable e) {
          super.onError(e);
        }
      });
      //默认全部歌曲为播放列表
      setPlayQueue(mAllSong);
      //通知栏样式
      SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
          Build.VERSION.SDK_INT < Build.VERSION_CODES.N);
    } else {
      //播放模式
      mPlayModel = SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
          PLAY_LOOP);
      //读取播放列表
      mPlayQueue = mDBRepository.getPlayQueue().blockingGet();

      mShowDesktopLyric = SPUtil
          .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false);
    }

    if (mPlayQueue == null || mPlayQueue.isEmpty()) {
      setPlayQueue(mAllSong);
    }

    //摇一摇
    if (SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHAKE, false)) {
      ShakeDetector.getInstance().beginListen();
    }
    //播放倍速
    mSpeed = Float.parseFloat(
        SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SPEED, "1.0"));
    restoreLastSong();
    mLoadFinished = true;
    mUpdateUIHandler.postDelayed(() -> sendLocalBroadcast(new Intent(META_CHANGE)), 400);
//        sendLocalBroadcast(new Intent(ACTION_LOAD_FINISH).putExtra("Song", mCurrentSong));
//        openAudioEffectSession();
  }


  /**
   * 初始化上一次退出时时正在播放的歌曲
   */
  private void restoreLastSong() {
    if (mPlayQueue == null || mPlayQueue.size() == 0) {
      return;
    }
    //读取上次退出时正在播放的歌曲的id
    int lastId = SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, -1);
    //上次退出时正在播放的歌曲是否还存在
    boolean isLastSongExist = false;
    //上次退出时正在播放的歌曲的pos
    int pos = 0;
    //查找上次退出时的歌曲是否还存在
    if (lastId != -1) {
      for (int i = 0; i < mPlayQueue.size(); i++) {
        if (lastId == mPlayQueue.get(i)) {
          isLastSongExist = true;
          pos = i;
          break;
        }
      }
    }

    Song item;
    mPlayAtBreakPoint = SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false);
    mLastProgress = mPlayAtBreakPoint ?
        SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0)
        :
            0;
    //上次退出时保存的正在播放的歌曲未失效
    if (isLastSongExist && (item = MediaStoreUtil.getSongById(lastId)) != null) {
      setUpDataSource(item, pos);
    } else {
      mLastProgress = 0;
      SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0);
      //重新找到一个歌曲id
      int id = mPlayQueue.get(0);
      for (int i = 0; i < mPlayQueue.size(); i++) {
        id = mPlayQueue.get(i);
        if (id != lastId) {
          break;
        }
      }
      item = MediaStoreUtil.getSongById(id);
      SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_SONG_ID, id);
      setUpDataSource(item, 0);
    }
  }

  public void deleteSongFromService(List<Song> deleteSongs) {
    if (deleteSongs != null && deleteSongs.size() > 0) {
      List<Integer> ids = new ArrayList<>();
      for (Song song : deleteSongs) {
        ids.add(song.getId());
      }
      mAllSong.removeAll(ids);
      mPlayQueue.removeAll(ids);
    }
  }


  /**
   * 释放电源锁
   */
  private void releaseWakeLock() {
    if (mWakeLock != null && mWakeLock.isHeld()) {
      mWakeLock.release();
    }
  }

  /**
   * 获得电源锁
   */
  private void acquireWakeLock() {
    if (mWakeLock != null) {
      mWakeLock.acquire(mCurrentSong != null ? mCurrentSong.getDuration() : 30000L);
    }
  }


  /**
   * 更新桌面歌词
   */
  private boolean mFirstUpdateLrc = true;

  private void updateDesktopLyric(boolean force) {
    if (checkNoPermission()) { //没有权限
      return;
    }
    if (!mShowDesktopLyric) { //移除桌面歌词
      mUpdateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC);
    } else if (!updatePlayStateOnly(getOperation()) || force || mFirstUpdateLrc) { //更新
      //屏幕点亮才更新
      if (mScreenOn) {
        createDesktopLyricThreadIfNeed();
        if (mUpdateDesktopLyricThread != null) {
          mUpdateDesktopLyricThread.setSongAndGetLyricRows(mCurrentSong);
        }
        mFirstUpdateLrc = false;
      }
    }
  }

  /**
   * 创建更新桌面歌词的线程
   */
  private boolean createDesktopLyricThreadIfNeed() {
    Timber.v("createDesktopLyricThreadIfNeed() %s", isDesktopLyricShowing());
    if (mShowDesktopLyric && mUpdateDesktopLyricThread == null) {
      mUpdateDesktopLyricThread = new UpdateDesktopLyricThread();
      mUpdateDesktopLyricThread.start();
      return true;
    }
    return false;
  }

  /**
   * 判断是否有悬浮窗权限 没有权限关闭桌面歌词
   */
  private boolean checkNoPermission() {
    try {
      if (!FloatWindowManager.getInstance().checkPermission(mService)) {
        closeDesktopLyric();
        return true;
      }
      return false;
    } catch (Exception e) {
      Timber.v(e);
    }
    return true;
  }

  /**
   * 复制bitmap
   */
  public static Bitmap copy(Bitmap bitmap) {
    if (bitmap == null || bitmap.isRecycled()) {
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
    //屏幕没点亮就不更新了
    if (!mScreenOn) {
      return;
    }
    for (Map.Entry<String, BaseAppwidget> entry : mAppWidgets.entrySet()) {
      if (entry.getValue() != null) {
        entry.getValue().updateWidget(mService, null, true);
      }
    }

    //暂停停止更新进度条和时间
    if (!isPlaying()) {
      stopUpdateAppWidget();
    } else {
      //开始播放后持续更新进度条和时间
      startUpdateAppWidget();
    }
  }

  private void stopUpdateAppWidget() {
    if (mWidgetTimer != null) {
      mWidgetTimer.cancel();
      mWidgetTimer = null;
    }
    if (mWidgetTask != null) {
      mWidgetTask.cancel();
      mWidgetTask = null;
    }
  }

  private void startUpdateAppWidget() {
    if (mWidgetTimer != null || !mScreenOn) {
      return;
    }
    mWidgetTimer = new Timer();
    mWidgetTask = new WidgetTask();
    mWidgetTimer.schedule(mWidgetTask, 1000, 1000);
  }

  /**
   * 更新桌面歌词
   */
//    public String mCurrentLrc;
  private UpdateDesktopLyricThread mUpdateDesktopLyricThread;
  private boolean mNeedShowDesktopLyric;
  private LyricRowWrapper mCurrentLrc = new LyricRowWrapper();

  private class UpdateDesktopLyricThread extends UpdateLyricThread {

    private final String TAG = UpdateDesktopLyricThread.class.getSimpleName();

    UpdateDesktopLyricThread() {
      super(MusicService.this);
      Timber.tag(TAG).v("创建线程");
    }

    void quitByNotification() {
      interrupt();
      mNeedShowDesktopLyric = true;
      mShowDesktopLyric = false;
    }

    void quitDelay() {
      mPlaybackHandler.postDelayed(this::quitImmediately, LRC_INTERVAL);
    }

    @Override
    public void run() {
      while (mShowDesktopLyric) {
        try {
//                    int interval = getInterval();
//                    LogUtil.d("DesktopLrc","间隔:" + interval);
          Thread.sleep(LRC_INTERVAL);
        } catch (InterruptedException e) {
          Timber.tag(TAG).v("捕获异常,线程退出");
          mUpdateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC);
          return;
        }
        //判断权限
        if (checkNoPermission()) {
          return;
        }
        if (mIsServiceStop) {
          mUpdateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC);
          return;
        }
        //当前应用在前台
        if (Util.isAppOnForeground()) {
          if (isDesktopLyricShowing()) {
            mUpdateUIHandler.sendEmptyMessage(REMOVE_DESKTOP_LRC);
          }

        } else {
          if (!isDesktopLyricShowing()) {
            mUpdateUIHandler.removeMessages(CREATE_DESKTOP_LRC);
            Timber.tag(TAG).v("请求创建桌面歌词");
            mUpdateUIHandler.sendEmptyMessageDelayed(CREATE_DESKTOP_LRC, 50);
          } else {
            mCurrentLrc = findCurrentLyric();
            Timber.tag(TAG).v("当前歌词: %s", mCurrentLrc);
            mUpdateUIHandler.obtainMessage(UPDATE_DESKTOP_LRC_CONTENT).sendToTarget();
          }
        }
      }
    }

  }

  /**
   * 创建桌面歌词悬浮窗
   */
  private boolean mIsDesktopLyricInitializing = false;

  private void createDesktopLyric() {
    if (checkNoPermission()) {
      return;
    }
    if (mIsDesktopLyricInitializing) {
      return;
    }
    mIsDesktopLyricInitializing = true;

    final WindowManager.LayoutParams param = new WindowManager.LayoutParams();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      param.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      param.type = WindowManager.LayoutParams.TYPE_PHONE;
    }

    param.format = PixelFormat.RGBA_8888;
    param.gravity = Gravity.TOP;
    param.width = mService.getResources().getDisplayMetrics().widthPixels;
    param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    param.x = 0;
    param.y = SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_Y, 0);

    if (mDesktopLyricView != null) {
      mWindowManager.removeView(mDesktopLyricView);
      mDesktopLyricView = null;
    }

    mDesktopLyricView = new DesktopLyricView(mService);
    mWindowManager.addView(mDesktopLyricView, param);
    mIsDesktopLyricInitializing = false;
    Timber.v("创建桌面歌词");
  }

  /**
   * 移除桌面歌词
   */
  private void removeDesktopLyric() {
    if (mDesktopLyricView != null) {
      Timber.v("移除桌面歌词");
//      mDesktopLyricView.cancelNotify();
      mWindowManager.removeView(mDesktopLyricView);
      mDesktopLyricView = null;
    }
  }

  /**
   * 桌面歌词是否显示
   */
  public boolean isDesktopLyricShowing() {
    return mDesktopLyricView != null;
  }

  /**
   * 桌面歌词是否锁定
   */
  public boolean isDesktopLyricLocked() {
    return mDesktopLyricView == null ?
        SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, false) :
        mDesktopLyricView.isLocked();
  }

  /**
   * 关闭桌面歌词
   */
  private void closeDesktopLyric() {
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW, false);
    mShowDesktopLyric = false;
    mUpdateDesktopLyricThread = null;
    mUpdateUIHandler.removeMessages(CREATE_DESKTOP_LRC);
    mUpdateUIHandler.sendEmptyMessageDelayed(REMOVE_DESKTOP_LRC, LRC_INTERVAL);
  }

  /**
   * 更新桌面部件进度
   */
  private Timer mWidgetTimer;
  private TimerTask mWidgetTask;

  private class WidgetTask extends TimerTask {

    @Override
    public void run() {
      for (Map.Entry<String, BaseAppwidget> entry : mAppWidgets.entrySet()) {
        if (entry.getValue() != null) {
          entry.getValue().partiallyUpdateWidget(mService);
        }
      }
      if (mPlayAtBreakPoint) {
        final int progress = getProgress();
        if (progress > 0) {
          SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS,
              progress);
        }
      }
    }
  }

  private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {

    //记录焦点变化之前是否在播放;
    private boolean mNeedContinue = false;

    @Override
    public void onAudioFocusChange(int focusChange) {
      switch (focusChange) {
        case AudioManager.AUDIOFOCUS_GAIN://获得AudioFocus
          mAudioFocus = true;
          if (mMediaPlayer == null) {
            setUp();
          } else if (mNeedContinue) {
            play(true);
            mNeedContinue = false;
            setOperation(Command.TOGGLE);
          }
          mVolumeController.directTo(1);
          break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://短暂暂停
          mNeedContinue = mIsPlay;
          if (mIsPlay && mMediaPlayer != null) {
            setOperation(Command.TOGGLE);
            pause(false);
          }
          break;
        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://减小音量
          mVolumeController.directTo(.1f);
          break;
        case AudioManager.AUDIOFOCUS_LOSS://暂停
          mAudioFocus = false;
          if (mIsPlay && mMediaPlayer != null) {
            setOperation(Command.TOGGLE);
            pause(false);
          }
          break;
      }
      //通知更新ui
//            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
//            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_META_DATA);
    }
  }


  private static class PlaybackHandler extends Handler {

    private final WeakReference<MusicService> mRef;

    PlaybackHandler(MusicService service, Looper looper) {
      super(looper);
      mRef = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
    }
  }

  private static class UpdateUIHandler extends Handler {

    private final WeakReference<MusicService> mRef;

    UpdateUIHandler(MusicService service) {
      super();
      mRef = new WeakReference<>(service);
    }

    @Override
    public void handleMessage(Message msg) {
      if (mRef.get() == null) {
        return;
      }
      MusicService musicService = mRef.get();
      switch (msg.what) {
        case UPDATE_PLAY_STATE:
          musicService.handlePlayStateChange();
          break;
        case UPDATE_META_DATA:
//                    musicService.handlePlayStateChange();
          musicService.handleMetaChange();
          break;
        case UPDATE_DESKTOP_LRC_CONTENT:
          LyricRowWrapper wrapper = musicService.mCurrentLrc;
          if (musicService.mDesktopLyricView != null) {
            if (wrapper == null) {
              musicService.mDesktopLyricView
                  .setText(UpdateLyricThread.NO_ROW, UpdateLyricThread.EMPTY_ROW);
            } else if (wrapper.getStatus() == UpdateLyricThread.Status.SEARCHING) {
              musicService.mDesktopLyricView
                  .setText(UpdateLyricThread.SEARCHING_ROW, UpdateLyricThread.EMPTY_ROW);
            } else if (wrapper.getStatus() == UpdateLyricThread.Status.ERROR ||
                wrapper.getStatus() == UpdateLyricThread.Status.NO) {
              musicService.mDesktopLyricView
                  .setText(UpdateLyricThread.NO_ROW, UpdateLyricThread.EMPTY_ROW);
            } else {
              musicService.mDesktopLyricView.setText(wrapper.getLineOne(), wrapper.getLineTwo());
            }
          }
          break;
        case REMOVE_DESKTOP_LRC:
          musicService.removeDesktopLyric();
          break;
        case CREATE_DESKTOP_LRC:
          musicService.createDesktopLyric();
          break;
      }
    }
  }

  private class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (Intent.ACTION_SCREEN_ON.equals(action)) {
        mScreenOn = true;
        //显示锁屏
        if (mIsPlay && SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCKSCREEN,
            Constants.APLAYER_LOCKSCREEN) == Constants.APLAYER_LOCKSCREEN) {
          context.startActivity(new Intent(context, LockScreenActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        //重新显示桌面歌词
        createDesktopLyricThreadIfNeed();
        //重新开始更新桌面部件
        startUpdateAppWidget();
      } else {
        mScreenOn = false;
        //停止更新桌面部件
        stopUpdateAppWidget();
        //屏幕熄灭 关闭桌面歌词
        if (mShowDesktopLyric && isDesktopLyricShowing() && mUpdateDesktopLyricThread != null) {
          mUpdateDesktopLyricThread.quitImmediately();
          mUpdateDesktopLyricThread = null;
        }
      }
    }
  }
}
