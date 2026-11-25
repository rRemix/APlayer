package remix.myplayer.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFileTest {
  @Test fun normalizeRelativePaths() {
    assertEquals("music/track.flac", remotePath("music//track.flac"))
    assertEquals("", remotePath(""))
    assertEquals("中文/A #%.m4a", remotePath("中文/A #%.m4a"))
  }

  @Test fun traversalIsRejected() {
    listOf("../secret", "music/../private", "music/./song.mp3", "/etc/passwd", "foo\\bar").forEach {
      try {
        remotePath(it)
        throw AssertionError("accepted unsafe path: $it")
      } catch (_: IllegalArgumentException) { }
    }
  }

  @Test fun audioDetectionIgnoresDirectoriesAndCase() {
    assertTrue(RemoteFile("TEST.FLAC", "TEST.FLAC", false).isAudio)
    assertFalse(RemoteFile("TEST.FLAC", "TEST.FLAC", true).isAudio)
    assertFalse(RemoteFile("file.txt", "file.txt", false).isAudio)
  }
}
