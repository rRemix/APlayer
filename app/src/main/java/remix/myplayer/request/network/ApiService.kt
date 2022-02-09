package remix.myplayer.request.network

import io.reactivex.Single
import remix.myplayer.BuildConfig
import remix.myplayer.bean.github.Release
import remix.myplayer.bean.kugou.KLrcResponse
import remix.myplayer.bean.kugou.KSearchResponse
import remix.myplayer.bean.lastfm.LastFmAlbum
import remix.myplayer.bean.lastfm.LastFmArtist
import remix.myplayer.bean.netease.NAlbumSearchResponse
import remix.myplayer.bean.netease.NArtistSearchResponse
import remix.myplayer.bean.netease.NLrcResponse
import remix.myplayer.bean.netease.NSongSearchResponse
import remix.myplayer.bean.netease.NDetailResponse
import remix.myplayer.bean.qq.QLrcResponse
import remix.myplayer.bean.qq.QSearchResponse
import retrofit2.http.*

/**
 * Created by Remix on 2017/11/20.
 */
interface ApiService {
  @GET("repos/{owner}/{repo}/releases/latest")
  @Headers("token: " + BuildConfig.GITHUB_SECRET_KEY)
  fun fetchLatestRelease(@Path("owner") owner: String?, @Path("repo") repo: String?): Single<Release>

  //New Api
  @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
  fun searchLastFMAlbum(@Query("album") albumName: String?,
                        @Query("artist") artistName: String?, @Query("lang") language: String?): Single<LastFmAlbum>

  @GET("$BASE_QUERY_PARAMETERS&method=artist.getinfo")
  fun searchLastFMArtist(@Query("artist") artistName: String?,
                         @Query("lang") language: String?): Single<LastFmArtist>

  @GET("search/get")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseSong(@Query("s") key: String?, @Query("offset") offset: Int,
                        @Query("limit") limit: Int, @Query("type") type: Int): Single<NSongSearchResponse>

  @GET("search/get")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseAlbum(@Query("s") key: String?, @Query("offset") offset: Int,
                         @Query("limit") limit: Int, @Query("type") type: Int): Single<NAlbumSearchResponse>

  @GET("search/get")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseArtist(@Query("s") key: String?, @Query("offset") offset: Int,
                          @Query("limit") limit: Int, @Query("type") type: Int): Single<NArtistSearchResponse>

  @GET("song/lyric")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseLyric(@Query("os") os: String?, @Query("id") id: Int,
                         @Query("lv") lv: Int, @Query("kv") kv: Int, @Query("tv") tv: Int): Single<NLrcResponse>

  @GET("song/detail")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  fun searchNeteaseDetail( @Query("id") id: Int, @Query("ids") ids: String?): Single<NDetailResponse>

  @GET("search")
  fun searchKuGou(@Query("ver") ver: Int, @Query("man") man: String?,
                  @Query("client") client: String?,
                  @Query("keyword") keyword: String?, @Query("duration") duration: Long,
                  @Query("hash") hash: String?): Single<KSearchResponse>

  @GET("download")
  fun searchKuGouLyric(@Query("ver") ver: Int, @Query("client") client: String?,
                       @Query("fmt") fmt: String?, @Query("charset") charSet: String?,
                       @Query("id") id: Int, @Query("accesskey") accessKey: String?): Single<KLrcResponse>

  @GET("soso/fcgi-bin/client_search_cp")
  fun searchQQ(@Query("n") n: Int,
               @Query("w") w: String?,
               @Query("format") format: String?): Single<QSearchResponse>

  @GET("lyric/fcgi-bin/fcg_query_lyric_new.fcg")
  @Headers("Referer: https://y.qq.com/portal/player.html")
  fun searchQQLyric(@Query("songmid") songmid: String?,
                    @Query("g_tk") g_tk: Int,
                    @Query("format") format: String?,
                    @Query("nobase64") nobase64: Int): Single<QLrcResponse>

  companion object {
    const val BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=" + BuildConfig.LASTFM_API_KEY
  }
}
