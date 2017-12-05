package remix.myplayer.uri;

import android.graphics.drawable.Animatable;
import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.R;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2017/12/4.
 */

public class LibraryUriRequest extends ImageUriRequest {
    protected SimpleDraweeView mImage;
    NSearchRequest mRequest;
    public LibraryUriRequest(SimpleDraweeView image,NSearchRequest request,RequestConfig config) {
        super(config);
        mImage = image;
        mRequest = request;
    }

    public void onError(String errMsg){
        String url = "res://remix.myplayer/" + (mRequest.getLType() == Constants.URL_ARTIST ?
                (ThemeStore.isDay() ? R.drawable.artist_empty_bg_day : R.drawable.artist_empty_bg_night) :
                ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night );
        mImage.setImageURI(url);
    }

    public void onSuccess(String url) {

        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url));
        if(mConfig.isResize()){
            imageRequestBuilder.setResizeOptions(ResizeOptions.forDimensions(mConfig.getWidth(),mConfig.getHeight()));
        }
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequestBuilder.build())
                .setControllerListener(new ControllerListener<ImageInfo>() {
                    @Override
                    public void onSubmit(String id, Object callerContext) {

                    }

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                    }

                    @Override
                    public void onIntermediateImageSet(String id, ImageInfo imageInfo) {

                    }

                    @Override
                    public void onIntermediateImageFailed(String id, Throwable throwable) {

                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {

                    }

                    @Override
                    public void onRelease(String id) {

                    }
                })
                .setOldController(mImage.getController())
                .build();

        mImage.setController(controller);
        mImage.setTag(url);
    }

    @Override
    public void load() {
        getThumbObservable(mRequest)
                .compose(RxUtil.applyScheduler())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }
}
