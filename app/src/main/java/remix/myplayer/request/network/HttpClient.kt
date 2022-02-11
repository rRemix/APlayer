package remix.myplayer.request.network

import io.reactivex.Single
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
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by Remix on 2017/11/20.
 */
object HttpClient {
  private val neteaseApi: ApiService
  private val kuGouApi: ApiService
  private val qqApi: ApiService
  private val lastfmApi: ApiService
  private val githubApi: ApiService

  //    private static OkHttpClient sOkHttpClient;
  //    private static SSLSocketFactory mSSLSocketFactory;
  //    public static SSLSocketFactory getSSLSocketFactory(){
  //        if(mSSLSocketFactory == null){
  //            try {
  //                // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
  //                final X509TrustManager trustAllCert =
  //                        new X509TrustManager() {
  //                            @Override
  //                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
  //                                return new java.security.cert.X509Certificate[]{};
  //                            }
  //                        };
  //                mSSLSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //        }
  //        return mSSLSocketFactory;
  //    }
  //
  //    public synchronized static OkHttpClient getOkHttpClient(){
  //        if (sOkHttpClient == null) {
  //            OkHttpClient.Builder builder = new OkHttpClient.Builder();
  //            try {
  //                // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
  //                final X509TrustManager trustAllCert =
  //                        new X509TrustManager() {
  //                            @Override
  //                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)  {
  //                            }
  //
  //                            @Override
  //                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
  //                                return new java.security.cert.X509Certificate[]{};
  //                            }
  //                        };
  //                builder.sslSocketFactory(getSSLSocketFactory(), trustAllCert);
  //            } catch (Exception e) {
  //                throw new RuntimeException(e);
  //            }
  //            builder.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
  //                    .cache(createDefaultCache(App.getContext()));
  //            sOkHttpClient = builder.build();
  //        }
  //        return sOkHttpClient;
  //    }

  fun fetchLatestRelease(owner: String, repo: String): Single<Release> {
    return githubApi.fetchLatestRelease(owner, repo)
  }

  //New Api
  fun searchLastFMAlbum(
    albumName: String?,
    artistName: String?,
    lang: String?
  ): Single<LastFmAlbum> {
    return lastfmApi.searchLastFMAlbum(albumName, artistName, lang)
  }

  fun searchLastFMArtist(artistName: String?, language: String?): Single<LastFmArtist> {
    return lastfmApi.searchLastFMArtist(artistName, language)
  }

  fun searchNeteaseSong(key: String?, offset: Int, limit: Int): Single<NSongSearchResponse> {
    return neteaseApi.searchNeteaseSong(key, offset, limit, 1)
  }

  fun searchNeteaseAlbum(key: String?, offset: Int, limit: Int): Single<NAlbumSearchResponse> {
    return neteaseApi.searchNeteaseAlbum(key, offset, limit, 10)
  }

  fun searchNeteaseArtist(key: String?, offset: Int, limit: Int): Single<NArtistSearchResponse> {
    return neteaseApi.searchNeteaseArtist(key, offset, limit, 100)
  }

  fun searchNeteaseLyric(id: Int): Single<NLrcResponse> {
    return neteaseApi.searchNeteaseLyric("pc", id, -1, -1, -1)
  }

  fun searchNeteaseDetail(id: Int, ids: String?): Single<NDetailResponse> {
    return neteaseApi.searchNeteaseDetail(id,"[$id]" )
  }

  fun searchKuGou(keyword: String?, duration: Long): Single<KSearchResponse> {
    return kuGouApi.searchKuGou(1, "yes", "pc", keyword, duration, "")
  }

  fun searchKuGouLyric(id: Int, accessKey: String?): Single<KLrcResponse> {
    return kuGouApi.searchKuGouLyric(1, "pc", "lrc", "utf8", id, accessKey)
  }

  fun searchQQ(key: String?): Single<QSearchResponse> {
    return qqApi.searchQQ(1, key, "json")
  }

  fun searchQQLyric(songmid: String?): Single<QLrcResponse> {
    return qqApi.searchQQLyric(songmid, 5381, "json", 1)
  }


  private const val NETEASE_BASE_URL = "http://music.163.com/api/"
  private const val KUGOU_BASE_URL = "http://lyrics.kugou.com/"
  private const val QQ_BASE_URL = "https://c.y.qq.com/"
  private const val LASTFM_BASE_URL = "http://ws.audioscrobbler.com/2.0/"
  private const val GITHUB_BASE_URL = "https://api.github.com/"

  init {
    val retrofitBuilder = Retrofit.Builder()
    val okHttpClient = OkHttpHelper.okHttpClient
    neteaseApi = retrofitBuilder
      .baseUrl(NETEASE_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    kuGouApi = retrofitBuilder
      .baseUrl(KUGOU_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    qqApi = retrofitBuilder
      .baseUrl(QQ_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    lastfmApi = retrofitBuilder
      .baseUrl(LASTFM_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
    githubApi = retrofitBuilder
      .baseUrl(GITHUB_BASE_URL)
      .client(okHttpClient)
      .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
      .addConverterFactory(GsonConverterFactory.create())
      .build().create(ApiService::class.java)
  }
}