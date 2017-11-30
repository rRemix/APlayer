package remix.myplayer.misc.imae;

import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;

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
        mImage.setImageURI(url);

//        if(mImage.getTag() != null && mImage.getTag().equals(url)){
//            return;
//        }
//        int size = DensityUtil.dip2px(APlayerApplication.getContext(),40);
//        DraweeController controller = Fresco.newDraweeControllerBuilder()
//                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
//                        .setResizeOptions(ResizeOptions.forDimensions(size,size))
//                        .build())
//                .setOldController(mImage.getController())
//                .build();
//
//        mImage.setController(controller);
//        mImage.setTag(url);
    }

    public abstract void load();

}
