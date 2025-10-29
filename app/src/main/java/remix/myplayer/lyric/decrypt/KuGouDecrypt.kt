package remix.myplayer.lyric.decrypt

import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

object KuGouDecrypt {

  private val krcKey = byteArrayOf(
    0x40,
    0x47,
    0x61,
    0x77,
    0x5E,
    0x32,
    0x74,
    0x47,
    0x51,
    0x36,
    0x31,
    0x2D,
    0xCE.toByte(),
    0xD2.toByte(),
    0x6E,
    0x69
  ) // "@Gaw^2tGQ61-\xce\xd2ni"

  /**
   * KRC 解密：跳过前4字节，按密钥异或，然后 zlib 解压
   * 参考 LDDC 的实现
   */
  fun krcDecrypt(content: ByteArray): String {
    // 跳过前4字节
    val encrypted = if (content.size > 4) content.copyOfRange(4, content.size) else ByteArray(0)
    val out = ByteArray(encrypted.size)
    for (i in encrypted.indices) {
      out[i] = (encrypted[i].toInt() xor krcKey[i % krcKey.size].toInt()).toByte()
    }
    val plainBytes = zlibDecompress(out)
    return (plainBytes ?: ByteArray(0)).toString(Charsets.UTF_8)
  }

  private fun zlibDecompress(input: ByteArray): ByteArray? {
    return try {
      val inflater = Inflater(false)
      inflater.setInput(input)
      val output = ByteArrayOutputStream()
      val buf = ByteArray(1024)
      while (!inflater.finished() && !inflater.needsInput()) {
        val count = inflater.inflate(buf)
        if (count > 0) {
          output.write(buf, 0, count)
        } else {
          break
        }
      }
      inflater.end()
      output.toByteArray()
    } catch (e: Exception) {
      Timber.w(e, "KRC zlib 解压失败")
      null
    }
  }
}