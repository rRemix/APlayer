package remix.myplayer.bean.misc

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import remix.myplayer.bean.mp3.APlayerModel

@Parcelize
data class CustomCover(
    val model: APlayerModel,
    val type: Int) : Parcelable