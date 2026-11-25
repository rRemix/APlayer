package remix.myplayer.data.adapter

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Sardine treats every 2xx response as success, including partial collection deletion (207). */
internal object WebDavDeleteInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val response = chain.proceed(chain.request())
    if (chain.request().method == "DELETE" && response.isSuccessful && response.code !in setOf(200, 204)) {
      val code = response.code
      response.close()
      throw IOException("WebDAV deletion was not confirmed complete (HTTP $code)")
    }
    return response
  }
}
