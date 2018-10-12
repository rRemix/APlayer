package remix.myplayer;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import remix.myplayer.bean.mp3.PlayList;

/**
 * 一些全局变量
 */
public class Global {
    /**
     * 操作类型
     */
    public static int Operation = -1;
    /**
     * 文件夹名与对应的所有歌曲id
     */
    public static Map<String, List<Integer>> FolderMap = new TreeMap<>(String::compareToIgnoreCase);
    /**
     * 播放列表
     */
    public static List<PlayList> PlayList = new ArrayList<>();

    public static void setOperation(int operation) {
        Operation = operation;
    }

    public static int getOperation() {
        return Operation;
    }

    /**
     * 耳机是否插入
     */
    private static boolean IsHeadsetOn = false;

    public static void setHeadsetOn(boolean headsetOn) {
        IsHeadsetOn = headsetOn;
    }

    public static boolean getHeadsetOn() {
        return IsHeadsetOn;
    }

    /**
     * 通知栏是否显示
     */
    private static boolean NotifyShowing = false;

    public static void setNotifyShowing(boolean isshow) {
        NotifyShowing = isshow;
    }

    public static boolean isNotifyShowing() {
        return NotifyShowing;
    }

    /**
     * 播放队列id
     */
    public static int PlayQueueID = 0;
    /**
     * 我的收藏id
     */
    public static int MyLoveID = 0;

}
