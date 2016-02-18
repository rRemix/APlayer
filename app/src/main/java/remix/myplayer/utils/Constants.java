package remix.myplayer.utils;

/**
 * Created by taeja on 16-2-17.
 */
public class Constants {
    //删除选项
    public final static int DELETE_SINGLE = 0;
    public final static int DELETE_ALBUM = 1;
    public final static int DELETE_ARTIST = 2;
    public final static int DELETE_FOLDER = 3;
    //启动哪一个fragment
    public final static int PLAYLIST_HOLDER = 0;
    public final static int ALBUM_HOLDER = 1;
    public final static int ARTIST_HOLDER = 2;
    public final static int FOLDER_HOLDER = 3;

    public final static String CTL_ACTION = "remix.music.CTL_ACTION";
    public final static String UPDATE_ACTION = "remix.music.UPDATE_ACTION";
    public final static String CONTROL_TIMER = "remix.music.CONTROL_TIMER";
    public final static String NOTIFY = "remix.music.NOTIFY";
    public final static String EXIT = "remix.music.EXIT";
    //控制命令
    public final static int PLAYSELECTEDSONG = 0;
    public final static int PREV = 1;
    public final static int PLAY = 2;
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
    //更新背景
    public final static int UPDATE_BG = 0x102;

    //腾讯Api Id
    public final static String TECENT_APIID = "1105030910";
    //微博Api Id
    public final static String WEIBO_APIID = "949172486";
    //微信APi Id
    public final static String WECHAT_APIID = "wx10775467a6664fbb";
    //获得专辑封面类型
    public final static int URL_ALBUM = 0;
    public final static int URL_ARTIST = 1;
    public final static int URL_SONGID = 2;
    public final static int URL_NAME = 3;
    public final static String ACTION_BUTTON = "ACTION_BUTTON";
    public final static int NOTIFY_PREV = 0;
    public final static int NOTIFY_PLAY = 1;
    public final static int NOTIFY_NEXT = 2;
}
