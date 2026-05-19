package remix.myplayer.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.misc.AppInfo
import remix.myplayer.misc.SystemInfo
import remix.myplayer.misc.log.LogFileWriter
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.nav.RouteSupport
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.Util
import remix.myplayer.util.ext.tryLaunch
import remix.myplayer.util.ext.zipFrom
import remix.myplayer.util.ext.zipOutputStream
import timber.log.Timber
import java.io.File

private const val REPO_URL = "https://github.com/rRemix/APlayer"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val PRIVACY_POLICY_URL = "$REPO_URL/blob/master/PrivacyPolicy.md"
private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val PLAY_STORE_MARKET_URL = "market://details?id=remix.myplayer"
private const val PLAY_STORE_WEB_URL =
  "https://play.google.com/store/apps/details?id=remix.myplayer"

@Composable
fun AboutScreen() {
  val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
  val appInfoDialogState = rememberDialogState()

  NormalDialog(
    dialogState = appInfoDialogState,
    content = AppInfo.prettyPrinted,
    positive = stringResource(R.string.close),
    negative = null
  )

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.about), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    if (isLandscape) {
      Row(
        modifier = Modifier
          .padding(contentPadding)
          .fillMaxSize()
      ) {
        Column(
          modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 220.dp, max = 300.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          AboutHeader(appInfoDialogState = appInfoDialogState, isLandscape = true)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, end = 16.dp, bottom = 24.dp)
        ) {
          AboutActionList()
        }
      }
    } else {
      Column(
        modifier = Modifier
          .padding(contentPadding)
          .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Spacer(modifier = Modifier.height(32.dp))

        AboutHeader(appInfoDialogState = appInfoDialogState, isLandscape = false)

        Spacer(modifier = Modifier.height(36.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
        ) {
          AboutActionList()

          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
private fun AboutHeader(
  appInfoDialogState: DialogState,
  isLandscape: Boolean
) {
  val logoSize = if (isLandscape) 72.dp else 100.dp
  val titleSize = if (isLandscape) 20.sp else 24.sp
  val titleSpacing = if (isLandscape) 12.dp else 16.dp

  Image(
    painter = painterResource(id = R.mipmap.ic_launcher),
    contentDescription = null,
    modifier = Modifier
      .size(logoSize)
      .clip(RoundedCornerShape(24.dp))
  )

  Spacer(modifier = Modifier.height(titleSpacing))

  TextPrimary(
    text = stringResource(id = R.string.app_name),
    fontSize = titleSize,
    fontWeight = FontWeight.Bold
  )

  TextSecondary(
    text = "v${BuildConfig.VERSION_NAME}",
    fontSize = 14.sp,
    modifier = Modifier
      .padding(top = 8.dp)
      .combinedClickable(
        interactionSource = null,
        indication = null,
        onClick = { appInfoDialogState.show() },
        onLongClick = { appInfoDialogState.show() }
      )
  )
}

@Composable
private fun AboutActionList() {
  val context = LocalContext.current
  val nav = LocalNavController.current
  val scope = rememberCoroutineScope()
  val feedbackDialogState = rememberDialogState()

  NormalDialog(
    dialogState = feedbackDialogState,
    titleRes = R.string.send_log,
    positiveRes = R.string.yes,
    negativeRes = R.string.no,
    neutralRes = R.string.cancel,
    onPositive = {
      sendFeedback(context, scope, true)
    },
    onNegative = {
      sendFeedback(context, scope, false)
    }
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
  ) {
    AboutItem(
      title = stringResource(R.string.support_develop),
      subtitle = stringResource(R.string.donate_tip),
      imageVector = Icons.Default.Favorite,
      onClick = { nav.navigate(RouteSupport) }
    )
    AboutItem(
      title = stringResource(R.string.about_github_title),
      subtitle = stringResource(R.string.about_github_subtitle),
      imageVector = Icons.Default.Info,
      onClick = { openUrl(context, REPO_URL) }
    )
    AboutItem(
      title = stringResource(R.string.about_release_title),
      subtitle = stringResource(R.string.about_release_subtitle),
      imageVector = Icons.Default.Build,
      onClick = { openUrl(context, RELEASES_URL) }
    )
    AboutItem(
      title = stringResource(R.string.about_feedback_title),
      subtitle = stringResource(R.string.about_feedback_subtitle),
      imageVector = Icons.Default.Email,
      onClick = { feedbackDialogState.show() }
    )
    AboutItem(
      title = stringResource(R.string.about_share_chooser),
      subtitle = stringResource(R.string.about_share_subtitle),
      imageVector = Icons.Default.Share,
      onClick = { shareApp(context) }
    )
    AboutItem(
      title = stringResource(R.string.about_privacy_title),
      subtitle = stringResource(R.string.about_privacy_subtitle),
      imageVector = Icons.Default.Lock,
      onClick = { openUrl(context, PRIVACY_POLICY_URL) }
    )
    if (BuildConfig.FLAVOR.contains("google")) {
      AboutItem(
        title = stringResource(R.string.about_rate_title),
        subtitle = stringResource(R.string.about_rate_subtitle),
        imageVector = Icons.Default.ThumbUp,
        onClick = { rateApp(context) }
      )
    }
  }
}

@Composable
private fun AboutItem(
  title: String,
  subtitle: String? = null,
  imageVector: ImageVector,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = imageVector,
      contentDescription = null,
      tint = LocalTheme.current.textPrimary,
      modifier = Modifier.size(24.dp)
    )

    Spacer(modifier = Modifier.width(16.dp))

    Column(modifier = Modifier.weight(1f)) {
      TextPrimary(text = title, fontSize = 16.sp)
      if (subtitle != null) {
        Spacer(modifier = Modifier.height(2.dp))
        TextSecondary(text = subtitle, fontSize = 12.sp)
      }
    }

    Icon(
      painter = painterResource(id = R.drawable.ic_navigate_next_white_24dp),
      contentDescription = null,
      tint = LocalTheme.current.textSecondary,
      modifier = Modifier.size(24.dp)
    )
  }
}

private fun openUrl(context: Context, url: String) {
  Util.startActivitySafely(context, Intent(Intent.ACTION_VIEW, url.toUri()))
}

private fun rateApp(context: Context) {
  val playStoreIntent = Intent(
    Intent.ACTION_VIEW,
    PLAY_STORE_MARKET_URL.toUri()
  ).apply {
    setPackage(PLAY_STORE_PACKAGE)
  }

  try {
    context.startActivity(playStoreIntent)
  } catch (_: ActivityNotFoundException) {
    openUrl(context, PLAY_STORE_WEB_URL)
  }
}

private fun shareApp(context: Context) {
  ShareCompat.IntentBuilder(context).setType("text/plain")
    .setChooserTitle(R.string.about_share_chooser)
    .setText(context.getString(R.string.about_share_message, PLAY_STORE_WEB_URL))
    .startChooser()
}

fun sendFeedback(context: Context, scope: CoroutineScope, sendLog: Boolean) {
  val emailIntent = Intent()
  emailIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_feedback_title))
  emailIntent.putExtra(
    Intent.EXTRA_TEXT,
    "\n\n\nApp info:\n${AppInfo.prettyPrinted}\n\nSystem info:\n${SystemInfo.prettyPrinted}"
  )

  scope.tryLaunch(catch = {
    Timber.w(it)
    MessageNotifier.show(R.string.send_error, it.toString())
  }, block = {
    val email = "rRemix.apps@gmail.com"
    if (sendLog) {
      withContext(Dispatchers.IO) {
        try {
          val logDir = LogFileWriter.getLogDir(context)
          val zipFile = File(logDir.parentFile, "logs.zip")
          zipFile.delete()
          zipFile.createNewFile()
          zipFile.zipOutputStream().zipFrom(
            logDir.absolutePath,
            "${context.applicationInfo.dataDir}/shared_prefs"
          )
          if (zipFile.length() > 0) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
              emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
              FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                zipFile
              )
            } else {
              "file://${zipFile.absoluteFile}".toUri()
            }
            emailIntent.action = Intent.ACTION_SEND
            emailIntent.type = "application/octet-stream"
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
            emailIntent.putExtra(
              Intent.EXTRA_EMAIL,
              arrayOf(email)
            )
          }
        } catch (e: Exception) {
          Timber.w(e)
        }
      }
    } else {
      emailIntent.action = Intent.ACTION_SENDTO
      emailIntent.data = "mailto:$email".toUri()
    }

    Util.startActivitySafely(context, emailIntent)
  })
}
