package remix.myplayer.request

import android.graphics.Bitmap
import io.reactivex.disposables.Disposable
import remix.myplayer.request.network.RxUtil

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest(private val request: UriRequest, config: RequestConfig) : ImageUriRequest<Bitmap>(config) {

  override fun load(): Disposable {
    return getThumbBitmapObservable(request)
        .compose(RxUtil.applySchedulerToIO())
        .subscribe({ bitmap -> onSuccess(bitmap) }, { throwable -> onError(throwable) })
  }


}
