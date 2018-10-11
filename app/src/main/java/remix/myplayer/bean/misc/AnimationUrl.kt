package remix.myplayer.bean.misc

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AnimationUrl(
        var albumId: Int = 0,
        var url: String? = null) : Parcelable
