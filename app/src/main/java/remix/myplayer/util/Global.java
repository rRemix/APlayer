package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import remix.myplayer.model.PlayListInfo;
import remix.myplayer.model.PlayListSongInfo;

/**
 * 一些全局变量
 */
public class Global {
    /**
     * 当前正在设置封面的专辑或艺术家或播放列表id
     */
    public static int mSetCoverID = 0;

    /**
     * 当前正在设置封面的专辑或艺术家或播放列表的名字
     */
    public static String mSetCoverName = "";

    /**
     * 当前正在设置的是专辑还是艺术家还是播放列表
     */
    public static int mSetCoverType = 1;

    /**
     * 操作类型
     */
    public static int mOperation = -1;
    /**
     * 所有歌曲id
     */
    public static ArrayList<Integer> mAllSongList = new ArrayList<>();
    /**
     * 正在播放歌曲id
     */
    public static ArrayList<Integer> mPlayQueue = new ArrayList<>();
    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String,ArrayList<Integer>> mFolderMap = new TreeMap<>(new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            return o1.compareToIgnoreCase(o2);
        }
    });
    /**
     * 播放列表
     */
    public static ArrayList<PlayListInfo> mPlayList = new ArrayList<>();

    public static void setOperation(int operation){
        mOperation = operation;
    }
    public static int getOperation(){
        return mOperation;
    }

    /**
     * 耳机是否插入
     */
    private static boolean mIsHeadsetOn = false;
    public static void setHeadsetOn(boolean headsetOn){
        mIsHeadsetOn = headsetOn;
    }
    public static boolean getHeadsetOn(){
        return mIsHeadsetOn;
    }

    /**
     * 通知栏是否显示
     */
    private static boolean mNotifyShowing = false;
    public static void setNotifyShowing(boolean isshow){
        mNotifyShowing = isshow;
    }
    public static boolean isNotifyShowing(){
        return mNotifyShowing;
    }

    /** 播放队列id */
    public static int mPlayQueueID = 0;
    /** 我的收藏id */
    public static int mMyLoveID = 0;
    /** 最近添加id */
    public static int mRecentlyID = 0;

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public static void setPlayQueue(final ArrayList<Integer> newQueueIdList) {
        new Thread(){
            @Override
            public void run() {
                if (newQueueIdList.equals(mPlayQueue))
                    return;
                ArrayList<Integer> oriPlayQueue = (ArrayList<Integer>) mPlayQueue.clone();
                mPlayQueue.clear();
                mPlayQueue.addAll(newQueueIdList);

                int deleteRow = PlayListUtil.deleteMultiSongs(oriPlayQueue, mPlayQueueID);
                long start = System.currentTimeMillis();
                int addRow = PlayListUtil.addMultiSongs(mPlayQueue,Constants.PLAY_QUEUE, mPlayQueueID);
                LogUtil.d("DeleteTest","Time:" + (System.currentTimeMillis() - start) + "  addRow:" + addRow + "  deleteRow:" + deleteRow);
            }
        }.start();
    }

    /**
     * 设置播放队列
     * @param newQueueIdList
     * @return
     */
    public static void setPlayQueue(final ArrayList<Integer> newQueueIdList, final Context context, final Intent intent) {
        if (newQueueIdList.equals(mPlayQueue)) {
            context.sendBroadcast(intent);
            return;
        }

        final ArrayList<Integer> oriPlayQueue = (ArrayList<Integer>) mPlayQueue.clone();
        mPlayQueue.clear();
        mPlayQueue.addAll(newQueueIdList);
        context.sendBroadcast(intent);
        new Thread(){
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                int deleteRow = PlayListUtil.deleteMultiSongs(oriPlayQueue, mPlayQueueID);
                LogUtil.d("TimeTest","DeleteTime:" + (System.currentTimeMillis() - start) );
                int addRow = PlayListUtil.addMultiSongs(mPlayQueue,Constants.PLAY_QUEUE, mPlayQueueID);
                LogUtil.d("TimeTest","AddTime:" + (System.currentTimeMillis() - start) );
            }
        }.start();
    }

    /**
     * 添加歌曲到正在播放列表
     * @param rawAddList
     * @return
     */
    public static int AddSongToPlayQueue(final ArrayList<Integer> rawAddList) {
        ArrayList<PlayListSongInfo> infos = new ArrayList<>();
        for(Integer id : rawAddList){
            infos.add(new PlayListSongInfo(id, mPlayQueueID,Constants.PLAY_QUEUE));
        }
        return PlayListUtil.addMultiSongs(infos);
    }

    /**
     * 当前播放歌曲的lrc文件路径
     */
    public static String mCurrentLrcPath = "";

}
