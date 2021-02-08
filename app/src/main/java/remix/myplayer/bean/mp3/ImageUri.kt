package remix.myplayer.bean.mp3

import android.net.Uri
import com.bumptech.glide.load.Key

interface ImageUri : Key {
  fun getImageUri(): Uri
}