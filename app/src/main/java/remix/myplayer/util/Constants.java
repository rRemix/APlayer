package remix.myplayer.util;

/**
 * Created by taeja on 16-2-17.
 */

import remix.myplayer.R;
import remix.myplayer.application.APlayerApplication;

/**
 * 常量值
 */
public class Constants {
    //应用包名
    public final static String PACKAGE_NAME = "remix.myplayer";
    //播放队列
    public final static String PLAY_QUEUE = APlayerApplication.getContext().getString(R.string.play_queue);
    //最近添加
    public final static String RECENTLY = APlayerApplication.getContext().getString(R.string.recently);
    //我的收藏
    public final static String MYLOVE = APlayerApplication.getContext().getString(R.string.my_favorite);

    //显示模式 1:列表 2:网格
    public final static int LIST_MODEL = 1;
    public final static int GRID_MODEL = 2;

    //操作类型
    public final static int SONG = 0;
    public final static int ALBUM = 1;
    public final static int ARTIST = 2;
    public final static int FOLDER = 3;
    public final static int PLAYLIST = 4;
    public final static int PLAYLISTSONG = 5;

    public final static String EXIT = "remix.music.EXIT";
    public final static String SOUNDEFFECT_ACTION = "remix.music.SOUNDEFFECT_ACTION";

    //控制命令
    public final static int PLAYSELECTEDSONG = 0;
    public final static int PREV = 1;
    public final static int TOGGLE = 2;
    public final static int NEXT = 3;
    public final static int PAUSE = 4;
    public final static int START = 5;
    public final static int CHANGE_MODEL = 6;
    public final static int LOVE = 7;
    public final static int TOGGLE_MEDIASESSION = 8;
    public final static int PLAY_TEMP = 9;
    public final static int TOGGLE_NOTIFY = 10;
    public final static int UNLOCK_DESTOP_LYRIC = 11;
    //播放模式
    public final static int PLAY_LOOP = 50;
    public final static int PLAY_SHUFFLE = 51;
    public final static int PLAY_REPEATONE = 52;
    //更新seekbar、已播放时间、未播放时间
    public final static int UPDATE_TIME_ALL = 0x010;
    //更新已播放时间、未播放时间
    public final static int UPDATE_TIME_ONLY = 0x011;
    //更新桌面部件
    public final static int UPDATE_APPWIDGET=  0x100;
    //更新播放信息
    public final static int UPDATE_UI = 0x101;
    //更新音量
    public final static int UPDATE_VOL = 0x102;
    //更新背景
    public final static int UPDATE_BG = 0x103;


    //腾讯Api Id
    public final static String TECENT_APIID = "1105030910";
    //微博Api Id
    public final static String WEIBO_APIID = "949172486";
    //微信APi Id
    public final static String WECHAT_APIID = "wx10775467a6664fbb";
    //有盟appkey
    public final static String UMENG_APPKEY = "56d6563367e58e6aa70005af";
    //获得专辑封面类型
    public final static int URL_ALBUM = 0;
    public final static int URL_ARTIST = 1;
    public final static int URL_PLAYLIST = 4;
    public final static String ACTION_BUTTON = "ACTION_BUTTON";
    public final static int NOTIFY_PREV = 0;
    public final static int NOTIFY_PLAY = 1;
    public final static int NOTIFY_NEXT = 2;
    //刷新适配器
    public final static int NOTIFYDATACHANGED = 0;


    //更新桌面歌词内容
    public static final int UPDATE_FLOAT_LRC_CONTENT = 500;
    //移除桌面歌词
    public static final int REMOVE_FLOAT_LRC = 501;
    //添加桌面歌词
    public static final int CREATE_FLOAT_LRC = 502;
    //开启或者关闭桌面歌词
    public static final int TOGGLE_FLOAT_LRC = 503;

    //扫描文件默认大小设置
    public static int SCAN_SIZE = 0;

    //分享心情还是歌曲
    public final static int SHARESONG = 1;
    public final static int SHARERECORD = 0;

    //更新
    public final static int UPDATE_FOLDER = 0;
    public final static int UPDATE_PLAYQUEUE = 1;
    public final static int UPDATE_PLAYLIST = 2;
    public final static int UPDATE_CHILDHOLDER = 3;


    //码率
    public final static int BIT_RATE = 0;
    //采样率
    public final static int SAMPLE_RATE = 1;
    //类型
    public final static int MIME = 2;

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
