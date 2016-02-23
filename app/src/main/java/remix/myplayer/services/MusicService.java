package remix.myplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import remix.myplayer.activities.MainActivity;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.SharedPrefsUtil;


/**
 * Created by Remix on 2015/12/1.
 */
public class MusicService extends Service {
    private final static String TAG = "MusicService";
    //实例
    public static MusicService mInstance;
    //是否第一次启动
    private static boolean mFlag = true;
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
    private static List<Callback> mCallBacklist  = new ArrayList<Callback>(){};;
    private static Context context;
    private PlayerReceiver mRecevier;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;
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
    private Handler mUpdateVolHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == Constants.UPDATE_VOL){

            }
        }
    };
    public MusicService(){}
    public MusicService(Context context)
    {
        this.context = context;
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
        mPlayer.release();
        mPlayer = null;
        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        unregisterReceiver(mRecevier);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        mAudioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            //记录焦点变化之前是否在播放;
            private boolean mFlag = false;
            @Override
            public void onAudioFocusChange(int focusChange) {
                mFlag = mIsplay;
                switch (focusChange){
                    //播放 获得焦点
                    case AudioManager.AUDIOFOCUS_GAIN:
                        //如果之前正在播放，则继续播放
//                        if(mFlag) {
//                            PlayStart();
//                            mIsplay = true;
//                        }
                        break;
                    //短暂失去焦点,比如有通知到来  暂停
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        if(mIsplay) {
                            mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            new Thread(){
                                @Override
                                public void run(){
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
                            }.start();
                        }
                        break;
                    //降低音量
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if(mIsplay){
                            mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            new Thread(){
                                @Override
                                public void run(){
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
                            }.start();

                        }
                        break;
                    //暂停
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if(mIsplay) {
                            Pause();
                            mIsplay = false;
                        }
                        break;
                }
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
                NotifyService.mInstance.UpdateNotify();

            }
        };
        mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        mRecevier = new PlayerReceiver();
        IntentFilter filter = new IntentFilter("remix.music.CTL_ACTION");
        registerReceiver(mRecevier,filter);

        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        if(DBUtil.mPlayingList != null && DBUtil.mPlayingList.size() > 0) {
            mId = mPos == -1 ? DBUtil.mPlayingList.get(0) : DBUtil.mPlayingList.get(mPos);
            mInfo = DBUtil.getMP3InfoById(mId);
            mCurrent = mPos == -1 ? 0 : mPos;
        }
        else
            mInfo = null;

        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                PlayNextOrPrev(true,true);
                mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
                //SendUpdate();
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


    private void Play(String Path) {
        PrepareAndPlay(Path);
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
                int ret = mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
                if(ret != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    return;
                mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
                mPlayer.start();
                if(mCurrentVolume == 0)
                    return;
                int temp = 0;
                int sleeptime = 500 / mCurrentVolume;
                while(temp++ < mCurrentVolume){
                    try {
                        sleep(sleeptime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, temp,
                            AudioManager.FLAG_PLAY_SOUND);
                }
            }
        }.start();

    }
    private void PlayOrPause() {
        if(mPlayer.isPlaying())
            Pause();
        else {
            if(mInfo == null)
                return;
            if(mFlag) {
                Play(mInfo.getUrl());
                mFlag = false;
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
                mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                if(mCurrentVolume == 0){
                    mPlayer.pause();
                    return;
                }
                int sleeptime = 500 / mCurrentVolume;
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
                mIsplay = false;
                mPlayer.pause();
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume,
                        AudioManager.FLAG_PLAY_SOUND);
            }
        }.start();
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
            int Control = intent.getIntExtra("Control",-1);

            switch (Control) {
                //播放listview选中的歌曲
                case Constants.PLAYSELECTEDSONG:
                    mCurrent = intent.getIntExtra("Position", -1);
                    if(mCurrent == -1)
                        return;
//                    if(mCurrent > DBUtil.mPlayingList.size() - 1){
//                        Toast.makeText(context,"请先添加歌曲到该播放列表",Toast.LENGTH_SHORT).show();
//                        return;
//                    }
                    mId = DBUtil.mPlayingList.get(mCurrent);
                    mInfo = DBUtil.getMP3InfoById(mId);
                    mIsplay = true;
                    if(mInfo == null)
                        break;
                    Play(mInfo.getUrl());
//                    mBinder.Play(mInfo.getUrl());
                    break;
                //播放上一首
                case Constants.PREV:
                    mIsplay = true;
                    PlayPrevious();
                    break;
                //播放下一首
                case Constants.NEXT:
                    mIsplay = true;
                    PlayNext();
                    break;
                //暂停或者继续播放
                case Constants.PLAY:
                    mIsplay = !mIsplay;
                    PlayOrPause();
                    break;
                //暂停
                case Constants.PAUSE:
                    mIsplay = false;
                    Pause();
                    break;
                //继续播放
                case Constants.CONTINUE:
                    mIsplay = true;
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
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
            boolean FromNotify = intent.getBooleanExtra("FromNotify", false);
            if(FromNotify){
//                NotifyService.mInstance.mHandler.sendEmptyMessage(0);
//                sendBroadcast(new Intent(Constants.NOTIFY));
                NotifyService.mInstance.UpdateNotify();
            }
        }
    }
    //准备播放
    private void PrepareAndPlay(String path) {
        try {
            int ret = mAudioManager.requestAudioFocus(mAudioFocusListener,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
            if(ret != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                return;
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
            mFlag = false;
            SharedPrefsUtil.putValue(MainActivity.mInstance,"setting","mPos",mCurrent);
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
            Play(mInfo.getUrl());
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
}
