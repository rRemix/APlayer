package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2017/10/22.
 */

data class Album(val albumID: Long,
                 val album: String,
                 val artistID: Long,
                 val artist: String,
                 val count: Int = 0)
