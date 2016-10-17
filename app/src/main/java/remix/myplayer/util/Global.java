package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import remix.myplayer.application.Application;
import remix.myplayer.db.PlayListSongInfo;
import remix.myplayer.db.PlayListSongs;
import remix.myplayer.model.PlayListItem;

/**
 * 一些全局变量
 */
public class Global {
    /**
     * 当前正在设置封面的专辑或艺术家id
     */
    public static int mAlbumArtistID = 0;

    /**
     * 当前正在设置封面的专辑或艺术家名字
     */
    public static String mAlbumArtistName = "";

    /**
     * 当前正在设置的是专辑还是艺术家封面
     * 1:专辑 2:艺术家
     */
    public static int mAlbunOrArtist = 1;


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
     * 最近添加列表
     */
    public static Map<String,ArrayList<PlayListItem>> mPlaylist = new HashMap<>();

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

    public static int mPlayQueueId = 0;
    public static int mMyLoveId;
    /**
     * 设置正在播放列表
     * @param newQueueIdList
     */
    public static void setPlayQueue(final ArrayList<Integer> newQueueIdList) {
//        mPlayQueue = (ArrayList<Integer>) list.clone();
//        XmlUtil.updatePlayQueue();
        new Thread(){
            @Override
            public void run() {
                mPlayQueue = (ArrayList<Integer>) newQueueIdList.clone();
                PlayListUtil.deleteMultiSongs(mPlayQueue,mPlayQueueId);
                ArrayList<PlayListSongInfo> infos = new ArrayList<>();
                for(Integer id : newQueueIdList){
                    infos.add(new PlayListSongInfo(id,mPlayQueueId,Constants.PLAY_QUEUE));
                }
                PlayListUtil.addMultiSongs(infos);
            }
        }.start();
    }

    /**
     * 添加歌曲到正在播放列表
     * @param newQueueIdList
     */
    public static void AddSongToPlayQueue(final ArrayList<Integer> newQueueIdList) {
//        ArrayList<Integer> songIdlist = new ArrayList<>();
//        for (Integer id : list){
//            if(!mPlayQueue.contains(id))
//                songIdlist.add(id);
//        }
//        mPlayQueue.clear();
//        mPlayQueue = songIdlist;
//        XmlUtil.updatePlayQueue();
        new Thread(){
            @Override
            public void run() {
                ArrayList<Integer> songIdlist = new ArrayList<>();
                for (Integer id : newQueueIdList){
                if(!mPlayQueue.contains(id))
                    songIdlist.add(id);
                }
                mPlayQueue.clear();
                mPlayQueue = songIdlist;
                PlayListUtil.deleteMultiSongs(mPlayQueue,mPlayQueueId);
                ArrayList<PlayListSongInfo> infos = new ArrayList<>();
                for(Integer id : newQueueIdList){
                    infos.add(new PlayListSongInfo(id,mPlayQueueId,Constants.PLAY_QUEUE));
                }
                PlayListUtil.addMultiSongs(infos);
            }
        }.start();
    }

}
