package remix.myplayer.request.network;

import android.content.Context;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import remix.myplayer.misc.cache.DiskCache;

public class OkHttpHelper {

  private static final int TIMEOUT = 30000;

  private static OkHttpClient sOkHttpClient;
  private static SSLSocketFactory sSSLSocketFactory;

  public static SSLSocketFactory getSSLSocketFactory() {
    if (sSSLSocketFactory == null) {
      try {
        // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
        final X509TrustManager trustAllCert =
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                  String authType) {
              }

              @Override
              public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                  String authType) {
              }

              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
              }
            };
        sSSLSocketFactory = new SSLSocketFactoryCompat(trustAllCert);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return sSSLSocketFactory;
  }

  public static OkHttpClient getOkHttpClient() {
    if (sOkHttpClient == null) {
      OkHttpClient.Builder builder = new OkHttpClient.Builder();
      try {
        // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
        final X509TrustManager trustAllCert =
            new X509TrustManager() {
              @Override
              public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                  String authType) {
              }

              @Override
              public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                  String authType) {
              }

              @Override
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
              }
            };
        builder.sslSocketFactory(getSSLSocketFactory(), trustAllCert);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      builder.connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
          .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
          .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
      sOkHttpClient = builder.build();
    }
    return sOkHttpClient;
  }

  @Nullable
  private Cache createDefaultCache(Context context) {
    File cacheDir = DiskCache.getDiskCacheDir(context, "okhttp");
    if (cacheDir.mkdirs() || cacheDir.isDirectory()) {
      return new Cache(cacheDir, 1024 * 1024 * 10);
    }
    return null;
  }

}
