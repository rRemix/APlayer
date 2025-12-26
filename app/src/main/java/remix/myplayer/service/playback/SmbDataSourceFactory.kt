package remix.myplayer.service.playback

import androidx.media3.datasource.DataSource

class SmbDataSourceFactory : DataSource.Factory {
  override fun createDataSource(): DataSource {
    return SmbDataSource()
  }
}
