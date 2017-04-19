package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;
import remix.myplayer.model.PlayListInfo;
import remix.myplayer.model.PlayListSongInfo;
import remix.myplayer.service.MusicService;

/**
 * 一些全局变量
 */
public class Global {
    /**
     * 当前正在设置封面的专辑或艺术家或播放列表id
     */
    public static int SetCoverID = 0;

    /**
     * 当前正在设置封面的专辑或艺术家或播放列表的名字
     */
    public static String SetCoverName = "";

    /**
     * 当前正在设置的是专辑还是艺术家还是播放列表
     */
    public static int SetCoverType = 1;

    /**
     * 操作类型
     */
    public static int Operation = -1;
    /**
     * 所有歌曲id
     */
    public static ArrayList<Integer> AllSongList = new ArrayList<>();
    /**
     * 正在播放歌曲id
     */
    public static ArrayList<Integer> PlayQueue = new ArrayList<>();
    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String,ArrayList<Integer>> FolderMap = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    });
    /**
     * 播放列表
     */
    public static ArrayList<PlayListInfo> PlayList = new ArrayList<>();

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
    /** 最近添加id */
    public static int RecentlyID = 0;

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public synchronized static void setPlayQueue(final ArrayList<Integer> newQueueIdList) {
        if(newQueueIdList == null || newQueueIdList.size() == 0){
            ToastUtil.show(APlayerApplication.getContext(),APlayerApplication.getContext().getString(R.string.try_set_empty_list));
            return;
        }
        new Thread(){
            @Override
            public void run() {
                if (newQueueIdList.equals(PlayQueue))
                    return;
                ArrayList<Integer> oriPlayQueue = (ArrayList<Integer>) PlayQueue.clone();
                PlayQueue.clear();
                PlayQueue.addAll(newQueueIdList);
                int deleteRow = 0;
                int addRow = 0;
                try {
                    deleteRow = PlayListUtil.deleteMultiSongs(oriPlayQueue, PlayQueueID);
                    addRow = PlayListUtil.addMultiSongs(PlayQueue,Constants.PLAY_QUEUE, PlayQueueID);
                } catch (Exception e){
                    CommonUtil.uploadException("setPlayQueue Error",e);
                } finally {
                    if(deleteRow == 0 || addRow == 0)
                        CommonUtil.uploadException("updateDB","deleteRow:" + deleteRow + " addRow:" + addRow);
                }

            }
        }.start();
    }

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public synchronized static void setPlayQueue(final ArrayList<Integer> newQueueIdList, final Context context, final Intent intent) {
        if(newQueueIdList == null || newQueueIdList.size() == 0){
            ToastUtil.show(context,context.getString(R.string.try_set_empty_list));
            return;
        }
        if (newQueueIdList.equals(PlayQueue)) {
            context.sendBroadcast(intent);
            return;
        }

        final ArrayList<Integer> oriPlayQueue = (ArrayList<Integer>) PlayQueue.clone();
        PlayQueue.clear();
        PlayQueue.addAll(newQueueIdList);
        if(intent.getBooleanExtra("shuffle",false)){
            MusicService.updateNextSong();
        }
        context.sendBroadcast(intent);
        new Thread(){
            @Override
            public void run() {
                int deleteRow = 0;
                int addRow = 0;
                try {
                    deleteRow = PlayListUtil.deleteMultiSongs(oriPlayQueue, PlayQueueID);
                    addRow = PlayListUtil.addMultiSongs(PlayQueue,Constants.PLAY_QUEUE, PlayQueueID);
                } catch (Exception e){
                    CommonUtil.uploadException("setPlayQueue Error",e);
                } finally {
                    if(deleteRow == 0 || addRow == 0)
                        CommonUtil.uploadException("updateDB","deleteRow:" + deleteRow + " addRow:" + addRow);
                }

            }
        }.start();
    }

    public static synchronized ArrayList<Integer> getPlayQueue(){
        return PlayQueue;
    }

    /**
     * 添加歌曲到正在播放列表
     * @param rawAddList
     * @return
     */
    public static int AddSongToPlayQueue(final ArrayList<Integer> rawAddList) {
        ArrayList<PlayListSongInfo> infos = new ArrayList<>();
        for(Integer id : rawAddList){
            infos.add(new PlayListSongInfo(id, PlayQueueID,Constants.PLAY_QUEUE));
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
