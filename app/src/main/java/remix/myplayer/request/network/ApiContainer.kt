package remix.myplayer.request.network

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ResponseBody
import remix.myplayer.BuildConfig
import remix.myplayer.bean.github.Release
import remix.myplayer.bean.lastfm.LastFmAlbum
import remix.myplayer.bean.lastfm.LastFmArtist
import remix.myplayer.bean.qq.QLrcResponse
import remix.myplayer.bean.qq.QSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

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

interface GithubApi {

  @GET("repos/{owner}/{repo}/releases/latest")
  suspend fun fetchLatestRelease(
    @Path("owner") owner: String?,
    @Path("repo") repo: String?
  ): Release

  @Streaming
  @GET
  suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>

  companion object {

    const val BASE_URL = "https://api.github.com/"
  }
}

interface LastFMApi {
  @EntryPoint
  @InstallIn(SingletonComponent::class)
  interface LastFMApiEntryPoint {

    fun lastFMApi(): LastFMApi
  }

  @GET("$BASE_QUERY_PARAMETERS&method=album.getinfo")
  suspend fun searchLastFMAlbum(
    @Query("album") albumName: String?,
    @Query("artist") artistName: String?, @Query("lang") language: String?
  ): LastFmAlbum

  @GET("$BASE_QUERY_PARAMETERS&method=artist.getinfo")
  suspend fun searchLastFMArtist(
    @Query("artist") artistName: String?,
    @Query("lang") language: String?
  ): LastFmArtist

  companion object {

    const val BASE_URL = "http://ws.audioscrobbler.com/2.0/"
    const val BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=" + BuildConfig.LASTFM_API_KEY
  }
}