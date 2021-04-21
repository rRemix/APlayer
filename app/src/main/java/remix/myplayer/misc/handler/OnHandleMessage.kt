package remix.myplayer.misc.handler

/**
 * Created by Remix on 2017/11/16.
 */
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class OnHandleMessage(val what: Int = 0, val action: String = "")