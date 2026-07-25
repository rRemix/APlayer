package remix.myplayer.request.qq

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import remix.myplayer.lyric.LrcParser
import remix.myplayer.lyric.decrypt.QrcDecrypt.qrcDecrypt
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class QQClient @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val okHttpClient: OkHttpClient
) {

  // 通用参数
  private val comm = mutableMapOf<String, Any>(
    "ct" to 11,
    // QQ 音乐目前只接受轻量版客户端标识；桌面版组合会返回 2001。
    "cv" to "1003006",
    "v" to "1003006",
    "os_ver" to "15",
    "phonetype" to "24122RKC7C", // REDMI K80 Pro
    "rom" to "Redmi/miro/miro:15/AE3A.240806.005/OS2.0.10${
      Random.nextInt(
        2,
        6
      )
    }.0.VOMCNXM:user/release-keys",
    "tmeAppID" to "qqmusiclight",
    "nettype" to "NETWORK_WIFI",
    "udid" to "0"
  )

  @Volatile
  private var inited = AtomicBoolean(false)

  @Synchronized
  private fun initIfNeeded() {
    if (inited.get()) return

    val param = mapOf(
      "caller" to 0,
      "uid" to "0",
      "vkey" to 0
    )

    val data = request("GetSession", "music.getSession.session", param)
    val session = data.getJSONObject("session")

    // 服务端返回的是数值 uid，保留其 JSON 类型以匹配轻量版客户端请求。
    comm["uid"] = session.getLong("uid")
    comm["sid"] = session.getString("sid")
    comm["userip"] = session.getString("userip")

    inited.set(true)
  }

  private fun request(method: String, module: String, param: Map<String, Any>): JSONObject {
    if (!inited.get() && method != "GetSession") {
      initIfNeeded()
    }

    val requestData = JSONObject().apply {
      put("comm", JSONObject(comm))
      put("request", JSONObject().apply {
        put("method", method)
        put("module", module)
        put("param", JSONObject(param))
      })
    }

    val domains = arrayOf("u.y.qq.com")
    val url = "https://${domains.random()}/cgi-bin/musicu.fcg"

    val request = Request.Builder()
      .url(url)
      .post(requestData.toString().toRequestBody("application/json".toMediaType()))
      .header("cookie", "tmeLoginType=-1;")
      .header("content-type", "application/json")
//      .header("accept-encoding", "gzip")
      .header("user-agent", "okhttp/3.14.9")
      .build()

    okHttpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        throw IllegalStateException("QQ API请求失败: HTTP ${response.code}")
      }

      val responseData = JSONObject(response.body?.string() ?: "{}")

      if (responseData.optInt("code") != 0 || responseData.getJSONObject("request")
          .optInt("code") != 0
      ) {
        val code =
          if (responseData.optInt("code") != 0) responseData.optInt("code") else responseData.getJSONObject(
            "request"
          ).optInt("code")
        throw IllegalStateException("QQ API请求错误,错误码:$code")
      }

      return responseData.getJSONObject("request").getJSONObject("data")
    }
  }

  /**
   * 搜索歌曲
   */
  fun searchSongList(keyword: String): List<QQSong> {
    val searchId = Random.nextLong(1, 21) * 18014398509481984L +
        Random.nextLong(0, 4194305) * 4294967296L +
        (System.currentTimeMillis() % 86400000)

    val param = mapOf(
      "search_id" to searchId.toString(),
      "remoteplace" to "search.android.keyboard",
      "query" to keyword,
      "search_type" to 0,
      "num_per_page" to 20,
      "page_num" to 1,
      "highlight" to 0,
      "nqc_flag" to 0,
      "page_id" to 1,
      "grp" to 1
    )

    val data = request("DoSearchForQQMusicLite", "music.search.SearchCgiService", param)
    val itemSong = data.getJSONObject("body").optJSONArray("item_song") ?: JSONArray()
    val result = ArrayList<QQSong>()
    for (i in 0 until itemSong.length()) {
      val obj = itemSong.getJSONObject(i)
      val singers = obj.optJSONArray("singer") ?: JSONArray()
      val artists = mutableListOf<String>()
      for (j in 0 until singers.length()) {
        val singer = singers.getJSONObject(j)
        val name = singer.optString("name", "")
        if (name.isNotEmpty()) artists.add(name)
      }
      result.add(
        QQSong(
          id = obj.optLong("id"),
          mid = obj.optString("mid"),
          title = obj.optString("title"),
          subtitle = obj.optString("subtitle"),
          artist = artists,
          album = obj.optJSONObject("album")?.optString("name") ?: "",
          duration = obj.optLong("interval") * 1000,
          language = obj.optInt("language")
        )
      )
    }
    return result
  }

  /**
   * 获取歌词
   */
  fun getLyrics(song: QQSong): Pair<String?, String?> {
    val albumNameB64 = Base64.encodeToString(song.album.toByteArray(), Base64.NO_WRAP)
    val singerNameB64 =
      Base64.encodeToString(song.artist.joinToString("、").toByteArray(), Base64.NO_WRAP)
    val songNameB64 = Base64.encodeToString(song.title.toByteArray(), Base64.NO_WRAP)

    val param = mapOf(
      "albumName" to albumNameB64,
      "crypt" to 1,
      "ct" to 19,
      "cv" to 2111,
      "interval" to song.duration / 1000, // 单位为秒
      "lrc_t" to 0,
      "qrc" to 1,
      "qrc_t" to 0,
      "roma" to 1,
      "roma_t" to 0,
      "singerName" to singerNameB64,
      "songID" to song.id,
      "songName" to songNameB64,
      "trans" to 1,
      "trans_t" to 0,
      "type" to 0
    )

    val response = request("GetPlayLyricInfo", "music.musichallSong.PlayLyricInfo", param)

    // 处理原文歌词
    val lyric = response.optString("lyric", "")
    val qrcT = response.optString("qrc_t", "0")
    val lrcT = response.optString("lrc_t", "0")

    var originalLyric: String? = null
    if (lyric.isNotEmpty() && (qrcT != "0" || lrcT != "0")) {
      val decryptedLyric = qrcDecrypt(lyric)
      if (decryptedLyric != null) {
        originalLyric = convertQrcToBestLrc(decryptedLyric)
      }
    }

    // 处理翻译歌词
    val trans = response.optString("trans", "")
    val transT = response.optString("trans_t", "0")

    var translationLyric: String? = null
    if (trans.isNotEmpty() && transT != "0") {
      val decryptedTrans = qrcDecrypt(trans)
      if (decryptedTrans != null) {
        translationLyric = convertQrcToBestLrc(decryptedTrans)
      }
    }

    return Pair(originalLyric, translationLyric)
  }

  /**
   * 将QRC格式转换为LRC格式
   */
  private fun convertQrcToBestLrc(qrc: String): String {
    val core = extractLyricContentIfXml(qrc)
    // 括号式逐字时间戳，如 (offset,duration[,extra])
    val hasWordTimestampsParen = Regex("""\(\d+,\d+(?:,\d+)?\)""").containsMatchIn(core)
    // 角括号形式逐字时间戳，如 <start,duration,*>word
    val hasWordTimestampsAngle = Regex("""<(?:\d+),(?:\d+),\d+>""").containsMatchIn(core)
    // 标准 LRC 行时间戳，如 [mm:ss.xx]
    val containsLrcTimestamps = Regex("""\[(\d+):(\d+)\.(\d+)\]""").containsMatchIn(core)

    return when {
      hasWordTimestampsParen -> qrcToEnhancedLrc(core).trim()
      hasWordTimestampsAngle -> qrcToLrc(core).trim()
      containsLrcTimestamps -> core.trim()
      else -> qrcToLrc(core).trim()
    }
  }

  private fun extractLyricContentIfXml(raw: String): String {
    return if (raw.startsWith("<?xml") || raw.contains("<QrcInfos")) {
      val regex = Regex("LyricContent=\"(.*?)\"", setOf(RegexOption.DOT_MATCHES_ALL))
      val m = regex.find(raw)
      val content = m?.groupValues?.getOrNull(1)
      content?.replace("\r\n", "\n")?.replace("\r", "\n")?.trim() ?: raw
    } else raw
  }

  /**
   * 将QRC格式转换为增强LRC格式
   */
  private fun qrcToEnhancedLrc(qrc: String): String {
    val lines = qrc.lines()
    val result = StringBuilder()

    for (raw in lines) {
      val line = raw.trim()
      if (line.isEmpty()) continue

      // 标签行: [ti:], [ar:], [offset:] 等保留
      if (line.matches(Regex("^\\[\\w+:.*\\]$"))) {
        result.append(line).append("\n")
        continue
      }

      var startMs: Int? = null
      var content = line

      // LRC行: [mm:ss.xx]content
      val lrcMatch = Regex("^\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)$").matchEntire(line)
      if (lrcMatch != null) {
        val m = lrcMatch.groupValues
        startMs = m[1].toInt() * 60000 + m[2].toInt() * 1000 + m[3].toInt() * 10
        content = m[4]
      } else {
        // QRC行: [start,duration]content
        val qrcMatch = Regex("^\\[(\\d+),(\\d+)\\](.*)$").matchEntire(line)
        if (qrcMatch != null) {
          startMs = qrcMatch.groupValues[1].toInt()
          content = qrcMatch.groupValues[3]
        }
      }

      if (startMs == null) {
        // 其他情况直接附加，避免丢失
        result.append(line).append("\n")
        continue
      }

      // 行级时间戳（行头照样保留）
      result.append("[").append(LrcParser.formatMs(startMs)).append("]")

      // 词级(后缀): (offset,duration[,extra]) 紧随词后；offset 为绝对毫秒
      val tagPattern = Regex("""\((\d+),(?:\d+)(?:,\d+)?\)""")
      var last = 0
      for (tag in tagPattern.findAll(content)) {
        val wordText = content.substring(last, tag.range.first)
        val wordOffsetAbs = tag.groupValues[1].toInt()
        result.append("<").append(LrcParser.formatMs(wordOffsetAbs)).append(">")
          .append(wordText)
        last = tag.range.last + 1
      }
      // 尾部剩余文本（无时间标签）
      val tail = content.substring(last)
      if (tail.isNotEmpty()) {
        result.append(tail)
      }

      result.append("\n")
    }

    return result.toString().trim()
  }

  private fun qrcToLrc(qrc: String): String {
    val lines = qrc.lines()
    val result = StringBuilder()

    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.isEmpty() || !trimmed.startsWith("[")) continue

      // 解析标签行
      if (trimmed.matches(Regex("^\\[\\w+:.*\\]$"))) {
        result.append(trimmed).append("\n")
        continue
      }

      // 解析时间行
      val lineMatch = Regex("^\\[(\\d+),(\\d+)\\](.*)$").matchEntire(trimmed)
      if (lineMatch != null) {
        val startMs = lineMatch.groupValues[1].toInt()
        val content = lineMatch.groupValues[3]

        result.append("[").append(LrcParser.formatMs(startMs)).append("]")

        // 解析词内时间标签
        val wordPattern =
          Regex("(?:\\[\\d+,\\d+\\])?<(\\d+),(\\d+),\\d+>(.*?)(?=<\\d+,\\d+,\\d+>|$)")
        val wordMatches = wordPattern.findAll(content)

        var hasWords = false
        for (wordMatch in wordMatches) {
          val wordStartMs = startMs + wordMatch.groupValues[1].toInt()
          val wordText = wordMatch.groupValues[3]
          result.append("<").append(LrcParser.formatMs(wordStartMs)).append(">").append(wordText)
          hasWords = true
        }

        if (!hasWords) {
          result.append(content)
        }

        result.append("\n")
      }
    }

    return result.toString().trim()
  }

}
