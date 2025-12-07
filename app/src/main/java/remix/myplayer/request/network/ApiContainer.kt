package remix.myplayer.request.network

import okhttp3.ResponseBody
import remix.myplayer.BuildConfig
import remix.myplayer.data.model.github.Release
import remix.myplayer.data.model.lastfm.LastFmAlbum
import remix.myplayer.data.model.lastfm.LastFmArtist
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

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
    const val BASE_QUERY_PARAMETERS =
      "?format=json&autocorrect=1&api_key=" + BuildConfig.LASTFM_API_KEY
  }
}