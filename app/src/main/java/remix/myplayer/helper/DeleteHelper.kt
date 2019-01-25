package remix.myplayer.helper

import io.reactivex.Single
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.util.MediaStoreUtil

object DeleteHelper {
  /**
   * 在曲库列表删除歌曲
   */
  @JvmStatic
  fun deleteSongs(songIds: List<Int>, deleteSource: Boolean, playlistId: Int = -1, deletePlaylist: Boolean = false):
      Single<Boolean> {
    return Single
        .fromCallable {
          MediaStoreUtil.delete(MediaStoreUtil.getSongsByIds(songIds), deleteSource)
        }
        .flatMap {
          if (deletePlaylist) {
            DatabaseRepository.getInstance()
                .deletePlayList(playlistId)
          } else {
            Single.just(it)
          }
        }
        .map {
          it > 0
        }

  }

  /**
   * 在列表内(如专辑、艺术家列表内删除歌曲)
   */
  @JvmStatic
  fun deleteSong(songId: Int, deleteSource: Boolean, deleteFromPlayList: Boolean = false, playListName: String = ""):
      Single<Boolean> {
    return Single
        .fromCallable {
          if (!deleteFromPlayList)
            MediaStoreUtil
                .delete(listOf(MediaStoreUtil.getSongById(songId)), deleteSource)
          else
            DatabaseRepository.getInstance()
                .deleteFromPlayList(arrayListOf(songId), playListName)
                .blockingGet()
        }
        .map {
          it > 0
        }
        .compose(applySingleScheduler())
  }
}