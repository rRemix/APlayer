package remix.myplayer.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.viewmodel.MultiSelectState

class RemoteBrowserSourceTest {
  @Test fun smbFolderRestorationKeepsShareRoot() {
    val config = Smb("NAS", null, "user", "password", "smb://nas:4450", "Music",
      "smb://nas:4450/Music/EN").also { it.id = 1 }
    val source = RemoteBrowserSource.SmbSource(config)
    assertEquals("smb:1", source.key)
    assertEquals("smb://nas:4450/Music", source.root)
    assertEquals(listOf(source.root, "${source.root}/EN"), source.buildPathStack())
  }

  @Test fun webDavFolderRestorationKeepsRoot() {
    val config = WebDav("DAV", "user", "password", "https://dav.test/music",
      "https://dav.test/music/Album").also { it.id = 1 }
    val source = RemoteBrowserSource.WebDavSource(config)
    assertEquals("webdav:1", source.key)
    assertEquals(listOf(source.root, "${source.root}/Album"), source.buildPathStack())
  }

  @Test fun remoteSelectionIsScopedToSourceAndDirectory() {
    val selected = MultiSelectState(where = MultiSelectState.Where.Remote,
      owner = "smb:1|smb://nas/Music/EN")
    assertTrue(selected.isShowInRemote("smb:1|smb://nas/Music/EN"))
    assertFalse(selected.isShowInRemote("smb:1|smb://nas/Music"))
    assertFalse(selected.isShowInRemote("webdav:1|https://dav.test/music/EN"))
  }
}
