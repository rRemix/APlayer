package remix.myplayer.misc.update

import remix.myplayer.bean.github.Release

interface Listener {
    fun onUpdateReturned(code: Int, message: String, release: Release?)
    fun onUpdateError(throwable: Throwable)
}
