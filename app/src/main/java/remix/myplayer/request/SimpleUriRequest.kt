package remix.myplayer.request

import android.annotation.SuppressLint
import android.net.Uri
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import remix.myplayer.request.network.RxUtil

abstract class SimpleUriRequest(private val request: UriRequest) : ImageUriRequest<Uri>() {

  @SuppressLint("CheckResult")
  override fun load(): Disposable {
    return getCoverObservable(request)
        .compose(RxUtil.applyScheduler())
        .subscribe({ s -> onSuccess(Uri.parse(s)) }, { throwable -> onError(throwable.toString()) })
  }
}
