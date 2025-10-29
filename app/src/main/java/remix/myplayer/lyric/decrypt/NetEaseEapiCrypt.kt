package remix.myplayer.lyric.decrypt

import okio.ByteString.Companion.decodeHex
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NetEaseEapiCrypt {
  private val eapiKey = "e82ckenh8dichen8".toByteArray()

  fun eapiParamsEncrypt(path: String, params: JSONObject): String {
    val paramsBytes = params.toString().toByteArray(Charsets.UTF_8)
    val signSrc =
      "nobody".toByteArray() + path.toByteArray() + "use".toByteArray() + paramsBytes + "md5forencrypt".toByteArray()
    val md5 = MessageDigest.getInstance("MD5").digest(signSrc)
    val signHex = md5.joinToString("") { "%02x".format(it) }
    val aesSrc =
      path.toByteArray() + "-36cd479b6b5-".toByteArray() + paramsBytes + "-36cd479b6b5-".toByteArray() + signHex.toByteArray()
    val encrypted = aesEncryptECB(aesSrc, eapiKey)
    val hexUpper = encrypted.joinToString("") { "%02X".format(it) }
    return "params=$hexUpper"
  }

  fun eapiResponseDecrypt(cipherBytes: ByteArray): String {
    // 先尝试直接解密
    try {
      val plain = aesDecryptECB(cipherBytes, eapiKey)
      return plain.toString(Charsets.UTF_8)
    } catch (e: Exception) {
      // 继续尝试 HEX 文本解码后再解密
      try {
        val ascii = cipherBytes.toString(Charsets.US_ASCII).trim()
        if (isHexAscii(ascii)) {
          val hexDecoded = ascii.decodeHex().toByteArray()
          val plain2 = aesDecryptECB(hexDecoded, eapiKey)
          return plain2.toString(Charsets.UTF_8)
        }
      } catch (_: Exception) {
        // ignore, fallback to plaintext
      }
      // 最后兜底：按明文处理，避免直接崩溃
      return cipherBytes.toString(Charsets.UTF_8)
    }
  }

  private fun aesEncryptECB(data: ByteArray, key: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.doFinal(data)
  }

  private fun aesDecryptECB(data: ByteArray, key: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
    return cipher.doFinal(data)
  }


  // 简单判断是否为十六进制 ASCII 文本且长度为偶数
  private fun isHexAscii(s: String): Boolean {
    if (s.length % 2 != 0) return false
    for (ch in s) {
      val isHex = (ch in '0'..'9') || (ch in 'a'..'f') || (ch in 'A'..'F')
      if (!isHex) return false
    }
    return true
  }
}