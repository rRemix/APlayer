package remix.myplayer.request;

import android.content.ContentUris;
import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.lyric.network.HttpClient;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.model.mp3.PlayList;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.model.netease.NAlbumSearchResponse;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends ImageUriRequest {
    private PlayList mPlayList;
    public PlayListUriRequest(SimpleDraweeView image, PlayList playList) {
        super(image);
        mPlayList = playList;
    }

    public PlayListUriRequest(SimpleDraweeView image, PlayList playList,RequestConfig config) {
        super(image,config);
        mPlayList = playList;
    }

    @Override
    public void load() {
        Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomThumbIfExist(mPlayList.getId(), Constants.URL_PLAYLIST);
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                //没有设置过封面，对于播放列表类型的查找播放列表下所有歌曲，直到有一首歌曲存在封面
                List<Song> songs = PlayListUtil.getMP3ListByIds(PlayListUtil.getIDList(mPlayList.getId()));
                if(songs == null || songs.size() == 0){
                    observer.onError(new Throwable(APlayerApplication.getContext().getString(R.string.no_song)));
                    return;
                }

                Observable.fromIterable(songs)
                        .flatMap(song -> {
                            Album album = new Album(song.getAlbumId(),song.getAlbum(),0,song.getArtist());
                            return  Observable.create((ObservableOnSubscribe<String>) e -> {
                                File customImage = ImageUriUtil.getCustomThumbIfExist(album.getAlbumID(), Constants.URL_ALBUM);
                                if(customImage != null && customImage.exists()){
                                    e.onNext("file://" + customImage.getAbsolutePath());
                                }
                                e.onComplete();
                                }).switchIfEmpty(new Observable<String>() {
                                    @Override
                                    protected void subscribeActual(Observer<? super String> observer1) {
                                        Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), album.getAlbumID());
                                        if(ImageUriUtil.isAlbumThumbExistInMediaCache(uri)){
                                            observer1.onNext(uri.toString());
                                        } else {
                                            if(mConfig.isForceDownload()){
                                                observer1.onComplete();
                                            } else{
                                                observer1.onError(new Throwable(""));
                                            }
                                        }
                                    }
                                }).switchIfEmpty(HttpClient.getNeteaseApiservice()
                                    .getNeteaseSearch(album.getAlbum(),0,1,10)
                                    .map(body -> {
                                        NAlbumSearchResponse response = new Gson().fromJson(body.string(), NAlbumSearchResponse.class);
                                        return response.result.albums.get(0).picUrl;
                                    }));
                        })
                        .subscribe(s -> {
                            observer.onNext(s);
                            observer.onComplete();
                        }, throwable -> observer.onError(new Throwable(APlayerApplication.getContext().getString(R.string.no_song))));

//                List<Integer> songIdList = PlayListUtil.getIDList(mPlayList.getId());
//                if(songIdList == null || songIdList.size() == 0){
//                    observer.onError(new Throwable(APlayerApplication.getContext().getString(R.string.no_song)));
//                    return;
//                }
            }
        }).compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess,throwable -> onError(throwable.toString()));
    }
}
