package remix.myplayer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import remix.myplayer.request.kugou.KuGouClient
import remix.myplayer.request.netease.NetEaseClient
import remix.myplayer.request.network.GithubApi
import remix.myplayer.request.network.LastFMApi
import remix.myplayer.request.network.OkHttpHelper
import remix.myplayer.request.qq.QQClient
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
  fun provideGithubApi(okHttpClient: OkHttpClient): GithubApi {
    return Retrofit.Builder()
      .baseUrl(GithubApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(GithubApi::class.java)
  }

  @Provides
  @Singleton
  fun provideNetEaseEapiClient(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
  ): NetEaseClient {
    return NetEaseClient(context, okHttpClient)
  }

  @Provides
  @Singleton
  fun provideKuGouClient(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
  ): KuGouClient {
    return KuGouClient(okHttpClient)
  }

  @Provides
  @Singleton
  fun provideQQClient(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
  ): QQClient {
    return QQClient(context, okHttpClient)
  }

  @Provides
  @Singleton
  fun provideLastFMApi(okHttpClient: OkHttpClient): LastFMApi {
    return Retrofit.Builder()
      .baseUrl(LastFMApi.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(LastFMApi::class.java)
  }
}