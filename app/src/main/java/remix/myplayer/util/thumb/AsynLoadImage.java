package remix.myplayer.util.thumb;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.util.DBUtil;

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
    @Override
    protected String doInBackground(Object... params) {
        mAutoPlayAnimation = (boolean)params[2];
        return DBUtil.getImageUrl(params[0].toString(), (int)params[1]);
    }
    @Override
    protected void onPostExecute(String url) {
        if(mImage != null && url != null) {
//            DraweeController controller = Fresco.newDraweeControllerBuilder()
//                    .setUri(Uri.parse("file:///" + url))
//                    .setOldController(mImage.getController())
//                    .build();
//            mImage.setController(controller);

            mImage.setImageURI(Uri.parse("file:///" + url));
        }
    }
}
