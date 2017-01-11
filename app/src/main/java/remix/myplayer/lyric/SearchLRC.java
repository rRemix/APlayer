package remix.myplayer.lyric;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import remix.myplayer.application.APlayerApplication;
import remix.myplayer.model.LrcRequest;
import remix.myplayer.model.MP3Item;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.DiskLruCache;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/7.
 */

/**
 * 根据歌曲名和歌手名 搜索歌词并解析成固定格式
 */
public class SearchLRC {
    private static final String TAG = "SearchLRC";
    private static final String DEFAULT_LOCAL = "GB2312";
    private static final String LRC_REQUEST_ROOT = "http://s.geci.me/lrc/";
    private ILrcParser mLrcBuilder;
    private MP3Item mInfo;
    private String mSongName;
    private String mArtistName;

    public SearchLRC(MP3Item item) {
        mInfo = item;
        mSongName = mInfo.getTitle();
        mArtistName = mInfo.getArtist();
        mLrcBuilder = new DefaultLrcParser();
    }

    public int getSongID(){
        return mInfo.getId();
    }

    /**
     * 根据歌手与歌手名,获得歌词id
     * @return 歌词id
     */
    public String getLrcUrl(){
        try {
            JSONObject lrcid = CommonUtil.getSongJsonObject(
                    URLEncoder.encode(mInfo.getTitle(), "utf-8"),
                    URLEncoder.encode(mInfo.getArtist(), "utf-8"),mInfo.getDuration());
            //歌词迷
            if(lrcid != null && lrcid.getInt("count") > 0 && lrcid.getInt("code") == 0){
                return lrcid.getJSONArray("result").getJSONObject(0).getString("lrc");
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取酷狗歌词接口的参数
     * @return
     */
    public LrcRequest getLrcParam(){
        //酷狗
        try {
            JSONObject response =
                    CommonUtil.getSongJsonObject(URLEncoder.encode(mInfo.getTitle(), "utf-8"),
                            URLEncoder.encode(mInfo.getArtist(), "utf-8"),mInfo.getDuration());
            if(response != null && response.length() > 0){
                JSONObject jsonObject = response.getJSONArray("candidates").getJSONObject(0);
                return new LrcRequest(jsonObject.getInt("id"),jsonObject.getString("accesskey"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new LrcRequest();
    }

    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词信息list
     */
    public List<LrcRow> getLrc(){
        BufferedReader br = null;
        //先判断该歌曲是否有缓存
        try {
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(CommonUtil.hashKeyForDisk(mSongName + "/" + mArtistName));
             if(snapShot != null && (br = new BufferedReader(new InputStreamReader(snapShot.getInputStream(0)))) != null ){
                return mLrcBuilder.getLrcRows(br,false,mSongName,mArtistName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        //查找本地目录
        String setLrcPath =  SPUtil.getValue(APlayerApplication.getContext(),"Setting","LrcPath","");
        if(setLrcPath.equals("") && !TextUtils.isEmpty(mInfo.getUrl())){
            //没有设置歌词路径
            for(int i = 0; i < Global.LyricDir.size() ; i++){
                File searchPath = Global.LyricDir.get(i);
                if(searchPath.exists() && searchPath.isDirectory() && !searchPath.equals(Environment.getExternalStorageDirectory()))
                    CommonUtil.searchFile(APlayerApplication.getContext(),mSongName,mArtistName, searchPath);
            }
        } else {
            //已设置歌词路径
            CommonUtil.searchFile(APlayerApplication.getContext(),mSongName,mArtistName, new File(setLrcPath));
        }

        if(!Global.CurrentLrcPath.equals("")){
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(Global.CurrentLrcPath)));
                return mLrcBuilder.getLrcRows(br,true,mSongName,mArtistName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                Global.CurrentLrcPath = "";
                if(br != null)
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }

        //无网络连接
        if(!CommonUtil.isNetWorkConnected()){
            return null;
        }
        //没有缓存，下载并解析歌词
        //1.酷狗
        LrcRequest lrcParam = getLrcParam();
        //2.歌词迷
//        String lrcUrl = getLrcUrl();
        URL url;
        try {
//            url = new URL(lrcUrl);
//            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//            httpConn.setConnectTimeout(10000);
//            httpConn.connect();
//            br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));

            url = new URL("http://lyrics.kugou.com/download?ver=1&client=pc&id=" + lrcParam.ID + "&accesskey=" + lrcParam.AccessKey + "&fmt=lrc&charset=utf8");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setConnectTimeout(10000);
            httpConn.connect();
            br = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            StringBuffer stringBuffer = new StringBuffer(128);
            String s;
            while ((s = br.readLine()) != null){
                stringBuffer.append(s);
            }
            if(TextUtils.isEmpty(stringBuffer)){
                return null;
            }
            br = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(Base64.decode(new JSONObject(stringBuffer.toString()).getString("content"), Base64.DEFAULT))));

            return mLrcBuilder.getLrcRows(br,true,mSongName,mArtistName);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            if(br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

}
