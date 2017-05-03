package remix.myplayer.asynctask;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.util.MediaStoreUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/22 16:12
 */
public class AsynLoadImage extends AsyncTask<Object,Integer,String> {
    private final SimpleDraweeView mImage;
    public AsynLoadImage(SimpleDraweeView imageView) {
        mImage = imageView;
    }

    /**
     * @param params param[0]参数 param[1]类型
     * @return
     */
    @Override
    protected String doInBackground(Object... params) {
        return MediaStoreUtil.getImageUrl((Integer) params[0], (int)params[1]);
    }

    @Override
    protected void onPostExecute(String url) {
        if(mImage != null && !TextUtils.isEmpty(url)) {
            ImageRequest imageRequest = null;
            if(mImage.getWidth() > 0 && mImage.getHeight() > 0){
                imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                        .setResizeOptions(new ResizeOptions(mImage.getWidth(),mImage.getHeight()))
                        .build();
            } else {
                imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                        .build();
            }
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(imageRequest)
                    .setOldController(mImage.getController())
                    .build();

            mImage.setController(controller);
//            mImage.setImageURI(Uri.parse(url));
        }
    }
}
