package remix.myplayer.lyric

import android.database.Cursor
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Observable
import io.reactivex.functions.Function
import remix.myplayer.App
import remix.myplayer.bean.kugou.KLrcResponse
import remix.myplayer.bean.kugou.KSearchResponse
import remix.myplayer.bean.misc.LyricPriority
import remix.myplayer.bean.mp3.Song
import remix.myplayer.bean.netease.NLrcResponse
import remix.myplayer.bean.netease.NSongSearchResponse
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.tageditor.TagEditor
import remix.myplayer.request.network.HttpClient
import remix.myplayer.request.network.RxUtil
import remix.myplayer.util.*
import java.io.*
import java.nio.charset.Charset
import java.util.*

/**
 * Created by Remix on 2015/12/7.
 */

/**
 * 根据歌曲名和歌手名 搜索歌词并解析成固定格式
 */
class SearchLrc(private val mSong: Song) {
    private val mLrcParser: ILrcParser
    private var mDisplayName: String? = null
    private var mCacheKey: String? = null
    private var mSearchKey: String? = null

    init {
        try {
            if (!TextUtils.isEmpty(mSong.displayname)) {
                val temp = mSong.displayname
                mDisplayName = if (temp.indexOf('.') > 0) temp.substring(0, temp.lastIndexOf('.')) else temp
            }
            mSearchKey = getLyricSearchKey(mSong)
        } catch (e: Exception) {
            LogUtil.d(TAG, e.toString())
            mDisplayName = mSong.title
        }

        mLrcParser = DefaultLrcParser()
    }

    /**
     * 根据歌词id,发送请求并解析歌词
     *
     * @return 歌词
     */
    fun getLyric(manualPath: String, clearCache: Boolean): Observable<List<LrcRow>> {
        val type = SPUtil.getValue(App.getContext(), SPUtil.LYRIC_KEY.NAME, mSong.id, SPUtil.LYRIC_KEY.LYRIC_DEFAULT)

        val observable = when (type) {
            SPUtil.LYRIC_KEY.LYRIC_IGNORE -> {
                LogUtil.d(TAG, "Ignore Lyric")
                Observable.error(Throwable("Ignore Lyric"))
            }
            SPUtil.LYRIC_KEY.LYRIC_EMBEDDED -> {
                getEmbeddedObservable()
            }
            SPUtil.LYRIC_KEY.LYRIC_LOCAL -> {
                getLocalObservable()
            }
            SPUtil.LYRIC_KEY.LYRIC_NETEASE -> {
                getNeteaseObservable()
            }
            SPUtil.LYRIC_KEY.LYRIC_KUGOU -> {
                getKuGouObservable()
            }
            SPUtil.LYRIC_KEY.LYRIC_MANUAL -> {
                getManualObservable(manualPath)
            }
            SPUtil.LYRIC_KEY.LYRIC_DEFAULT -> {
                //默认优先级排序 网易-酷狗-本地-内嵌
                val priority = Gson().fromJson<List<LyricPriority>>(SPUtil.getValue(App.getContext(), SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.PRIORITY_LYRIC, SPUtil.LYRIC_KEY.DEFAULT_PRIORITY),
                        object : TypeToken<List<LyricPriority>>() {}.type)

                val observables = mutableListOf<Observable<List<LrcRow>>>()
                priority.forEach {
                    when (it.priority) {
                        LyricPriority.NETEASE.priority -> observables.add(getNeteaseObservable())
                        LyricPriority.KUGOU.priority -> observables.add(getKuGouObservable())
                        LyricPriority.LOCAL.priority -> observables.add(getLocalObservable())
                        LyricPriority.EMBEDED.priority -> observables.add(getEmbeddedObservable())
                    }
                }
                Observable.concat(observables).firstOrError().toObservable()
            }
            else -> {
                Observable.error<List<LrcRow>>(Throwable("Not support type"))
            }
        }

        return if (isTypeAvailable(type)) Observable.concat(getCacheObservable(), observable)
                .firstOrError()
                .toObservable()
                .doOnSubscribe { disposable ->
                    LOCAL_LYRIC_PATH = ""
                    mCacheKey = Util.hashKeyForDisk(mSong.id.toString() + "-" +
                            (if (!TextUtils.isEmpty(mSong.artist)) mSong.artist else "") + "-" +
                            if (!TextUtils.isEmpty(mSong.title)) mSong.title else "")
                    LogUtil.d(TAG, "CacheKey: $mCacheKey SearchKey: $mSearchKey")
                    if (clearCache) {
                        LogUtil.d(TAG, "clearCache")
                        DiskCache.getLrcDiskCache().remove(mCacheKey)
                    }
                }.compose(RxUtil.applyScheduler())
        else
            observable
    }

