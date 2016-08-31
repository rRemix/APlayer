package remix.myplayer.util;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
     * 是否开启字母索引
     */
    public static boolean mIndexOpen = false;

    /**
     * 操作类型
     */
    public static int mOperation = -1;
    /**
     * 所有歌曲id
     */
    public static ArrayList<Long> mAllSongList = new ArrayList<>();
    /**
     * 正在播放歌曲id
     */
    public static ArrayList<Long> mPlayingList = new ArrayList<>();
    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String,ArrayList<Long>> mFolderMap = new HashMap<>();
    /**
     * 最近添加列表
     */
    public static ArrayList<Long> mTodayList = new ArrayList<>();
    public static ArrayList<Long> mWeekList = new ArrayList<>();

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
    public static boolean getNotifyShowing(){
        return mNotifyShowing;
    }

    /**
     * 更新内存与本地的正在播放列表
     * @param list
     */
    public static void setPlayingList(ArrayList<Long> list) {
        mPlayingList = list;
        XmlUtil.updatePlayingList();
    }



}
