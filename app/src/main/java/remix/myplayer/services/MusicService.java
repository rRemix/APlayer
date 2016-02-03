package remix.myplayer.services;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;


/**
 * Created by Remix on 2015/12/1.
 */
public class MusicService extends Service {
    //实例
    public static MusicService mInstance;
    //播放列表
//    private static ArrayList<MP3Info> mInfoList = Utility.mInfoList;
    //是否第一次启动
    private static boolean mFlag = true;
    //播放模式
    private static int mPlayModel = Utility.PLAY_LOOP;
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
    //下一首播放的mp3
    private static MP3Info mNextInfo;
    private Handler mHandler;
    private static MediaPlayer mPlayer;
    private static PlayerBinder mBinder;
    //回调接口的集合
    private static List<Callback> mCallBacklist  = new ArrayList<Callback>(){};;
    //当前播放歌曲的路径
    private String mPath = null;
    private static Context context;
    private Timer mTimer;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == Utility.UPDATE_INFORMATION)
            {
                if(mCallBacklist != null)
                    System.out.println(mCallBacklist);
                    //遍历集合，通知所有的实现类，即activity
                for (int i = 0; i < mCallBacklist.size(); i++)
                {
                    if(mCallBacklist.get(i) != null)
                        try
                        {
                            mCallBacklist.get(i).UpdateUI(mInfo,mIsplay);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                }
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
        if(mBinder == null)
            mBinder = new PlayerBinder();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        if(Utility.mPlayingList != null && Utility.mPlayingList.size() > 0)
            mId = Utility.mPlayingList.get(0);
        mInfo = Utility.getMP3InfoById(mId);
//        if(Utility.mPlayingList.size() > 1)
//            mNextInfo = Utility.getMP3InfoById(Utility.mPlayingList.get(1));


        mPlayer = new MediaPlayer();
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //一定要先设置mediaplay的数据源
//        try
//        {
//            if(mInfo != null)
//                mPlayer.setDataSource(mInfo.getUrl());
//        }
//        catch (Exception e) {e.printStackTrace();}

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                PlayNextOrPrev(true,true);
                handler.sendEmptyMessage(Utility.UPDATE_INFORMATION);
                //SendUpdate();
            }
        });
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayer.start();
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d("Error","what = " + what + " extar = " + extra);
                return true;
            }
        });
    }

    public void UpdateBottomStatus(MP3Info info,boolean isPlaying)
    {
        BottomActionBarFragment fragment = BottomActionBarFragment.mInstance;
        fragment.UpdateBottomStatus(info, isPlaying);
    }



    private void Play(String Path)
    {
        mIsplay = true;
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
    private void PlayContinue()
    {
        mIsplay = true;
        mPlayer.start();
    }
    private void PlayOrPause()
    {
        if(mPlayer.isPlaying())
            Pause();
        else
        {
            mIsplay = true;
            if(mFlag)
            {
                Play(mInfo.getUrl());
                mFlag = false;
                return;
            }
            mPlayer.start();
        }
    }
    private void Pause()
    {
        mIsplay = false;
        mPlayer.pause();
    }
    private void Stop()
    {
        mIsplay = false;
        mPlayer.stop();
    }








    public class PlayerBinder extends Binder
    {
        public MusicService getService() {
            return MusicService.this;
        }
        private void Play(String path)
        {
            mIsplay = true;
            PrepareAndPlay(path);
        }
        private void PlayNext()
        {
            PlayNextOrPrev(true,true);
        }
        private void PlayPrevious()
        {
            PlayNextOrPrev(false,true);
        }
        private void PlayContinue()
        {
            mIsplay = true;
            mPlayer.start();
        }
        private void PlayOrPause()
        {
            if(mPlayer.isPlaying())
                Pause();
            else
            {
                mIsplay = true;
                if(mFlag)
                {
                    Play(mInfo.getUrl());
                    mFlag = false;
                    return;
                }
                mPlayer.start();
            }
        }
        private void Pause()
        {
            mIsplay = false;
            mPlayer.pause();
        }
        private void Stop()
        {
            mIsplay = false;
            mPlayer.stop();
        }
    }


    //回调接口，当发生更新时，通知所有activity接受更新
    public interface Callback {
        public void UpdateUI(MP3Info MP3info, boolean isplay);
    }
    //添加回调接口的一个实现类
    public static void addCallback(Callback callback,int positon)
    {
        mCallBacklist.add(callback);
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
            switch (Control)
            {
                //播放listview选中的歌曲
                case Utility.PLAYSELECTEDSONG:
                    mCurrent = intent.getIntExtra("Position", -1);
                    if(mCurrent == -1)
                        System.out.println("参数错误");
                    mId = Utility.mPlayingList.get(mCurrent);
                    mInfo = Utility.getMP3InfoById(mId);
                    if(mInfo == null)
                        break;
                    Play(mInfo.getUrl());
//                    mBinder.Play(mInfo.getUrl());
                    break;
                //播放上一首
                case Utility.PREV:
                    PlayPrevious();
                    break;
                //播放下一首
                case Utility.NEXT:
                    PlayNext();
                    break;
                //暂停或者继续播放
                case Utility.PLAY:
                    PlayOrPause();
                    break;
                //暂停
                case Utility.PAUSE:
                    Pause();
                    break;
                //继续播放
                case Utility.CONTINUE:
                    PlayContinue();
                    break;
                //顺序播放
                case Utility.PLAY_LOOP:
                    mPlayModel = Utility.PLAY_LOOP;
                    break;
                //随机播放
                case Utility.PLAY_SHUFFLE:
                    mPlayModel = Utility.PLAY_SHUFFLE;
                    break;
                //单曲循环
                case Utility.PLAY_REPEATONE:
                    mPlayModel = Utility.PLAY_REPEATONE;
                default:break;
            }
            handler.sendEmptyMessage(Utility.UPDATE_INFORMATION);

        }
    }
    //准备播放
    private void PrepareAndPlay(String path)
    {

        try
        {
            if(mFlag) {
                Intent intent = new Intent(Utility.NOTIFY);
                MainActivity.mInstance.sendBroadcast(intent);
            }
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
            mFlag = false;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    //根据当前播放列表的长度，得到一个随机数
    private int getShuffle()
    {
        if(Utility.mPlayingList.size() == 1)
            return 0;
        return new Random().nextInt(Utility.mPlayingList.size() - 1);
    }
    //根据当前播放模式，播放上一首或者下一首
    public void PlayNextOrPrev(boolean IsNext,boolean NeedPlay)
    {
        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
            return;

        if(mPlayModel == Utility.PLAY_SHUFFLE) {
            mCurrent = getShuffle();
            mId = Utility.mPlayingList.get(mCurrent);
            mInfo = Utility.getMP3InfoById(mId);
        }
        else if(mPlayModel == Utility.PLAY_LOOP) {
            if(IsNext) {
                if ((++mCurrent) > Utility.mPlayingList.size() - 1)
                    mCurrent = 0;
                mId = Utility.mPlayingList.get(mCurrent);
                mInfo = Utility.getMP3InfoById(mId);
            }
            else {
                if ((--mCurrent) < 0)
                    mCurrent = Utility.mPlayingList.size() - 1;
                mId = Utility.mPlayingList.get(mCurrent);
                mInfo = Utility.getMP3InfoById(mId);
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

    //设置当前播放的角标
//    public void UpdateNextSong(int current)
//    {
//        mCurrent = current;
//        mNext = mCurrent + 1 == Utility.mPlayingList.size() ? 0 : mCurrent + 1;
//        mNextInfo = Utility.getMP3InfoById(Utility.mPlayingList.get(mNext));
//    }
    //设置当前播放列表
    public static void setCurrentList(ArrayList<Long> list)
    {
        Utility.mPlayingList = list;
    }
    //返回当前播放列表
    public static ArrayList<Long> getCurrentList()
    {
        return Utility.mPlayingList;
    }
    //返回当前播放歌曲
    public static MP3Info getCurrentMP3()
    {
        return mInfo;
    }
    //返回下一首播放的歌曲
    public static MP3Info getNextMP3()
    {
        return mNextInfo;
    }
    //获得当前播放进度
    public static int getCurrentTime()
    {
        if(mPlayer != null)
            return mPlayer.getCurrentPosition();
        return -1;
    }
}
