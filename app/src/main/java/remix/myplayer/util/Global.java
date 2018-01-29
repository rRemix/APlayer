package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.bean.mp3.PlayListSong;
import remix.myplayer.service.MusicService;

/**
 * 一些全局变量
 */
public class Global {
    /**
     * 操作类型
     */
    public static int Operation = -1;
    /**
     * 所有歌曲id
     */
    public static List<Integer> AllSongList = new ArrayList<>();
    /**
     * 正在播放歌曲id
     */
    public static List<Integer> PlayQueue = new ArrayList<>();
    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String,List<Integer>> FolderMap = new TreeMap<>(String::compareToIgnoreCase);
    /**
     * 播放列表
     */
    public static List<PlayList> PlayList = new ArrayList<>();

    public static void setOperation(int operation){
        Operation = operation;
    }
    public static int getOperation(){
        return Operation;
    }

    /**
     * 耳机是否插入
     */
    private static boolean IsHeadsetOn = false;
    public static void setHeadsetOn(boolean headsetOn){
        IsHeadsetOn = headsetOn;
    }
    public static boolean getHeadsetOn(){
        return IsHeadsetOn;
    }

    /**
     * 通知栏是否显示
     */
    private static boolean NotifyShowing = false;
    public static void setNotifyShowing(boolean isshow){
        NotifyShowing = isshow;
    }
    public static boolean isNotifyShowing(){
        return NotifyShowing;
    }

    /** 播放队列id */
    public static int PlayQueueID = 0;
    /** 我的收藏id */
    public static int MyLoveID = 0;

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public synchronized static void setPlayQueue(final List<Integer> newQueueIdList) {
        new Thread(){
            @Override
            public void run() {
                if(newQueueIdList == null || newQueueIdList.size() == 0){
                    return;
                }
                if (newQueueIdList.equals(PlayQueue))
                    return;
                PlayQueue.clear();
                PlayQueue.addAll(newQueueIdList);
                int deleteRow = 0;
                int addRow = 0;
                try {
                    deleteRow = PlayListUtil.clearTable(Constants.PLAY_QUEUE);
                    addRow = PlayListUtil.addMultiSongs(PlayQueue,Constants.PLAY_QUEUE, PlayQueueID);
                } catch (Exception e){
                    Util.uploadException("setPlayQueue Error",e);
                } finally {
                    if(addRow == 0)
                        Util.uploadException("updateDB","deleteRow:" + deleteRow + " addRow:" + addRow);
                }

            }
        }.start();
    }

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public synchronized static void setPlayQueue(final List<Integer> newQueueIdList, final Context context, final Intent intent) {
        new Thread(){
            @Override
            public void run() {
                //当前模式是随机播放 或者即将设置为随机播放 都要更新mRandomList
                boolean shuffle = intent.getBooleanExtra("shuffle",false) | SPUtil.getValue(context,"Setting", "PlayModel",Constants.PLAY_LOOP) == Constants.PLAY_SHUFFLE;
                if(newQueueIdList == null || newQueueIdList.size() == 0){
                    return;
                }
                //设置的播放队列相等
                boolean equals = newQueueIdList.equals(PlayQueue);
                if(!equals){
                    PlayQueue.clear();
                    PlayQueue.addAll(newQueueIdList);
                }
                if(shuffle){
                    MusicService.getInstance().setPlayModel(Constants.PLAY_SHUFFLE);
                    MusicService.getInstance().updateNextSong();
                }
                context.sendBroadcast(intent);

                if(equals){
                    return;
                }

                int deleteRow = 0;
                int addRow = 0;
                try {
                    deleteRow = PlayListUtil.clearTable(Constants.PLAY_QUEUE);
                    addRow = PlayListUtil.addMultiSongs(PlayQueue,Constants.PLAY_QUEUE, PlayQueueID);
                } catch (Exception e){
                    Util.uploadException("setPlayQueue Error",e);
                } finally {
                    if(addRow == 0)
                        Util.uploadException("updateDB","deleteRow:" + deleteRow + " addRow:" + addRow);
                }
            }
        }.start();
    }

    public static synchronized List<Integer> getPlayQueue(){
        return PlayQueue;
    }

    /**
     * 添加歌曲到正在播放列表
     * @param rawAddList
     * @return
     */
    public static int AddSongToPlayQueue(final List<Integer> rawAddList) {
        List<PlayListSong> infos = new ArrayList<>();
        for(Integer id : rawAddList){
            infos.add(new PlayListSong(id, PlayQueueID,Constants.PLAY_QUEUE));
        }
        return PlayListUtil.addMultiSongs(infos);
    }

    /**
     * 当前播放歌曲的lrc文件路径
     */
    public static String CurrentLrcPath = "";

    /**
     * 所有可能的歌词目录
     */
    public static ArrayList<File> LyricDir = new ArrayList<>();

    /**
     * 上次退出时正在播放的歌曲的id
     */
    public static int LastSongID = -1;

    /**
     * 上次退出时正在播放的歌曲的pos
     */
    public static int LastSongPos = 0;
}
