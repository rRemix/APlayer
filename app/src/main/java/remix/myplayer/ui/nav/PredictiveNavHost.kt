package remix.myplayer.ui.nav

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.ComposeNavigatorDestinationBuilder
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.createGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.KType

private typealias ScreenContent = @Composable (NavBackStackEntry) -> Unit
private const val PreviewScrimAlpha = 0.32f
internal const val SkipNextTransition = "skip_next_transition"

/** Registers ordinary ComposeNavigator destinations without accessing its internal content API. */
internal class ScreenGraphBuilder(
  private val graph: NavGraphBuilder,
  private val contentByDestination: MutableMap<Int, ScreenContent>,
) {
  fun composable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: ScreenContent,
  ) = register(
    ComposeNavigatorDestinationBuilder(graph.provider.getNavigator("composable"), route) {
      content(it)
    }.apply {
      arguments.forEach { argument(it.name, it.argument) }
    },
    content,
  )

  inline fun <reified T : Any> composable(
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    noinline content: ScreenContent,
  ) = composable(T::class, typeMap, content)

  fun composable(
    route: KClass<*>,
    typeMap: Map<KType, NavType<*>>,
    content: ScreenContent,
  ) = register(
    ComposeNavigatorDestinationBuilder(graph.provider.getNavigator("composable"), route, typeMap) {
      content(it)
    },
    content,
  )

  private fun register(builder: ComposeNavigatorDestinationBuilder, content: ScreenContent) {
    val destination = builder.build()
    graph.addDestination(destination)
    contentByDestination[destination.id] = content
  }
}

private class BackPreview(
  val from: NavBackStackEntry,
  val to: NavBackStackEntry,
  val isVerticalDrag: Boolean = false,
) {
  var progress by mutableFloatStateOf(0f)
  var isSettling by mutableStateOf(false)
  var swipeEdge by mutableIntStateOf(BackEventCompat.EDGE_LEFT)
  var committed = false
}

