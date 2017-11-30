package remix.myplayer.misc.imae;

import android.text.TextUtils;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import remix.myplayer.lyric.network.RxUtil;
import remix.myplayer.model.mp3.PlayList;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;

import static remix.myplayer.util.MediaStoreUtil.getAlbumUrlByAlbumId;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends ImageUriRequest {
    private PlayList mPlayList;
    public PlayListUriRequest(SimpleDraweeView image, PlayList playList) {
        super(image);
        mPlayList = playList;
    }

    @Override
    public void load() {
        Observable.create((ObservableOnSubscribe<String>) e -> {
            File customImage = ImageUriUtil.getCustomCoverIfExist(mPlayList.getId(), Constants.URL_PLAYLIST);
            if(customImage != null && customImage.exists()){
                e.onNext("file://" + customImage.getAbsolutePath());
            }
            e.onComplete();
        }).switchIfEmpty(new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> observer) {
                //没有设置过封面，对于播放列表类型的查找播放列表下所有歌曲，直到有一首歌曲存在封面
                List<Integer> songIdList = PlayListUtil.getIDList(mPlayList.getId());
                if(songIdList == null || songIdList.size() == 0){
                    observer.onError(new Throwable("该列表下无歌曲"));
                    return;
                }
                for (Integer songId : songIdList){
                    Song item = MediaStoreUtil.getMP3InfoById(songId);
                    if(item == null)
                        continue;
                    String imgUrl = getAlbumUrlByAlbumId(item.getAlbumId());
                    if(!TextUtils.isEmpty(imgUrl)) {
                        File playlistImgFile = new File(imgUrl);
                        if(playlistImgFile.exists()) {
                            observer.onNext("file://" + playlistImgFile.getAbsolutePath());
                            return;
                        }
                    }
                }
                observer.onComplete();
            }
        }).compose(RxUtil.applyScheduler())
        .subscribe(this::onSuccess,throwable -> onError(throwable.toString()));
    }
}
