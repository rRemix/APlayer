package remix.myplayer.misc.imae;

import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class CommonUriRequest extends ImageUriRequest {

    public CommonUriRequest(SimpleDraweeView image) {
        super(image);
    }

    void onError(String errMsg){
        onSuccess("res://remix.myplayer/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
    }

    void onSuccess(String url) {
        mImage.setImageURI(url);
    }
}
