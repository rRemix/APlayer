package remix.myplayer.bean.mp3

import android.os.Parcelable

/**
 * created by Remix on 2021/4/30
 */

interface APlayerModel : Parcelable {
  fun getKey(): String
}