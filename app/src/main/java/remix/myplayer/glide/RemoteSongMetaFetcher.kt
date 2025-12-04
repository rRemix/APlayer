package remix.myplayer.glide

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import remix.myplayer.data.bean.mp3.Song
import remix.myplayer.service.MusicService
import timber.log.Timber

internal object RemoteSongMetaFetcher {
  private const val WAIT_INTERVAL = 50L
  private const val DEFAULT_TIMEOUT = 5_000L

  fun fetchBlocking(song: Song.Remote, timeoutMs: Long = DEFAULT_TIMEOUT) {
    val endTime = System.currentTimeMillis() + timeoutMs
    runBlocking {
      ensureFetched(song, endTime)
    }
  }

  private suspend fun ensureFetched(song: Song.Remote, endTime: Long) {
    while (System.currentTimeMillis() < endTime) {
      val state = song.metaFetchState.get()
      if (state == 2 || state == 3) return

      if (state == 1) {
        awaitExternalFetch(song, endTime - System.currentTimeMillis())
        continue
      }

      if (state == 0) {
        val remaining = endTime - System.currentTimeMillis()
        if (remaining <= 0) return

        try {
          withTimeout(remaining) {
            withContext(Dispatchers.IO) {
              MusicService.Companion.retrieveRemoteSong(song, song)
            }
          }
        } catch (e: TimeoutCancellationException) {
          Timber.Forest.v("RemoteSongMetaFetcher fetch timeout: ${song.data}")
          return
        }
      }
    }
  }

  private suspend fun awaitExternalFetch(song: Song.Remote, timeoutMs: Long) {
    if (timeoutMs <= 0) return
    try {
      withTimeout(timeoutMs) {
        while (song.metaFetchState.get() == 1) {
          delay(WAIT_INTERVAL)
        }
      }
    } catch (e: TimeoutCancellationException) {
      Timber.Forest.v("RemoteSongMetaFetcher wait timeout: ${song.data}")
    }
  }
}