package remix.myplayer.misc.interfaces

import remix.myplayer.bean.mp3.Song

interface OnTagEditListener{
    fun onTagEdit(newSong:Song?)
}
