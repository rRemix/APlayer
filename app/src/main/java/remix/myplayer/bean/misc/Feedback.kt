package remix.myplayer.bean.misc

data class Feedback(
    val appVersion: String,
    val appVersionCode: String,
    val cpuABI: String,
    val deviceManufacturer: String,
    val deviceModel: String,
    val releaseVersion: String,
    val sdkVersion: String,
    val display: String)