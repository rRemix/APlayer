package remix.myplayer.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
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
import remix.myplayer.receivers.NotifyReceiver;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.SharedPrefsUtil;


/**
 * Created by Remix on 2015/12/1.
 */
public class MusicService extends Service {
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
    private static PlayerBinder mBinder;
    //回调接口的集合
    private static List<Callback> mCallBacklist  = new ArrayList<Callback>(){};;
    private static Context context;
    private NotifyReceiver mNotifyReceiver;
    private static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            if(msg.what == Constants.UPDATE_INFORMATION)
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
        mPlayer = null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        if(DBUtil.mPlayingList != null && DBUtil.mPlayingList.size() > 0) {
            mId = mPos == -1 ? DBUtil.mPlayingList.get(0) : DBUtil.mPlayingList.get(mPos);
            mInfo = DBUtil.getMP3InfoById(mId);
            mCurrent = mPos == -1 ? 0 : mPos;
        }
        else
            mInfo = null;

//        if(CommonUtil.mPlayingList.size() > 1)
//            mNextInfo = CommonUtil.getMP3InfoById(CommonUtil.mPlayingList.get(1));


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
                handler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
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



    private static void Play(String Path)
    {
        mIsplay = true;
        PrepareAndPlay(Path);
    }
    private static void PlayNext()
    {
        PlayNextOrPrev(true,true);
    }
    private static void PlayPrevious()
    {
        PlayNextOrPrev(false,true);
    }
    private static void PlayContinue()
    {
        mIsplay = true;
        mPlayer.start();
    }
    private static void PlayOrPause()
    {
        if(mPlayer.isPlaying())
            Pause();
        else
        {
            if(mInfo == null)
                return;
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
    private static void Pause()
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


    //回调接口，当发生更新时，通知所有activity更新
    public interface Callback {
        public void UpdateUI(MP3Info MP3info, boolean isplay);
        public int getType();
    }
    //添加回调接口的一个实现类
    public static void addCallback(Callback callback)
    {
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
        System.out.println(mCallBacklist.toString());

    }
    //返回回调接口链表的长度
    public int getCallBackListSize()
    {
        return mCallBacklist.size();
    }
    public static class PlayerReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            int Control = intent.getIntExtra("Control",-1);
            switch (Control)
            {
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
                    if(mInfo == null)
                        break;
                    Play(mInfo.getUrl());
//                    mBinder.Play(mInfo.getUrl());
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
                case Constants.PLAY:
                    PlayOrPause();
                    break;
                //暂停
                case Constants.PAUSE:
                    Pause();
                    break;
                //继续播放
                case Constants.CONTINUE:
                    PlayContinue();
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
            handler.sendEmptyMessage(Constants.UPDATE_INFORMATION);
            boolean FromNotify = intent.getBooleanExtra("FromNotify",false);
            if(FromNotify){
                NotifyService.mInstance.UpdateNotify();
            }
        }
    }
    //准备播放
    private static void  PrepareAndPlay(String path)
    {
        try
        {
//            if(mFlag) {
//                Intent intent = new Intent(CommonUtil.NOTIFY);
//                MainActivity.mInstance.sendBroadcast(intent);
//            }
            mPlayer.reset();
            mPlayer.setDataSource(path);
            mPlayer.prepareAsync();
            mFlag = false;
            SharedPrefsUtil.putValue(MainActivity.mInstance,"setting","mPos",mCurrent);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    //根据当前播放列表的长度，得到一个随机数
    private static int getShuffle()
    {
        if(DBUtil.mPlayingList.size() == 1)
            return 0;
        return new Random().nextInt(DBUtil.mPlayingList.size() - 1);
    }
    //根据当前播放模式，播放上一首或者下一首
    public static void PlayNextOrPrev(boolean IsNext,boolean NeedPlay)
    {
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
    public static int getCurrentTime()
    {
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
