package remix.myplayer.request

import android.annotation.SuppressLint
import io.reactivex.disposables.Disposable

@Deprecated("replace with glide")
abstract class SimpleUriRequest(private val request: UriRequest) : ImageUriRequest<String>() {

  @SuppressLint("CheckResult")
  override fun load(): Disposable {
    return getCoverObservable(request)
        .subscribe({ s -> onSuccess(s) }, { throwable -> onError(throwable) })
  }


}
