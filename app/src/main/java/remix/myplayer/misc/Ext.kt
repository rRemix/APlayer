package remix.myplayer.misc

import android.provider.MediaStore
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.PlayList
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.PlayListUtil

fun Album.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ALBUM_ID + "=?", arrayOf((albumID.toString())))
}

fun Artist.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ARTIST_ID + "=?", arrayOf(artistID.toString()))
}

fun Folder.getSongIds(): List<Int> {
    return MediaStoreUtil.getSongIdsByParentId(parentId)
}

fun PlayList.getSongIds(): List<Int> {
    return PlayListUtil.getSongIds(_Id)
}
