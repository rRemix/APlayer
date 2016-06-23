package remix.myplayer.utils.thumb;

/**
 * Created by taeja on 16-6-21.
 */

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import remix.myplayer.utils.CommonUtil;

/**
 * 根据歌曲名和歌手名 搜索专辑封面
 */
public class SearchCover {
    private static final String TAG = "SearchCover";
    private static final String DEFAULT_LOCAL = "GB2312";
    private String mSongName;
    private String mArtistName;
    private String mType;
    public static final String COVER = "COVER";
    public static final String THUMB = "THUMB";

    public SearchCover(String songname, String singerName,String type) {
        mType = type;
        //传进来的如果是汉字，那么就要进行编码转化
        try {
            mSongName = URLEncoder.encode(songname, "utf-8");
            mArtistName = URLEncoder.encode(singerName, "utf-8");
        } catch (UnsupportedEncodingException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
    }


    /**
     * 返回专辑封面或者缩略图的url
     * @return
     */
    public String getImgUrl(){
        int albumId = 0;
        try {
            JSONObject songJsonObject = CommonUtil.getSongJsonObject(mSongName,mArtistName);
            if(songJsonObject != null && songJsonObject.getInt("count") > 0 && songJsonObject.getInt("code") == 0){
                albumId =  songJsonObject.getJSONArray("result").getJSONObject(0).getInt("aid");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(albumId == 0)
            return "";

        String imgurl;

        try {
            JSONObject coverJsonObject = CommonUtil.getCoverJsonObject(albumId);
            if(coverJsonObject != null && coverJsonObject.getInt("count") > 0 && coverJsonObject.getInt("code") == 0){
                imgurl = mType.equals(COVER) ? coverJsonObject.getJSONObject("result").getString("cover") :
                        coverJsonObject.getJSONObject("result").getString("thumb");
                return imgurl;
            }

        }  catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}
