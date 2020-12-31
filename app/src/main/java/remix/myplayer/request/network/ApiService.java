package remix.myplayer.request.network;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import remix.myplayer.BuildConfig;
import remix.myplayer.bean.github.Release;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Remix on 2017/11/20.
 */

public interface ApiService {

  @POST("search/pc")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  Observable<ResponseBody> getNeteaseSearch(@Query("s") String key, @Query("offset") int offset,
      @Query("limit") int limit, @Query("type") int type);

  @GET("song/lyric")
  @Headers("User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36")
  Observable<ResponseBody> getNeteaseLyric(@Query("os") String os, @Query("id") int id,
      @Query("lv") int lv, @Query("kv") int kv, @Query("tv") int tv);

  @GET("search")
  Observable<ResponseBody> getKuGouSearch(@Query("ver") int ver, @Query("man") String man,
      @Query("client") String client,
      @Query("keyword") String keyword, @Query("duration") long duration,
      @Query("hash") String hash);

  @GET("download")
  Observable<ResponseBody> getKuGouLyric(@Query("ver") int ver, @Query("client") String client,
      @Query("fmt") String fmt, @Query("charset") String charSet,
      @Query("id") int id, @Query("accesskey") String accessKey);

  @GET("soso/fcgi-bin/client_search_cp")
  Observable<ResponseBody> getQQSearch(@Query("n") int n,
      @Query("w") String w,
      @Query("format") String format);

  @GET("lyric/fcgi-bin/fcg_query_lyric_new.fcg")
  @Headers("Referer: https://y.qq.com/portal/player.html")
  Observable<ResponseBody> getQQLyric(@Query("songmid") String songmid,
      @Query("g_tk") int g_tk,
      @Query("format") String format,
      @Query("nobase64") int nobase64);

  String BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=" + BuildConfig.LASTFM_API_KEY;

  @GET(BASE_QUERY_PARAMETERS + "&method=album.getinfo")
  Observable<ResponseBody> getAlbumInfo(@Query("album") String albumName,
      @Query("artist") String artistName, @Nullable @Query("lang") String language);

  @GET(BASE_QUERY_PARAMETERS + "&method=artist.getinfo")
  Observable<ResponseBody> getArtistInfo(@Query("artist") String artistName,
      @Nullable @Query("lang") String language);

  @GET("repos/{owner}/{repo}/releases/latest")
  @Headers({"token: " + BuildConfig.GITHUB_SECRET_KEY})
  Single<Release> getLatestRelease(@Path("owner") String owner, @Path("repo") String repo);
}
