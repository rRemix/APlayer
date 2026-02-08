package remix.myplayer.misc.manager

import android.content.Context
import remix.myplayer.BuildConfig
import com.google.android.play.core.splitinstall.testing.FakeSplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDynamicModuleManager @Inject constructor(
  @param:ApplicationContext private val context: Context
) : DynamicModuleManager {

  private val splitInstallManager: SplitInstallManager by lazy {
    if (BuildConfig.DEBUG) {
      FakeSplitInstallManagerFactory.create(context)
    } else {
      SplitInstallManagerFactory.create(context)
    }
  }

  override fun isModuleSupport(moduleName: String): Boolean {
//    return moduleName == "feature_smb"
    return false
  }

  override fun isModuleInstalled(moduleName: String): Boolean {
    return splitInstallManager.installedModules.contains(moduleName)
  }

  override fun installModule(moduleName: String): Flow<DynamicModuleStatus> = callbackFlow {
    if (isModuleInstalled(moduleName)) {
      trySend(DynamicModuleStatus.Installed)
      close()
      return@callbackFlow
    }

    val request = SplitInstallRequest.newBuilder()
      .addModule(moduleName)
      .build()

    val listener = SplitInstallStateUpdatedListener { state ->
      val status = when (state.status()) {
        SplitInstallSessionStatus.DOWNLOADING -> {
          val total = state.totalBytesToDownload()
          val progress = if (total > 0) state.bytesDownloaded().toDouble() / total else 0.0
          DynamicModuleStatus.Downloading(progress)
        }

        SplitInstallSessionStatus.INSTALLED -> DynamicModuleStatus.Installed
        SplitInstallSessionStatus.FAILED -> DynamicModuleStatus.Failed(state.errorCode())
        SplitInstallSessionStatus.CANCELED -> DynamicModuleStatus.Canceled
        SplitInstallSessionStatus.PENDING -> DynamicModuleStatus.Pending
        SplitInstallSessionStatus.INSTALLING -> DynamicModuleStatus.Installing
        else -> DynamicModuleStatus.Unknown
      }
      trySend(status)

      if (state.status() == SplitInstallSessionStatus.INSTALLED) {
        close()
      }
    }

    splitInstallManager.registerListener(listener)
    splitInstallManager.startInstall(request)
      .addOnFailureListener { e ->
        trySend(DynamicModuleStatus.Error(e))
        close()
      }

    awaitClose {
      splitInstallManager.unregisterListener(listener)
    }
  }

  override fun uninstallModule(moduleName: String) {
    splitInstallManager.deferredUninstall(listOf(moduleName))
  }
}
