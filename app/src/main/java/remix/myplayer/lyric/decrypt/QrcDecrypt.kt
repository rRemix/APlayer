package remix.myplayer.lyric.decrypt

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

object QrcDecrypt {

  // 3DES S盒
  private val sbox = arrayOf(
    // sbox1
    intArrayOf(
      14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
      0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
      4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
      15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13
    ),
    // sbox2
    intArrayOf(
      15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
      3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
      0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
      13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9
    ),
    // sbox3
    intArrayOf(
      10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
      13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
      13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
      1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12
    ),
    // sbox4
    intArrayOf(
      7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
      13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
      10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
      3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14
    ),
    // sbox5
    intArrayOf(
      2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
      14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
      4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
      11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3
    ),
    // sbox6
    intArrayOf(
      12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
      10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
      9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
      4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13
    ),
    // sbox7
    intArrayOf(
      4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
      13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
      1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
      6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12
    ),
    // sbox8
    intArrayOf(
      13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
      1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
      7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
      2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
    )
  )

  // QRC解密密钥
  private val qrcKey = "!@#)(*\$%123ZXC!@!@#)(NHL".toByteArray()

  /**
   * QRC解密
   */
  fun qrcDecrypt(encryptedQrc: String): String? {
    return try {
      val encryptedBytes = hexStringToByteArray(encryptedQrc)

      val schedule = tripleDesKeySetup(qrcKey, false) // DECRYPT
      val out = ByteArrayOutputStream()
      var i = 0
      while (i < encryptedBytes.size) {
        val block = encryptedBytes.copyOfRange(i, i + 8)
        val dec = tripleDesCrypt(block, schedule)
        out.write(dec)
        i += 8
      }
      var decryptedBytes = out.toByteArray()

      // 检测 QRC 魔法头（本地容器场景），跳过11字节
      val qrcMagic = byteArrayOf(
        0x98.toByte(), 0x25, 0xb0.toByte(), 0xac.toByte(), 0xe3.toByte(), 0x02,
        0x83.toByte(), 0x68, 0xe8.toByte(), 0xfc.toByte(), 0x6c
      )
      if (decryptedBytes.size >= qrcMagic.size &&
        decryptedBytes.copyOfRange(0, qrcMagic.size).contentEquals(qrcMagic)
      ) {
        decryptedBytes = decryptedBytes.copyOfRange(11, decryptedBytes.size)
      }

      // 优先 zlib，失败时回退 raw deflate
      return try {
        val inflater = Inflater()
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        inflater.setInput(decryptedBytes)
        while (!inflater.finished()) {
          val count = inflater.inflate(buffer)
          if (count == 0 && inflater.needsInput()) break
          result.write(buffer, 0, count)
        }
        inflater.end()
        result.toString("UTF-8")
      } catch (_: Exception) {
        try {
          val inflater = Inflater(true)
          val result = ByteArrayOutputStream()
          val buffer = ByteArray(1024)
          inflater.setInput(decryptedBytes)
          while (!inflater.finished()) {
            val count = inflater.inflate(buffer)
            if (count == 0 && inflater.needsInput()) break
            result.write(buffer, 0, count)
          }
          inflater.end()
          result.toString("UTF-8")
        } catch (_: Exception) {
          null
        }
      }
    } catch (_: Exception) {
      null
    }
  }

