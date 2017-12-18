package remix.myplayer.bean.netease;

import java.util.List;

import remix.myplayer.bean.BaseData;

/**
 * Created by Remix on 2017/11/30.
 */

public class NArtistSearchResponse extends BaseData {
    /**
     * result : {"artistCount":6,"artists":[{"id":18355,"name":"AKB48","picUrl":"http://p1.music.126.net/AjQKwXEp0VXXAkyISRl7Xw==/109951163016683093.jpg","alias":["エーケービー フォーティエイト"],"albumSize":200,"picId":109951163016683093,"img1v1Url":"http://p1.music.126.net/ybqv1sPl7l1J-inzlcMRRg==/109951163073222217.jpg","img1v1":109951163073222217,"mvSize":217,"followed":false,"alia":["エーケービー フォーティエイト"],"trans":null}]}
     * code : 200
     */

    private ResultBean result;
    private int code;

    public ResultBean getResult() {
        return result;
    }

    public void setResult(ResultBean result) {
        this.result = result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static class ResultBean {
        /**
         * artistCount : 6
         * artists : [{"id":18355,"name":"AKB48","picUrl":"http://p1.music.126.net/AjQKwXEp0VXXAkyISRl7Xw==/109951163016683093.jpg","alias":["エーケービー フォーティエイト"],"albumSize":200,"picId":109951163016683093,"img1v1Url":"http://p1.music.126.net/ybqv1sPl7l1J-inzlcMRRg==/109951163073222217.jpg","img1v1":109951163073222217,"mvSize":217,"followed":false,"alia":["エーケービー フォーティエイト"],"trans":null}]
         */

        private int artistCount;
        private List<ArtistsBean> artists;

        public int getArtistCount() {
            return artistCount;
        }

        public void setArtistCount(int artistCount) {
            this.artistCount = artistCount;
        }

        public List<ArtistsBean> getArtists() {
            return artists;
        }

        public void setArtists(List<ArtistsBean> artists) {
            this.artists = artists;
        }

        public static class ArtistsBean {
            /**
             * id : 18355
             * name : AKB48
             * picUrl : http://p1.music.126.net/AjQKwXEp0VXXAkyISRl7Xw==/109951163016683093.jpg
             * alias : ["エーケービー フォーティエイト"]
             * albumSize : 200
             * picId : 109951163016683093
             * img1v1Url : http://p1.music.126.net/ybqv1sPl7l1J-inzlcMRRg==/109951163073222217.jpg
             * img1v1 : 109951163073222217
             * mvSize : 217
             * followed : false
             * alia : ["エーケービー フォーティエイト"]
             * trans : null
             */

            private int id;
            private String name;
            private String picUrl;
            private int albumSize;
            private long picId;
            private String img1v1Url;
            private long img1v1;
            private int mvSize;
            private boolean followed;
            private Object trans;
            private List<String> alias;
            private List<String> alia;

            public int getId() {
                return id;
            }

            public void setId(int id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPicUrl() {
                return picUrl;
            }

            public void setPicUrl(String picUrl) {
                this.picUrl = picUrl;
            }

            public int getAlbumSize() {
                return albumSize;
            }

            public void setAlbumSize(int albumSize) {
                this.albumSize = albumSize;
            }

            public long getPicId() {
                return picId;
            }

            public void setPicId(long picId) {
                this.picId = picId;
            }

            public String getImg1v1Url() {
                return img1v1Url;
            }

            public void setImg1v1Url(String img1v1Url) {
                this.img1v1Url = img1v1Url;
            }

            public long getImg1v1() {
                return img1v1;
            }

            public void setImg1v1(long img1v1) {
                this.img1v1 = img1v1;
            }

            public int getMvSize() {
                return mvSize;
            }

            public void setMvSize(int mvSize) {
                this.mvSize = mvSize;
            }

            public boolean isFollowed() {
                return followed;
            }

            public void setFollowed(boolean followed) {
                this.followed = followed;
            }

            public Object getTrans() {
                return trans;
            }

            public void setTrans(Object trans) {
                this.trans = trans;
            }

            public List<String> getAlias() {
                return alias;
            }

            public void setAlias(List<String> alias) {
                this.alias = alias;
            }

            public List<String> getAlia() {
                return alia;
            }

            public void setAlia(List<String> alia) {
                this.alia = alia;
            }
        }
    }
}
