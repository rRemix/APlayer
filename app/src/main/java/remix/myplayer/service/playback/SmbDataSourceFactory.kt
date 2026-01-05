package remix.myplayer.service.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource

@UnstableApi
class SmbDataSourceFactory : DataSource.Factory {
  override fun createDataSource(): DataSource {
    return SmbDataSource()
  }
}
