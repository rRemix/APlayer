package remix.myplayer.request.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.RequestConfig;

import static remix.myplayer.service.MusicService.copy;

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest extends ImageUriRequest<Bitmap> {
    private NSearchRequest mRequest;

    public RemoteUriRequest(@NonNull NSearchRequest request,@NonNull RequestConfig config){
        super(config);
        mRequest = request;
    }

    @Override
    public void load() {
        Context context = APlayerApplication.getContext();

        getThumbObservable(mRequest)
                .flatMap(new Function<String, ObservableSource<Bitmap>>() {
                    @Override
                    public ObservableSource<Bitmap> apply(String url) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<Bitmap>() {
                            @Override
                            public void subscribe(ObservableEmitter<Bitmap> e) throws Exception {
                                Uri imageUri = !TextUtils.isEmpty(url) ? Uri.parse(url) : Uri.EMPTY;
                                ImageRequest imageRequest =
                                        ImageRequestBuilder.newBuilderWithSource(imageUri)
                                                .setResizeOptions(new ResizeOptions(mConfig.getWidth(),mConfig.getHeight()))
                                                .build();
                                DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);
                                dataSource.subscribe(new BaseBitmapDataSubscriber() {
                                    @Override
                                    protected void onNewResultImpl(Bitmap bitmap) {
                                        Bitmap result = copy(bitmap);
                                        if(result == null) {
                                            result = BitmapFactory.decodeResource(context.getResources(),R.drawable.album_empty_bg_night);
                                        }
                                        e.onNext(result);
                                        e.onComplete();
                                    }

                                    @Override
                                    protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                                        e.onError(dataSource.getFailureCause());
                                    }
                                }, CallerThreadExecutor.getInstance());
                            }
                        });
                    }
                }).compose(RxUtil.applyScheduler())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
