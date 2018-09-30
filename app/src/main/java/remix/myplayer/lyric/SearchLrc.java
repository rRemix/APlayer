package remix.myplayer.lyric;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import remix.myplayer.App;
import remix.myplayer.bean.kugou.KLrcResponse;
import remix.myplayer.bean.kugou.KSearchResponse;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NLrcResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.cache.DiskLruCache;
import remix.myplayer.misc.tageditor.TagEditor;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.LyricUtil;
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

    public static String LOCAL_LYRIC_PATH;

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private ILrcParser mLrcParser;
    private Song mSong;
    private String mDisplayName;
    private String mCacheKey;
    private String mSearchKey;

    public SearchLrc(Song item) {
        mSong = item;
        try {
            if (!TextUtils.isEmpty(mSong.getDisplayname())) {
                String temp = mSong.getDisplayname();
                mDisplayName = temp.indexOf('.') > 0 ? temp.substring(0, temp.lastIndexOf('.')) : temp;
            }
            mSearchKey = getLyricSearchKey(mSong);
        } catch (Exception e) {
            LogUtil.d(TAG, e.toString());
            mDisplayName = mSong.getTitle();
        }
        mLrcParser = new DefaultLrcParser();
    }

    /**
     * 根据歌词id,发送请求并解析歌词
     *
     * @return 歌词
     */
    @SuppressLint("CheckResult")
    public Observable<List<LrcRow>> getLyric(String manualPath, boolean clearCache) {
        int type = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_KEY.NAME, mSong.getId() + "", SPUtil.LYRIC_KEY.LYRIC_DEFAULT);

        return type == SPUtil.LYRIC_KEY.LYRIC_IGNORE ? Observable.error(new Throwable("Ignore")) :
                Observable.concat(getCacheObservable(), getManualObservable(manualPath), getContentObservable(type), getEmbeddedObservable()).firstOrError().toObservable()
                        .doOnSubscribe(disposable -> {
                            LOCAL_LYRIC_PATH = "";
                            mCacheKey = Util.hashKeyForDisk(String.valueOf(mSong.getId()) + "-" +
                                    (!TextUtils.isEmpty(mSong.getArtist()) ? mSong.getArtist() : "") + "-" +
                                    (!TextUtils.isEmpty(mSong.getTitle()) ? mSong.getTitle() : ""));
                            if (clearCache) {
                                DiskCache.getLrcDiskCache().remove(mCacheKey);
                            }
                        })
                        .compose(RxUtil.applyScheduler());

    }


    /**
     * 根据歌词id,发送请求并解析歌词
     *
     * @return 歌词
     */
    public Observable<List<LrcRow>> getLyric() {
        return getLyric("", false);
    }

    /**
     * 内嵌歌词
     *
     * @return
     */
    private Observable<List<LrcRow>> getEmbeddedObservable() {
        return Observable.create(e -> {
            TagEditor editor = new TagEditor(mSong.getUrl());
            final String lyric = editor.getLyric();
            if (!TextUtils.isEmpty(lyric)) {
                e.onNext(mLrcParser.getLrcRows(getBufferReader(lyric.getBytes(UTF_8)), true, mCacheKey, mSearchKey));
            }
            e.onComplete();
        });
    }

    /**
     * 网络或者本地歌词
     *
     * @param type
     * @return
     */
    public Observable<List<LrcRow>> getContentObservable(int type) {
        Observable<List<LrcRow>> networkObservable = getNetworkObservable(type);
        Observable<List<LrcRow>> localObservable = getLocalObservable(type);
        boolean onlineFirst = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ONLINE_LYRIC_FIRST, false);
        return Observable.concat(onlineFirst ? networkObservable : localObservable, onlineFirst ? localObservable : networkObservable).firstOrError().toObservable();
    }

    /**
     * 手动设置歌词
     */
    private Observable<List<LrcRow>> getManualObservable(final String manualPath) {
        return Observable.create(e -> {
            //手动设置的歌词
            if (!TextUtils.isEmpty(manualPath)) {
                e.onNext(mLrcParser.getLrcRows(getBufferReader(manualPath), true, mCacheKey, mSearchKey));
            }
            e.onComplete();
        });
    }

    /**
     * 缓存
     */
    private Observable<List<LrcRow>> getCacheObservable() {
        return Observable.create(e -> {
            //缓存
            DiskLruCache.Snapshot snapShot = DiskCache.getLrcDiskCache().get(mCacheKey);
            if (snapShot != null) {
                BufferedReader br = new BufferedReader(new BufferedReader(new InputStreamReader(snapShot.getInputStream(0))));
                e.onNext(new Gson().fromJson(br.readLine(), new TypeToken<List<LrcRow>>() {
                }.getType()));
                snapShot.close();
                br.close();
            }
            e.onComplete();
        });
    }


    /**
     * 本地歌词
     *
     * @param type
     * @return
     */
    private Observable<List<LrcRow>> getLocalObservable(int type) {
        return type == SPUtil.LYRIC_KEY.LYRIC_NETEASE || type == SPUtil.LYRIC_KEY.LYRIC_KUGOU ?
                Observable.empty() :
                Observable.create(e -> {
                    List<String> localPaths = getAllLocalLrcPath();
                    if (localPaths.size() > 0) {
                        if (localPaths.size() > 1 && isCN()) {
                            Collections.sort(localPaths, (o1, o2) -> o2.compareTo(o1));
                            String localPath = localPaths.get(0);
                            String translatePath = null;
                            for (String path : localPaths) {
                                if (path.contains("translate") && !path.equals(localPath)) {
                                    translatePath = path;
                                    break;
                                }
                            }
                            if (translatePath == null) {
                                e.onNext(mLrcParser.getLrcRows(getBufferReader(localPath), true, mCacheKey, mSearchKey));
                            } else {
                                //合并歌词
                                List<LrcRow> source = mLrcParser.getLrcRows(getBufferReader(localPath), false, mCacheKey, mSearchKey);
                                List<LrcRow> translate = mLrcParser.getLrcRows(getBufferReader(translatePath), false, mCacheKey, mSearchKey);
                                if (translate != null && translate.size() > 0) {
//                                    int j = 0;
//                                    for (int i = 0; i < source.size(); ) {
//                                        boolean match = Math.abs(translate.get(j).getTime() - source.get(i).getTime()) < 1000;
//                                        if (match) {
//                                            source.get(i).setTranslate(translate.get(j).getContent());
//                                            i++;
//                                        } else if(translate.get(j).getTime() > source.get(i).getTime()){
//                                            i++;
//                                        } else {
//                                            j++;
//                                        }
//                                    }
                                    for (int i = 0; i < translate.size(); i++) {
                                        for (int j = 0; j < source.size(); j++) {
                                            if (isTranslateCanUse(translate.get(i).getContent()) &&
                                                    translate.get(i).getTime() == source.get(j).getTime()) {
                                                source.get(j).setTranslate(translate.get(i).getContent());
                                                break;
                                            }
                                        }
                                    }
                                    mLrcParser.saveLrcRows(source, mCacheKey, mSearchKey);
                                    e.onNext(source);
                                }
                            }
                        } else {
                            String localPath = localPaths.get(0);
                            e.onNext(mLrcParser.getLrcRows(getBufferReader(localPath), true, mCacheKey, mSearchKey));
                        }
                    }
                    e.onComplete();
                });
    }


    /**
     * 在线歌词
     *
     * @param type
     * @return
     */
    private Observable<List<LrcRow>> getNetworkObservable(int type) {
        if (mSong == null) {
            return Observable.error(new Throwable("no available song"));
        }
        if (TextUtils.isEmpty(mSearchKey)) {
            return Observable.error(new Throwable("no available key"));
        }
        if (type == SPUtil.LYRIC_KEY.LYRIC_DEFAULT)
            type = SPUtil.LYRIC_KEY.LYRIC_NETEASE;
        if (type == SPUtil.LYRIC_KEY.LYRIC_KUGOU) {
            //酷狗歌词
            return HttpClient.getKuGouApiservice().getKuGouSearch(1, "yes", "pc", mSearchKey, mSong.getDuration(), "")
                    .flatMap(body -> {
                        final KSearchResponse searchResponse = new Gson().fromJson(body.string(), KSearchResponse.class);
                        return HttpClient.getKuGouApiservice().getKuGouLyric(1, "pc", "lrc", "utf8", searchResponse.candidates.get(0).id,
                                searchResponse.candidates.get(0).accesskey)
                                .map(lrcBody -> {
                                    final KLrcResponse lrcResponse = new Gson().fromJson(lrcBody.string(), KLrcResponse.class);
                                    return mLrcParser.getLrcRows(getBufferReader(Base64.decode(lrcResponse.content, Base64.DEFAULT)), true, mCacheKey, mSearchKey);
                                });
                    });
        } else {
            //网易歌词
            return HttpClient.getNeteaseApiservice()
                    .getNeteaseSearch(mSearchKey, 0, 1, 1)
                    .flatMap(body -> HttpClient.getInstance()
                            .getNeteaseLyric(new Gson().fromJson(body.string(), NSongSearchResponse.class).result.songs.get(0).id)
                            .map(body1 -> {
                                final String bodyStr = body1.string();
                                final NLrcResponse lrcResponse = new Gson().fromJson(bodyStr, NLrcResponse.class);
                                List<LrcRow> combine = mLrcParser.getLrcRows(getBufferReader(lrcResponse.lrc.lyric.getBytes(UTF_8)), false, mCacheKey, mSearchKey);
                                //有翻译 合并
                                if (isCN() && lrcResponse.tlyric != null && !TextUtils.isEmpty(lrcResponse.tlyric.lyric)) {
                                    List<LrcRow> translate = mLrcParser.getLrcRows(getBufferReader(lrcResponse.tlyric.lyric.getBytes(UTF_8)), false, mCacheKey, mSearchKey);
                                    if (translate != null && translate.size() > 0) {
                                        for (int i = 0; i < translate.size(); i++) {
                                            for (int j = 0; j < combine.size(); j++) {
                                                if (translate.get(i).getTime() == combine.get(j).getTime()) {
                                                    combine.get(j).setTranslate(translate.get(i).getContent());
                                                    break;
                                                }
                                            }
                                        }

                                    }
                                }
                                mLrcParser.saveLrcRows(combine, mCacheKey, mSearchKey);
                                return combine;
                            })).onErrorResumeNext(throwable -> {
                        return Observable.empty();
                    });
        }
    }

    private boolean isCN() {
        return "zh".equalsIgnoreCase(Locale.getDefault().getLanguage());
    }

    private boolean isTranslateCanUse(String translate) {
        return !TextUtils.isEmpty(translate) && !translate.startsWith("词") && !translate.startsWith("曲");
    }

    /**
     * 搜索本地所有歌词文件
     *
     * @return
     */
    private String getLocalLrcPath() {
        //查找本地目录
        String searchPath = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "");
        if (mSong == null)
            return "";
        if (!TextUtils.isEmpty(searchPath)) {
            //已设置歌词路径
            LyricUtil.searchFile(mDisplayName, mSong.getTitle(), mSong.getArtist(), new File(searchPath));
            if (!TextUtils.isEmpty(SearchLrc.LOCAL_LYRIC_PATH)) {
                return SearchLrc.LOCAL_LYRIC_PATH;
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
                    return "";
                while (filesCursor.moveToNext()) {
                    File file = new File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.isFile() && file.canRead()) {
                        if (LyricUtil.isRightLrc(file, mDisplayName, mSong.getTitle(), mSong.getArtist())) {
                            return file.getAbsolutePath();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (filesCursor != null && !filesCursor.isClosed())
                    filesCursor.close();
            }
        }
        return "";
    }

    private List<String> getAllLocalLrcPath() {
        List<String> results = new ArrayList<>();
        //查找本地目录
        String searchPath = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "");
        if (mSong == null)
            return results;
        if (!TextUtils.isEmpty(searchPath)) {
            //已设置歌词搜索路径
            LyricUtil.searchFile(mDisplayName, mSong.getTitle(), mSong.getArtist(), new File(searchPath));
            if (!TextUtils.isEmpty(LOCAL_LYRIC_PATH))
                results.add(SearchLrc.LOCAL_LYRIC_PATH);
            return results;
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
                                new String[]{"%lyric%", "%Lyric%", "%.lrc"},
                                null);
                if (filesCursor == null || !(filesCursor.getCount() > 0))
                    return results;
                while (filesCursor.moveToNext()) {
                    File file = new File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)));
                    if (file.exists() && file.isFile() && file.canRead()) {
                        if (LyricUtil.isRightLrc(file, mDisplayName, mSong.getTitle(), mSong.getArtist())) {
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
     *
     * @param song
     * @return
     */
    private String getLyricSearchKey(Song song) {
        if (song == null)
            return "";
        boolean isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(song.getTitle());
        boolean isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(song.getAlbum());
        boolean isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(song.getArtist());

        //歌曲名合法
        if (isTitleAvailable) {
            //艺术家合法
            if (isArtistAvailable) {
                return song.getArtist() + "-" + song.getTitle();
            } else if (isAlbumAvailable) {
                //专辑名合法
                return song.getAlbum() + "-" + song.getTitle();
            } else {
                return song.getTitle();
            }
        }
        return "";
    }

    private BufferedReader getBufferReader(String path) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(path), LyricUtil.getCharset(path)));
    }

    private BufferedReader getBufferReader(byte[] bytes) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes), UTF_8));
    }
}
