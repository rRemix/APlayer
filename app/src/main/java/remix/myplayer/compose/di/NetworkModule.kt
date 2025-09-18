package remix.myplayer.compose.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import remix.myplayer.request.network.KuGouApi
import remix.myplayer.request.network.NetEaseApi
import remix.myplayer.request.network.OkHttpHelper
import remix.myplayer.request.network.QQApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpHelper.okHttpClient!!
  }

  @Provides
  @Singleton
  fun provideKuGouApi(okHttpClient: OkHttpClient): KuGouApi {
    return Retrofit.Builder()
      .baseUrl(KuGouApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(KuGouApi::class.java)
  }

  @Provides
  @Singleton
  fun provideQQApi(okHttpClient: OkHttpClient): QQApi {
    return Retrofit.Builder()
      .baseUrl(QQApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(QQApi::class.java)
  }

  @Provides
  @Singleton
  fun provideNetEaseApi(okHttpClient: OkHttpClient): NetEaseApi {
    return Retrofit.Builder()
      .baseUrl(NetEaseApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(NetEaseApi::class.java)
  }
}