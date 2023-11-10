package remix.myplayer.bean.mp3

import java.io.Serializable

/**
 * created by Remix on 2021/4/30
 */

interface APlayerModel: Serializable {
  fun getKey(): String
}