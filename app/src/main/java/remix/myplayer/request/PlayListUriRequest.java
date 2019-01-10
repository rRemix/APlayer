package remix.myplayer.request;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

import android.net.Uri;
import com.facebook.drawee.view.SimpleDraweeView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import java.util.List;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {

  public PlayListUriRequest(SimpleDraweeView image, UriRequest request, RequestConfig config) {
    super(image, request, config);
  }

  @Override
  public void onError(String errMsg) {
//        mImage.setImageURI(Uri.EMPTY);
  }

  @Override
  public Disposable load() {
    Observable<String> coverObservable = Observable.create(new ObservableOnSubscribe<Song>() {
      @Override
      public void subscribe(ObservableEmitter<Song> emitter) throws Exception {
        List<Song> songs = PlayListUtil
            .getMP3ListByIds(PlayListUtil.getSongIds(mRequest.getID()), mRequest.getID());
        for (Song song : songs) {
          emitter.onNext(song);
        }
        emitter.onComplete();
      }
    }).concatMapDelayError(new Function<Song, ObservableSource<String>>() {
      @Override
      public ObservableSource<String> apply(Song song) throws Exception {
        return getCoverObservable(getSearchRequestWithAlbumType(song));
      }
    });

    return Observable.concat(getCustomThumbObservable(mRequest), coverObservable)
        .firstOrError()
        .toObservable()
        .compose(RxUtil.applyScheduler())
        .subscribeWith(new DisposableObserver<String>() {
          @Override
          protected void onStart() {
            mImage.setImageURI(Uri.EMPTY);
          }

          @Override
          public void onNext(String s) {
            onSuccess(s);
          }

          @Override
          public void onError(Throwable e) {
            PlayListUriRequest.this.onError(e.toString());
          }

          @Override
          public void onComplete() {

          }
        });
  }

}