    private fun isTypeAvailable(type: Int): Boolean {
        return type != SPUtil.LYRIC_KEY.LYRIC_IGNORE
    }

    /**
     * 根据歌词id,发送请求并解析歌词
     *
     * @return 歌词
     */
    fun getLyric(): Observable<List<LrcRow>> {
        return getLyric("", false)
    }

    /**
     * 内嵌歌词
     *
     * @return
     */
    private fun getEmbeddedObservable(): Observable<List<LrcRow>> {
        return Observable.create { e ->
            val lyric = TagEditor(mSong.url).lyric
            if (!lyric.isNullOrEmpty()) {
                e.onNext(mLrcParser.getLrcRows(getBufferReader(lyric!!.toByteArray(UTF_8)), true, mCacheKey, mSearchKey))
                LogUtil.d(TAG, "EmbeddedLyric")
            }
            e.onComplete()
        }
    }

    /**
     * 缓存
     */
    private fun getCacheObservable(): Observable<List<LrcRow>> {
        return Observable.create { e ->
            DiskCache.getLrcDiskCache().get(mCacheKey)?.run {
                BufferedReader(InputStreamReader(getInputStream(0))).use {
                    it.readLine().run {
                        e.onNext(Gson().fromJson(this, object : TypeToken<List<LrcRow>>() {}.type))
                        LogUtil.d(TAG, "CacheLyric")
                    }
                }
            }
            e.onComplete()
        }
    }


    private val isCN: Boolean
        get() = "zh".equals(Locale.getDefault().language, ignoreCase = true)

    /**
     * 搜索本地所有歌词文件
     * 查找本地目录
     * 已设置歌词路径
     * 没有设置歌词路径 搜索所有歌词文件
     *
     * @return
     */
    private val localLrcPath: String
        get() {
            val searchPath = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "")
            if (!TextUtils.isEmpty(searchPath)) {
                LyricUtil.searchFile(mDisplayName, mSong.title, mSong.artist, File(searchPath))
                if (!TextUtils.isEmpty(SearchLrc.LOCAL_LYRIC_PATH)) {
                    return SearchLrc.LOCAL_LYRIC_PATH
                }
            } else {
                var filesCursor: Cursor? = null
                try {
                    filesCursor = App.getContext().contentResolver.query(MediaStore.Files.getContentUri("external"), null,
                            MediaStore.Files.FileColumns.DATA + " like ? or " +
                                    MediaStore.Files.FileColumns.DATA + " like ? or " +
                                    MediaStore.Files.FileColumns.DATA + " like ? ",
                            arrayOf("%lyric%", "%Lyric%", "%.lrc%"), null)
                    if (filesCursor == null || filesCursor.count <= 0)
                        return ""
                    while (filesCursor.moveToNext()) {
                        val file = File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
                        if (file.exists() && file.isFile && file.canRead()) {
                            if (LyricUtil.isRightLrc(file, mDisplayName, mSong.title, mSong.artist)) {
                                return file.absolutePath
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    if (filesCursor != null && !filesCursor.isClosed)
                        filesCursor.close()
                }
            }
            return ""
        }

    /**
     * 搜索本地所有歌词文件
     */
    private val allLocalLrcPath: List<String>
        get() {
            val results = ArrayList<String>()
            val searchPath = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "")
            if (!TextUtils.isEmpty(searchPath)) {//已设置歌词搜索路径
                LyricUtil.searchFile(mDisplayName, mSong.title, mSong.artist, File(searchPath))
                if (!TextUtils.isEmpty(LOCAL_LYRIC_PATH))
                    results.add(SearchLrc.LOCAL_LYRIC_PATH)
                return results
            } else {//没有设置歌词路径 搜索所有歌词文件
                App.getContext().contentResolver.query(MediaStore.Files.getContentUri("external"), null,
                        MediaStore.Files.FileColumns.DATA + " like ? or " +
                                MediaStore.Files.FileColumns.DATA + " like ? or " +
                                MediaStore.Files.FileColumns.DATA + " like ? ",
                        arrayOf("%lyric%", "%Lyric%", "%.lrc"), null)
                        .use { filesCursor ->
                            while (filesCursor.moveToNext()) {
                                val file = File(filesCursor.getString(filesCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)))
                                if (file.exists() && file.isFile && file.canRead()) {
                                    if (LyricUtil.isRightLrc(file, mDisplayName, mSong.title, mSong.artist)) {
                                        results.add(file.absolutePath)
                                    }
                                }
                            }
                            return results
                        }
            }

        }

