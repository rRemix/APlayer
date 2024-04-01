package remix.myplayer.misc

import remix.myplayer.BuildConfig

object AppInfo {
    val prettyPrinted by lazy {
        """
            APPLICATION_ID: ${BuildConfig.APPLICATION_ID}
            VERSION_CODE: ${BuildConfig.VERSION_CODE}
            VERSION_NAME: ${BuildConfig.VERSION_NAME}
            FLAVOR: ${BuildConfig.FLAVOR}
            BUILD_TYPE: ${BuildConfig.BUILD_TYPE}
            GITHUB_SHA: ${BuildConfig.GITHUB_SHA}
        """.trimIndent()
    }
}
