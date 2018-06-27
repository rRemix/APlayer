package remix.myplayer.ui.activity.artist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.functions.Consumer
import remix.myplayer.bean.lastfm.LastFmArtist
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.network.HttpClient
import remix.myplayer.request.network.RxUtil
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import java.util.concurrent.Executors

class ArtistViewModel : ViewModel(){
    private val mArtistId = MutableLiveData<Int>()
    private val mArtistName = MutableLiveData<String>()

    fun getArtistIntro():LiveData<LastFmArtist>{
        return Transformations.switchMap(mArtistName) {
            LastFMArtistLiveData(it)
        }
    }

    fun getSongs(): LiveData<List<Song>>{
        return Transformations.switchMap(mArtistId) {
            SongLiveData(it)
        }
    }

    fun getArtist():LiveData<Artist?>{
        return Transformations.switchMap(mArtistId) {
            ArtistLiveData(it)
        }
    }

    fun getAlbums(): LiveData<List<Album>>{
        return Transformations.switchMap(mArtistId) {
            AlbumLiveData(it)
        }
    }

    fun setArtistID(id:Int){
        mArtistId.value = id
    }

    fun setArtistName(name:String){
        mArtistName.value = name
    }

    class ArtistLiveData(val id:Int) : LiveData<Artist?>(),Runnable{
        init {
            Executors.newSingleThreadExecutor().execute(this)
        }

        override fun run() {
            Single.fromCallable {
                MediaStoreUtil.getArtist(id)
            }.subscribe(Consumer {
                postValue(it)
            })
        }
    }

    class AlbumLiveData(val id:Int) : LiveData<List<Album>>(),Runnable{
        init {
            Executors.newSingleThreadExecutor().execute(this)
        }

        override fun run() {
            Single.fromCallable {
                MediaStoreUtil.getAlbum(id)
            }.subscribe(Consumer {
                it.addAll(it)
                it.addAll(it)
                it.addAll(it)
                postValue(it)
            })
        }
    }

    class SongLiveData(val id:Int) : LiveData<List<Song>>(),Runnable{
        init {
            Executors.newSingleThreadExecutor().execute(this)
        }

        override fun run() {
            Single.fromCallable {
                MediaStoreUtil.getMP3InfoByArg(id, Constants.ARTIST)
            }.subscribe(Consumer {
                it.addAll(it)
                it.addAll(it)
                it.addAll(it)
                postValue(it)
            })
        }
    }

    class LastFMArtistLiveData(val name:String) : LiveData<LastFmArtist>(),Runnable{
        init {
            Executors.newSingleThreadExecutor().execute(this)
        }

        override fun run() {
            HttpClient.getLastFMApiservice().getArtistInfo(name, null)
                    .compose(RxUtil.applyScheduler())
                    .subscribe({
                        val lastFmArtist = Gson().fromJson<LastFmArtist>(it.string(), LastFmArtist::class.java)
                        postValue(lastFmArtist)
                    }, {

                    })
        }
    }
}
