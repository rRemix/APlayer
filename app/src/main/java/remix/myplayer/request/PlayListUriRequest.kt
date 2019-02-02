package remix.myplayer.request

import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType

import android.net.Uri
import com.facebook.drawee.view.SimpleDraweeView
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.ObservableSource
import io.reactivex.SingleSource
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableObserver
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.request.network.RxUtil

/**
 * Created by Remix on 2017/11/30.
 */

class PlayListUriRequest(image: SimpleDraweeView, request: UriRequest, config: RequestConfig) : LibraryUriRequest(image, request, config) {

  override fun onError(errMsg: String?) {
    super.onError(errMsg)
//    mImage.setImageURI(Uri.EMPTY);
  }

  override fun load(): Disposable {
    val coverObservables = DatabaseRepository.getInstance()
        .getPlayList(mRequest.id)
        .flatMap(Function<PlayList, SingleSource<List<Song>>> { playList ->
          DatabaseRepository.getInstance()
              .getPlayListSongs(App.getContext(), playList, true)
        })
        .flatMapObservable(Function<List<Song>, ObservableSource<Song>> { songs ->
          Observable.create { emitter ->
            for (song in songs) {
              emitter.onNext(song)
            }
            emitter.onComplete()
          }
        })
        .concatMapDelayError(Function<Song, ObservableSource<String>> { song ->
          getCoverObservable(getSearchRequestWithAlbumType(song)) })

    return Observable.concat(getCustomThumbObservable(mRequest), coverObservables)
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribeWith(object : DisposableObserver<String>() {
          override fun onStart() {
            mImageRef.get()?.setImageURI(Uri.EMPTY)
          }

          override fun onNext(s: String) {
            onSuccess(s)
          }

          override fun onError(e: Throwable) {
            this@PlayListUriRequest.onError(e.toString())
          }

          override fun onComplete() {

          }
        })
  }

}
