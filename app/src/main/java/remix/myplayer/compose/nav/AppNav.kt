package remix.myplayer.compose.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.compose.ui.dialog.DialogContainer
import remix.myplayer.compose.ui.screen.AboutScreen
import remix.myplayer.compose.ui.screen.CustomSortScreen
import remix.myplayer.compose.ui.screen.history.HistoryScreen
import remix.myplayer.compose.ui.screen.HomeScreen
import remix.myplayer.compose.ui.screen.LastAddedScreen
import remix.myplayer.compose.ui.screen.SongChooseScreen
import remix.myplayer.compose.ui.screen.detail.DetailScreen
import remix.myplayer.compose.ui.screen.playing.PlayingScreen
import remix.myplayer.compose.ui.screen.setting.SettingScreen
import remix.myplayer.db.room.model.PlayList
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

const val RouteHome = "home"
const val RouteSetting = "setting"
const val RouteSongChoose = "song_choose"
const val RoutePlayingScreen = "playing_screen"
const val RouteAbout = "about"
const val RouteCustomSort = "custom_sort"
const val RouterLastAdded = "last_added"
const val RouterHistory = "history"

val playingScreenDeepLink = "aplayer://playingScreen".toUri()

@Composable
fun AppNav() {
  DialogContainer()

  NavHost(LocalNavController.current, startDestination = RouteHome) {
    normalAnimatedScreen(
      RouteHome,
    ) {
      HomeScreen()
    }

    normalAnimatedScreen(RouteSetting) {
      SettingScreen()
    }

    normalAnimatedScreen("${RouteSongChoose}/{id}/{name}", arguments = listOf(navArgument("id") {
      type = NavType.LongType
    })) {
      val id = it.arguments?.getLong("id") ?: return@normalAnimatedScreen
      val name = it.arguments?.getString("name") ?: return@normalAnimatedScreen
      SongChooseScreen(id, name)
    }

    normalAnimatedScreen(RouteAbout) {
      AboutScreen()
    }

    composable<DetailScreenRoute>(
      typeMap = mapOf(
        typeOf<Album?>() to DetailScreenRouteType.album,
        typeOf<Artist?>() to DetailScreenRouteType.artist,
        typeOf<Genre?>() to DetailScreenRouteType.genre,
        typeOf<PlayList?>() to DetailScreenRouteType.playList,
        typeOf<Folder?>() to DetailScreenRouteType.folder,
      ),
      enterTransition = enterTransition(),
      exitTransition = exitTransition(),
      popEnterTransition = popEnterTransition(),
      popExitTransition = popExitTransition(),
    ) {
      val route = it.toRoute<DetailScreenRoute>()

      DetailScreen(route.findNotNull())
    }

    composable(
      RoutePlayingScreen,
      deepLinks = listOf(navDeepLink {
        uriPattern = playingScreenDeepLink.toString()
      }),
      // playingScreen has special animation
      enterTransition = {
        slideInFromBottom()
      },
      popExitTransition = {
        slideOutToBottom()

      }) {
      PlayingScreen()
    }

    normalAnimatedScreen("${RouteCustomSort}/{id}", arguments = listOf(navArgument("id") {
      type = NavType.LongType
    })) {
      val id = it.arguments?.getLong("id") ?: return@normalAnimatedScreen
      CustomSortScreen(id)
    }

    normalAnimatedScreen(RouterLastAdded) {
      LastAddedScreen()
    }

    normalAnimatedScreen(RouterHistory) {
      HistoryScreen()
    }
  }
}

private fun NavGraphBuilder.normalAnimatedScreen(
  route: String,
  arguments: List<NamedNavArgument> = emptyList(),
  deepLinks: List<NavDeepLink> = emptyList(),
  content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
  composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = enterTransition(),
    exitTransition = exitTransition(),
    popEnterTransition = popEnterTransition(),
    popExitTransition = popExitTransition(),
    content = content
  )
}


@Serializable
data class DetailScreenRoute(
  val album: Album? = null,
  val artist: Artist? = null,
  val genre: Genre? = null,
  val playList: PlayList? = null,
  val folder: Folder? = null
) {

  fun findNotNull(): APlayerModel {
    return when {
      album != null -> album
      artist != null -> artist
      genre != null -> genre
      playList != null -> playList
      folder != null -> folder
      else -> error("valid model not found")
    }
  }

}

private object DetailScreenRouteType {

  val album = RouteType(Album::class)
  val artist = RouteType(Artist::class)
  val genre = RouteType(Genre::class)
  val playList = RouteType(PlayList::class)
  val folder = RouteType(Folder::class)

  @OptIn(InternalSerializationApi::class)
  class RouteType<T : APlayerModel>(private val kClass: KClass<T>) : NavType<T?>(true) {

    override fun put(bundle: SavedState, key: String, value: T?) {
      if (value != null) {
        bundle.putString(key, Json.encodeToString(kClass.serializer(), value))
      }
    }

    override fun get(bundle: SavedState, key: String): T? {
      return Json.decodeFromString(kClass.serializer(), bundle.getString(key) ?: return null)
    }

    override fun parseValue(value: String): T? {
      if (value.isEmpty()) {
        return null
      }
      return Json.decodeFromString(kClass.serializer(), Uri.decode(value))
    }

    override fun serializeAsValue(value: T?): String {
      if (value == null) {
        return Uri.EMPTY.toString()
      }
      return Uri.encode(Json.encodeToString(kClass.serializer(), value))
    }

  }

}
