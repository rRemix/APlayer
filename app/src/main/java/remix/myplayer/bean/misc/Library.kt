package remix.myplayer.bean.misc

import android.content.Context
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.ui.fragment.*

@Parcelize
data class Library(
    @SerializedName("mTag")
    val tag: Int,
    @SerializedName("mOrder")
    val order: Int = tag,
    @SerializedName("mClassName")
    val className: String = when (tag) {
      TAG_SONG -> SongFragment::class.java.name
      TAG_ALBUM -> AlbumFragment::class.java.name
      TAG_ARTIST -> ArtistFragment::class.java.name
      TAG_PLAYLIST -> PlayListFragment::class.java.name
      else -> FolderFragment::class.java.name
    }) : Parcelable {

  fun isPlayList(): Boolean {
    return className == PlayListFragment::class.java.name
  }

  fun getTitle(): String {
    return App.context.getString(
        when (tag) {
          TAG_SONG -> R.string.tab_song
          TAG_ALBUM -> R.string.tab_album
          TAG_ARTIST -> R.string.tab_artist
          TAG_PLAYLIST -> R.string.tab_playlist
          else -> R.string.tab_folder
        })
  }


  companion object {
    const val TAG_SONG = 0
    const val TAG_ALBUM = 1
    const val TAG_ARTIST = 2
    const val TAG_PLAYLIST = 3
    const val TAG_FOLDER = 4

    fun getDefaultLibrary(): List<Library> {
      return listOf(
          Library(TAG_SONG),
          Library(TAG_ALBUM),
          Library(TAG_ARTIST),
          Library(TAG_PLAYLIST),
          Library(TAG_FOLDER))
    }

    fun getAllLibraryString(context: Context): List<String> {
      return listOf(context.resources.getString(R.string.tab_song),
          context.resources.getString(R.string.tab_album),
          context.resources.getString(R.string.tab_artist),
          context.resources.getString(R.string.tab_playlist),
          context.resources.getString(R.string.tab_folder))
    }
  }

}