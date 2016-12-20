package remix.myplayer.service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.gsm.GsmCellLocation;
import android.view.KeyEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.db.PlayLists;
import remix.myplayer.model.MP3Item;
import remix.myplayer.observer.DBObserver;
import remix.myplayer.observer.MediaStoreObserver;
import remix.myplayer.receiver.HeadsetPlugReceiver;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.EQActivity;
import remix.myplayer.ui.activity.FolderActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
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
public class MusicService extends BaseService {
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


    /** AudiaoManager */
    private AudioManager mAudioManager;

    /** 回调接口的集合 */
    private static List<Callback> mCallBacklist  = new ArrayList<Callback>(){};

    /** 播放控制的Receiver */
    private ControlReceiver mRecevier;

    /** 监测耳机拔出的Receiver*/
    private HeadsetPlugReceiver mHeadSetReceiver;

    /** 监听AudioFocus的改变 */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;

    /** MediaSession */
    private MediaSessionCompat mMediaSession = null;

    /** 当前是否获得AudioFocus */
    private boolean mAudioFouus = false;

    /** 更新相关Activity的Handler */
    private static Handler mUpdateUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.UPDATE_UI) {
                for (int i = 0; i < mCallBacklist.size(); i++) {
                    if(mCallBacklist.get(i) != null){
                        try {
                            mCallBacklist.get(i).UpdateUI(mCurrentInfo,mIsplay);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    };

    private MediaStoreObserver mMediaStoreObserver;
    private DBObserver mPlayListObserver;
    private DBObserver mPlayListSongObserver;
    private static Context mContext;

    public MusicService(){}
    public MusicService(Context context) {
        mContext = context;
    }

    public static MusicService getInstance(){
        return mInstance;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unInit();
    }
    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        mInstance = this;
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        Global.setHeadsetOn(mAudioManager.isWiredHeadsetOn());
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
                        playStart();
                        mNeedContinue = false;
                        Global.setOperation(Constants.PLAYORPAUSE);
                    }
                    mMediaPlayer.setVolume(1.0f,1.0f);
                }

                //暂停播放
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
                    mNeedContinue = mIsplay;
                    if(mIsplay && mMediaPlayer != null){
                        Global.setOperation(Constants.PLAYORPAUSE);
                        pause();
                    }
                }

                //失去audiofocus 暂停播放
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                    mAudioFouus = false;
                    if(mIsplay && mMediaPlayer != null) {
                        Global.setOperation(Constants.PLAYORPAUSE);
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
        init();
    }

    private void init() {
        //初始化两个Receiver
        mRecevier = new ControlReceiver();
        registerReceiver(mRecevier,new IntentFilter("remix.music.CTL_ACTION"));
        mHeadSetReceiver = new HeadsetPlugReceiver();
        registerReceiver(mHeadSetReceiver,new IntentFilter(Intent.ACTION_HEADSET_PLUG));

        //监听媒体库变化
        Handler updateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //更新adapter
                if(msg.what == Constants.UPDATE_ADAPTER ){
                    if (FolderActivity.mInstance != null ) {
                        FolderActivity.mInstance.UpdateList();
                    }
                    if(ChildHolderActivity.mInstance != null ){
                        ChildHolderActivity.mInstance.UpdateList();
                    }
                }
            }
        };
        mMediaStoreObserver = new MediaStoreObserver(updateHandler);
        //监听数据库变化

        mPlayListObserver = new DBObserver(updateHandler);
        mPlayListSongObserver = new DBObserver(updateHandler);
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
            }
        });
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playStart();
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                LogUtil.d(TAG, "what = " + what + " extar = " + extra);
                return true;
            }
        });
        //初始化MediaExtractor
        mMediaExtractor = new MediaExtractor();

        //初始化音效设置
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
        if(!CommonUtil.isIntentAvailable(this,i)){
            EQActivity.Init();
        }
    }

    /**
     * 初始化mediaplayer
     * @param item
     * @param pos
     */
    public static void initDataSource(MP3Item item,int pos){
        if(item == null)
            return;
        //初始化当前播放歌曲
        mCurrentInfo = item;
        mCurrentId = mCurrentInfo.getId();
        mCurrentIndex = pos;
        try {
            if(mMediaPlayer == null)
                mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(mCurrentInfo.getUrl());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化下一首歌曲
        mNextId = SPUtil.getValue(APlayerApplication.getContext(),"Setting","NextSongId",-1);
        if(mNextId == -1)
            return;
        //查找上次退出时保存的下一首歌曲是否还存在
        //如果不存在，重新设置下一首歌曲
        mNextIndex = Global.mPlayQueue.indexOf(mNextId);
        mNextInfo = MediaStoreUtil.getMP3InfoById(mNextId);
        if(mNextInfo != null)
            return;
        mNextIndex = mCurrentIndex + 1;
        updateNextSong();
    }

    private void unInit(){
        if(mMediaPlayer != null)
            mMediaPlayer.release();
        if(mMediaExtractor != null)
            mMediaExtractor.release();
        mMediaPlayer = null;

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSession.release();
        unregisterReceiver(mRecevier);
        unregisterReceiver(mHeadSetReceiver);
        getContentResolver().unregisterContentObserver(mMediaStoreObserver);
        getContentResolver().unregisterContentObserver(mPlayListObserver);
        getContentResolver().unregisterContentObserver(mPlayListSongObserver);
    }


    /**
     * 播放下一首
     */
    private void playNext() {
        playNextOrPrev(true,true);
    }

    /**
     * 播放上一首
     */
    private void playPrevious() {
        playNextOrPrev(false, true);
    }

    /**
     * 开始播放
     */
    private void playStart() {
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
                Update(Global.getOperation());
                //保存当前播放的下一首播放的歌曲的id
                SPUtil.putValue(mContext,"Setting","LastSongId", mCurrentId);
                SPUtil.putValue(mContext,"Setting","NextSongId",mNextId);
            }
        }.start();
    }


    /**
     * 根据当前播放状态暂停或者继续播放
     */
    private void PlayOrPause() {
        if(mMediaPlayer.isPlaying()) {
            pause();
        }
        else {
            if(mCurrentInfo == null)
                return;
            if(mFirstFlag) {
                prepareAndPlay(mCurrentInfo.getUrl());
                mFirstFlag = false;
                return;
            }
            playStart();
        }
    }

    /**
     * 暂停
     */
    private void pause() {
        mIsplay = false;
        mMediaPlayer.pause();
        //更新所有界面
        Update(Global.getOperation());
    }

    /**
     * 播放选中的歌曲
     * 比如在全部歌曲或者专辑详情里面选中某一首歌曲
     * @param position 播放索引
     */
    private void playSelectSong(int position){
        if((mCurrentIndex = position) == -1 || (mCurrentIndex > Global.mPlayQueue.size() - 1))
            return;

        mCurrentId = Global.mPlayQueue.get(mCurrentIndex);
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


    /**
     * 回调接口，当发生更新时，通知相关activity更新
     */
    public interface Callback {
        void UpdateUI(MP3Item MP3Item, boolean isplay);
        int getType();
    }

    /**
     * @param callback
     */
    public static void addCallback(Callback callback) {
        for(int i = mCallBacklist.size() - 1 ; i >= 0 ; i--){
            if(callback.getType() == mCallBacklist.get(i).getType()){
                mCallBacklist.remove(i);
            }
        }
        mCallBacklist.add(callback);
    }

    /**
     * 将callback移出队列
     * @param callback
     */
    public static void removeCallback(Callback callback){
        if(mCallBacklist != null && mCallBacklist.size() > 0){
            mCallBacklist.remove(callback);
        }
    }

    /**
     * 接受控制命令
     * 包括暂停、播放、上下首、播放模式
     */
    public class ControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getExtras() == null)
                return;
            int Control = intent.getIntExtra("Control",-1);
            //保存控制命令,用于播放界面判断动画
            Global.setOperation(Control);
            //先判断是否是关闭通知栏
            if(intent.getExtras().getBoolean("Close")){
                NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(0);
                Global.setNotifyShowing(false);
                pause();
                Update(Control);
                return;
            }

            if(Control == Constants.PLAYSELECTEDSONG || Control == Constants.PREV || Control == Constants.NEXT
                    || Control == Constants.PLAYORPAUSE || Control == Constants.PAUSE || Control == Constants.START){
                if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
                    return;
                if(CommonUtil.isFastDoubleClick()) {
                    ToastUtil.show(mContext,R.string.not_operate_fast);
                    return;
                }
            }

            switch (Control) {
                //播放listview选中的歌曲
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
                case Constants.PLAYORPAUSE:
                    if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
                        return;
                    mIsplay = !mIsplay;
                    PlayOrPause();
                    break;
                //暂停
                case Constants.PAUSE:
                    pause();
                    break;
                //继续播放
                case Constants.START:
                    playStart();
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
    private void Update(int control){
        if(control != Constants.PLAY_LOOP &&
                control != Constants.PLAY_SHUFFLE &&
                control != Constants.PLAY_REPEATONE) {
            //更新相关activity
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
            //更新通知栏
            sendBroadcast(new Intent(Constants.NOTIFY));
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
                    if(!mAudioFouus)
                        return;
                    mIsIniting = true;
                    mMediaPlayer.reset();
                    mMediaPlayer.setDataSource(path);

                    mMediaPlayer.prepareAsync();
                    mFirstFlag = false;
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
     * @return 随机索引
     */
    private static int getShuffle(){
        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return 0;
        if(Global.mPlayQueue.size() == 1)
            return 0;
        return new Random().nextInt(Global.mPlayQueue.size() - 1);
    }

    /**
     * 根据当前播放模式，切换到上一首或者下一首
     * @param isNext 是否是播放下一首
     * @param needPlay 是否需要播放
     */
    public void playNextOrPrev(boolean isNext, boolean needPlay){
        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return;

        if(isNext){
            //如果是点击下一首 播放预先设置好的下一首歌曲
            mCurrentId = mNextId;
            mCurrentIndex = mNextIndex;
            mCurrentInfo = new MP3Item(mNextInfo);
        } else {
            //如果点击上一首 根据播放模式查找上一首歌曲 再重新设置当前歌曲为上一首
            if ((--mCurrentIndex) < 0)
                mCurrentIndex = Global.mPlayQueue.size() - 1;
            if(mCurrentIndex  == -1 || (mCurrentIndex > Global.mPlayQueue.size() - 1))
                return;
            mCurrentId = Global.mPlayQueue.get(mCurrentIndex);
            mCurrentInfo = MediaStoreUtil.getMP3InfoById(mCurrentId);
            mNextIndex = mCurrentIndex;
            mNextId = mCurrentId;
        }

        MP3Item temp = mCurrentInfo;
        if(mCurrentInfo == null) {
            mCurrentInfo = temp;
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
    private static void updateNextSong(){
        if(mPlayModel == Constants.PLAY_LOOP || mPlayModel == Constants.PLAY_REPEATONE){
            if ((++mNextIndex) > Global.mPlayQueue.size() - 1)
                mNextIndex = 0;
            mNextId = Global.mPlayQueue.get(mNextIndex);
        } else{
            mNextIndex = getShuffle();
            mNextId = Global.mPlayQueue.get(mNextIndex);
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
    }

    /**
     * 获得是否正在播放
     * @return
     */
    public static boolean getIsplay() {
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
                int arg = keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ? Constants.PLAYORPAUSE :
                        keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT : Constants.PREV;
                intent_ctl.putExtra("Control", arg);
                getApplicationContext().sendBroadcast(intent_ctl);
                return true;
            }

            LogUtil.d(TAG,"count=" + mCount);
            LogUtil.d(TAG,"AudioFocus:" + mAudioFouus);
//            if(!mAudioFouus)
//                return true;
            //如果是第一次按下，开启一条线程去判断用户操作
            if(mCount == 0){
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            sleep(800);
                            int arg = -1;
                            arg = mCount == 1 ? Constants.PLAYORPAUSE : mCount == 2 ? Constants.NEXT : Constants.PREV;
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
}
