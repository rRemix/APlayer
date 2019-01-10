package remix.myplayer.helper

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.PlayListUtil

object DeleteHelper {
  /**
   * 在曲库列表删除歌曲
   */
  @JvmStatic
  fun deleteSongs(songIds: List<Int>, deleteSource: Boolean, playlistId: Int = -1, deletePlaylist: Boolean = false):
      Single<Boolean> {
    return Single
        .fromCallable {
          if (deleteSource || !deletePlaylist) {
            //如果删除的是播放列表
            if (deletePlaylist) {
              PlayListUtil.deletePlayList(playlistId)
            }
            MediaStoreUtil.delete(MediaStoreUtil.getSongsByIds(songIds), deleteSource) > 0
          } else {
            PlayListUtil.deletePlayList(playlistId)
          }
        }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
  }

  /**
   * 在列表内(如专辑、艺术家列表内删除歌曲)
   */
  @JvmStatic
  fun deleteSong(songId: Int, deleteSource: Boolean, deleteFromPlayList: Boolean = false, playListName: String = ""): Single<Boolean> {
    return Single
        .fromCallable {
          if (!deleteFromPlayList)
            MediaStoreUtil
                .delete(listOf(MediaStoreUtil.getSongById(songId)), deleteSource) > 0
          else
            PlayListUtil.deleteSong(songId, playListName)

          true
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}