    /**
     * 网络或者本地歌词
     *
     * @param type
     * @return
     */
    @Deprecated("")
    private fun getContentObservable(type: Int): Observable<List<LrcRow>> {
        val networkObservable = getNetworkObservable(type)
        val localObservable = getLocalObservable()
        val onlineFirst = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ONLINE_LYRIC_FIRST, false)
        return Observable.concat(if (onlineFirst) networkObservable else localObservable, if (onlineFirst) localObservable else networkObservable).firstOrError().toObservable()
    }

    /**
     * 手动设置歌词
     */
    private fun getManualObservable(manualPath: String): Observable<List<LrcRow>> {
        return Observable.create { e ->
            //手动设置的歌词
            if (!TextUtils.isEmpty(manualPath)) {
                LogUtil.d(TAG, "ManualLyric")
                e.onNext(mLrcParser.getLrcRows(getBufferReader(manualPath), true, mCacheKey, mSearchKey))
            }
            e.onComplete()
        }
    }


    /**
     * 本地歌词
     *
     * @param type
     * @return
     */
    private fun getLocalObservable(): Observable<List<LrcRow>> {
        return Observable.create { e ->
            val localPaths = ArrayList<String>(allLocalLrcPath)
            if (localPaths.isNotEmpty()) {
                if (localPaths.size > 1 && isCN) {
                    localPaths.sortWith(Comparator { o1, o2 -> o2.compareTo(o1) })
                    val localPath = localPaths[0]
                    var translatePath: String? = null
                    for (path in localPaths) {
                        if (path.contains("translate") && path != localPath) {
                            translatePath = path
                            break
                        }
                    }
                    LogUtil.d(TAG, "LocalLyric")
                    if (translatePath == null) {
                        e.onNext(mLrcParser.getLrcRows(getBufferReader(localPath), true, mCacheKey, mSearchKey))
                    } else {
                        //合并歌词
                        val source = mLrcParser.getLrcRows(getBufferReader(localPath), false, mCacheKey, mSearchKey)
                        val translate = mLrcParser.getLrcRows(getBufferReader(translatePath), false, mCacheKey, mSearchKey)
                        if (translate != null && translate.size > 0) {
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
                            for (i in translate.indices) {
                                for (j in source.indices) {
                                    if (isTranslateCanUse(translate[i].content) && translate[i].time == source[j].time) {
                                        source[j].translate = translate[i].content
                                        break
                                    }
                                }
                            }
                            mLrcParser.saveLrcRows(source, mCacheKey, mSearchKey)
                            e.onNext(source)
                        }
                    }
                } else {
                    val localPath = localPaths[0]
                    e.onNext(mLrcParser.getLrcRows(getBufferReader(localPath), true, mCacheKey, mSearchKey))
                }
            }
            e.onComplete()
        }
    }

    /**
     * 网易歌词
     */
    private fun getNeteaseObservable(): Observable<List<LrcRow>> {
        return HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mSearchKey, 0, 1, 1)
                .flatMap { it ->
                    HttpClient.getInstance()
                            .getNeteaseLyric(Gson().fromJson(it.string(), NSongSearchResponse::class.java).result.songs.get(0).id)
                            .map {
                                val lrcResponse = Gson().fromJson(it.string(), NLrcResponse::class.java)
                                val combine = mLrcParser.getLrcRows(getBufferReader(lrcResponse.lrc.lyric.toByteArray()), false, mCacheKey, mSearchKey)
                                if (isCN && lrcResponse.tlyric != null && !TextUtils.isEmpty(lrcResponse.tlyric.lyric)) {
                                    val translate = mLrcParser.getLrcRows(getBufferReader(lrcResponse.tlyric.lyric.toByteArray()), false, mCacheKey, mSearchKey)
                                    if (translate != null && translate.size > 0) {
                                        for (i in translate.indices) {
                                            for (j in combine.indices) {
                                                if (translate[i].time == combine[j].time) {
                                                    combine[j].translate = translate[i].content
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                                LogUtil.d(TAG, "NeteaseLyric")
                                mLrcParser.saveLrcRows(combine, mCacheKey, mSearchKey)
                                combine
                            }
                }.onErrorResumeNext(Function {
                    Observable.empty()
                })
    }

    /**
     * 酷狗歌词
     */
    private fun getKuGouObservable(): Observable<List<LrcRow>> {
        //酷狗歌词
        return HttpClient.getKuGouApiservice().getKuGouSearch(1, "yes", "pc", mSearchKey, mSong.duration, "")
                .flatMap { body ->
                    val searchResponse = Gson().fromJson(body.string(), KSearchResponse::class.java)
                    HttpClient.getKuGouApiservice().getKuGouLyric(1, "pc", "lrc", "utf8",
                            searchResponse.candidates[0].id,
                            searchResponse.candidates[0].accesskey)
                            .map { lrcBody ->
                                val lrcResponse = Gson().fromJson(lrcBody.string(), KLrcResponse::class.java)
                                LogUtil.d(TAG, "KugouLyric")
                                mLrcParser.getLrcRows(getBufferReader(Base64.decode(lrcResponse.content, Base64.DEFAULT)), true, mCacheKey, mSearchKey)
                            }
                }
    }

    /**
     * 在线歌词
     *
     * @param type
     * @return
     */
    private fun getNetworkObservable(type: Int): Observable<List<LrcRow>> {
        var newType = type

        if (TextUtils.isEmpty(mSearchKey)) {
            return Observable.error(Throwable("no available key"))
        }
        if (newType == SPUtil.LYRIC_KEY.LYRIC_DEFAULT)
            newType = SPUtil.LYRIC_KEY.LYRIC_NETEASE
        if (newType == SPUtil.LYRIC_KEY.LYRIC_KUGOU) {
            //酷狗歌词
            return getKuGouObservable()
        } else {
            //网易歌词
            return getNeteaseObservable()
        }
    }

    private fun isTranslateCanUse(translate: String): Boolean {
        return !TextUtils.isEmpty(translate) && !translate.startsWith("词") && !translate.startsWith("曲")
    }

    /**
     * 获得搜索歌词的关键字
     *
     * @param song
     * @return
     */
    private fun getLyricSearchKey(song: Song?): String {
        if (song == null)
            return ""
        val isTitleAvailable = !ImageUriUtil.isSongNameUnknownOrEmpty(song.title)
        val isAlbumAvailable = !ImageUriUtil.isAlbumNameUnknownOrEmpty(song.album)
        val isArtistAvailable = !ImageUriUtil.isArtistNameUnknownOrEmpty(song.artist)

        //歌曲名合法
        return if (isTitleAvailable) {
            when {
                isArtistAvailable -> song.artist + "-" + song.title //艺术家合法
                isAlbumAvailable -> //专辑名合法
                    song.album + "-" + song.title
                else -> song.title
            }
        } else ""
    }

    @Throws(FileNotFoundException::class, UnsupportedEncodingException::class)
    private fun getBufferReader(path: String): BufferedReader {
        return BufferedReader(InputStreamReader(FileInputStream(path), LyricUtil.getCharset(path)))
    }

    private fun getBufferReader(bytes: ByteArray): BufferedReader {
        return BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), UTF_8))
    }

    companion object {
        private const val TAG = "SearchLrc"

        @JvmStatic
        lateinit var LOCAL_LYRIC_PATH: String

        private val UTF_8 = Charset.forName("UTF-8")
    }
}
