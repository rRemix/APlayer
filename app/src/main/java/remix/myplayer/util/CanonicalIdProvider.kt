package remix.myplayer.util

import remix.myplayer.data.model.audio.Song
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 生成跨平台稳定的歌曲 canonicalId、contentHash 与 pathHint。
 *
 * 本地歌曲优先使用规范化元数据哈希；在线歌曲使用来源 URI 哈希。
 */
@Singleton
class CanonicalIdProvider @Inject constructor() {

  fun canonicalId(song: Song): String {
    val base = when (song) {
      is Song.Local -> listOf(song.title, song.artist, song.album, song.duration.toString())
        .joinToString("|") { it.trim().lowercase() }
      is Song.Remote -> song.data
    }
    return "sha256:" + sha256(base)
  }

  fun contentHash(song: Song): String? = "sha256:" + sha256(song.data)

  fun pathHint(song: Song): String? =
    song.data.substringAfterLast('/').takeIf { it.isNotEmpty() }

  private fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
