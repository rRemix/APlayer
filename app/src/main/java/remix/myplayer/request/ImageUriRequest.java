package remix.myplayer.request;

import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest {
    public static final int GRID_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),125);
    public static final int LIST_IMAGE_SIZE = DensityUtil.dip2px(APlayerApplication.getContext(),45);

    protected SimpleDraweeView mImage;
    RequestConfig mConfig = DEFAULT_CONFIG;

    private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
            .forceDownload(true).build();

    ImageUriRequest(SimpleDraweeView image, RequestConfig config){
        mImage = image;
        mConfig = config;
    }

    ImageUriRequest(SimpleDraweeView image){
        mImage = image;
    }

    void onError(String errMsg){
        onSuccess("res://remix.myplayer/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
    }

    void onSuccess(String url) {
        if(mImage.getTag() != null && mImage.getTag().equals(url)){
            return;
        }

        ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url));
        if(mConfig.isResize()){
            imageRequestBuilder.setResizeOptions(ResizeOptions.forDimensions(mConfig.getWidth(),mConfig.getHeight()));
        }

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequestBuilder.build())
                .setOldController(mImage.getController())
                .build();

        mImage.setController(controller);
        mImage.setTag(url);
    }

    public abstract void load();

}
