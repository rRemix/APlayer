package remix.myplayer.service.playback

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.repo.PlayableSource
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE, application = Application::class)
class SongDataSourceTest {
  @get:Rule val folder = TemporaryFolder()
  private lateinit var cache: SimpleCache
  private lateinit var database: StandaloneDatabaseProvider

  @Before fun setup() {
    val context = RuntimeEnvironment.getApplication() as Context
    database = StandaloneDatabaseProvider(context)
    cache = SimpleCache(folder.newFolder("audio"), NoOpCacheEvictor(), database)
  }

  @After fun close() { cache.release(); database.close() }

  @Test
  fun sourceRootChangesUseFreshBytesEvenWhenTheMediaItemStillContainsTheOldAddress() {
    val song = Song(SourceId("webdav:1"), "song.flac", "https://old.test/song.flac", "song.flac")
    var current = PlayableSource(song.data, mapOf("Authorization" to "old-password"))
    val upstream = RecordingUpstream { spec -> "audio from ${spec.uri.host}" }
    var resolutions = 0
    val source = SongDataSource(song, { resolutions++; current }, upstream) { cache }
    val request = DataSpec(Uri.parse(song.data))

    assertEquals("audio from old.test", read(source, request))
    assertEquals("audio from old.test", read(source, request))
    assertEquals(1, upstream.opens.size)
    // Resolving a changed password must not change the stable byte key for the same resource.
    current = PlayableSource(song.data, mapOf("Authorization" to "new-password"))
    assertEquals("audio from old.test", read(source, request))
    assertEquals(1, upstream.opens.size)

    current = PlayableSource("https://new.test/library/song.flac", mapOf("Authorization" to "new-password"))
    assertEquals("audio from new.test", read(source, request))
    assertEquals(2, upstream.opens.size)
    assertEquals("new-password", upstream.opens.last().httpRequestHeaders["Authorization"])
    assertEquals(4, resolutions)
    assertEquals(2, cache.keys.size)
  }

  @Test
  fun sourcesWithTheSameAddressKeepTheirByteCachesSeparate() {
    val first = Song(SourceId("webdav:1"), "song.flac", "https://music.test/song.flac", "song.flac")
    val second = first.copy(sourceId = SourceId("webdav:2"))
    val upstream = RecordingUpstream { it.httpRequestHeaders.getValue("Authorization") }
    fun source(song: Song, account: String) = SongDataSource(song, {
      PlayableSource(it.data, mapOf("Authorization" to account))
    }, upstream) { cache }
    assertEquals("first account", read(source(first, "first account"), DataSpec(Uri.parse(first.data))))
    assertEquals("second account", read(source(second, "second account"), DataSpec(Uri.parse(second.data))))
    assertEquals(2, upstream.opens.size)
  }

  @Test
  fun smbCredentialsReachOnlyTheUpstreamAndNeverTheCacheIndex() {
    val song = Song(SourceId("smb:1"), "song.flac", "smb://nas.test/Music/song.flac", "song.flac")
    val authenticated = "smb://WORK;user:secret-password@nas.test/Music/song.flac"
    val upstream = RecordingUpstream { "SMB audio" }
    val source = SongDataSource(song, { PlayableSource(authenticated) }, upstream) { cache }
    assertEquals("SMB audio", read(source, DataSpec(Uri.parse(song.data))))
    assertEquals(authenticated, upstream.opens.single().uri.toString())
    cache.keys.forEach { key ->
      assertFalse(key.contains("secret-password"))
      assertFalse(key.contains("user"))
      val redirected = ContentMetadata.getRedirectedUri(cache.getContentMetadata(key))
      assertFalse(redirected?.toString().orEmpty().contains("secret-password"))
    }
  }

  private fun read(source: DataSource, request: DataSpec): String {
    val bytes = ByteArrayOutputStream()
    try {
      source.open(request)
      assertFalse(source.uri.toString().contains("secret-password"))
      val buffer = ByteArray(256)
      while (true) {
        val count = source.read(buffer, 0, buffer.size)
        if (count == C.RESULT_END_OF_INPUT) break
        bytes.write(buffer, 0, count)
      }
      return bytes.toString("UTF-8")
    } finally { source.close() }
  }

  private class RecordingUpstream(private val content: (DataSpec) -> String) : DataSource.Factory {
    val opens = mutableListOf<DataSpec>()
    override fun createDataSource(): DataSource = object : DataSource {
      private var delegate: ByteArrayDataSource? = null
      private val listeners = mutableListOf<TransferListener>()
      override fun addTransferListener(listener: TransferListener) { listeners += listener }
      override fun open(dataSpec: DataSpec): Long {
        opens += dataSpec
        val source = ByteArrayDataSource(content(dataSpec).toByteArray())
        delegate = source
        listeners.forEach(source::addTransferListener)
        return source.open(dataSpec)
      }
      override fun getUri(): Uri? = delegate?.uri
      override fun read(buffer: ByteArray, offset: Int, length: Int) = checkNotNull(delegate).read(buffer, offset, length)
      override fun close() { delegate?.close() }
    }
  }
}
