package remix.myplayer.util;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 常量值
 */
public class Constants {
  //todo 删除无用的
  //应用包名
  public final static String PACKAGE_NAME = "remix.myplayer";
  //播放队列
//  public final static String PLAY_QUEUE = App.getContext().getString(R.string.play_queue);
  //最近添加
//  public final static String RECENTLY = App.getContext().getString(R.string.recently);
  //我的收藏
//  public final static String MYLOVE = App.getContext().getString(R.string.my_favorite);

  //操作类型
  public final static int SONG = 0;
  public final static int ALBUM = 1;
  public final static int ARTIST = 2;
  public final static int FOLDER = 3;
  public final static int PLAYLIST = 4;
  public final static int PLAYLISTSONG = 5;

  public final static String EXIT = "remix.music.EXIT";
  public final static String SOUNDEFFECT_ACTION = "remix.music.SOUNDEFFECT_ACTION";
  public final static String TAG_EDIT = "remix.music.TAG_EDIT";

  //播放模式
  public final static int PLAY_LOOP = 50;
  public final static int PLAY_SHUFFLE = 51;
  public final static int PLAY_REPEATONE = 52;
  //更新seekbar、已播放时间、未播放时间
  public final static int UPDATE_TIME_ALL = 0x010;
  //更新已播放时间、未播放时间
  public final static int UPDATE_TIME_ONLY = 0x011;
  //更新桌面部件
  public final static int UPDATE_APPWIDGET = 0x100;
  //更新播放信息
  public final static int UPDATE_UI = 0x101;
  //更新正在播放歌曲
  public final static int UPDATE_META_DATA = 0x102;
  //更新播放状态
  public final static int UPDATE_PLAY_STATE = 0x103;


  //更新桌面歌词内容
  public static final int UPDATE_FLOAT_LRC_CONTENT = 500;
  //移除桌面歌词
  public static final int REMOVE_FLOAT_LRC = 501;
  //添加桌面歌词
  public static final int CREATE_FLOAT_LRC = 502;

  //更新适配器
  public final static int UPDATE_ADAPTER = 100;
  //多选更新
  public final static int CLEAR_MULTI = 101;
  //重建activity
  public final static int RECREATE_ACTIVITY = 102;
  //更新全部歌曲adapter
  public final static int UPDATE_ALLSONG_ADAPTER = 103;
  //更新文件夹适配器
  public final static int UPDATE_FOLDER_ADAPTER = 104;
  //更新子目录适配器
  public final static int UPDATE_CHILDHOLDER_ADAPTER = 105;

  //0:软件锁屏 1:系统锁屏 2:关闭
  public final static int APLAYER_LOCKSCREEN = 0;
  public final static int SYSTEM_LOCKSCREEN = 1;
  public final static int CLOSE_LOCKSCREEN = 2;

}
