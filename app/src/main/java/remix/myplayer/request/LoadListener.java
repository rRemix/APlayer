package remix.myplayer.request;

/**
 * Created by Remix on 2017/11/30.
 */

public interface LoadListener {
    void onLoadSuccess(String uri);
    void onLoadFailed(String error);
}
