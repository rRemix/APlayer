package remix.myplayer.misc.log

import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import timber.log.Timber

/**
 * Created by remix on 2019/1/15
 */
open class LogObserver : SingleObserver<Any> {
  override fun onSuccess(value: Any) {
    Timber.v("onSuccess: %s", value.toString())
  }

  override fun onSubscribe(d: Disposable) {
    Timber.v("onSubscribe")
  }

  override fun onError(e: Throwable) {
    Timber.v("onError: %s", e.message)
  }
}