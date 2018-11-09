package remix.myplayer.misc.exception

class MultipChoiceException(cause: Throwable?, val multiChoice: String) : Exception(cause) {

}