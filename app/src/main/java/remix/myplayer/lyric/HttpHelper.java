package remix.myplayer.lyric;

import io.reactivex.Observable;
import okhttp3.ResponseBody;

/**
 * Created by Remix on 2017/11/21.
 */

public interface HttpHelper {
    Observable<ResponseBody> getNeteaseSearch(String key,int offset,int limit,int type);

    Observable<ResponseBody> getNeteaseLyric(int id);

    Observable<ResponseBody> getKuGouSearch(String keyword, long duration, String hash);

    Observable<ResponseBody> getKuGouLyric(int id,String accessKey);
}
