package remix.myplayer.bean.mp3

/**
 * Created by Remix on 2017/10/22.
 */

data class Album(val albumID: Int,
                 val album: String,
                 val artistID: Int,
                 val artist: String,
                 val count: Int = 0)
