package remix.myplayer.model.network;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Created by Remix on 2017/11/20.
 */

public interface ApiService {
    @POST("search/pc")
    @Headers("Cookie: appver=1.5.0.75771")
    Observable<ResponseBody> getNeteaseSearch(@Query("s") String key, @Query("offset") int offset,
                                              @Query("limit") int limit, @Query("type") int type);

}
