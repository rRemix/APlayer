package remix.myplayer.request;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2017/12/4.
 */

public class LibraryUriRequest extends ImageUriRequest<String> {
    private static final String TAG = LibraryUriRequest.class.getSimpleName();
    protected SimpleDraweeView mImage;
    UriRequest mRequest;
    public LibraryUriRequest(@NonNull SimpleDraweeView image, @NonNull UriRequest request, RequestConfig config) {
        super(config);
        mImage = image;
        mRequest = request;
    }

    public void onError(String errMsg){
//        mImage.setImageURI(Uri.EMPTY);
        LogUtil.i(TAG,"Error: " + errMsg);
    }

    public void onSuccess(String result) {
        LogUtil.i(TAG,"success: " + result);
        if(result.equals("https://lastfm-img2.akamaized.net/i/u/300x300/e1d60ddbcaaa6acdcbba960786f11360.png")){
            result = "";
        }
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(result));
        if(mConfig.isResize()){
            imageRequestBuilder.setResizeOptions(ResizeOptions.forDimensions(mConfig.getWidth(),mConfig.getHeight()));
        }
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequestBuilder.build())
                .setOldController(mImage.getController())
                .build();

        mImage.setController(controller);
    }

    public Disposable loadImage(){
        return getCoverObservable(mRequest)
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
                        LibraryUriRequest.this.onError(e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @SuppressLint("CheckResult")
    @Override
    public void load() {
        getCoverObservable(mRequest)
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
                        LibraryUriRequest.this.onError(e.toString());
                    }

                    @Override
                    public void onComplete() {
                    }
                });
//                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
