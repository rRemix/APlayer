package remix.myplayer.misc.imae;

import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest {
    SimpleDraweeView mImage;

    public ImageUriRequest(SimpleDraweeView image){
        mImage = image;
    }

    void onSuccess(String url) {
        mImage.setImageURI(url);

//        if(mImage.getTag() != null && mImage.getTag().equals(url)){
//            return;
//        }
//        DraweeController controller = Fresco.newDraweeControllerBuilder()
//                .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
//                        .build())
//                .setOldController(mImage.getController())
//                .build();
//
//        mImage.setController(controller);
//        mImage.setTag(url);
    }

    public abstract void load();

}
