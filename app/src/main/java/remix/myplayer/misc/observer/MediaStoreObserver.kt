package remix.myplayer.misc.observer

import android.content.Intent
import android.net.Uri
import remix.myplayer.service.MusicService
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.Util.sendLocalBroadcast

/**
 * Created by taeja on 16-3-30.
 */
class MediaStoreObserver
/**
 * Creates a content observer.
 *
 * @param service
 * @param handler The handler to run [.onChange] on, or null if none.
 */
(service: MusicService) : BaseObserver(service, null) {

    internal override fun onAccept(uri: Uri) {
        //        MusicServiceRemote.setAllSong(MediaStoreUtil.getAllSongsId());
        mService.get()?.allSong = MediaStoreUtil.getAllSongsId()
        sendLocalBroadcast(Intent(MusicService.MEDIA_STORE_CHANGE))
//        mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER)
    }

    internal override fun onFilter(uri: Uri?): Boolean {
        return uri != null && uri.toString().contains("content://media/")
    }
}
