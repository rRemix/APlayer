package remix.myplayer;

/**
 * Created by taeja on 16-4-15.
 */

import java.util.ArrayList;
import java.util.List;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.misc.receiver.HeadsetPlugReceiver;
import remix.myplayer.util.SPUtil;

/**
 * 一些全局变量
 */
public class Global {
  /**
   * 播放列表
   */
  public static List<PlayList> PlayList = new ArrayList<>();

  /**
   * 播放队列id
   */
  public static int PlayQueueID = 0;
  /**
   * 我的收藏id
   */
  public static int MyLoveID = 0;

}
