package remix.myplayer.misc.imae;

import android.content.ContentUris;
import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;

import java.io.File;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.lyric.network.HttpClient;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.model.netease.NAlbumSearchResponse;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public class AlbumUriRequest extends ImageUriRequest {
    private Album mAlbum;
    BaseViewHolder mViewHolder;
    int mPosition;
    public AlbumUriRequest(int position,BaseViewHolder holder,SimpleDraweeView simpleDraweeView, Album album) {
        super(simpleDraweeView);
        mAlbum = album;
        mViewHolder = holder;
        mPosition = position;
    }

    long start1;
    long start2;
    @Override
    public void load() {
        if(true){
            HttpClient.getNeteaseApiservice()
                    .getNeteaseSearch(mAlbum.getAlbum(),0,1,10)
                    .compose(RxUtil.applyScheduler())
                    .map(new Function<ResponseBody, String>() {
                        @Override
                        public String apply(ResponseBody body) throws Exception {
                            NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                            return response.result.albums.get(0).picUrl;
                        }
                    })
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            onSuccess(s);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            onSuccess("res://remix.myplayer/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
                        }
                    });
            return;
        }

        start1 = System.currentTimeMillis();
        Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomCoverIfExist(mAlbum.getAlbumID(), Constants.URL_ALBUM);
            if(customImage != null && customImage.exists()){
                e.onComplete();
//                    e.onNext("file://" + customImage.getAbsolutePath());
            } else {
                e.onComplete();
            }
            LogUtil.e("ImageUriRequest","查找自定义封面耗时:" + (System.currentTimeMillis() - start1));
            start2 = System.currentTimeMillis();
        }).switchIfEmpty(observer -> {
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mAlbum.getAlbumID());
            if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
//                    observer.onNext(uri.toString());
                observer.onComplete();
            }else {
                observer.onComplete();
            }
            LogUtil.e("ImageUriRequest","查找内嵌封面耗时:" + (System.currentTimeMillis() - start2));
        }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                .getNeteaseSearch(mAlbum.getAlbum(),0,1,10)
                .doOnSubscribe(disposable -> {
                    //延迟 避免接口请求频繁
//                    Thread.sleep(50);
                })
                .map(body -> {
                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                    return response.result.albums.get(0).picUrl;
                }))
        .compose(RxUtil.applyScheduler())
        .subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                LogUtil.e("ImageUriRequest","耗时:" + (System.currentTimeMillis() - start1));
                LogUtil.d("AlbumUriRequest","Pos:" + mPosition + " APositon:" + mViewHolder.getAdapterPosition() + " LPosition:" + mViewHolder.getLayoutPosition());
                onSuccess(s);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                onSuccess("res://remix.myplayer/" + (ThemeStore.isDay() ? R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night));
            }
        });
        if(true)
            return;

        //是否有自定义封面
//        long start = System.currentTimeMillis();
//        Handler handler = new Handler(Looper.getMainLooper());
//        new Thread(){
//            @Override
//            public void run() {
//                File customImage = ImageUriUtil.getCustomCoverIfExist(mAlbum.getAlbumID(), Constants.URL_ALBUM);
//                if(customImage != null && customImage.exists()){
//                    handler.post(() -> onSuccess("file://" + customImage.getAbsolutePath()));
//                    return;
//                }
//                LogUtil.e("ImageUriRequest","查找自定义封面耗时:" + (System.currentTimeMillis() - start));
//
//                long start1 = System.currentTimeMillis();
//                //是否存在于数据库
//                Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mAlbum.getAlbumID());
//                boolean existInMediaStore = ImageUriUtil.isAlbumThumbExistInMediaCache(uri);
//                LogUtil.e("ImageUriRequest","查找内嵌封面耗时:" + (System.currentTimeMillis() - start1));
//                if(existInMediaStore){
//                    handler.post(() -> onSuccess(uri.toString()));
//                } else {
//                    //获得网易的封面
//                    HttpClient.getNeteaseApiservice()
//                            .getNeteaseSearch(mAlbum.getAlbum(),0,1,10)
//                            .compose(RxUtil.applyScheduler())
//                            .subscribe(new Consumer<ResponseBody>() {
//                                @Override
//                                public void accept(ResponseBody body) throws Exception {
//                                    NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
//                                    if(response != null && response.result.albums != null && response.result.albums.size() > 0)
//                                        handler.post(() -> onSuccess(response.result.albums.get(0).picUrl));
//                                }
//                            }, new Consumer<Throwable>() {
//                                @Override
//                                public void accept(Throwable throwable) throws Exception {
//                                    ToastUtil.show(APlayerApplication.getContext(),"onError:" + throwable);
//                                }
//                            });
//                }
//            }
//        }.start();


    }


}
