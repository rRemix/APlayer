package remix.myplayer.lyric;

import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.APlayerApplication;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NLrcResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.cache.DiskLruCache;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2018/1/3.
 */

public class NewSearchLrc {
    private static final String TAG = "NewSearchLrc";
    private ILrcParser mLrcParser;
    private Song mSong;
    private String mDisplayName;
    private String mKey;

    public NewSearchLrc(Song song){
        mSong = song;
        mKey = mSong.getTitle() + "/" + mSong.getArtist();
        try {
            if(!TextUtils.isEmpty(mSong.getDisplayname())){
                String temp = mSong.getDisplayname();
                mDisplayName = temp.indexOf('.') > 0 ? temp.substring(0,temp.lastIndexOf('.')) : temp;
            }
        } catch (Exception e){
            Util.uploadException("SearchLrc Init Error","DisPlayName:" + mSong.getDisplayname() + " Title:" + mSong.getTitle());
            mDisplayName = mSong.getTitle();
        }
        mLrcParser = new DefaultLrcParser();
    }

    public void getLrc(String manualPath,Observer<List<LrcRow>> observer){
        //网易歌词
        Observable<List<LrcRow>> neteaseObservable = HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mSong.getArtist() + "-" + mSong.getTitle(),0,1,1)
                .flatMap(body -> HttpClient.getInstance()
                        .getNeteaseLyric(new Gson().fromJson(body.string(),NSongSearchResponse.class).result.songs.get(0).id)
                        .map(body1 -> {
                            final NLrcResponse lrcResponse = new Gson().fromJson(body1.string(),NLrcResponse.class);
                            final Charset utf8 = Charset.forName("utf8");
                            List<LrcRow> combine = mLrcParser.getLrcRows(new BufferedReader(
                                    new InputStreamReader(
                                            new ByteArrayInputStream(lrcResponse.lrc.lyric.getBytes(utf8)),utf8)),false,mSong.getTitle(),mSong.getArtist() + "/original");
                            //有翻译 合并
//                            if(lrcResponse.tlyric != null && !TextUtils.isEmpty(lrcResponse.tlyric.lyric)){
//                                List<LrcRow> translate = lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(lrcResponse.tlyric.lyric.getBytes(utf8)),utf8)),false,mInfo.getTitle(),mInfo.getArtist() + "/translate");
//                                if(translate != null && translate.size() > 0){
//                                    for(int i = 0 ; i < translate.size();i++){
//                                        for(int j = 0 ; j < combine.size();j++){
//                                            if(translate.get(i).getTime() == combine.get(j).getTime()){
//                                                combine.get(j).setTranslate(translate.get(i).getContent());
//                                                break;
//                                            }
//                                        }
//                                    }
//
//                                }
//                            }
                            mLrcParser.saveLrcRows(combine,mKey);
                            return combine;
                        }))
                .onErrorResumeNext(throwable -> {
                    return Observable.empty();
                });
        //本地歌词
        Observable<List<LrcRow>> localObservable = Observable.create(e -> {
            String localPath = getlocalLrcPath();
            if(!TextUtils.isEmpty(localPath)){
                e.onNext(mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(localPath))),true, mSong.getTitle(),mSong.getArtist()));
            }
            e.onComplete();
        });
        //根据在线和本地的优先级 确定最后一级
        boolean onlineFirst = SPUtil.getValue(APlayerApplication.getContext(),"Setting","OnlineLrc",false);
        Observable<List<LrcRow>> last = Observable.concat(onlineFirst ? neteaseObservable : localObservable ,onlineFirst ? localObservable : neteaseObservable).firstOrError().toObservable();

        Observable.create((ObservableOnSubscribe<List<LrcRow>>) e -> {
            //手动设置的歌词
            if(!TextUtils.isEmpty(manualPath)){
                e.onNext(mLrcParser.getLrcRows(new BufferedReader(new InputStreamReader(new FileInputStream(manualPath))),true, mSong.getTitle(),mSong.getArtist()));
            }
            e.onComplete();
        }).switchIfEmpty(Observable.create(e -> {
            //缓存
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(Util.hashKeyForDisk(mKey));
            if(snapShot != null){
                BufferedReader br = new BufferedReader(new BufferedReader(new InputStreamReader(snapShot.getInputStream(0))));
                String buffer = br.readLine();
                e.onNext(new Gson().fromJson(buffer,new TypeToken<List<LrcRow>>(){}.getType()));
//                e.onNext(lrcParser.getLrcRows(new BufferedReader(new InputStreamReader(snapShot.getInputStream(0))),false, mInfo.getTitle(),mInfo.getArtist()));
            }
            e.onComplete();
        })).switchIfEmpty(last)
        .compose(RxUtil.applyScheduler())
        .subscribe(observer);
    }

    /**
     * 搜索本地所有歌词文件
     * @return
     */
    private String getlocalLrcPath() {
        //查找本地目录
        String searchPath =  SPUtil.getValue(APlayerApplication.getContext(),"Setting","LrcSearchPath","");
        if(mSong == null)
            return "";
        if(!TextUtils.isEmpty(searchPath)){
            //已设置歌词路径
            Util.searchFile(mSong.getDisplayname(),mSong.getTitle(),mSong.getArtist(), new File(searchPath));
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
                        if(Util.isRightLrc(file, mSong.getDisplayname(),mSong.getTitle(),mSong.getArtist())){
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
