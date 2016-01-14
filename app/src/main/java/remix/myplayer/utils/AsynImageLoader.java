package remix.myplayer.utils;

import android.widget.ImageView;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Remix on 2015/12/27.
 */
public class AsynImageLoader {
    private Map<ImageView,String> mImageViews = Collections.synchronizedMap(new LinkedHashMap<ImageView, String>());

}
