package remix.myplayer.misc.exception

class MusicServiceException : Exception {
    constructor(cause: Throwable) : super(cause) {}

    constructor(message: String, cause: Throwable) : super(message, cause) {}
}
