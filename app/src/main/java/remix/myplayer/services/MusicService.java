package remix.myplayer.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.KeyEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.SharedPrefsUtil;


/**
 * Created by Remix on 2015/12/1.
 */
public class MusicService extends Service {
    private final static String TAG = "MusicService";
    //实例
    public static MusicService mInstance;
    //是否第一次启动
    private static boolean mFirstFlag = true;
    //播放模式
    private static int mPlayModel = Constants.PLAY_LOOP;
    //当前是否在播放
    private static Boolean mIsplay = false;
    //记录当前播放的角标
    private static int mCurrent = 0;
    //记录下一首歌曲的角标
    private static int mNext = 1;
    //当前正在播放的歌曲id
    private static long mId = -1;
    //当前正在播放的mp3
    private static MP3Info mInfo = null;
    private static MediaPlayer mPlayer;
    private int mMaxVolume = -1;
    private int mCurrentVolume = -1;
    private AudioManager mAudioManager;
    //回调接口的集合
    private static List<Callback> mCallBacklist  = new ArrayList<Callback>(){};
    private Context mContext;
    private PlayerReceiver mRecevier;
    private ComponentName mMediaComName;
    private PendingIntent mMediaPendingIntent;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
    private RemoteControlClient mRemoteCtrlClient;
    public static MediaSessionCompat mMediaSession = null;
    private boolean mAudioFouus = false;
    PlaybackStateCompat mPlayingbackState = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PLAY_PAUSE   |  PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .setState(PlaybackStateCompat.STATE_PLAYING,PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,SystemClock.elapsedRealtime() + 10000)
            .build();
    PlaybackStateCompat mNotPlaybackState = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PLAY_PAUSE   |  PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            .setState(PlaybackStateCompat.STATE_PAUSED,PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,1,SystemClock.elapsedRealtime() + 10000)
            .build();

