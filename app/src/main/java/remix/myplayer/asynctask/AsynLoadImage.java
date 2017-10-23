package remix.myplayer.asynctask;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.CloseableImage;
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
            if(mImage.getTag() != null && mImage.getTag().equals(url)){
                return;
            }

            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setImageRequest(ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                            .build())
                    .setOldController(mImage.getController())
                    .build();

            mImage.setController(controller);
            mImage.setTag(url);
        }
    }
}
