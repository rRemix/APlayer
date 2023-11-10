package remix.myplayer.request.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import remix.myplayer.misc.cache.DiskCache
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

class OkHttpHelper {
  private fun createDefaultCache(context: Context): Cache? {
    val cacheDir = DiskCache.getDiskCacheDir(context, "okhttp")
    return if (cacheDir.mkdirs() || cacheDir.isDirectory) {
      Cache(cacheDir, 1024 * 1024 * 10)
    } else null
  }

  companion object {
    private const val TIMEOUT = 30000
    private var sOkHttpClient: OkHttpClient? = null
    private var sSSLSocketFactory: SSLSocketFactory? = null

    // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
    val sSLSocketFactory: SSLSocketFactory?
      get() {
        if (sSSLSocketFactory == null) {
          try {
            // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
            val trustAllCert: X509TrustManager = object : X509TrustManager {
              override fun checkClientTrusted(chain: Array<X509Certificate>,
                                              authType: String) {
              }

              override fun checkServerTrusted(chain: Array<X509Certificate>,
                                              authType: String) {
              }

              override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
              }
            }
            sSSLSocketFactory = SSLSocketFactoryCompat(trustAllCert)
          } catch (e: Exception) {
            throw RuntimeException(e)
          }
        }
        return sSSLSocketFactory
      }

    // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
    val okHttpClient: OkHttpClient?
      get() {
        if (sOkHttpClient == null) {
          val builder = OkHttpClient.Builder()
          try {
            // 自定义一个信任所有证书的TrustManager，添加SSLSocketFactory的时候要用到
            val trustAllCert: X509TrustManager = object : X509TrustManager {
              override fun checkClientTrusted(chain: Array<X509Certificate>,
                                              authType: String) {
              }

              override fun checkServerTrusted(chain: Array<X509Certificate>,
                                              authType: String) {
              }

              override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
              }
            }
            builder.sslSocketFactory(sSLSocketFactory!!, trustAllCert)
          } catch (e: Exception) {
            throw RuntimeException(e)
          }
          builder.connectTimeout(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
              .readTimeout(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
              .writeTimeout(TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
          sOkHttpClient = builder.build()
        }
        return sOkHttpClient
      }
  }
}