    private static Handler mUpdateUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == Constants.UPDATE_INFORMATION) {
                try {
                    for (int i = 0; i < mCallBacklist.size(); i++) {
                        if(mCallBacklist.get(i) != null)
                            mCallBacklist.get(i).UpdateUI(mInfo,mIsplay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private PlaybackStateCompat getPlaybackStateCompat(int state,int position) {
        return new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_PAUSE   |  PlaybackStateCompat.ACTION_PAUSE
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(state,position,1.0f,SystemClock.elapsedRealtime() + 10000)
                .build();
    }
    public MusicService(){}
    public MusicService(Context context)
    {
        this.mContext = context;
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
//        if(mBinder == null)
//            mBinder = new PlayerBinder();
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        UnInit();
    }
    @Override
    public void onCreate() {

        super.onCreate();
        mContext = getApplicationContext();
        mInstance = this;
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            //记录焦点变化之前是否在播放;
            private boolean mFlag = false;
            @Override
            public void onAudioFocusChange(int focusChange) {

                switch (focusChange){
                    //播放 获得焦点
                    case AudioManager.AUDIOFOCUS_GAIN:
                        mAudioFouus = true;
                        if(mPlayer == null)
                            InitMediaPlayer();
                        else if(!mIsplay){
//                            PlayStart();
//                            mIsplay = true;
                        }
                        mPlayer.setVolume(1.0f,1.0f);
                        //如果之前正在播放，则继续播放
                        break;
                    //短暂失去焦点,比如有通知到来  暂停
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if(mIsplay && mPlayer != null) {
//                            new VolDownThread().start();
                            mPlayer.setVolume(0.1f,0.1f);
                        }
                        break;
                    //降低音量
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if(mIsplay && mPlayer != null){
//                            new VolDownThread().start();
                            mPlayer.setVolume(0.1f,0.1f);
                        }
                        break;
                    //暂停
                    case AudioManager.AUDIOFOCUS_LOSS:
                        mAudioFouus = false;
                        if(mIsplay && mPlayer != null) {
                            AudioHolderActivity.mOperation = Constants.PLAYORPAUSE;
                            Pause();
//                           UnInit();
                        }
                        break;
                }
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
//                NotifyService.mInstance.UpdateNotify();
                sendBroadcast(new Intent(Constants.NOTIFY));
            }
        };
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","Pos",-1);
        if(DBUtil.mPlayingList != null && DBUtil.mPlayingList.size() > 0) {
            mId = mPos == -1 ? DBUtil.mPlayingList.get(0) : DBUtil.mPlayingList.get(mPos);
            mInfo = DBUtil.getMP3InfoById(mId);
            mCurrent = mPos == -1 ? 0 : mPos;
        }
        else
            mInfo = null;

        InitMediaPlayer();

//        new Thread(){
//            @Override
//            public void run() {
//                while (true) {
//                    if (mInfo != null && mIsplay) {
//                        try {
//                            sleep(1000);
//                            UpdateLockScreen();
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        }.start();
    }

    private void InitMediaPlayer() {
        mRecevier = new PlayerReceiver();
        IntentFilter filter = new IntentFilter("remix.music.CTL_ACTION");
        registerReceiver(mRecevier,filter);

//        mMediaComName = new ComponentName(getPackageName(), LineCtlReceiver.class.getName());
//        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//        mediaButtonIntent.setComponent(mMediaComName);
//        mMediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent,0);

        mMediaSession = new MediaSessionCompat(getApplicationContext(),"session");

        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
//        mMediaSession.setPlaybackState(mNotPlaybackState);
        UpdateLockScreen();
        mMediaSession.setCallback(new SessionCallBack());
        mMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);

//        mMediaSession.setActive(true);


//        mRemoteCtrlClient = new RemoteControlClient(mMediaPendingIntent);
//        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
//                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
//                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
//                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
//                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
//                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
//        mRemoteCtrlClient.setTransportControlFlags(flags);
//        mAudioManager.registerRemoteControlClient(mRemoteCtrlClient);

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                PlayNextOrPrev(true,true);
                AudioHolderActivity.mOperation = Constants.NEXT;
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
                //更新锁屏界面
                UpdateLockScreen();
                //更新通知栏
                sendBroadcast(new Intent(Constants.NOTIFY));
//                NotifyService.mInstance.UpdateNotify();
                //更新所有Activity
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                PlayStart();
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG,"what = " + what + " extar = " + extra);
                return true;
            }
        });
    }

    private void UpdateLockScreen() {
        if(mInfo != null) {
//            Bitmap bitmap = DBUtil.CheckBitmapByAlbumId((int) mInfo.getAlbumId(), false);
//            if(bitmap == null)
//                bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.no_art_normal);
//            mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
//                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
//                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mInfo.getDisplayname())
//                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, mInfo.getAlbum() + " - " + mInfo.getArtist())
//                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,"res://remix.myplayer/"+ R.drawable.stat_notify)
//                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,mInfo.getDuration())
//                    .build());
        }
    }

    private void UnInit(){
        mPlayer.release();
        mPlayer = null;
//        mAudioManager.unregisterMediaButtonEventReceiver(mMediaPendingIntent);
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSession.release();
        unregisterReceiver(mRecevier);
    }


    private void PlayNext()
    {
        PlayNextOrPrev(true,true);
    }
    private void PlayPrevious()
    {
        PlayNextOrPrev(false,true);
    }
    private void PlayStart() {
        new Thread(){
            @Override
            //音量逐渐增大
            public void run(){
                mAudioFouus =  mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN) ==
                                              AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                if(!mAudioFouus)
                    return;
                mIsplay = true;
                mMediaSession.setPlaybackState(getPlaybackStateCompat(PlaybackStateCompat.STATE_PLAYING,getCurrentTime()));
                mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                mPlayer.start();
                if(mCurrentVolume == 0)
                    return;
                int temp = 0;
                int sleeptime = 100 / mCurrentVolume;
                while(temp++ < mCurrentVolume){
                    try {
                        sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, temp,
                            AudioManager.FLAG_PLAY_SOUND);
                }
//                mRemoteCtrlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }
        }.start();

    }
    private void PlayOrPause() {
        if(mPlayer.isPlaying())
            Pause();
        else {
            if(mInfo == null)
                return;
            if(mFirstFlag) {
//                PlayStart();
                PrepareAndPlay(mInfo.getUrl());
                mFirstFlag = false;
                return;
            }
            PlayStart();
        }
    }

    private void Pause() {
//        mIsplay = false;
//        mPlayer.pause();
        new Thread(){
            //音量逐渐减小后暂停
            @Override
            public void run(){
                mIsplay = false;
                mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mMediaSession.setPlaybackState(getPlaybackStateCompat(PlaybackStateCompat.STATE_PAUSED,getCurrentTime()));
                if(mCurrentVolume == 0){
                    mPlayer.pause();
                    return;
                }
                int sleeptime = 100 / mCurrentVolume;
                int temp = mCurrentVolume;
                while(temp-- > 0){
                    try {
                        sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, temp,
                            AudioManager.FLAG_PLAY_SOUND);
                }
                mPlayer.pause();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume,
                        AudioManager.FLAG_PLAY_SOUND);
//                mRemoteCtrlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }
        }.start();
    }
    private void PlaySelectSong(int position){
        if((mCurrent = position) == -1 || (mCurrent > DBUtil.mPlayingList.size()))
            return;
        mId = DBUtil.mPlayingList.get(mCurrent);
        mInfo = DBUtil.getMP3InfoById(mId);
        mIsplay = true;
        if(mInfo == null)
            return;
        PrepareAndPlay(mInfo.getUrl());
    }

    //回调接口，当发生更新时，通知所有activity更新
    public interface Callback {
        public void UpdateUI(MP3Info MP3info, boolean isplay);
        public int getType();
    }
    //添加回调接口的一个实现类
    public static void addCallback(Callback callback) {
        if(mCallBacklist.size() == 0)
            mCallBacklist.add(callback);
        else {
            for(int i = 0 ; i < mCallBacklist.size() ;i++){
               if(callback.getType() == mCallBacklist.get(i).getType()){
                   mCallBacklist.remove(i);
                   mCallBacklist.add(callback);
                   break;
               }
            }
            mCallBacklist.add(callback);
        }
    }
    //返回回调接口链表的长度
    public int getCallBackListSize()
    {
        return mCallBacklist.size();
    }
    public class PlayerReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getExtras().getBoolean("Close")){
                NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(0);
                return;
            }

            int Control = intent.getIntExtra("Control",-1);
            switch (Control) {
                //播放listview选中的歌曲
                case Constants.PLAYSELECTEDSONG:
                    PlaySelectSong(intent.getIntExtra("Position", -1));
                    break;
                //播放上一首
                case Constants.PREV:
                    PlayPrevious();
                    break;
                //播放下一首
                case Constants.NEXT:
                    PlayNext();
                    break;
                //暂停或者继续播放
                case Constants.PLAYORPAUSE:
                    mIsplay = !mIsplay;
                    PlayOrPause();
                    break;
                //暂停
                case Constants.PAUSE:
                    Pause();
                    break;
                //继续播放
                case Constants.CONTINUE:
                    PlayStart();
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
            //保存控制命令,用于播放界面判断动画
            AudioHolderActivity.mOperation = Control;

            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
//            if(intent.getBooleanExtra("FromNotify", false)){
//                NotifyService.mInstance.UpdateNotify();
//            }
//            NotifyService.mInstance.UpdateNotify();
            if(Control == Constants.NEXT ||
                    Control == Constants.PREV ||
                    Control == Constants.PLAYSELECTEDSONG ||
                    Control == Constants.PLAYORPAUSE) {
                //更新锁屏界面
                UpdateLockScreen();
                //更新通知栏
                sendBroadcast(new Intent(Constants.NOTIFY));
            }
        }
    }
    //准备播放
    private void PrepareAndPlay(String path) {
        try {
            mAudioFouus =  mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            if(!mAudioFouus)
                return;
//            mRemoteCtrlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            mMediaSession.setPlaybackState(getPlaybackStateCompat(PlaybackStateCompat.STATE_PLAYING,getCurrentTime()));
            mIsplay = true;
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
            mFirstFlag = false;
            SharedPrefsUtil.putValue(MainActivity.mInstance,"setting","Pos",mCurrent);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    //根据当前播放列表的长度，得到一个随机数
    private static int getShuffle(){
        if(DBUtil.mPlayingList.size() == 1)
            return 0;
        return new Random().nextInt(DBUtil.mPlayingList.size() - 1);
    }
    //根据当前播放模式，播放上一首或者下一首
    public void PlayNextOrPrev(boolean IsNext,boolean NeedPlay){
        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return;
        mIsplay = true;
        if(mPlayModel == Constants.PLAY_SHUFFLE) {
            mCurrent = getShuffle();
            mId = DBUtil.mPlayingList.get(mCurrent);
            mInfo = DBUtil.getMP3InfoById(mId);
        }
        else if(mPlayModel == Constants.PLAY_LOOP) {
            if(IsNext) {
                if ((++mCurrent) > DBUtil.mPlayingList.size() - 1)
                    mCurrent = 0;
                mId = DBUtil.mPlayingList.get(mCurrent);
                mInfo = DBUtil.getMP3InfoById(mId);
            }
            else {
                if ((--mCurrent) < 0)
                    mCurrent = DBUtil.mPlayingList.size() - 1;
                mId = DBUtil.mPlayingList.get(mCurrent);
                mInfo = DBUtil.getMP3InfoById(mId);
            }
        }
        else {

        }
        if(NeedPlay)
            PrepareAndPlay(mInfo.getUrl());

//        RemoteControlClient.MetadataEditor editor = mRemoteCtrlClient.editMetadata(false);
//        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK,DBUtil.CheckBitmapByAlbumId((int)mInfo.getAlbumId(),false));
//        editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM,mInfo.getAlbum());
//        editor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST,mInfo.getArtist());
//        editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE,mInfo.getDisplayname());
    }
    //获得播放状态
    public static int getPlayModel() {
        return mPlayModel;
    }
    //设置播放状态
    public static void setPlayModel(int PlayModel) {
        mPlayModel = PlayModel;
    }

    //获得是否在播放
    public static boolean getIsplay()
    {
        return mIsplay;
    }
    //获得mediaplayer
    public static void setProgress(int current)
    {
        mPlayer.seekTo(current);
    }

    //设置当前播放列表
    public static void setCurrentList(ArrayList<Long> list)
    {
        DBUtil.mPlayingList = list;
    }
    //返回当前播放列表
    public static ArrayList<Long> getCurrentList()
    {
        return DBUtil.mPlayingList;
    }
    //返回当前播放歌曲
    public static MP3Info getCurrentMP3()
    {
        return mInfo;
    }
    //获得当前播放进度
    public static int getCurrentTime() {
        if(mPlayer != null)
            return mPlayer.getCurrentPosition();
        return -1;
    }
    //获得当前播放索引
    public static int getCurrentPos()
    {
        return mCurrent;
    }
    //设置当前索引
    public static void setCurrentPos(int pos)
    {
        mCurrent = pos;
    }

    public class VolDownThread extends Thread{
        @Override
        public void run(){
            mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            if((double)mCurrentVolume / mMaxVolume < 0.15)
                return;
            int sleeptime = (int)(1000 / (mCurrentVolume - (double)mCurrentVolume / mMaxVolume));
            int temp = mCurrentVolume;
            if(sleeptime > 0){
                while (temp-- < mMaxVolume * 0.15){
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, temp,
                            AudioManager.FLAG_PLAY_SOUND);
                    try {
                        sleep(sleeptime);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume,
                    AudioManager.FLAG_PLAY_SOUND);
        }
    }

    private static int mCount = 0;
    public class SessionCallBack extends MediaSessionCompat.Callback{
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Intent intent_ctl = null;
            KeyEvent event = (KeyEvent)mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
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

            Log.d(TAG,"count=" + mCount);
            Log.d(TAG,"AudioFocus:" + mAudioFouus);
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
