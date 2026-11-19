package remix.myplayer.ui.nav

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import androidx.savedstate.SavedState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.Song
import remix.myplayer.ui.dialog.DialogContainer
import remix.myplayer.ui.screen.AboutScreen
import remix.myplayer.ui.screen.CustomSortScreen
import remix.myplayer.ui.screen.EQScreen
import remix.myplayer.ui.screen.LastAddedScreen
import remix.myplayer.ui.screen.SearchScreen
import remix.myplayer.ui.screen.SongChooserScreen
import remix.myplayer.ui.screen.SupportScreen
import remix.myplayer.ui.screen.TagEditScreen
import remix.myplayer.ui.screen.crop.CropScreen
import remix.myplayer.ui.screen.detail.DetailScreen
import remix.myplayer.ui.screen.history.HistoryScreen
import remix.myplayer.ui.screen.home.HomeScreen
import remix.myplayer.ui.screen.playing.PlayingPanel
import remix.myplayer.ui.screen.setting.SettingDetailScreen
import remix.myplayer.ui.screen.setting.SettingScreen
import remix.myplayer.ui.screen.setting.ReplayGainSettingScreen
import remix.myplayer.ui.screen.smb.SmbDetailScreen
import remix.myplayer.ui.screen.webdav.WebDavDetailScreen
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.typeOf

const val RouteHome = "home"
const val RouteSetting = "setting"
const val RouteSettingDetail = "setting_detail"
const val RouteReplayGain = "replay_gain"
const val RouteSongChoose = "song_choose"
const val RouteAbout = "about"
const val RouteCustomSort = "custom_sort"
const val RouteLastAdded = "last_added"
const val RouteHistory = "history"
const val RouteSearch = "search"
const val RouteTagEdit = "tag_edit"
const val RouteCustomCoverCrop = "custom_cover_crop"
const val RouteTagEditCrop = "tag_edit_crop"
const val RouteEq = "eq"
const val RouteSupport = "support"

const val RoutePlaying = "playing"

val playingScreenDeepLink = "aplayer://playingScreen".toUri()

