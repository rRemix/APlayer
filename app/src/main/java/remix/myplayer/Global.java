package remix.myplayer;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.List;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.util.SPUtil;

/**
 * 一些全局变量
 */
public class Global {

  /**
   * 操作类型
   */
  public static int Operation = -1;
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
   * 播放队列id
   */
  public static int PlayQueueID = 0;
  /**
   * 我的收藏id
   */
  public static int MyLoveID = 0;

  /**
   * 所有列表是否显示文件名
   */
  public static boolean SHOW_DISPLAYNAME = SPUtil
      .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SHOW_DISPLAYNAME,
          false);
}
