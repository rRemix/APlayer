package remix.myplayer.lyric;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import remix.myplayer.model.netease.NSearchResponse;

/**
 * Created by Remix on 2017/11/21.
 */

public interface HttpHelper {
    Observable<NSearchResponse> getNeteaseSearch2(String key,int offset, int limit,int type);

    Observable<ResponseBody> getNeteaseSearch(String key,int offset,int limit,int type);

    Observable<ResponseBody> getNeteaseLyric(int id);

    Observable<ResponseBody> getKuGouSearch(String keyword, int duration, String hash);

    Observable<ResponseBody> getKuGouLyric(int id,String accessKey);
}
