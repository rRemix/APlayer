package remix.myplayer.request.network

import remix.myplayer.bean.kugou.KLrcResponse
import remix.myplayer.bean.kugou.KSearchResponse
import remix.myplayer.bean.netease.NLrcResponse
import remix.myplayer.bean.netease.NSongSearchResponse
import remix.myplayer.bean.qq.QLrcResponse
import remix.myplayer.bean.qq.QSearchResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface KuGouApi {

  @GET("search")
  suspend fun searchSong(
    @Query("ver") ver: Int, @Query("man") man: String?,
    @Query("client") client: String?,
    @Query("keyword") keyword: String?, @Query("duration") duration: Long,
    @Query("hash") hash: String?
  ): KSearchResponse

  @GET("download")
  suspend fun searchLyric(
    @Query("ver") ver: Int, @Query("client") client: String?,
    @Query("fmt") fmt: String?, @Query("charset") charSet: String?,
    @Query("id") id: Int, @Query("accesskey") accessKey: String?
  ): KLrcResponse

  companion object {

    const val BASE_URL = "http://lyrics.kugou.com/"
  }
}

interface QQApi {

  @GET("soso/fcgi-bin/client_search_cp")
  suspend fun searchSong(
    @Query("n") n: Int,
    @Query("w") w: String?,
    @Query("format") format: String?
  ): QSearchResponse

  @GET("lyric/fcgi-bin/fcg_query_lyric_new.fcg")
  @Headers("Referer: https://y.qq.com/portal/player.html")
  suspend fun searchLyric(
    @Query("songmid") songmid: String?,
    @Query("g_tk") g_tk: Int,
    @Query("format") format: String?,
    @Query("nobase64") nobase64: Int
  ): QLrcResponse

  companion object {

    const val BASE_URL = "https://c.y.qq.com/"
  }
}

interface NetEaseApi {

  @GET("search/get")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  suspend fun searchSong(
    @Query("s") key: String?, @Query("offset") offset: Int,
    @Query("limit") limit: Int, @Query("type") type: Int
  ): NSongSearchResponse

  @GET("song/lyric")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  suspend fun searchLyric(
    @Query("os") os: String?, @Query("id") id: Long,
    @Query("lv") lv: Int, @Query("kv") kv: Int, @Query("tv") tv: Int
  ): NLrcResponse

  companion object {
    const val BASE_URL = "http://music.163.com/api/"
  }
}