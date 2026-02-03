package remix.myplayer.misc.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

interface DynamicModuleManager {

  fun isModuleSupport(moduleName: String): Boolean
  fun isModuleInstalled(moduleName: String): Boolean
  fun installModule(moduleName: String): Flow<DynamicModuleStatus>
  fun uninstallModule(moduleName: String)
}

@Singleton
class DefaultDynamicModuleManager @Inject constructor() : DynamicModuleManager {

  override fun isModuleSupport(moduleName: String): Boolean = false

  override fun isModuleInstalled(moduleName: String): Boolean = false

  override fun installModule(moduleName: String): Flow<DynamicModuleStatus> = flowOf(DynamicModuleStatus.UnAvailable)

  override fun uninstallModule(moduleName: String) {}
}


sealed class DynamicModuleStatus {
  object UnAvailable : DynamicModuleStatus()
  object Unknown : DynamicModuleStatus()
  object Pending : DynamicModuleStatus()
  data class Downloading(val progress: Double) : DynamicModuleStatus()
  object Installing : DynamicModuleStatus()
  object Installed : DynamicModuleStatus()
  data class Failed(val errorCode: Int) : DynamicModuleStatus()
  object Canceled : DynamicModuleStatus()
  data class Error(val exception: Throwable) : DynamicModuleStatus()
}