@Composable
fun AppNav() {
  val snackBarHostState = remember { SnackbarHostState() }
  ProvideSnackBarHostState(snackBarHostState) {
    Box(modifier = Modifier.fillMaxSize().background(LocalTheme.current.mainBackground)) {
      DialogContainer()

      PredictiveNavHost(
        navController = LocalNavController.current,
        startDestination = RouteHome,
        backgroundColor = LocalTheme.current.mainBackground,
      ) {
        composable(
          RouteHome,
        ) {
          HomeScreen()
        }

        composable(RoutePlaying) {
          PlayingPanel()
        }

        composable(RouteSetting) {
          SettingScreen()
        }

        composable(
          "$RouteSettingDetail/{category}",
          arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
          val category = it.arguments?.getString("category") ?: return@composable
          SettingDetailScreen(category)
        }

        composable(RouteReplayGain) {
          ReplayGainSettingScreen()
        }

        composable(
          "${RouteSongChoose}/{id}/{name}", arguments = listOf(navArgument("id") {
            type = NavType.LongType
          })
        ) {
          val id = it.arguments?.getLong("id") ?: return@composable
          val name = Uri.decode(it.arguments?.getString("name") ?: return@composable)
          SongChooserScreen(id, name)
        }

        composable(RouteAbout) {
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
        ) {
          val route = it.toRoute<DetailScreenRoute>()

          DetailScreen(route.findNotNull(), openFromLibrary = route.openFromLibrary)
        }

        composable("${RouteCustomSort}/{id}", arguments = listOf(navArgument("id") {
          type = NavType.LongType
        })) {
          val id = it.arguments?.getLong("id") ?: return@composable
          CustomSortScreen(id)
        }

        composable(RouteLastAdded) {
          LastAddedScreen()
        }

        composable(RouteHistory) {
          HistoryScreen()
        }

        composable(RouteSearch) {
          SearchScreen()
        }

        composable(RouteTagEdit) {
          TagEditScreen(it)
        }

        composable<WebDav> {
          val webDav = it.toRoute<WebDav>()
          WebDavDetailScreen(webDav)
        }

        composable<Smb> {
          val smb = it.toRoute<Smb>()
          SmbDetailScreen(smb)
        }

        composable(
          "${RouteCustomCoverCrop}/{id}/{type}",
          arguments = listOf(
            navArgument("id") { type = NavType.LongType },
            navArgument("type") { type = NavType.IntType })
        ) {
          val id = it.arguments?.getLong("id") ?: return@composable
          val type = it.arguments?.getInt("type") ?: return@composable
          val context = LocalContext.current
          val nav = LocalNavController.current
          val libraryVM = libraryViewModel

          val destinationUri = remember(id, type) {
            val cacheDir = File(context.cacheDir, "thumbnail")
            if (!cacheDir.isDirectory && !cacheDir.mkdirs()) {
              Uri.EMPTY
            } else {
              val file = File(cacheDir, "$type-${id}.jpg")
              Uri.fromFile(file)
            }
          }

          CropScreen(destinationUri = destinationUri, onCropSuccess = {
            libraryVM.invalidateCover(type, id)
            nav.popBackStack()
          }, onCancel = {
            nav.popBackStack()
          })
        }

        composable(
          "$RouteTagEditCrop/{uri}",
          arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) {
          val uriString = it.arguments?.getString("uri") ?: return@composable
          val destinationUri = Uri.decode(uriString).toUri()
          val nav = LocalNavController.current

          CropScreen(destinationUri = destinationUri, onCropSuccess = {
            nav.previousBackStackEntry?.savedStateHandle?.set(
              "song_crop_result", System.currentTimeMillis()
            )
            nav.popBackStack()
          }, onCancel = {
            nav.popBackStack()
          })
        }

        composable(RouteEq) {
          EQScreen()
        }

        composable(RouteSupport) {
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

    val nav = LocalNavController.current
    val mainVM = mainViewModel
    LaunchedEffect(nav, mainVM) {
      mainVM.openPlayingScreenRequests.collect { requested ->
        if (requested) {
          // The entry can change while a notification waits for a gesture/transition to finish.
          while (true) {
            val entry = nav.currentBackStackEntryFlow.first()
            val state = entry.lifecycle.currentStateFlow.first {
              it == Lifecycle.State.RESUMED || it == Lifecycle.State.DESTROYED
            }
            if (state == Lifecycle.State.RESUMED && nav.currentBackStackEntry == entry) break
          }
          nav.openPlayingScreen(animate = false)
          mainVM.consumePlayingScreenRequest()
        }
      }
    }
  }
}

@Serializable
data class DetailScreenRoute(
  val album: Album? = null,
  val artist: Artist? = null,
  val genre: Genre? = null,
  val playList: PlayList? = null,
  val folder: Folder? = null,
  val openFromLibrary: Boolean = false
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

/** Only local songs carry MediaStore album/artist ids that map to a library detail page. */
private val Song.canOpenLibraryDetail: Boolean
  get() = valid() && isLocal()

fun Song.artistDetailRoute(): DetailScreenRoute? =
  if (canOpenLibraryDetail && artistId > 0 && artist.isNotBlank()) {
    DetailScreenRoute(artist = Artist(artistId, artist, 0))
  } else null

fun Song.albumDetailRoute(): DetailScreenRoute? =
  if (canOpenLibraryDetail && albumId > 0 && album.isNotBlank()) {
    DetailScreenRoute(album = Album(albumId, album, artistId, artist))
  } else null

/** True when [entry] already shows the album/artist detail page [route] points to. */
internal fun NavBackStackEntry.isSameDetail(route: DetailScreenRoute): Boolean {
  if (!destination.hasRoute<DetailScreenRoute>()) return false
  val current = toRoute<DetailScreenRoute>()
  return (route.album != null && route.album.albumID == current.album?.albumID) ||
    (route.artist != null && route.artist.artistID == current.artist?.artistID)
}

/** Open a song's library detail, reusing the current page or the source directly under the player. */
fun NavHostController.openSongLibraryDetail(route: DetailScreenRoute) {
  val current = currentBackStackEntry ?: return
  // Ignore taps while a previous navigation transition is still running.
  if (current.lifecycle.currentState != Lifecycle.State.RESUMED) return
  if (current.isSameDetail(route)) return
  if (current.destination.route == RoutePlaying && previousBackStackEntry?.isSameDetail(route) == true) {
    popBackStack()
  } else {
    navigate(route)
  }
}

/** Reuse the player already in this stack instead of stacking another player above its children. */
fun NavHostController.openPlayingScreen(animate: Boolean = true) {
  val current = currentBackStackEntry ?: return
  if (current.lifecycle.currentState != Lifecycle.State.RESUMED) return
  if (current.destination.route == RoutePlaying) return
  if (runCatching { getBackStackEntry(RoutePlaying) }.isSuccess) {
    popBackStack(RoutePlaying, inclusive = false)
  } else {
    navigate(RoutePlaying) { launchSingleTop = true }
  }
  if (!animate) {
    currentBackStackEntry?.savedStateHandle?.set(SkipNextTransition, true)
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
