package remix.myplayer.bean.netease;

import java.io.Serializable;

/**
 * Created by Remix on 2017/12/4.
 */

public class SearchRequest implements Serializable {
    public static final int TYPE_NETEASE_SONG = 1;
    public static final int TYPE_NETEASE_ALBUM = 10;
    public static final int TYPE_NETEASE_ARTIST = 100;

    public static final SearchRequest DEFAULT_REQUEST = new SearchRequest(-1,"",0,0);
    private static final long serialVersionUID = -4168031236748350436L;

    private String mKey;
    //网易查询的类型
    private int mNeteaseType;
    //本地数据库查询的类型
    private int mLocalType;
    private int mID;

    public SearchRequest(int id, String key, int ntype, int ltype) {
        this.mID = id;
        this.mKey = key;
        this.mNeteaseType = ntype;
        this.mLocalType = ltype;
    }

    public int getLocalType(){
        return mLocalType;
    }

    public int getID() {
        return mID;
    }

    public String getKey() {
        return mKey;
    }

    public int getNeteaseType() {
        return mNeteaseType;
    }

    @Override
    public String toString() {
        return "SearchRequest{" +
                "mKey='" + mKey + '\'' +
                ", mNeteaseType=" + mNeteaseType +
                ", mLocalType=" + mLocalType +
                ", mID=" + mID +
                '}';
    }
}
