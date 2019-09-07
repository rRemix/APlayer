package remix.myplayer.misc.observer

import android.annotation.SuppressLint
import android.content.Intent
import android.content.UriMatcher
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import remix.myplayer.service.MusicService
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by taeja on 16-3-30.
 * @param service
 * @param handler The handler to run [.onChange] on, or null if none.
 */
class MediaStoreObserver(service: MusicService) : ContentObserver(null), Runnable {
  private val handler = Handler()
  private val service: WeakReference<MusicService> = WeakReference(service)
  private var match = -1

  override fun run() {
    Completable
        .fromAction {
          service.get()?.setAllSong(MediaStoreUtil.getAllSongsId())
        }
        .subscribeOn(Schedulers.io())
        .subscribe()
    sendLocalBroadcast(Intent(MusicService.MEDIA_STORE_CHANGE))
  }

  @SuppressLint("CheckResult")
  override fun onChange(selfChange: Boolean, uri: Uri?) {
    Timber.v("onChange, selfChange: $selfChange uri: $uri")
    if (!selfChange && uri != null) {
      match = sUriMatcher.match(uri)
      if (match > 0) {
        handler.removeCallbacks(this)
        handler.postDelayed(this, 800)
      }
    }
  }


  companion object {
    private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    const val TYPE_INSERT = 1
    const val TYPE_UPDATE = 2
    const val TYPE_DELETE = 3

    init {
      sUriMatcher.addURI("media", "external/audio/media/#", TYPE_INSERT)//insert
      sUriMatcher.addURI("media", "external/audio/media", TYPE_UPDATE)// update
      sUriMatcher.addURI("media", "external", TYPE_DELETE)// delete
    }
  }
}
