package remix.myplayer.request;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
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
    private static final String LASTFM_DEFAULT_COVER = "https://lastfm-img2.akamaized.net/i/u/300x300/e1d60ddbcaaa6acdcbba960786f11360.png";

    UriRequest mRequest;

    public LibraryUriRequest(@NonNull SimpleDraweeView image, @NonNull UriRequest request, RequestConfig config) {
        super(config);
        mImage = image;
        mRequest = request;
    }

    public void onError(String errMsg) {
//        mImage.setImageURI(Uri.EMPTY);
        LogUtil.i(TAG, "onError: " + errMsg);
    }

    public void onSuccess(String result) {
        LogUtil.i(TAG, "onSuccess: " + result);
        if (LASTFM_DEFAULT_COVER.equals(result)) {
            return;
        }
        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(result));
        if (mConfig.isResize()) {
            imageRequestBuilder.setResizeOptions(ResizeOptions.forDimensions(mConfig.getWidth(), mConfig.getHeight()));
        }
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequestBuilder.build())
                .setOldController(mImage.getController())
                .setControllerListener(new ControllerListener<ImageInfo>() {
                    @Override
                    public void onSubmit(String s, Object o) {

                    }

                    @Override
                    public void onFinalImageSet(String s, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
                    }

                    @Override
                    public void onIntermediateImageSet(String s, @Nullable ImageInfo imageInfo) {
                    }

                    @Override
                    public void onIntermediateImageFailed(String s, Throwable throwable) {
//                        Util.writeLogToExternalStorage("fresco", "onIntermediateImageFailed\nresult: " + result + "\nthrowable: " +throwable + "\n");
                    }

                    @Override
                    public void onFailure(String s, Throwable throwable) {
//                        Util.writeLogToExternalStorage("fresco", "onFailure\nresult: " + result + "\nthrowable: " +throwable + "\n");
                    }

                    @Override
                    public void onRelease(String s) {

                    }
                })
                .build();

        mImage.setController(controller);
    }

    @Override
    public Disposable load() {
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
}
