package remix.myplayer.service.playback.extractor

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorsFactory

@OptIn(UnstableApi::class)
class ApeExtractorsFactory : ExtractorsFactory {
  private val delegate = DefaultExtractorsFactory()

  override fun createExtractors(): Array<Extractor> =
    arrayOf(ApeExtractor(), *delegate.createExtractors())

  override fun createExtractors(
    uri: Uri,
    responseHeaders: Map<String, List<String>>,
  ): Array<Extractor> = arrayOf(ApeExtractor(), *delegate.createExtractors(uri, responseHeaders))
}
