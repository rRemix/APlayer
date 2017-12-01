package remix.myplayer.request;

import android.graphics.drawable.Animatable;
import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest {
    SimpleDraweeView mImage;

    public ImageUriRequest(SimpleDraweeView image){
        mImage = image;
    }

    void onError(String errMsg){
        onSuccess("res://remix.myplayer/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
    }

    void onSuccess(String url) {
//        mImage.setImageURI(url);

        if(mImage.getTag() != null && mImage.getTag().equals(url)){
            return;
        }
        int size = DensityUtil.dip2px(APlayerApplication.getContext(),40);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                        .setResizeOptions(ResizeOptions.forDimensions(size,size))
                        .build())
                .setOldController(mImage.getController())
                .setControllerListener(new ControllerListener<ImageInfo>() {
                    @Override
                    public void onSubmit(String id, Object callerContext) {

                    }

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        LogUtil.d("Fresco","onIntermediateImageSet：" + id);
                    }

                    @Override
                    public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
                        LogUtil.d("Fresco","onIntermediateImageSet：" + id);
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
                .build();

        mImage.setController(controller);
        mImage.setTag(url);
    }

    public abstract void load();

}
