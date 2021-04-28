package remix.myplayer.request.network

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.ResponseBody
import remix.myplayer.BuildConfig
import remix.myplayer.bean.github.Release
import remix.myplayer.bean.lastfm.LastFmAlbum
import remix.myplayer.bean.netease.NSongSearchResponse
import retrofit2.http.*

/**
 * Created by Remix on 2017/11/20.
 */
interface ApiService {
  @POST("search/pc")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun getNeteaseSearch(@Query("s") key: String?, @Query("offset") offset: Int,
                       @Query("limit") limit: Int, @Query("type") type: Int): Observable<ResponseBody>

  @GET("song/lyric")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun getNeteaseLyric(@Query("os") os: String?, @Query("id") id: Int,
                      @Query("lv") lv: Int, @Query("kv") kv: Int, @Query("tv") tv: Int): Observable<ResponseBody>

  @GET("search")
  fun getKuGouSearch(@Query("ver") ver: Int, @Query("man") man: String?,
                     @Query("client") client: String?,
                     @Query("keyword") keyword: String?, @Query("duration") duration: Long,
                     @Query("hash") hash: String?): Observable<ResponseBody>

  @GET("download")
  fun getKuGouLyric(@Query("ver") ver: Int, @Query("client") client: String?,
                    @Query("fmt") fmt: String?, @Query("charset") charSet: String?,
                    @Query("id") id: Int, @Query("accesskey") accessKey: String?): Observable<ResponseBody>

  @GET("soso/fcgi-bin/client_search_cp")
  fun getQQSearch(@Query("n") n: Int,
                  @Query("w") w: String?,
                  @Query("format") format: String?): Observable<ResponseBody>

  @GET("lyric/fcgi-bin/fcg_query_lyric_new.fcg")
  @Headers("Referer: https://y.qq.com/portal/player.html")
  fun getQQLyric(@Query("songmid") songmid: String?,
                 @Query("g_tk") g_tk: Int,
                 @Query("format") format: String?,
                 @Query("nobase64") nobase64: Int): Observable<ResponseBody>

  @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
  fun getAlbumInfo(@Query("album") albumName: String?,
                   @Query("artist") artistName: String?, @Query("lang") language: String?): Observable<ResponseBody>

  @GET("$BASE_QUERY_PARAMETERS&method=artist.getinfo")
  fun getArtistInfo(@Query("artist") artistName: String?,
                    @Query("lang") language: String?): Observable<ResponseBody>

  @GET("repos/{owner}/{repo}/releases/latest")
  @Headers("token: " + BuildConfig.GITHUB_SECRET_KEY)
  fun getLatestRelease(@Path("owner") owner: String?, @Path("repo") repo: String?): Single<Release>

  //TODO New Api
  @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
  fun searchLastFMAlbum(@Query("album") albumName: String?,
                        @Query("artist") artistName: String?, @Query("lang") language: String?): Single<LastFmAlbum>

  @POST("search/pc")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseSong(@Query("s") key: String?, @Query("offset") offset: Int,
                        @Query("limit") limit: Int, @Query("type") type: Int): Single<NSongSearchResponse>


  companion object {
    const val BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=" + BuildConfig.LASTFM_API_KEY
  }
}