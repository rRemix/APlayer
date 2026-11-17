package remix.myplayer.coil

import android.net.Uri
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song

// Each candidate goes through Coil so a decode failure can fall back to the next cover.
class CoverInterceptor(private val resolver: CoverUriResolver) : Interceptor {
  private val locks = mutableMapOf<String, LockEntry>()

  override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
    val model = chain.request.data as? APlayerModel ?: return chain.proceed()
    val diskKey = chain.request.diskCacheKey ?: return chain.proceed()

    // Count callers waiting for this lock too; otherwise it could be removed too early.
    val lock = synchronized(locks) {
      locks.getOrPut(diskKey) { LockEntry() }.also { it.users++ }
    }
    try {
      return lock.mutex.withLock { loadCover(chain, model, diskKey) }
    } finally {
      synchronized(locks) {
        if (--lock.users == 0) locks.remove(diskKey)
      }
    }
  }

  private suspend fun loadCover(
    chain: Interceptor.Chain,
    model: APlayerModel,
    diskKey: String
  ): ImageResult {
    // Collections need to query their songs before the final disk key is known.
    val cachedRequest = if (model is Song) chain.request else {
      chain.request.newBuilder().diskCachePolicy(CachePolicy.DISABLED).build()
    }
    var result = chain.withRequest(cachedRequest).proceed()
    if (result is SuccessResult) return result

    val sources = withContext(Dispatchers.IO) { resolver.resolveSources(model, diskKey) }
    val request = chain.request.newBuilder().diskCacheKey(sources.diskKey).build()

    if (sources.diskKey != diskKey) {
      result = chain.withRequest(request).proceed()
      if (result is SuccessResult) return result
    }

    for (candidate in sources.candidates) {
      currentCoroutineContext().ensureActive()
      if (candidate.network && !request.networkCachePolicy.readEnabled) continue

      try {
        val uri = withContext(Dispatchers.IO) { candidate.fetch() }
        if (uri == Uri.EMPTY) continue

        val candidateRequest = request.newBuilder()
          .data(uri.toString().toUri())
          // A failed decode may have left invalid bytes at this disk key.
          .diskCachePolicy(
            if (request.diskCachePolicy.writeEnabled) CachePolicy.WRITE_ONLY
            else CachePolicy.DISABLED
          )
          .build()
        result = chain.withRequest(candidateRequest).proceed()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        result = ErrorResult(image = null, request = request, throwable = e)
      }
      if (result is SuccessResult) return result
    }

    return result
  }

  private class LockEntry(val mutex: Mutex = Mutex(), var users: Int = 0)
}