  /**
   * 十六进制字符串转字节数组
   */
  private fun hexStringToByteArray(hex: String): ByteArray {
    val len = hex.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
      data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
      i += 2
    }
    return data
  }

  private fun bitnum(a: ByteArray, b: Int, c: Int): Int {
    // 修复：使用 and 0xFF 避免符号扩展问题
    return (((a[(b / 32) * 4 + 3 - (b % 32) / 8].toInt() and 0xFF) shr (7 - b % 8)) and 1) shl c
  }

  // 所属方法: bitnumIntr
  private fun bitnumIntr(a: Int, b: Int, c: Int): Int {
    return ((a ushr (31 - b)) and 1) shl c
  }

  // 方法: bitnumIntl
  private fun bitnumIntl(a: Int, b: Int, c: Int): Int {
    // 使用 Long 与无符号右移，避免 0x80000000 在 Int 上的符号影响
    return ((((a.toLong() shl b) and 0x80000000L) ushr c).toInt())
  }

  private fun sboxBit(a: Int): Int {
    return (a and 32) or ((a and 31) shr 1) or ((a and 1) shl 4)
  }

  private fun initialPermutation(inputData: ByteArray): Pair<Int, Int> {
    val s0 = (bitnum(inputData, 57, 31) or bitnum(inputData, 49, 30) or bitnum(
      inputData,
      41,
      29
    ) or bitnum(inputData, 33, 28) or
        bitnum(inputData, 25, 27) or bitnum(inputData, 17, 26) or bitnum(
      inputData,
      9,
      25
    ) or bitnum(inputData, 1, 24) or
        bitnum(inputData, 59, 23) or bitnum(inputData, 51, 22) or bitnum(
      inputData,
      43,
      21
    ) or bitnum(inputData, 35, 20) or
        bitnum(inputData, 27, 19) or bitnum(inputData, 19, 18) or bitnum(
      inputData,
      11,
      17
    ) or bitnum(inputData, 3, 16) or
        bitnum(inputData, 61, 15) or bitnum(inputData, 53, 14) or bitnum(
      inputData,
      45,
      13
    ) or bitnum(inputData, 37, 12) or
        bitnum(inputData, 29, 11) or bitnum(inputData, 21, 10) or bitnum(
      inputData,
      13,
      9
    ) or bitnum(inputData, 5, 8) or
        bitnum(inputData, 63, 7) or bitnum(inputData, 55, 6) or bitnum(inputData, 47, 5) or bitnum(
      inputData,
      39,
      4
    ) or
        bitnum(inputData, 31, 3) or bitnum(inputData, 23, 2) or bitnum(inputData, 15, 1) or bitnum(
      inputData,
      7,
      0
    ))

    val s1 = (bitnum(inputData, 56, 31) or bitnum(inputData, 48, 30) or bitnum(
      inputData,
      40,
      29
    ) or bitnum(inputData, 32, 28) or
        bitnum(inputData, 24, 27) or bitnum(inputData, 16, 26) or bitnum(
      inputData,
      8,
      25
    ) or bitnum(inputData, 0, 24) or
        bitnum(inputData, 58, 23) or bitnum(inputData, 50, 22) or bitnum(
      inputData,
      42,
      21
    ) or bitnum(inputData, 34, 20) or
        bitnum(inputData, 26, 19) or bitnum(inputData, 18, 18) or bitnum(
      inputData,
      10,
      17
    ) or bitnum(inputData, 2, 16) or
        bitnum(inputData, 60, 15) or bitnum(inputData, 52, 14) or bitnum(
      inputData,
      44,
      13
    ) or bitnum(inputData, 36, 12) or
        bitnum(inputData, 28, 11) or bitnum(inputData, 20, 10) or bitnum(
      inputData,
      12,
      9
    ) or bitnum(inputData, 4, 8) or
        bitnum(inputData, 62, 7) or bitnum(inputData, 54, 6) or bitnum(inputData, 46, 5) or bitnum(
      inputData,
      38,
      4
    ) or
        bitnum(inputData, 30, 3) or bitnum(inputData, 22, 2) or bitnum(inputData, 14, 1) or bitnum(
      inputData,
      6,
      0
    ))

    return Pair(s0, s1)
  }

  private fun inversePermutation(s0: Int, s1: Int): ByteArray {
    val data = ByteArray(8)
    data[3] = (bitnumIntr(s1, 7, 7) or bitnumIntr(s0, 7, 6) or bitnumIntr(s1, 15, 5) or
        bitnumIntr(s0, 15, 4) or bitnumIntr(s1, 23, 3) or bitnumIntr(s0, 23, 2) or
        bitnumIntr(s1, 31, 1) or bitnumIntr(s0, 31, 0)).toByte()

    data[2] = (bitnumIntr(s1, 6, 7) or bitnumIntr(s0, 6, 6) or bitnumIntr(s1, 14, 5) or
        bitnumIntr(s0, 14, 4) or bitnumIntr(s1, 22, 3) or bitnumIntr(s0, 22, 2) or
        bitnumIntr(s1, 30, 1) or bitnumIntr(s0, 30, 0)).toByte()

    data[1] = (bitnumIntr(s1, 5, 7) or bitnumIntr(s0, 5, 6) or bitnumIntr(s1, 13, 5) or
        bitnumIntr(s0, 13, 4) or bitnumIntr(s1, 21, 3) or bitnumIntr(s0, 21, 2) or
        bitnumIntr(s1, 29, 1) or bitnumIntr(s0, 29, 0)).toByte()

    data[0] = (bitnumIntr(s1, 4, 7) or bitnumIntr(s0, 4, 6) or bitnumIntr(s1, 12, 5) or
        bitnumIntr(s0, 12, 4) or bitnumIntr(s1, 20, 3) or bitnumIntr(s0, 20, 2) or
        bitnumIntr(s1, 28, 1) or bitnumIntr(s0, 28, 0)).toByte()

    data[7] = (bitnumIntr(s1, 3, 7) or bitnumIntr(s0, 3, 6) or bitnumIntr(s1, 11, 5) or
        bitnumIntr(s0, 11, 4) or bitnumIntr(s1, 19, 3) or bitnumIntr(s0, 19, 2) or
        bitnumIntr(s1, 27, 1) or bitnumIntr(s0, 27, 0)).toByte()

    data[6] = (bitnumIntr(s1, 2, 7) or bitnumIntr(s0, 2, 6) or bitnumIntr(s1, 10, 5) or
        bitnumIntr(s0, 10, 4) or bitnumIntr(s1, 18, 3) or bitnumIntr(s0, 18, 2) or
        bitnumIntr(s1, 26, 1) or bitnumIntr(s0, 26, 0)).toByte()

    data[5] = (bitnumIntr(s1, 1, 7) or bitnumIntr(s0, 1, 6) or bitnumIntr(s1, 9, 5) or
        bitnumIntr(s0, 9, 4) or bitnumIntr(s1, 17, 3) or bitnumIntr(s0, 17, 2) or
        bitnumIntr(s1, 25, 1) or bitnumIntr(s0, 25, 0)).toByte()

    data[4] = (bitnumIntr(s1, 0, 7) or bitnumIntr(s0, 0, 6) or bitnumIntr(s1, 8, 5) or
        bitnumIntr(s0, 8, 4) or bitnumIntr(s1, 16, 3) or bitnumIntr(s0, 16, 2) or
        bitnumIntr(s1, 24, 1) or bitnumIntr(s0, 24, 0)).toByte()

    return data
  }

  // 所属方法: f
  private fun f(state: Int, key: IntArray): Int {
    // 扩展置换：将对带掩码的右移从 shr 改为 ushr，避免负数算术右移带来的符号扩展
    val t1 = (
        bitnumIntl(state, 31, 0) or
            ((state and 0xf0000000.toInt()) ushr 1) or
            bitnumIntl(state, 4, 5) or
            bitnumIntl(state, 3, 6) or
            ((state and 0x0f000000) ushr 3) or
            bitnumIntl(state, 8, 11) or
            bitnumIntl(state, 7, 12) or
            ((state and 0x00f00000) ushr 5) or
            bitnumIntl(state, 12, 17) or
            bitnumIntl(state, 11, 18) or
            ((state and 0x000f0000) ushr 7) or
            bitnumIntl(state, 16, 23)
        )

    val t2 = (
        bitnumIntl(state, 15, 0) or
            ((state and 0x0000f000) shl 15) or
            bitnumIntl(state, 20, 5) or
            bitnumIntl(state, 19, 6) or
            ((state and 0x00000f00) shl 13) or
            bitnumIntl(state, 24, 11) or
            bitnumIntl(state, 23, 12) or
            ((state and 0x000000f0) shl 11) or
            bitnumIntl(state, 28, 17) or
            bitnumIntl(state, 27, 18) or
            ((state and 0x0000000f) shl 9) or
            bitnumIntl(state, 0, 23)
        )

    val lrgstate = intArrayOf(
      (t1 ushr 24) and 0x000000ff,
      (t1 ushr 16) and 0x000000ff,
      (t1 ushr 8) and 0x000000ff,
      (t2 ushr 24) and 0x000000ff,
      (t2 ushr 16) and 0x000000ff,
      (t2 ushr 8) and 0x000000ff
    )

    for (i in 0..5) {
      lrgstate[i] = lrgstate[i] xor key[i]
    }

    val newState = (
        (sbox[0][sboxBit(lrgstate[0] ushr 2)] shl 28) or
            (sbox[1][sboxBit(((lrgstate[0] and 0x03) shl 4) or (lrgstate[1] ushr 4))] shl 24) or
            (sbox[2][sboxBit(((lrgstate[1] and 0x0f) shl 2) or (lrgstate[2] ushr 6))] shl 20) or
            (sbox[3][sboxBit(lrgstate[2] and 0x3f)] shl 16) or
            (sbox[4][sboxBit(lrgstate[3] ushr 2)] shl 12) or
            (sbox[5][sboxBit(((lrgstate[3] and 0x03) shl 4) or (lrgstate[4] ushr 4))] shl 8) or
            (sbox[6][sboxBit(((lrgstate[4] and 0x0f) shl 2) or (lrgstate[5] ushr 6))] shl 4) or
            sbox[7][sboxBit(lrgstate[5] and 0x3f)]
        )

    return (
        bitnumIntl(newState, 15, 0) or bitnumIntl(newState, 6, 1) or bitnumIntl(newState, 19, 2) or
            bitnumIntl(newState, 20, 3) or bitnumIntl(newState, 28, 4) or bitnumIntl(
          newState,
          11,
          5
        ) or
            bitnumIntl(newState, 27, 6) or bitnumIntl(newState, 16, 7) or bitnumIntl(
          newState,
          0,
          8
        ) or
            bitnumIntl(newState, 14, 9) or bitnumIntl(newState, 22, 10) or bitnumIntl(
          newState,
          25,
          11
        ) or
            bitnumIntl(newState, 4, 12) or bitnumIntl(newState, 17, 13) or bitnumIntl(
          newState,
          30,
          14
        ) or
            bitnumIntl(newState, 9, 15) or bitnumIntl(newState, 1, 16) or bitnumIntl(
          newState,
          7,
          17
        ) or
            bitnumIntl(newState, 23, 18) or bitnumIntl(newState, 13, 19) or bitnumIntl(
          newState,
          31,
          20
        ) or
            bitnumIntl(newState, 26, 21) or bitnumIntl(newState, 2, 22) or bitnumIntl(
          newState,
          8,
          23
        ) or
            bitnumIntl(newState, 18, 24) or bitnumIntl(newState, 12, 25) or bitnumIntl(
          newState,
          29,
          26
        ) or
            bitnumIntl(newState, 5, 27) or bitnumIntl(newState, 21, 28) or bitnumIntl(
          newState,
          10,
          29
        ) or
            bitnumIntl(newState, 3, 30) or bitnumIntl(newState, 24, 31)
        )
  }

  private fun crypt(inputData: ByteArray, key: Array<IntArray>): ByteArray {
    val (s0Initial, s1Initial) = initialPermutation(inputData)
    var s0 = s0Initial
    var s1 = s1Initial

    // 15轮迭代
    for (idx in 0..14) {
      val previousS1 = s1
      s1 = f(s1, key[idx]) xor s0
      s0 = previousS1
    }
    // 第16轮
    s0 = f(s1, key[15]) xor s0

    return inversePermutation(s0, s1)
  }

  private fun keySchedule(key: ByteArray, isDecrypt: Boolean): Array<IntArray> {
    val schedule = Array(16) { IntArray(6) }
    val keyRndShift = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
    val keyPermC = intArrayOf(
      56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
      58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35
    )
    val keyPermD = intArrayOf(
      62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5,
      60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3
    )
    val keyCompression = intArrayOf(
      13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
      40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31
    )

    // 修复：使用sum而不是or操作，与Python保持一致
    var c = 0
    var d = 0

    for (i in 0..27) {
      c += bitnum(key, keyPermC[i], 31 - i)
      d += bitnum(key, keyPermD[i], 31 - i)
    }

    for (i in 0..15) {
      c = ((c shl keyRndShift[i]) or (c ushr (28 - keyRndShift[i]))) and 0xfffffff0.toInt()
      d = ((d shl keyRndShift[i]) or (d ushr (28 - keyRndShift[i]))) and 0xfffffff0.toInt()

      val togen = if (isDecrypt) 15 - i else i

      for (j in 0..5) {
        schedule[togen][j] = 0
      }

      for (j in 0..23) {
        schedule[togen][j / 8] =
          schedule[togen][j / 8] or bitnumIntr(c, keyCompression[j], 7 - (j % 8))
      }

      for (j in 24..47) {
        schedule[togen][j / 8] =
          schedule[togen][j / 8] or bitnumIntr(d, keyCompression[j] - 27, 7 - (j % 8))
      }
    }

    return schedule
  }

  private fun tripleDesKeySetup(key: ByteArray, isEncrypt: Boolean): Array<Array<IntArray>> {
    return if (isEncrypt) {
      // ENCRYPT模式：key[0:8] ENCRYPT -> key[8:16] DECRYPT -> key[16:24] ENCRYPT
      arrayOf(
        keySchedule(key.copyOfRange(0, 8), false),  // ENCRYPT (isDecrypt = false)
        keySchedule(key.copyOfRange(8, 16), true),  // DECRYPT (isDecrypt = true)
        keySchedule(key.copyOfRange(16, 24), false) // ENCRYPT (isDecrypt = false)
      )
    } else {
      // DECRYPT模式：key[16:24] DECRYPT -> key[8:16] ENCRYPT -> key[0:8] DECRYPT
      arrayOf(
        keySchedule(key.copyOfRange(16, 24), true), // DECRYPT (isDecrypt = true)
        keySchedule(key.copyOfRange(8, 16), false), // ENCRYPT (isDecrypt = false)
        keySchedule(key.copyOfRange(0, 8), true)    // DECRYPT (isDecrypt = true)
      )
    }
  }

  private fun tripleDesCrypt(data: ByteArray, key: Array<Array<IntArray>>): ByteArray {
    // Python版本：直接传递数据给crypt，crypt函数只处理前8字节
    var result = data
    for (i in 0..2) {
      result = crypt(result, key[i])
    }
    return result
  }
}