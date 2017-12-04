package remix.myplayer.model.netease;

import remix.myplayer.model.BaseData;

/**
 * Created by Remix on 2017/12/4.
 */

public class NSearchRequest extends BaseData {
    public static final NSearchRequest DEFAULT_REQUEST = new NSearchRequest(-1,"",0,0);

    private String mKey;
    //网易查询的类型
    private int mNeteaseType;
    //本地数据库查询的类型
    private int mLocalType;
    private int mID;

    public NSearchRequest(int id,String key, int ntype,int ltype) {
        this.mID = id;
        this.mKey = key;
        this.mNeteaseType = ntype;
        this.mLocalType = ltype;
    }

    public int getLType(){
        return mLocalType;
    }

    public int getID() {
        return mID;
    }

    public String getKey() {
        return mKey;
    }

    public int getNType() {
        return mNeteaseType;
    }
}
