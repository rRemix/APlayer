package remix.myplayer.request

import android.annotation.SuppressLint
import android.net.Uri
import io.reactivex.disposables.Disposable
import remix.myplayer.request.network.RxUtil

abstract class SimpleUriRequest(private val request: UriRequest) : ImageUriRequest<String>() {

  @SuppressLint("CheckResult")
  override fun load(): Disposable {
    return getCoverObservable(request)
        .subscribe({ s -> onSuccess(s) }, { throwable -> onError(throwable) })
  }


}
