package remix.myplayer.service

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/29 09:12
 */

interface Playback {
    fun playSelectSong(position: Int)

    fun toggle()

    fun playNext()

    fun playPrevious()

    fun play(fadeIn: Boolean)

    fun pause(updateMediasessionOnly: Boolean)
}
