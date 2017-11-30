package remix.myplayer.lyric;

import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import remix.myplayer.APlayerApplication;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.cache.DiskLruCache;
import remix.myplayer.model.LrcRequest;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.model.netease.NSongSearchResponse;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
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
    private Song mInfo;
    private String mTitle;
    private String mArtistName;
    private String mDisplayName;
    private String mManualPath;

    public SearchLRC(Song item) {
        mInfo = item;
        mTitle = mInfo.getTitle();
        mArtistName = mInfo.getArtist();
        try {
            if(!TextUtils.isEmpty(mInfo.getDisplayname())){
                String temp = mInfo.getDisplayname();
                mDisplayName = temp.indexOf('.') > 0 ? temp.substring(0,temp.lastIndexOf('.')) : temp;
            }
        } catch (Exception e){
            CommonUtil.uploadException("SearchLrc Init Error","DisPlayName:" + item.getDisplayname() + " Title:" + item.getTitle());
            mDisplayName = mTitle;
        }
        mLrcBuilder = new DefaultLrcParser();
    }

    public SearchLRC(Song item, String manualPath){
        this(item);
        mManualPath = manualPath;
    }

    public int getSongID(){
        return mInfo.getId();
    }


    /**
     * 获取酷狗歌词接口的参数
     * @return
     */
    public LrcRequest getLrcParam(){
        //酷狗
        try {
            JSONObject response = CommonUtil.getSongJsonObject(URLEncoder.encode(mInfo.getTitle(), "utf-8"),
                            URLEncoder.encode(mInfo.getArtist(), "utf-8"),mInfo.getDuration());
            if(response != null && response.length() > 0){
                if(response.getJSONArray("candidates").length() > 0){
                    JSONObject jsonObject = response.getJSONArray("candidates").getJSONObject(0);
                    if(jsonObject.getInt("score") >= 60)
                        return new LrcRequest(jsonObject.getInt("id"),jsonObject.getString("accesskey"));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new LrcRequest();
    }

    public void test(){
        new Thread(){
            @Override
            public void run() {
                try {


                    Map<String,String> params = new HashMap<>();
                    params.put("s","Seemann");
                    params.put("offset","0");
                    params.put("limit","0");
                    params.put("type","1");
                    StringBuilder paramStr = new StringBuilder();

                    int pos = 0;
                    for(String key : params.keySet()){
                        if(pos > 0)
                            paramStr.append("&");
                        paramStr.append(String.format("%s=%s", key, URLEncoder.encode(params.get(key), "utf-8")));
                        pos++;
                    }

                    final String url = "http://music.163.com/api/search/pc?s=Seemann&offset=0&limit=10&type=1006";
                    OkHttpClient client = new OkHttpClient.Builder().build();
                    RequestBody requestBody = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"),"");

                    Request request = new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .addHeader("Cookie","appver=1.5.0.75771")
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            String responseString = response.body().string();
                            NSongSearchResponse searchResponse = new Gson().fromJson(responseString,NSongSearchResponse.class);
                            LogUtil.d("NSongSearchResponse", searchResponse + "");
                        }
                    });

//                    Response response = client.newCall(request).execute();


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();

    }

    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词信息list
     */
    public List<LrcRow> getLrc(String lrcPath){
        //判断是否是忽略的歌词
        Set<String> ignoreLrcId = SPUtil.getStringSet(APlayerApplication.getContext(),"Setting","IgnoreLrcID");
        if(ignoreLrcId != null && ignoreLrcId.size() > 0){
            for (String id : ignoreLrcId){
                if((mInfo.getId() + "").equals(id)){
                    return null;
                }
            }
        }
        BufferedReader br = null;
        //manualPath不为空说明为手动设置歌词
        try {
            if(!TextUtils.isEmpty(lrcPath)){
                br = new BufferedReader(new InputStreamReader(new FileInputStream(lrcPath)));
                return mLrcBuilder.getLrcRows(br,true, mTitle,mArtistName);
            }
        } catch (Exception e){
            LogUtil.e(TAG,e.toString());
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        //搜索时判断该歌曲是否有缓存
        try {
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(CommonUtil.hashKeyForDisk(mTitle + "/" + mArtistName));
             if(snapShot != null && (br = new BufferedReader(new InputStreamReader(snapShot.getInputStream(0)))) != null ){
                return mLrcBuilder.getLrcRows(br,false, mTitle,mArtistName);
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

        //搜索歌词
        try {
            //是否优先搜索在线歌词
            boolean onlineFirst = SPUtil.getValue(APlayerApplication.getContext(),"Setting","OnlineLrc",false);
            if(onlineFirst){
                String onlineLrcContent = getOnlineLrcContent();
                if(!TextUtils.isEmpty(onlineLrcContent)){
                    br = new BufferedReader(
                            new InputStreamReader(new ByteArrayInputStream(Base64.decode(onlineLrcContent, Base64.DEFAULT))));
                    return mLrcBuilder.getLrcRows(br,true, mTitle,mArtistName);
                } else {
                    String localLrcPath = getlocalLrcPath();
                    if(!localLrcPath.equals("")){
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(localLrcPath)));
                        return mLrcBuilder.getLrcRows(br,true, mTitle,mArtistName);
                    }
                }
            } else {
                String localLrcPath = getlocalLrcPath();
                if(!localLrcPath.equals("")){
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(localLrcPath)));
                    return mLrcBuilder.getLrcRows(br,true, mTitle,mArtistName);
                } else {
                    String onlineLrcContent = getOnlineLrcContent();
                    if(!TextUtils.isEmpty(onlineLrcContent)){
                        br = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Base64.decode(onlineLrcContent, Base64.DEFAULT))));
                        return mLrcBuilder.getLrcRows(br,true, mTitle,mArtistName);
                    }
                }
            }
        }catch (Exception e){
//            LogUtil.e(TAG,e.toString());
            CommonUtil.uploadException("Search Error",e);
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    /**
     * 获得在线歌词
     * @return
     */
    private String getOnlineLrcContent(){
        LrcRequest lrcParam = getLrcParam();
        BufferedReader br = null;
        if(lrcParam != null && !TextUtils.isEmpty(lrcParam.AccessKey)){
            try {
                URL url = new URL("http://lyrics.kugou.com/download?ver=1&client=pc&id=" + lrcParam.ID + "&accesskey=" + lrcParam.AccessKey + "&fmt=lrc&charset=utf8");
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
                return new JSONObject(stringBuffer.toString()).getString("content");
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
        }
        return null;
    }

    /**
     * 搜索本地所有歌词文件
     * @return
     */
    private String getlocalLrcPath() {
        //查找本地目录
        String searchPath =  SPUtil.getValue(APlayerApplication.getContext(),"Setting","LrcSearchPath","");
        if(mInfo == null)
            return "";
        if(!TextUtils.isEmpty(searchPath)){
            //已设置歌词路径
            CommonUtil.searchFile(mDisplayName,mTitle,mArtistName, new File(searchPath));
            if(!TextUtils.isEmpty(Global.CurrentLrcPath)){
                return Global.CurrentLrcPath;
            }

        } else{
            //没有设置歌词路径 搜索所有歌词文件
            Cursor allLrcFiles = null;
            try {
                allLrcFiles = APlayerApplication.getContext().getContentResolver().
                        query(MediaStore.Files.getContentUri("external"),
                                null,
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                MediaStore.Files.FileColumns.DATA + " like ? ",
                                new String[]{"%lyric%","%Lyric%","%.lrc"},
                                null);
                if(allLrcFiles == null || !(allLrcFiles.getCount() > 0))
                    return "";
                while (allLrcFiles.moveToNext()){
                    File file = new File(allLrcFiles.getString(allLrcFiles.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.canRead()) {
                        if(CommonUtil.isRightLrc(file, mDisplayName,mTitle,mArtistName)){
                            return file.getAbsolutePath();
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                if(allLrcFiles != null && !allLrcFiles.isClosed())
                    allLrcFiles.close();
            }
        }

        return "";
    }

}
