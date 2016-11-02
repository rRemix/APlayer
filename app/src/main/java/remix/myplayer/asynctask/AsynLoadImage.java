package remix.myplayer.asynctask;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.DrawableRes;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
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
    private boolean mAutoPlayAnimation = true;
    public AsynLoadImage(SimpleDraweeView imageView) {
        mImage = imageView;
    }

    /**
     * @param params param[0]参数 param[1]类型
     * @return
     */
    @Override
    protected String doInBackground(Object... params) {
        mAutoPlayAnimation = (boolean)params[2];
        return MediaStoreUtil.getImageUrl(params[0].toString(), (int)params[1]);
    }


    @Override
    protected void onPostExecute(String url) {
        if(mImage != null && url != null) {
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(Uri.parse("file:///" + url))
                    .setOldController(mImage.getController())
                    .setAutoPlayAnimations(mAutoPlayAnimation)
                    .build();
            mImage.setController(controller);

//            mImage.setImageURI(Uri.parse("file:///" + url));
        }
    }
}
