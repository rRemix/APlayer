package remix.myplayer.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import remix.myplayer.bean.mp3.Song
import java.io.InputStream

//TODO Album Artist PlayList
class APlayerUriLoader(private val concreteLoader: ModelLoader<Uri, InputStream>) : ModelLoader<Song, InputStream> {
  override fun buildLoadData(song: Song, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
    val uri = UriFetcher.getInstance().fetch(song)
    return concreteLoader.buildLoadData(uri, width, height, options)
  }

  override fun handles(song: Song): Boolean {
    return true
  }
}