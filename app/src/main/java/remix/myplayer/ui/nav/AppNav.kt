package remix.myplayer.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import remix.myplayer.compose.ui.screen.SupportScreen
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.ui.dialog.DialogContainer
import remix.myplayer.ui.screen.AboutScreen
import remix.myplayer.ui.screen.CustomSortScreen
import remix.myplayer.ui.screen.EQScreen
import remix.myplayer.ui.screen.HomeScreen
import remix.myplayer.ui.screen.LastAddedScreen
import remix.myplayer.ui.screen.SearchScreen
import remix.myplayer.ui.screen.SongChooserScreen
import remix.myplayer.ui.screen.crop.CropScreen
import remix.myplayer.ui.screen.detail.DetailScreen
import remix.myplayer.ui.screen.history.HistoryScreen
import remix.myplayer.ui.screen.playing.PlayingScreen
import remix.myplayer.ui.screen.setting.SettingScreen
import remix.myplayer.ui.screen.webdav.WebDavDetailScreen
import remix.myplayer.ui.screen.smb.SmbDetailScreen
import remix.myplayer.data.db.room.entity.Smb
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

const val RouteHome = "home"
const val RouteSetting = "setting"
const val RouteSongChoose = "song_choose"
const val RoutePlayingScreen = "playing_screen"
const val RouteAbout = "about"
const val RouteCustomSort = "custom_sort"
const val RouteLastAdded = "last_added"
const val RouteHistory = "history"
const val RouteSearch = "search"
const val RouteWebDav = "webdav"
const val RouteSmb = "smb"
const val RouteCrop = "crop"
const val RouteEq = "eq"
const val RouteSupport = "support"

val playingScreenDeepLink = "aplayer://playingScreen".toUri()

@Composable
fun AppNav() {
  val snackBarHostState = remember { SnackbarHostState() }
  ProvideSnackBarHostState(snackBarHostState) {
    Box(modifier = Modifier.fillMaxSize()) {
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

        normalAnimatedScreen(
          "${RouteSongChoose}/{id}/{name}",
          arguments = listOf(navArgument("id") {
            type = NavType.LongType
          })
        ) {
          val id = it.arguments?.getLong("id") ?: return@normalAnimatedScreen
          val name = it.arguments?.getString("name") ?: return@normalAnimatedScreen
          SongChooserScreen(id, name)
        }

        normalAnimatedScreen(RouteAbout) {
          AboutScreen()
        }

        composable<DetailScreenRoute>(
          typeMap = mapOf(
            typeOf<Album?>() to ModelRouteType.album,
            typeOf<Artist?>() to ModelRouteType.artist,
            typeOf<Genre?>() to ModelRouteType.genre,
            typeOf<PlayList?>() to ModelRouteType.playList,
            typeOf<Folder?>() to ModelRouteType.folder,
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

        normalAnimatedScreen(RouteLastAdded) {
          LastAddedScreen()
        }

        normalAnimatedScreen(RouteHistory) {
          HistoryScreen()
        }

        normalAnimatedScreen(RouteSearch) {
          SearchScreen()
        }

        composable<WebDav>(
          enterTransition = enterTransition(),
          exitTransition = exitTransition(),
          popEnterTransition = popEnterTransition(),
          popExitTransition = popExitTransition(),
        ) {
          val webDav = it.toRoute<WebDav>()
          WebDavDetailScreen(webDav)
        }

        composable<Smb>(
          enterTransition = enterTransition(),
          exitTransition = exitTransition(),
          popEnterTransition = popEnterTransition(),
          popExitTransition = popExitTransition(),
        ) {
          val smb = it.toRoute<Smb>()
          SmbDetailScreen(smb)
        }

        normalAnimatedScreen(
          "${RouteCrop}/{id}/{type}",
          arguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("type") { type = NavType.IntType })
        ) {
          val id = it.arguments?.getLong("id") ?: return@normalAnimatedScreen
          val type = it.arguments?.getInt("type") ?: return@normalAnimatedScreen
          CropScreen(id, type)
        }

        normalAnimatedScreen(RouteEq) {
          EQScreen()
        }

        normalAnimatedScreen(RouteSupport) {
          SupportScreen()
        }
      }

      SnackbarHost(
        hostState = snackBarHostState,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues())
      )
    }

    LaunchedEffect(Unit) {
      MessageNotifier.messages.collect {
        snackBarHostState.currentSnackbarData?.dismiss()
        snackBarHostState.showSnackbar(it)
      }
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

private object ModelRouteType {

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
