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
     * #EXTM3U
    #EXTINF:202580,5 Seconds of Summer - She Looks So Perfect
    /storage/emulated/0/Music/欧美/She Looks So Perfect.mp3
    #EXTINF:215406,복면가왕 - 07. 매일 매일 기다려 (티삼스)
    /storage/emulated/0/Music/日韩/ (河铉雨) -    (每天每天等待着) (Live).mp3
    #EXTINF:312242,薛之谦 - 暧昧
    /storage/emulated/0/Music/测试/薛之谦 - 暧昧.mp3
    #EXTINF:272692,MISIA - Angel
    /storage/emulated/0/Music/日韩/Angel.mp3
    #EXTINF:283115,<unknown> - Angel-live
    /storage/emulated/0/Music/日韩/Angel-live.mp3
    #EXTINF:266632,아이유 - 斑马，斑马
    /storage/emulated/0/Music/日韩/ (IU) - 斑马，斑马 (Live).mp3
    #EXTINF:326165,华语群星 - 半缘修道》ft. 若菲飞
    /storage/emulated/0/Music/测试/兔裹煎蛋卷若菲飞 - 半缘修道.mp3
    #EXTINF:199445,Jessie J/Ariana Grande/Nicki Minaj - Bang Bang
    /storage/emulated/0/Music/欧美/Bang Bang.mp3
    #EXTINF:216294,Linkin Park - Battle Symphony
    /storage/emulated/0/Music/One More Night/Battle Symphony.mp3
    #EXTINF:222067,Kelly Clarkson - Because Of You
    /storage/emulated/0/Music/欧美/Because Of You.mp3
    #EXTINF:226821,Rammstein - AMERIKA
    /storage/emulated/0/Music/Rammstein/Amerika.mp3
     */

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