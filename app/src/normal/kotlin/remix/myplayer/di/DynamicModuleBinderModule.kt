package remix.myplayer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import remix.myplayer.misc.manager.DefaultDynamicModuleManager
import remix.myplayer.misc.manager.DynamicModuleManager

@Module
@InstallIn(SingletonComponent::class)
abstract class DynamicModuleBinderModule {

  @Binds
  abstract fun bindDynamicModuleManager(impl: DefaultDynamicModuleManager): DynamicModuleManager
}
