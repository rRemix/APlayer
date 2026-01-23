package remix.myplayer.service

import android.annotation.SuppressLint
import android.content.Intent
import android.content.UriMatcher
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import remix.myplayer.util.Util
import timber.log.Timber

/**
 * Created by taeja on 16-3-30.
 * @param service
 * @param handler The handler to run [.onChange] on, or null if none.
 */
internal class MediaStoreObserver : ContentObserver(null), Runnable {
  private val handler = Handler(Looper.getMainLooper())
  private var match = -1

  override fun run() {
    Util.sendLocalBroadcast(Intent(MusicService.MEDIA_STORE_CHANGE))
  }

  @SuppressLint("CheckResult")
  override fun onChange(selfChange: Boolean, uri: Uri?) {
    Timber.Forest.v("onChange, selfChange: $selfChange uri: $uri")
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

    private const val TYPE_INSERT = 1
    private const val TYPE_UPDATE = 2
    private const val TYPE_DELETE = 3

    init {
      sUriMatcher.addURI("media", "external/audio/media/#", TYPE_INSERT)//insert
      sUriMatcher.addURI("media", "external/audio/media", TYPE_UPDATE)// update
      sUriMatcher.addURI("media", "external", TYPE_DELETE)// delete
    }
  }
}