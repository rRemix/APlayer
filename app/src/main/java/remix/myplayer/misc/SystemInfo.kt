package remix.myplayer.misc

import android.os.Build

object SystemInfo {
    private val supportedAbis: Array<String> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS
        } else {
            @Suppress("deprecation")
            arrayOf(Build.CPU_ABI, Build.CPU_ABI2)
        }
    }

    val prettyPrinted: String by lazy {
        """
            DISPLAY: ${Build.DISPLAY}
            SUPPORTED_ABIS: ${supportedAbis.contentToString()}
            MANUFACTURER: ${Build.MANUFACTURER}
            MODEL: ${Build.MODEL}
            RELEASE: ${Build.VERSION.RELEASE}
            SDK_INT: ${Build.VERSION.SDK_INT}
        """.trimIndent()
    }
}
