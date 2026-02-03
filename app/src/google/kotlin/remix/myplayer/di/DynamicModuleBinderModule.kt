package remix.myplayer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.misc.manager.DynamicModuleManager
import remix.myplayer.misc.manager.GoogleDynamicModuleManager

@Module
@InstallIn(SingletonComponent::class)
abstract class DynamicModuleBinderModule {

  @Binds
  abstract fun bindDynamicModuleManager(impl: GoogleDynamicModuleManager): DynamicModuleManager
}
