package remix.myplayer.lyric;

import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.LrcRequest;
import remix.myplayer.bean.kugou.KLrcResponse;
import remix.myplayer.bean.kugou.KSearchResponse;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NLrcResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.cache.DiskLruCache;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2015/12/7.
 */

/**
 * 根据歌曲名和歌手名 搜索歌词并解析成固定格式
 */
public class SearchLrc {
    private static final String TAG = "SearchLrc";
    /**
     * 当前播放歌曲的lrc文件路径
     */
    public static String CurrentLrcPath = "";
    private ILrcParser mLrcParser;
    private Song mSong;
    private String mDisplayName;
    private String mKey;

    public SearchLrc(Song item) {
        mSong = item;
        try {
            if(!TextUtils.isEmpty(mSong.getDisplayname())){
                String temp = mSong.getDisplayname();
                mDisplayName = temp.indexOf('.') > 0 ? temp.substring(0,temp.lastIndexOf('.')) : temp;
            }
        } catch (Exception e){
            LogUtil.d(TAG,e.toString());
            mDisplayName = mSong.getTitle();
        }
        mLrcParser = new DefaultLrcParser();
    }


    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词
     */
    public Observable<List<LrcRow>> getLyric(String manualPath,boolean clearCache){
        int type = SPUtil.getValue(App.getContext(),SPUtil.LYRIC_KEY.LYRIC_NAME,mSong.getId() + "",SPUtil.LYRIC_KEY.LYRIC_NETEASE);
        Observable<List<LrcRow>> networkObservable = getNetworkObservable(type);
        Observable<List<LrcRow>> localObservable = getLocalObservable();

        //根据在线和本地的优先级 确定最后一级
        boolean onlineFirst = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.ONLINE_LYRIC_FIRST,false);
        Observable<List<LrcRow>> last = Observable.concat(onlineFirst ? networkObservable : localObservable ,onlineFirst ? localObservable : networkObservable).firstOrError().toObservable();

