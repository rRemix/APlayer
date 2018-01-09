package remix.myplayer.bean.kugou;

import java.util.List;

import remix.myplayer.bean.BaseData;

/**
 * Created by Remix on 2017/11/21.
 */

public class KSearchResponse extends BaseData {
    private static final long serialVersionUID = -4932979292293561928L;
    public String info;
    public int status;
    public String proposal;
    public String keyword;
    public List<SearchActutalData> candidates;

    public static class SearchActutalData extends BaseData{
        private static final long serialVersionUID = -2720016148055276583L;
        public String soundname;
        public int krctype;
        public String nickname;
        public String originame;
        public String accesskey;
        public String origiuid;
        public int score;
        public int hitlayer;
        public int duration;
        public String sounduid;
        public String song;
        public String uid;
        public String transuid;
        public String transname;
        public int adjust;
        public int id;
        public String singer;
        public String language;
    }
}