private fun GraphicsLayerScope.applyBackPreview(entry: NavBackStackEntry, preview: BackPreview?) {
  scaleX = 1f
  scaleY = 1f
  translationX = 0f
  translationY = 0f
  alpha = 1f
  shape = RectangleShape
  clip = false
  if (preview == null) return

  val progress = preview.progress
  if (preview.isVerticalDrag) {
    if (preview.from == entry) {
      translationY = size.height * progress
      alpha = FastOutSlowInEasing.transform(1f - progress)
    } else if (preview.to == entry) {
      translationY = -size.height * 0.2f * (1f - progress)
      alpha = 1f - 0.5f * (1f - progress)
    }
  } else if (preview.from == entry) {
    scaleX = 1f - 0.1f * progress
    scaleY = scaleX
    val direction = if (preview.swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
    translationX = direction * minOf(24.dp.toPx(), size.width * 0.04f) * progress
    shape = RoundedCornerShape(24.dp * progress)
    clip = progress > 0f
  }
}

/**
 * Keeps NavController/ComposeNavigator in charge of the stack and saved state. Unlike NavHost 2.9,
 * the gesture only seeks to the start of the return transition: preview scale has its own progress,
 * and the actual pop transition starts after the gesture is committed.
 *
 * Lifecycle/state ownership follows androidx/navigation/compose/NavHost.kt and LocalOwnersProvider.
 */
@Composable
internal fun PredictiveNavHost(
  navController: NavHostController,
  startDestination: String,
  backgroundColor: Color,
  modifier: Modifier = Modifier,
  builder: ScreenGraphBuilder.() -> Unit,
) {
  val lifecycleOwner = LocalLifecycleOwner.current
  val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
  val screens = remember(navController, startDestination) { mutableMapOf<Int, ScreenContent>() }
  val graph = remember(navController, startDestination) {
    navController.createGraph(startDestination) { ScreenGraphBuilder(this, screens).builder() }
  }
  navController.setViewModelStore(viewModelStoreOwner.viewModelStore)
  navController.graph = graph
  DisposableEffect(navController, lifecycleOwner) {
    navController.setLifecycleOwner(lifecycleOwner)
    onDispose { }
  }

  val navigator = remember(navController) {
    navController.navigatorProvider.getNavigator<ComposeNavigator>("composable")
  }
  val backStack by navigator.backStack.collectAsState()
  val saveableStateHolder = rememberSaveableStateHolder()
  val scope = rememberCoroutineScope()
  val dispatcherOwner = checkNotNull(LocalOnBackPressedDispatcherOwner.current)
  var preview by remember { mutableStateOf<BackPreview?>(null) }
  var settleJob by remember { mutableStateOf<Job?>(null) }
  val currentEntry = backStack.lastOrNull() ?: return
  val transitionState = remember(navController) { SeekableTransitionState(currentEntry) }
  val transition = rememberTransition(transitionState, label = "navigation")
  val zIndices = remember { mutableMapOf<String, Float>() }
  val preparedEntries = remember { mutableSetOf<NavBackStackEntry>() }
  var viewportHeight by remember { mutableIntStateOf(0) }
  val dragVelocityThreshold = with(LocalDensity.current) { 125.dp.toPx() }

  fun previewScrimAlpha(): Float {
    val activePreview = preview ?: return 0f
    if (transitionState.currentState != activePreview.from) return 0f
    val progress = if (activePreview.isVerticalDrag) 1f - activePreview.progress else activePreview.progress
    return PreviewScrimAlpha * progress * (1f - transitionState.fraction)
  }

  fun completeTransitions() {
    (navController.visibleEntries.value + preparedEntries).distinctBy { it.id }.forEach {
      navigator.onTransitionComplete(it)
    }
    preparedEntries.clear()
  }

  suspend fun preparePreview(activePreview: BackPreview) {
    settleJob?.cancelAndJoin()
    transitionState.snapTo(activePreview.from)
    completeTransitions()
    preview = activePreview
    preparedEntries.addAll(listOf(activePreview.from, activePreview.to))
    navigator.prepareForTransition(activePreview.from)
    navigator.prepareForTransition(activePreview.to)
    transitionState.seekTo(0f, activePreview.to)
  }

  suspend fun finishPreview(activePreview: BackPreview, commit: Boolean) {
    if (preview !== activePreview) return
    if (commit && navController.currentBackStackEntry != activePreview.from) return
    if (commit) {
      activePreview.committed = true
      navController.popBackStack()
    } else {
      transitionState.snapTo(navController.currentBackStackEntry ?: activePreview.from)
      preview = null
    }
  }

  fun settlePreview(activePreview: BackPreview, dismiss: Boolean = false, velocity: Float = 0f) {
    activePreview.isSettling = true
    // Outlast the gesture coroutine and draggable node, which can be cancelled or detached.
    settleJob = scope.launch {
      animate(
        initialValue = activePreview.progress,
        targetValue = if (dismiss) 1f else 0f,
        initialVelocity = velocity,
        animationSpec = spring(
          dampingRatio = if (activePreview.isVerticalDrag) {
            Spring.DampingRatioLowBouncy
          } else {
            Spring.DampingRatioNoBouncy
          },
          stiffness = Spring.StiffnessMediumLow,
        ),
      ) { value, _ -> activePreview.progress = value.coerceIn(0f, 1f) }
      finishPreview(activePreview, commit = dismiss && navController.currentBackStackEntry == activePreview.from)
    }
  }

  DisposableEffect(navigator) {
    onDispose { completeTransitions() }
  }

  LaunchedEffect(currentEntry, preview) {
    val activePreview = preview
    if (activePreview != null && !activePreview.committed && currentEntry == activePreview.from) {
      return@LaunchedEffect
    }
    // A navigation from elsewhere supersedes an uncommitted preview.
    if (activePreview != null && currentEntry != activePreview.to) {
      settleJob?.cancelAndJoin()
      preview = null
    }
    val skipTransition = currentEntry.savedStateHandle.get<Boolean>(SkipNextTransition) == true
    if (skipTransition || (activePreview?.isVerticalDrag == true && activePreview.committed)) {
      // Notifications enter directly; a committed drag has already moved the player off-screen.
      transitionState.snapTo(currentEntry)
    } else {
      transitionState.animateTo(currentEntry)
    }
    // Consume after snapping so a restarted effect still skips the transition.
    if (skipTransition) currentEntry.savedStateHandle.remove<Boolean>(SkipNextTransition)
    if (preview === activePreview) preview = null
  }

  LaunchedEffect(transition.currentState, transition.targetState, currentEntry, preview) {
    if (preview == null && transition.currentState == currentEntry &&
      transition.targetState == currentEntry
    ) {
      completeTransitions()
      zIndices.keys.retainAll(setOf(currentEntry.id))
    }
  }

  PredictiveBackHandler(enabled = backStack.size > 1) { events ->
    val entries = navigator.backStack.value
    if (entries.size <= 1) {
      events.collect { }
      return@PredictiveBackHandler
    }
    val activePreview = BackPreview(entries.last(), entries[entries.lastIndex - 1])
    try {
      preparePreview(activePreview)
      events.collect { event ->
        activePreview.swipeEdge = event.swipeEdge
        activePreview.progress = event.progress.coerceIn(0f, 1f)
      }
      finishPreview(activePreview, commit = true)
    } catch (cancelled: CancellationException) {
      // PredictiveBackHandler cancels this coroutine on cancellation. Restore in the host scope.
      settlePreview(activePreview)
      throw cancelled
    }
  }

  transition.AnimatedContent(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged { viewportHeight = it.height }
      .background(backgroundColor)
      .drawBehind {
        // Also dim the area exposed when the player's underlying page is shifted upward.
        val scrimAlpha = previewScrimAlpha()
        if (scrimAlpha > 0f) drawRect(Color.Black.copy(alpha = scrimAlpha))
      },
    contentKey = { it.id },
    transitionSpec = {
      val activePreview = preview
      val isPreviewReturn = activePreview?.from == initialState && activePreview.to == targetState
      val isPop = isPreviewReturn || backStack.none { it.id == initialState.id }
      val initialZ = zIndices.getOrPut(initialState.id) { 0f }
      val targetZ = when {
        targetState.id == initialState.id -> initialZ
        isPop -> initialZ - 1f
        else -> initialZ + 1f
      }
      zIndices[targetState.id] = targetZ
      ContentTransform(
        targetContentEnter = when {
          // Vertical dragging positions the lower page itself; ordinary previews keep it static.
          isPreviewReturn && (activePreview?.isVerticalDrag == true ||
              initialState.destination.route != RoutePlaying) -> EnterTransition.None
          isPop -> popEnterTransition().invoke(this)
          else -> enterTransition().invoke(this)
        },
        initialContentExit = if (isPop) {
          popExitTransition().invoke(this)
        } else {
          exitTransition().invoke(this)
        },
        targetContentZIndex = targetZ,
      )
    },
  ) { entry ->
    val entryState by entry.lifecycle.currentStateFlow.collectAsState()
    val activeDrag = preview?.takeIf { it.isVerticalDrag && it.from == entry }
    val dragEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        entry.destination.route == RoutePlaying && entry == currentEntry &&
        backStack.size > 1 && viewportHeight > 0 &&
        ((entryState == Lifecycle.State.RESUMED && preview == null) ||
            (activeDrag != null && !activeDrag.isSettling))
    val dragState = rememberDraggableState { delta ->
      val activePreview = preview
      if (activePreview?.isVerticalDrag == true && activePreview.from == entry &&
        !activePreview.isSettling && viewportHeight > 0
      ) {
        activePreview.progress =
          (activePreview.progress + delta / viewportHeight).coerceIn(0f, 1f)
      }
    }
    val inactiveDispatcherOwner = remember(entry) {
      object : OnBackPressedDispatcherOwner {
        override val lifecycle = entry.lifecycle
        override val onBackPressedDispatcher = OnBackPressedDispatcher()
      }
    }
    // Previewing a background destination must not let its BackHandlers steal the next gesture.
    CompositionLocalProvider(
      LocalOnBackPressedDispatcherOwner provides if (entry == currentEntry) {
        dispatcherOwner
      } else {
        inactiveDispatcherOwner
      },
    ) {
      if (entryState == Lifecycle.State.DESTROYED) return@CompositionLocalProvider
      entry.LocalOwnersProvider(saveableStateHolder) {
        Box(
          Modifier
            .fillMaxSize()
            .draggable(
              state = dragState,
              orientation = Orientation.Vertical,
              enabled = dragEnabled,
              onDragStarted = {
                val entries = navigator.backStack.value
                if (entries.size > 1 && entries.last() == entry) {
                  preparePreview(BackPreview(entry, entries[entries.lastIndex - 1], isVerticalDrag = true))
                }
              },
              onDragStopped = { velocity ->
                val activePreview = preview
                if (activePreview?.isVerticalDrag == true && activePreview.from == entry &&
                  !activePreview.isSettling
                ) {
                  val dismiss = when {
                    velocity > dragVelocityThreshold -> true
                    velocity < -dragVelocityThreshold -> false
                    else -> activePreview.progress >= 0.5f
                  }
                  settlePreview(activePreview, dismiss, velocity / viewportHeight.coerceAtLeast(1))
                }
              },
            )
            .graphicsLayer { applyBackPreview(entry, preview) }
            .background(backgroundColor)
            .drawWithContent {
              drawContent()
              if (preview?.to == entry) {
                val scrimAlpha = previewScrimAlpha()
                if (scrimAlpha > 0f) drawRect(Color.Black.copy(alpha = scrimAlpha))
              }
            },
        ) {
          val content = checkNotNull(screens[entry.destination.id]) {
            "Unregistered screen: ${entry.destination.route}"
          }
          content(entry)
        }
      }
    }
  }
}
