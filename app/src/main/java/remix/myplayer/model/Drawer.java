package remix.myplayer.model;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

/**
 * Created by Remix on 2017/10/22.
 */

public class Drawer {
    private int TitleResId;
    private int ImageResID;

    public Drawer(int title, int imageResID) {
        TitleResId = title;
        ImageResID = imageResID;
    }

    @StringRes
    public int getTitleResId() {
        return TitleResId;
    }

    public void setTitleResId(int titleResId) {
        TitleResId = titleResId;
    }

    @DrawableRes
    public int getImageResID() {
        return ImageResID;
    }

    public void setImageResID(int imageResID) {
        ImageResID = imageResID;
    }
}
