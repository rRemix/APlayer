package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import remix.myplayer.model.PlayListNewInfo;
import remix.myplayer.model.PlayListSongInfo;
import remix.myplayer.model.PlayListItem;

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
    public static Map<String,ArrayList<Integer>> mFolderMap = new HashMap<>();
    /**
     * 播放列表
     */
//    public static Map<String,ArrayList<PlayListItem>> mPlayList = new HashMap<>();
    public static ArrayList<PlayListNewInfo> mPlayList = new ArrayList<>();

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
    public static int mPlayQueueId = 0;
    /** 我的收藏id */
    public static int mMyLoveId;
    /**
     * 设置正在播放列表
     * @param newQueueIdList
     * @return
     */
    public static int setPlayQueue(final ArrayList<Integer> newQueueIdList) {
//        mPlayQueue = (ArrayList<Integer>) list.clone();
//        XmlUtil.updatePlayQueue();
        if (newQueueIdList.equals(mPlayQueue))
            return 0;
        long start = System.currentTimeMillis();
        int deleteRow = PlayListUtil.deleteMultiSongs(mPlayQueue,mPlayQueueId);
        mPlayQueue.clear();
        mPlayQueue.addAll(newQueueIdList);
        ArrayList<PlayListSongInfo> infos = new ArrayList<>();
        for(Integer id : newQueueIdList){
            infos.add(new PlayListSongInfo(id,mPlayQueueId,Constants.PLAY_QUEUE));
        }
        int addRow = PlayListUtil.addMultiSongs(infos);
        LogUtil.d("DBTest","Time:" + (System.currentTimeMillis() - start) + "  addRow:" + addRow + "  deleteRow:" + deleteRow);
        return addRow;

    }

    /**
     * 添加歌曲到正在播放列表
     * @param rawAddList
     * @return
     */
    public static int AddSongToPlayQueue(final ArrayList<Integer> rawAddList) {
        ArrayList<PlayListSongInfo> infos = new ArrayList<>();
        for(Integer id : rawAddList){
            infos.add(new PlayListSongInfo(id,mPlayQueueId,Constants.PLAY_QUEUE));
        }
        return PlayListUtil.addMultiSongs(infos);

    }

}
