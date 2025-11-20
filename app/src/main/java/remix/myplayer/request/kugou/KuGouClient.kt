package remix.myplayer.request.kugou

import android.util.Base64
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import remix.myplayer.lyric.LrcParser
import remix.myplayer.lyric.decrypt.KuGouDecrypt.krcDecrypt
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 新的酷狗接口
 */
@Singleton
class KuGouClient @Inject constructor(
  private val okHttpClient: OkHttpClient
) {

  private val kgSecret = "LnT6xpN3khm36zse0QzvmgTZ3waWdRSA"

  /**
   * 搜索歌曲
   */
  fun searchSongList(keyword: String, page: Int = 1): List<KuGouSong> {
    val url = "http://complexsearch.kugou.com/v2/search/song"
    val params = mutableMapOf<String, Any>(
      "sorttype" to "0",
      "keyword" to keyword,
      "pagesize" to 20,
      "page" to page
    )
    val headers = mutableMapOf("x-router" to "complexsearch.kugou.com")
    val resp =
      kgRequest(url, params, module = "SearchSong", method = "GET", data = null, headers = headers)
    val lists = resp.optJSONObject("data")?.optJSONArray("lists") ?: JSONArray()
    val result = ArrayList<KuGouSong>()
    for (i in 0 until lists.length()) {
      val obj = lists.getJSONObject(i)
      val singersArr = obj.optJSONArray("Singers") ?: JSONArray()
      val artists = (0 until singersArr.length()).mapNotNull { ii ->
        singersArr.optJSONObject(ii)?.optString("name")
      }.filter { it.isNotBlank() }
      result.add(
        KuGouSong(
          id = obj.optLong("ID"),
          hash = obj.optString("FileHash"),
          title = obj.optString("SongName"),
          artists = artists,
          album = obj.optString("AlbumName"),
          durationMs = obj.optLong("Duration") * 1000
        )
      )
    }
    return result
  }

  /**
   * 根据歌曲信息获取歌词：
   */
  fun getLyrics(song: KuGouSong): Pair<String, String?> {
    // 先拉歌词候选列表
    val candidates = getLyricsCandidates(song)
    if (candidates.isEmpty()) {
      throw IllegalStateException("Kugou: 没有找到歌词候选")
    }
    val best = candidates.first()

    // 下载歌词内容
    val dlParams = mutableMapOf<String, Any>(
      "accesskey" to best.accesskey,
      "charset" to "utf8",
      "client" to "mobi",
      "fmt" to "krc",
      "id" to best.id.toString(),
      "ver" to "1"
    )
    val dlResp = kgRequest(
      "http://lyrics.kugou.com/download",
      dlParams,
      module = "Lyric",
      method = "GET",
      data = null,
      headers = emptyMap()
    )

    val contentType = dlResp.optInt("contenttype")
    val contentB64 = dlResp.optString("content", "")
    if (contentB64.isBlank()) {
      throw IllegalStateException("Kugou: 歌词下载内容为空")
    }
    val rawBytes = Base64.decode(contentB64, Base64.DEFAULT)

    return if (contentType == 2) {
      // 纯文本
      val text = rawBytes.toString(Charsets.UTF_8)
      Pair(text, null)
    } else {
      // KRC：解密并转换为增强LRC
      val krcPlain = krcDecrypt(rawBytes)
      val (enhanced, translation) = krcToEnhancedLrc(krcPlain)
      Pair(enhanced, translation)
    }
  }

  /**
   * 获取歌词候选列表
   */
  private fun getLyricsCandidates(song: KuGouSong): List<KugouLyricCandidate> {
    val keyword = (if (song.artists.isNotEmpty()) song.artists.joinToString("、") else "") +
        " - " + (song.title.ifBlank { "" })

    val params = mutableMapOf<String, Any>(
      "album_audio_id" to song.id.toString(),
      "duration" to song.durationMs.toString(),
      "hash" to song.hash,
      "keyword" to keyword,
      "lrctxt" to "1",
      "man" to "no"
    )
    val resp = kgRequest(
      "https://lyrics.kugou.com/v1/search",
      params,
      module = "Lyric",
      method = "GET",
      data = null,
      headers = emptyMap()
    )
    val candidates = resp.optJSONArray("candidates") ?: JSONArray()
    val list = ArrayList<KugouLyricCandidate>()
    for (i in 0 until candidates.length()) {
      val obj = candidates.getJSONObject(i)
      list.add(
        KugouLyricCandidate(
          id = obj.optLong("id"),
          accesskey = obj.optString("accesskey"),
          durationMs = obj.optLong("duration"),
          score = obj.optInt("score")
        )
      )
    }
    // 这里默认返回服务端顺序
    return list
  }

  /**
   * 核心请求：按 KGAPI 逻辑构造参数、头和签名
   */
  private fun kgRequest(
    url: String,
    paramsIn: MutableMap<String, Any>,
    module: String,
    method: String,
    data: String?,
    headers: Map<String, String>
  ): JSONObject {
    val headersAll = mutableMapOf(
      "User-Agent" to "Android14-1070-11070-201-0-$module-wifi",
      "Connection" to "Keep-Alive",
//      "Accept-Encoding" to "gzip, deflate",
      "KG-Rec" to "1",
      "KG-RC" to "1",
      "KG-CLIENTTIMEMS" to System.currentTimeMillis().toString()
    )
    headersAll.putAll(headers)

    val mid = md5Hex(System.currentTimeMillis().toString())
    headersAll["mid"] = mid

    val params = when (module) {
      "Lyric" -> {
        mutableMapOf<String, Any>(
          "appid" to "3116",
          "clientver" to "11070"
        ).apply { putAll(paramsIn) }
      }

      else -> {
        mutableMapOf<String, Any>(
          "userid" to "0",
          "appid" to "3116",
          "token" to "",
          "clienttime" to (System.currentTimeMillis() / 1000).toInt(),
          "iscorrection" to "1",
          "uuid" to "-",
          "mid" to mid,
          "dfid" to "-", // 简化处理，必要时可实现 dfid 获取与缓存
          "clientver" to "11070",
          "platform" to "AndroidFilter"
        ).apply { putAll(paramsIn) }
      }
    }

    // 计算签名
    val signature = buildSignature(params, data ?: "")
    params["signature"] = signature

    val urlWithQuery = url.toHttpUrl().newBuilder().apply {
      params.forEach { (k, v) -> addQueryParameter(k, v.toString()) }
    }.build()

    val reqBuilder = Request.Builder().url(urlWithQuery)
    headersAll.forEach { (k, v) -> reqBuilder.header(k, v) }

    val req = if (method.equals("POST", ignoreCase = true)) {
      val bodyStr = data ?: ""
      reqBuilder.post(RequestBody.Companion.create("application/json".toMediaType(), bodyStr))
        .build()
    } else {
      reqBuilder.get().build()
    }

    okHttpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IllegalStateException("Kugou 请求失败: HTTP ${resp.code}")
      }
      val bodyStr = resp.body?.string() ?: "{}"
      val obj = JSONObject(bodyStr)
      val errCode = obj.optInt("error_code", 0)
      if (errCode !in listOf(0, 200)) {
        val msg = obj.optString("error_msg", "unknown")
        throw IllegalStateException("Kugou API错误: code=$errCode msg=$msg")
      }
      return obj
    }
  }

  private fun buildSignature(params: Map<String, Any>, data: String): String {
    val sb = StringBuilder()
    sb.append(kgSecret)
    params.toSortedMap().forEach { (k, v) ->
      val vStr = when (v) {
        is JSONObject -> v.toString()
        is JSONArray -> v.toString()
        else -> v.toString()
      }
      sb.append("$k=$vStr")
    }
    sb.append(data)
    sb.append(kgSecret)
    return md5Hex(sb.toString())
  }

  private fun md5Hex(s: String): String {
    val md = MessageDigest.getInstance("MD5")
    val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  /**
   * 将 KRC 明文转换为增强型 LRC 文本：
   * 行格式：[mm:ss.xx] 内容，词内时间标签：<mm:ss.xx>
   * 同时解析 language 标签中的翻译(type=1)，输出为单独的 LRC 文本（用于 LrcParser 合并翻译）
   */
  private fun krcToEnhancedLrc(krc: String): Pair<String, String?> {
    val tagRegex = Regex("""^\[(\w+):([^\]]*)\]$""")
    val lineRegex = Regex("""^\[(\d+),(\d+)\](.*)$""")
    val wordRegex = Regex("""(?:\[\d+,\d+\])?<(\d+),(\d+),\d+>(.*?)(?=(?:<\d+,\d+,\d+>)|$)""")

    data class KrcWord(val start: Int, val end: Int, val text: String)
    data class KrcLine(
      val start: Int,
      val end: Int,
      val words: List<KrcWord>,
      val rawContent: String
    )

    val tags = HashMap<String, String>()
    val origLines = ArrayList<KrcLine>()

    // 先解析原始行与标签
    krc.lineSequence().forEach { raw ->
      val line = raw.trim()
      if (line.isEmpty()) return@forEach
      if (!line.startsWith("[")) return@forEach

      val tm = tagRegex.matchEntire(line)
      if (tm != null) {
        tags[tm.groupValues[1]] = tm.groupValues[2]
        return@forEach
      }

      val lm = lineRegex.matchEntire(line) ?: return@forEach
      val lineStart = lm.groupValues[1].toInt()
      val lineDur = lm.groupValues[2].toInt()
      val content = lm.groupValues[3]
      val lineEnd = lineStart + lineDur

      val words = ArrayList<KrcWord>()
      var matchedAnyWord = false
      wordRegex.findAll(content).forEach { wm ->
        val wStart = lineStart + wm.groupValues[1].toInt()
        val wDur = wm.groupValues[2].toInt()
        val wEnd = wStart + wDur
        val wText = wm.groupValues[3]
        words.add(KrcWord(wStart, wEnd, wText))
        matchedAnyWord = true
      }
      if (!matchedAnyWord) {
        // 整行作为一个词
        words.add(KrcWord(lineStart, lineEnd, content))
      }
      origLines.add(KrcLine(lineStart, lineEnd, words, content))
    }

    // 原文增强LRC
    val sbOrig = StringBuilder()
    for (ln in origLines) {
      sbOrig.append('[').append(LrcParser.formatMs(ln.start)).append(']').append(' ')
      val hasWords = ln.words.any { it.text.isNotBlank() }
      if (hasWords) {
        ln.words.forEach { w ->
          sbOrig.append('<').append(LrcParser.formatMs(w.start)).append('>').append(w.text)
        }
      } else {
        sbOrig.append(ln.rawContent)
      }
      sbOrig.append('\n')
    }
    val enhanced = sbOrig.toString().trimEnd()

    // 翻译LRC（language标签）
    var translation: String? = null
    val langB64 = tags["language"]?.trim()
    if (!langB64.isNullOrEmpty()) {
      try {
        val langJsonStr = Base64.decode(langB64, Base64.DEFAULT).toString(Charsets.UTF_8)
        val langObj = JSONObject(langJsonStr)
        val contentArr = langObj.optJSONArray("content") ?: JSONArray()

        // 逐行翻译(type=1)
        for (i in 0 until contentArr.length()) {
          val item = contentArr.getJSONObject(i)
          val type = item.optInt("type")
          if (type == 1) {
            val lyricContent = item.optJSONArray("lyricContent") ?: JSONArray()
            val sbTrans = StringBuilder()
            // 和 LDDC 的 offset 处理一致：跳过原文中空内容的行
            var offset = 0
            for (lineIdx in origLines.indices) {
              val ln = origLines[lineIdx]
              val hasMeaningful = ln.words.any { it.text.isNotBlank() }
              if (!hasMeaningful) {
                offset += 1
                continue
              }
              val idxInTrans = lineIdx - offset
              if (idxInTrans < 0 || idxInTrans >= lyricContent.length()) continue
              val transLineArr = lyricContent.getJSONArray(idxInTrans)
              val transText = if (transLineArr.length() > 0) transLineArr.getString(0) else ""
              if (transText.isNotBlank()) {
                sbTrans.append('[').append(LrcParser.formatMs(ln.start)).append(']').append(' ')
                  .append(transText).append('\n')
              }
            }
            translation = sbTrans.toString().trimEnd()
            break
          }
        }
      } catch (e: Exception) {
        Timber.w(e, "KRC 翻译解析失败")
      }
    }

    return Pair(enhanced, translation)
  }
}