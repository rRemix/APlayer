package remix.myplayer.bean.netease;

import remix.myplayer.bean.BaseData;

/**
 * Created by Remix on 2017/11/19.
 */

public class NLrcResponse extends BaseData {
    public boolean sgc;
    public boolean sfy;
    public boolean qfy;
    public int code;
    public LrcActualData lrc;
    public LrcActualData klyric;
    public LrcActualData tlyric;
    public TransUserData transUser;

    public static class TransUserData extends BaseData{
        public int id;
        public int status;
        public int demand;
        public int userid;
        public String nickname;
        public long uptime;
    }

    public static class LrcActualData extends BaseData{
        public int version;
        public String lyric;
    }


}
