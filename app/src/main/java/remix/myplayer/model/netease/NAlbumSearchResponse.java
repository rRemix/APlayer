package remix.myplayer.model.netease;

import java.util.List;

import remix.myplayer.model.BaseData;

/**
 * Created by Remix on 2017/11/30.
 */

public class NAlbumSearchResponse extends BaseData {
    public ResultBean result;
    public int code;

    public static class ResultBean {

        public int albumCount;
        public List<AlbumsBean> albums;

        public static class AlbumsBean {
            public String name;
            public int id;
            public String type;
            public int size;
            public long picId;
            public String blurPicUrl;
            public int companyId;
            public long pic;
            public String picUrl;
            public long publishTime;
            public String description;
            public String tags;
            public String company;
            public String briefDesc;
            public int status;
            public int copyrightId;
            public String commentThreadId;
            public String picId_str;

        }
    }
}
