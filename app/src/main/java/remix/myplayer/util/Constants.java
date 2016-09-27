package remix.myplayer.util;

/**
 * Created by taeja on 16-2-17.
 */

/**
 * 常量值
 */
public class Constants {
    //操作类型
    public final static int SONG = 0;
    public final static int ALBUM = 1;
    public final static int ARTIST = 2;
    public final static int FOLDER = 3;
    public final static int PLAYLIST = 4;

    public final static String THEME_ACTION = "remix.music.THEME_ACTION";
    public final static String CTL_ACTION = "remix.music.CTL_ACTION";
    public final static String UPDATE_ACTION = "remix.music.UPDATE_ACTION";
    public final static String CONTROL_TIMER = "remix.music.CONTROL_TIMER";
    public final static String NOTIFY = "remix.music.NOTIFY";
    public final static String EXIT = "remix.music.EXIT";
    public final static String SOUNDEFFECT_ACTION = "remix.music.SOUNDEFFECT_ACTION";


    //控制命令
    public final static int PLAYSELECTEDSONG = 0;
    public final static int PREV = 1;
    public final static int PLAYORPAUSE = 2;
    public final static int NEXT = 3;
    public final static int PAUSE = 4;
    public final static int CONTINUE = 5;
    //播放模式
    public final static int PLAY_LOOP = 6;
    public final static int PLAY_SHUFFLE = 7;
    public final static int PLAY_REPEATONE = 8;
    //当前状态
    public final static int STATUS_PLAY = 0x010;
    public final static int STATUS_PAUSE = 0x011;
    //更新seekbar、已播放时间、未播放时间
    public final static int UPDATE_TIME_ALL = 0x010;
    //更新已播放时间、未播放时间
    public final static int UPDATE_TIME_ONLY = 0x011;
    //更新播放信息
    public final static int UPDATE_INFORMATION = 0x101;
    //更新音量
    public final static int UPDATE_VOL = 0x102;
    //更新背景
    public final static int UPDATE_BG = 0x102;

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
    public final static int URL_SONGID = 2;
    public final static int URL_NAME = 3;
    public final static String ACTION_BUTTON = "ACTION_BUTTON";
    public final static int NOTIFY_PREV = 0;
    public final static int NOTIFY_PLAY = 1;
    public final static int NOTIFY_NEXT = 2;
    //刷新适配器
    public final static int NOTIFYDATACHANGED = 0;
    //今天添加或者最近一周
    public final static int DAY = 0;
    public final static int WEEK = 1;

    public final static int MAINACTIVITY = 0;
    public final static int AUDIOHOLDERACTIVITY = 1;
    public final static int CHILDHOLDERACTIVITY = 2;
    public final static int RECENTLYACTIVITY = 3;
    public final static int PLAYLISTACTIVITY = 4;
    public final static int LOCKSCREENACTIIVITY = 5;

    //扫描文件默认大小设置
    public static int SCAN_SIZE = 512000;

    //分享心情还是歌曲
    public final static int SHARESONG = 1;
    public final static int SHARERECORD = 0;

    //更新文件夹与文件夹详情
    public final static int UPDATE_FOLDER = 0;
    public final static int UPDATE_PLAYINGLIST = 1;
    public final static int UPDATE_PLAYLIST = 2;


    //码率
    public final static int BIT_RATE = 0;
    //采样率
    public final static int SAMPLE_RATE = 1;
    //类型
    public final static int MIME = 2;

    //更新适配器
    public final static int UPDATE_ADAPTER = 100;
    //多选更新
    public final static int UPDATE_MULTI = 101;
    //重建activity
    public final static int RECREATE_ACTIVITY = 102;
}
