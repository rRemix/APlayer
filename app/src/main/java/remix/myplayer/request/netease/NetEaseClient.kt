package remix.myplayer.request.netease

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import remix.myplayer.lyric.LrcParser
import remix.myplayer.lyric.decrypt.NetEaseEapiCrypt.eapiParamsEncrypt
import remix.myplayer.lyric.decrypt.NetEaseEapiCrypt.eapiResponseDecrypt
import timber.log.Timber
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import androidx.core.content.edit


/**
 * 新的网易云接口
 */
@Singleton
class NetEaseClient @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val okHttpClient: OkHttpClient
) {

  private val json = Json { ignoreUnknownKeys = true }
  private val apiBase = "https://interface.music.163.com"
  private val cacheKeyAes = ")(13daqP@ssw0rd~".toByteArray()

  @Volatile
  private var inited = false

  @Volatile
  private var expireTs: Long = 0L
  private val cookies: LinkedHashMap<String, String> = LinkedHashMap()
  private var userId: Long = 0L

  private fun nowSeconds() = System.currentTimeMillis() / 1000

  private fun genMac(): String {
    return List(6) { "%02X".format(Random.Default.nextInt(0, 256)) }.joinToString(":")
  }

  private fun genRandomUpperLetters(n: Int): String {
    val sb = StringBuilder(n)
    repeat(n) {
      sb.append(('A' + Random.Default.nextInt(26)))
    }
    return sb.toString()
  }

  private fun genTokenHex(bytes: Int): String {
    val arr = ByteArray(bytes) { Random.Default.nextInt(0, 256).toByte() }
    return arr.joinToString("") { "%02x".format(it) }
  }

  private fun getDeviceId(): String {
    val sp = context.getSharedPreferences("netease_config", Context.MODE_PRIVATE)
    var deviceId = sp.getString("device_id", null)
    if (deviceId.isNullOrEmpty()) {
      deviceId = genTokenHex(16)
      sp.edit { putString("device_id", deviceId) }
    }
    return deviceId
  }

  private fun getAnonymousUsername(deviceId: String): String {
    val xorKey = "3go8&$8*3*3h0k(2)2"
    val xored = deviceId.mapIndexed { i, ch ->
      (ch.code xor xorKey[i % xorKey.length].code).toChar()
    }.joinToString("")
    val md5 = MessageDigest.getInstance("MD5").digest(xored.toByteArray(Charsets.UTF_8))
    val md5b64 = Base64.encodeToString(md5, Base64.NO_WRAP)
    val combined = "$deviceId $md5b64"
    return Base64.encodeToString(combined.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
  }

  private fun getParamsHeader(cookies: Map<String, String>): String {
    val obj = JSONObject()
    obj.put("clientSign", cookies["clientSign"])
    obj.put("os", cookies["os"])
    obj.put("appver", cookies["appver"])
    obj.put("deviceId", cookies["deviceId"])
    obj.put("requestId", 0)
    obj.put("osver", cookies["osver"])
    return obj.toString()
  }

  private fun buildCookieHeader(): String {
    return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
  }

  @Synchronized
  private fun initIfNeeded() {
    if (inited && expireTs > nowSeconds()) return

    val preCookies = LinkedHashMap<String, String>().apply {
      put("os", "pc")
      put("deviceId", getDeviceId())
      put("osver", "Microsoft-Windows-10--build-${Random.Default.nextInt(200, 300)}00-64bit")
      val clientSign = "${genMac()}@@@${genRandomUpperLetters(8)}@@@@@@${genTokenHex(32)}"
      put("clientSign", clientSign)
      put("channel", "netease")
      put(
        "mode",
        listOf(
          "MS-iCraft B760M WIFI",
          "ASUS ROG STRIX Z790",
          "MSI MAG B550 TOMAHAWK",
          "ASRock X670E Taichi"
        ).random()
      )
      put("appver", "3.1.3.203419")
    }

    val path = "/eapi/register/anonimous"
    val params = JSONObject().apply {
      put("username", getAnonymousUsername(preCookies["deviceId"]!!))
      put("e_r", true)
      put("header", getParamsHeader(preCookies))
    }
    val bodyStr = eapiParamsEncrypt(path.replace("eapi", "api"), params)
    val req = Request.Builder()
      .url(apiBase + path)
      .post(
        RequestBody.Companion.create(
          "application/x-www-form-urlencoded".toMediaType(),
          bodyStr
        )
      )
      .header("accept", "*/*")
      .header("content-type", "application/x-www-form-urlencoded")
      .header("Cookie", preCookies.entries.joinToString("; ") { "${it.key}=${it.value}" })
      .header("mconfig-info", """{"IuRPVVmc3WWul9fT":{"version":733184,"appver":"3.1.3.203419"}}""")
      .header("origin", "orpheus://orpheus")
      .header(
        "user-agent",
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419"
      )
      .header("sec-ch-ua", "\"Chromium\";v=\"91\"")
      .header("sec-ch-ua-mobile", "?0")
      .header("sec-fetch-site", "cross-site")
      .header("sec-fetch-mode", "cors")
      .header("sec-fetch-dest", "empty")
//      .header("accept-encoding", "gzip, deflate, br")
      .header("accept-language", "en-US,en;q=0.9")
      .build()

    okHttpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IllegalStateException("NetEase anonymous login failed: HTTP ${resp.code}")
      }
      val respBytes = resp.body?.bytes() ?: ByteArray(0)
      val jsonStr = eapiResponseDecrypt(respBytes)
      val obj = JSONObject(jsonStr)
      Timber.Forest.i("ne anonymous login code: ${obj.optInt("code")}")
      // Collect cookies
      val setCookies = resp.headers("Set-Cookie")
      val cookieMap = LinkedHashMap<String, String>(preCookies)
      setCookies.forEach {
        val part = it.substringBefore(';')
        val kv = part.split('=', limit = 2)
        if (kv.size == 2) {
          val k = kv[0]
          val v = kv[1]
          if (k in listOf("NMTID", "MUSIC_A", "__csrf")) {
            cookieMap[k] = v
          }
        }
      }
      // remove empty values
      val iterator = cookieMap.iterator()
      while (iterator.hasNext()) {
        val e = iterator.next()
        if (e.value.isEmpty()) iterator.remove()
      }
      cookies.clear()
      cookies.putAll(cookieMap)

      userId = obj.optLong("userId", 0L)
      // csrf 15天过期，这里设置 10 天缓存
      expireTs = nowSeconds() + 864000
      inited = true
    }
  }

  private fun eapiRequest(path: String, params: JSONObject): JSONObject {
    params.put("e_r", true)
    params.put("header", getParamsHeader(cookies))
    val bodyStr = eapiParamsEncrypt(path.replace("eapi", "api"), params)

    initIfNeeded()

    val req = Request.Builder()
      .url(apiBase + path)
      .post(
        RequestBody.Companion.create(
          "application/x-www-form-urlencoded".toMediaType(),
          bodyStr
        )
      )
      .header("accept", "*/*")
      .header("content-type", "application/x-www-form-urlencoded")
      .header("Cookie", buildCookieHeader())
      .header("mconfig-info", """{"IuRPVVmc3WWul9fT":{"version":733184,"appver":"3.1.3.203419"}}""")
      .header("origin", "orpheus://orpheus")
      .header(
        "user-agent",
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.3.203419"
      )
      .header("sec-ch-ua", "\"Chromium\";v=\"91\"")
      .header("sec-ch-ua-mobile", "?0")
      .header("sec-fetch-site", "cross-site")
      .header("sec-fetch-mode", "cors")
      .header("sec-fetch-dest", "empty")
//      .header("accept-encoding", "gzip, deflate, br")
      .header("accept-language", "en-US,en;q=0.9")
      .build()

    okHttpClient.newCall(req).execute().use { resp ->
      if (!resp.isSuccessful) {
        throw IllegalStateException("NetEase eapi request failed: HTTP ${resp.code}")
      }
      val respBytes = resp.body?.bytes() ?: ByteArray(0)
      val jsonStr = eapiResponseDecrypt(respBytes)
      val obj = JSONObject(jsonStr)
      if (obj.optInt("code") != 200) {
        val msg = obj.optString("message", "unknown")
        throw IllegalStateException("ne API error: code=${obj.optInt("code")} msg=$msg")
      }
      return obj
    }
  }

  /**
   * 将 yrc/ytlrc（每字时间格式）转换为常见增强型 LRC 文本：
   * 行格式：[mm:ss.xx]，词内时间标签：<mm:ss.xx>
   */
  private fun yrcToEnhancedLrc(yrc: String): String {
    val lineRegex = Regex("^\\[(\\d+),(\\d+)\\](.*)$")
    val wordRegex = Regex(
      "(?:\\[\\d+,\\d+\\])?\\((\\d+),(\\d+),\\d+\\)([\\s\\S]*?)(?=\\(\\d+,\\d+,\\d+\\)|$)",
      setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)
    )
    val sb = StringBuilder()
    yrc.lineSequence().forEach { rawLine ->
      val line = rawLine.trim()
      if (!line.startsWith("[")) return@forEach
      val m = lineRegex.matchEntire(line) ?: return@forEach
      val (startStr, _, content) = m.destructured
      val lineStartMs = startStr.toInt()

      sb.append('[').append(LrcParser.formatMs(lineStartMs)).append(']').append(' ')

      var matchedAnyWord = false
      wordRegex.findAll(content).forEach { wm ->
        val wordStartMs = wm.groupValues[1].toInt()
        val wordContent = wm.groupValues[3]
        sb.append('<').append(LrcParser.formatMs(wordStartMs)).append('>').append(wordContent)
        matchedAnyWord = true
      }

      if (!matchedAnyWord) {
        sb.append(content)
      }
      sb.append('\n')
    }
    return sb.toString().trim()
  }

  /**
   * 搜索歌曲
   */
  fun searchSongList(keyword: String): List<NetEaseSong> {
    val params = JSONObject().apply {
      put("limit", "20")
      put("offset", "0")
      put("keyword", keyword)
      put("scene", "NORMAL")
      put("needCorrect", "true")
    }
    val obj = eapiRequest("/eapi/search/song/list/page", params)
    val data = obj.optJSONObject("data") ?: return emptyList()
    val resources = data.optJSONArray("resources") ?: JSONArray()
    if (resources.length() == 0) return emptyList()
    val result = mutableListOf<NetEaseSong>()
    for (i in 0 until resources.length()) {
      val baseInfo = resources.optJSONObject(i)?.optJSONObject("baseInfo")
      val simple = baseInfo?.optJSONObject("simpleSongData")
      if (simple != null) {
        try {
          result.add(json.decodeFromString<NetEaseSong>(simple.toString()))
        } catch (e: Exception) {
          Timber.w("searchSongList: $e")
        }
      }
    }
    return result
  }

  /**
   * 搜索专辑
   */
  fun searchAlbumList(keyword: String): List<NetEaseAlbum> {
    val params = JSONObject().apply {
      put("limit", "20")
      put("offset", "0")
      put("s", keyword)
      put("queryCorrect", "true")
    }
    val obj = eapiRequest("/eapi/v1/search/album/get", params)
    val albums = obj.optJSONObject("result")?.optJSONArray("albums") ?: return emptyList()
    val result = mutableListOf<NetEaseAlbum>()
    for (i in 0 until albums.length()) {
      val item = albums.optJSONObject(i) ?: continue
      try {
        result.add(json.decodeFromString<NetEaseAlbum>(item.toString()))
      } catch (e: Exception) {
        Timber.w("searchAlbumList: $e")
      }
    }
    return result
  }

  /**
   * 搜索艺术家
   */
  fun searchArtistList(keyword: String): List<NetEaseArtist> {
    val params = JSONObject().apply {
      put("limit", "20")
      put("offset", "0")
      put("s", keyword)
      put("queryCorrect", "true")
    }
    val obj = eapiRequest("/eapi/v1/search/artist/get", params)
    val artists = obj.optJSONObject("result")?.optJSONArray("artists") ?: return emptyList()
    val result = mutableListOf<NetEaseArtist>()
    for (i in 0 until artists.length()) {
      val item = artists.optJSONObject(i) ?: continue
      try {
        result.add(json.decodeFromString<NetEaseArtist>(item.toString()))
      } catch (e: Exception) {
        Timber.w("searchArtistList: $e")
      }
    }
    return result
  }

  /**
   * 获取歌词，返回 Pair(lrcText, tlyricText?)
   * 逻辑：
   * - 若存在增强歌词(yrc)，返回增强歌词与增强翻译(ytlrc)
   * - 否则返回普通歌词(lrc)与其普通翻译(tlyric)
   */
  fun getLyrics(song: NetEaseSong): Pair<String?, String?> {
    val params = JSONObject().apply {
      put("id", song.id)
      put("lv", "-1")
      put("tv", "-1")
      put("rv", "-1")
      put("yv", "-1")
    }
    val obj = eapiRequest("/eapi/song/lyric/v1", params)

    val yrc = obj.optJSONObject("yrc")?.optString("lyric")
    if (!yrc.isNullOrBlank()) {
      return Pair(yrcToEnhancedLrc(yrc), obj.optJSONObject("ytlrc")?.optString("lyric"))
    }

    // 没有增强歌词，使用普通歌词与其翻译
    return Pair(
      obj.optJSONObject("lrc")?.optString("lyric"),
      obj.optJSONObject("tlyric")?.optString("lyric")
    )
  }
}