        return type == SPUtil.LYRIC_KEY.LYRIC_IGNORE ? Observable.error(new Throwable("Ignore")) :
                Observable.concat(getCacheObservable(),getManualObservable(manualPath),last).firstOrError().toObservable()
                .doOnSubscribe(disposable -> {
                    mKey = Util.hashKeyForDisk(mSong.getTitle() + "/" + mSong.getArtist());
                    CurrentLrcPath = "";
                    if(clearCache){
                        DiskCache.getLrcDiskCache().remove(mKey);
                    }
                })
                .compose(RxUtil.applyScheduler());

    }


    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词
     */
    public Observable<List<LrcRow>> getLyric(){
        return getLyric("",false);
    }


    /**
     * 手动设置歌词
     */
    private Observable<List<LrcRow>> getManualObservable(final String manualPath){
        return Observable.create(e -> {
            //手动设置的歌词
            if(!TextUtils.isEmpty(manualPath)){
                e.onNext(mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(manualPath))),true, mSong.getTitle(),mSong.getArtist()));
            }
            e.onComplete();
        });
    }


    /**
     * 缓存
     */
    private Observable<List<LrcRow>> getCacheObservable(){
        return Observable.create(e -> {
            //缓存
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(mKey);
            if(snapShot != null){
                BufferedReader br = new BufferedReader(new BufferedReader(new InputStreamReader(snapShot.getInputStream(0))));
                e.onNext(new Gson().fromJson(br.readLine(),new TypeToken<List<LrcRow>>(){}.getType()));
                snapShot.close();
                br.close();
            }
            e.onComplete();
        });
    }


    /**
     * 本地歌词
     * @return
     */
    private Observable<List<LrcRow>> getLocalObservable() {
        return Observable.create(e -> {
            List<String> localPaths = getAllLocalLrcPath();
            if(localPaths.size()>0) {
                if(localPaths.size()==1) {
                    String localPath = localPaths.get(0);
                    e.onNext(mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(localPath))), true, mSong.getTitle(), mSong.getArtist()));
                }else{
                    String localPath = localPaths.get(0);
                    String translatePath=null;
                    for (String path : localPaths) {
                        if(path.contains("translate")&&!path.equals(localPath)){
                            translatePath=path;
                            break;
                        }
                    }
                    if(translatePath==null){
                        e.onNext(mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(localPath))), true, mSong.getTitle(), mSong.getArtist()));
                    }else{
                        //合并歌词
                        List<LrcRow> source = mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(localPath))), true, mSong.getTitle(), mSong.getArtist());
                        List<LrcRow> translate = mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(translatePath))),false,mSong.getTitle(),mSong.getArtist() + "/translate");
                        if(translate != null && translate.size() > 0) {
                            int j = 0;
                            for (int i = 0; i < source.size(); ) {
                                boolean match = Math.abs(translate.get(j).getTime() - source.get(i).getTime()) < 1000;
                                if (match) {
                                    source.get(i).setTranslate(translate.get(j).getContent());
                                    i++;
                                } else if(translate.get(j).getTime()>source.get(i).getTime()){
                                    i++;
                                } else {
                                    j++;
                                }
                            }
                            mLrcParser.saveLrcRows(source,mKey);
                            e.onNext(source);
                        }
                    }
                }
            }
            e.onComplete();
        });
    }


    /**
     * 在线歌词
     * @return
     * @param type
     */
    private Observable<List<LrcRow>> getNetworkObservable(int type) {
        if(mSong == null){
            return Observable.error(new Throwable("no available song"));
        }
        String key = getLyricSearchKey(mSong);
        if(TextUtils.isEmpty(key)){
            return Observable.error(new Throwable("no available key"));
        }
        if(type == SPUtil.LYRIC_KEY.LYRIC_KUGOU){
            //酷狗歌词
            return HttpClient.getKuGouApiservice().getKuGouSearch(1,"yes","pc",key,mSong.getDuration(),"")
                    .flatMap(body -> {
                        final KSearchResponse searchResponse = new Gson().fromJson(body.string(),KSearchResponse.class);
                        return HttpClient.getKuGouApiservice().getKuGouLyric(1,"pc","lrc","utf8",searchResponse.candidates.get(0).id,
                                searchResponse.candidates.get(0).accesskey)
                                .map(lrcBody -> {
                                    final KLrcResponse lrcResponse = new Gson().fromJson(lrcBody.string(),KLrcResponse.class);
                                    return mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(Base64.decode(lrcResponse.content, Base64.DEFAULT)))),
                                            true,mSong.getTitle(),
                                            mSong.getArtist());
                                });
                    });
        }else {
            //网易歌词
            return HttpClient.getNeteaseApiservice()
                    .getNeteaseSearch(key,0,1,1)
                    .flatMap(body -> HttpClient.getInstance()
                            .getNeteaseLyric(new Gson().fromJson(body.string(),NSongSearchResponse.class).result.songs.get(0).id)
                            .map(body1 -> {
                                final NLrcResponse lrcResponse = new Gson().fromJson(body1.string(),NLrcResponse.class);
                                final Charset utf8 = Charset.forName("utf8");
                                List<LrcRow> combine = mLrcParser.getLrcRows(new BufferedReader(
                                        new InputStreamReader(
                                                new ByteArrayInputStream(lrcResponse.lrc.lyric.getBytes(utf8)),utf8)),false,mSong.getTitle(),mSong.getArtist() + "/original");
                                //有翻译 合并
                                if(lrcResponse.tlyric != null && !TextUtils.isEmpty(lrcResponse.tlyric.lyric)){
                                    List<LrcRow> translate = mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(lrcResponse.tlyric.lyric.getBytes(utf8)),utf8)),false,mSong.getTitle(),mSong.getArtist() + "/translate");
                                    if(translate != null && translate.size() > 0){
                                        for(int i = 0 ; i < translate.size();i++){
                                            for(int j = 0 ; j < combine.size();j++){
                                                if(translate.get(i).getTime() == combine.get(j).getTime()){
                                                    combine.get(j).setTranslate(translate.get(i).getContent());
                                                    break;
                                                }
                                            }
                                        }

                                    }
                                }

                                mLrcParser.saveLrcRows(combine,mKey);
                                return combine;
                            })).onErrorResumeNext(throwable -> {
                        return Observable.empty();
                    });
        }

    }

    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词信息list
     */
    @Deprecated
    public List<LrcRow> getLrc(String manualPath){
        //判断是否是忽略的歌词
        Set<String> ignoreLrcId = SPUtil.getStringSet(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,"IgnoreLrcID");
        if(ignoreLrcId != null && ignoreLrcId.size() > 0){
            for (String id : ignoreLrcId){
                if((mSong.getId() + "").equals(id)){
                    return null;
                }
            }
        }
        BufferedReader br = null;
        //manualPath不为空说明为手动设置歌词
        try {
            if(!TextUtils.isEmpty(manualPath)){
                br = new BufferedReader(new InputStreamReader(new FileInputStream(manualPath)));
                return mLrcParser.getLrcRows(br,true,mSong.getTitle(),mSong.getArtist());
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
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(Util.hashKeyForDisk(mKey));
             if(snapShot != null && (br = new BufferedReader(new InputStreamReader(snapShot.getInputStream(0)))) != null ){
                 List<LrcRow> lrcRows = mLrcParser.getLrcRows(br,false, mSong.getTitle(),mSong.getArtist());
                 snapShot.close();
                 return lrcRows;
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
            boolean onlineFirst = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.ONLINE_LYRIC_FIRST,false);
            if(onlineFirst){
                String onlineLrcContent = getOnlineLrcContent();
                if(!TextUtils.isEmpty(onlineLrcContent)){
                    br = new BufferedReader(
                            new InputStreamReader(new ByteArrayInputStream(Base64.decode(onlineLrcContent, Base64.DEFAULT))));
                    return mLrcParser.getLrcRows(br,true, mSong.getTitle(),mSong.getArtist());
                } else {
                    String localLrcPath = getLocalLrcPath();
                    if(!localLrcPath.equals("")){
                        br = new BufferedReader(new InputStreamReader(new FileInputStream(localLrcPath)));
                        return mLrcParser.getLrcRows(br,true, mSong.getTitle(),mSong.getArtist());
                    }
                }
            } else {
                String localLrcPath = getLocalLrcPath();
                if(!localLrcPath.equals("")){
                    br = new BufferedReader(new InputStreamReader(new FileInputStream(localLrcPath)));
                    return mLrcParser.getLrcRows(br,true, mSong.getTitle(),mSong.getArtist());
                } else {
                    String onlineLrcContent = getOnlineLrcContent();
                    if(!TextUtils.isEmpty(onlineLrcContent)){
                        br = new BufferedReader(
                                new InputStreamReader(new ByteArrayInputStream(Base64.decode(onlineLrcContent, Base64.DEFAULT))));
                        return mLrcParser.getLrcRows(br,true, mSong.getTitle(),mSong.getArtist());
                    }
                }
            }
        }catch (Exception e){
//            LogUtil.e(TAG,e.toString());
            LogUtil.d(TAG,e.toString());
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
     * 获取酷狗歌词接口的参数
     * @return
     */
    public LrcRequest getLrcParam(){
        //酷狗
        try {
            JSONObject response = Util.getSongJsonObject(URLEncoder.encode(mSong.getTitle(), "utf-8"),
                    URLEncoder.encode(mSong.getArtist(), "utf-8"), mSong.getDuration());
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
    private String getLocalLrcPath() {
        //查找本地目录
        String searchPath =  SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR,"");
        if(mSong == null)
            return "";
        if(!TextUtils.isEmpty(searchPath)){
            //已设置歌词路径
            Util.searchFile(mDisplayName,mSong.getTitle(),mSong.getArtist(), new File(searchPath));
            if(!TextUtils.isEmpty(CurrentLrcPath)){
                return CurrentLrcPath;
            }
        } else{
            //没有设置歌词路径 搜索所有歌词文件
            Cursor filesCursor = null;
            try {
                filesCursor = App.getContext().getContentResolver().
                        query(MediaStore.Files.getContentUri("external"),
                                null,
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? ",
                                new String[]{"%lyric%","%Lyric%","%.lrc%"},
                                null);
                if(filesCursor == null || !(filesCursor.getCount() > 0))
                    return "";
                while (filesCursor.moveToNext()){
                    File file = new File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.isFile() && file.canRead()) {
                        if(Util.isRightLrc(file, mDisplayName,mSong.getTitle(),mSong.getArtist())){
                            return file.getAbsolutePath();
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            } finally {
                if(filesCursor != null && !filesCursor.isClosed())
                    filesCursor.close();
            }
        }

        return "";
    }

    private List<String> getAllLocalLrcPath() {
        List<String> results = new ArrayList<>();
        //查找本地目录
        String searchPath = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "");
        if (mSong == null)
            return results;
        if (!TextUtils.isEmpty(searchPath)) {
            //已设置歌词路径
            Util.searchFile(mDisplayName, mSong.getTitle(), mSong.getArtist(), new File(searchPath));
            if (!TextUtils.isEmpty(CurrentLrcPath)) {
                results.add(CurrentLrcPath);
                return results;
            }
        } else {
            //没有设置歌词路径 搜索所有歌词文件
            Cursor filesCursor = null;
            try {
                filesCursor = App.getContext().getContentResolver().
                        query(MediaStore.Files.getContentUri("external"),
                                null,
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? or " +
                                        MediaStore.Files.FileColumns.DATA + " like ? ",
                                new String[]{"%lyric%", "%Lyric%", "%.lrc%"},
                                null);
                if (filesCursor == null || !(filesCursor.getCount() > 0))
                    return results;
                while (filesCursor.moveToNext()) {
                    File file = new File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.isFile() && file.canRead()) {
                        if (Util.isRightLrc(file, mDisplayName, mSong.getTitle(), mSong.getArtist())) {
                            results.add(file.getAbsolutePath());
                        }
                    }
                }
                return results;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (filesCursor != null && !filesCursor.isClosed())
                    filesCursor.close();
            }
        }

        return results;
    }

    /**
     * 获得搜索歌词的关键字
     * @param song
     * @return
     */
    private String getLyricSearchKey(Song song){
        if(song == null)
            return "";
        boolean isTitleAvailable = !TextUtils.isEmpty(song.getTitle()) && !song.getTitle().contains(App.getContext().getString(R.string.unknown_song));
        boolean isAlbumAvailable = !TextUtils.isEmpty(song.getAlbum()) && !song.getAlbum().contains(App.getContext().getString(R.string.unknown_album));
        boolean isArtistAvailable = !TextUtils.isEmpty(song.getArtist()) && !song.getArtist().contains(App.getContext().getString(R.string.unknown_artist));

        //歌曲名合法
        if(isTitleAvailable){
            //艺术家合法
            if(isArtistAvailable){
                return song.getTitle() + "-" + song.getArtist();
            } else if(isAlbumAvailable){
                //专辑名合法
                return song.getTitle() + "-" + song.getAlbum();
            } else {
                return song.getTitle();
            }
        }
        return "";
    }
}
