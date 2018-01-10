package remix.myplayer.bean.netease;

import java.io.Serializable;

/**
 * Created by Remix on 2017/11/19.
 */

public class NLrcResponse implements Serializable {
    private static final long serialVersionUID = 3447785606973976839L;
    public boolean sgc;
    public boolean sfy;
    public boolean qfy;
    public int code;
    public LrcActualData lrc;
    public LrcActualData klyric;
    public LrcActualData tlyric;
    public TransUserData transUser;

    public static class TransUserData implements Serializable{
        private static final long serialVersionUID = 6288902941579934143L;
        public int id;
        public int status;
        public int demand;
        public int userid;
        public String nickname;
        public long uptime;
    }

    public static class LrcActualData implements Serializable{
        private static final long serialVersionUID = -1692673455916843725L;
        public int version;
        public String lyric;
    }


}
