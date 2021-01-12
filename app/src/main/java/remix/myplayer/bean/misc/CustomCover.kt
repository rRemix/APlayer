package remix.myplayer.bean.misc

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CustomCover(
    val id: Long,
    val type: Int,
    val key: String) : Parcelable