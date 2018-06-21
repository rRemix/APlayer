package remix.myplayer.helper

import io.reactivex.Observable
import org.jetbrains.anko.collections.forEachWithIndex
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.request.network.RxUtil
import remix.myplayer.util.Global
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.PlayListUtil
import remix.myplayer.util.ToastUtil
import java.io.File

object M3UHelper {
    private val TAG = "M3UHelper"

    private val EXTENSION = "m3u"
    private val HEADER = "#EXTM3U"
    private val ENTRY = "#EXTINF:"
    private val DURATION_SEPARATOR = ","

    /**
     * 导入歌单
     */
    fun importM3UFile(file: File, playlistName: String, newCreate: Boolean){
        Observable.just(file).filter({ it.isFile && it.canRead() })
                .doOnSubscribe({
                    if(newCreate){
                        val playListId = PlayListUtil.addPlayList(playlistName)
                        if(playListId == -1){
                            throw Throwable("name must not be empty")
                        }
                        if(playListId == -2){
                            throw Throwable("$playlistName already exist")
                        }
                    }
                })
                .map {
                    val playlistSongs = ArrayList<Int>()
                    it.readLines().forEachWithIndex({ i: Int, path: String ->
                        if(i != 0 && !path.startsWith(ENTRY)){
                            val song = File(path)
                            if(song.exists() && song.isFile){
                               playlistSongs.add(MediaStoreUtil.getSongIdByUrl(path))
                            }
                        }
                    })
                    return@map PlayListUtil.addMultiSongs(playlistSongs,playlistName)
                }
                .compose(RxUtil.applyScheduler())
                .subscribe({
                    ToastUtil.show(App.getContext(), App.getContext().getString(R.string.import_playlist_to_count,playlistName,it))
                }, {
                    ToastUtil.show(App.getContext(),R.string.import_fail,it.toString())
                })
    }

    fun importLocalPlayList(map: Map<String, List<Int>>, select: Array<CharSequence>) {
        var count = 0
        Observable.fromIterable(map.entries)
                .filter({
                    select.contains(it.key)
                })
                .map {
                    var exist = false
                    Global.PlayList.forEach { playlist ->
                        if(playlist.name == it.key){
                            exist = true
                            return@forEach
                        }
                    }
                    if(!exist){
                        val playListId = PlayListUtil.addPlayList(it.key)
                        if(playListId == -1){
                            throw Throwable("name must not be empty")
                        }
                        if(playListId == -2){
                            throw Throwable("${it.key} already exist")
                        }
                    }
                    PlayListUtil.addMultiSongs(it.value,it.key)
                }
                .compose(RxUtil.applyScheduler())
                .doFinally {
                    ToastUtil.show(App.getContext(),R.string.import_count,count.toString())
                }
                .subscribe({
                    count += it
                }, {
                    ToastUtil.show(App.getContext(),R.string.import_fail,it.toString())
                })
    }
}