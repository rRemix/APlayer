package remix.myplayer.lyric.provider

import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Song
import remix.myplayer.util.SearchKeyUtil
import kotlin.math.abs
import kotlin.math.max

/**
 * 歌曲信息评分器，用于评估provider返回的歌曲信息质量
 * 参考LDDC项目的评分算法实现
 */
object SearchScorer {

  private const val MIN_SCORE_THRESHOLD = 55f
  private const val DURATION_TOLERANCE_MS = 4000L

  /**
   * 评分结果
   */
  data class ScoreResult(
    val score: Float,
    val isValid: Boolean = score >= MIN_SCORE_THRESHOLD,
    val titleScore: Float = 0f,
    val artistScore: Float? = null,
    val albumScore: Float? = null,
    val durationMatch: Boolean = true
  )

  /**
   * 计算歌曲匹配评分
   * @param targetSong 目标歌曲
   * @param candidateTitle 候选歌曲标题
   * @param candidateArtist 候选歌曲艺术家
   * @param candidateAlbum 候选歌曲专辑（可选）
   * @param candidateDuration 候选歌曲时长（毫秒，可选）
   * @return 评分结果
   */
  fun calculateSongScore(
    targetSong: Song,
    candidateTitle: String?,
    candidateArtist: String?,
    candidateAlbum: String? = null,
    candidateDuration: Long? = null
  ): ScoreResult {
    // 时长匹配检查
    if (targetSong is Song.Local) {
      if (!checkDurationMatch(targetSong.duration, candidateDuration)) {
        return ScoreResult(score = 0f, durationMatch = false)
      }
    }

    // 计算各项得分
    val titleScore = calculateTitleScore(targetSong.title, candidateTitle)
    val artistScore = calculateArtistScore(targetSong.artist, candidateArtist)
    val albumScore = calculateAlbumScore(targetSong.album, candidateAlbum)

    // 综合评分计算
    val finalScore = calculateFinalScore(titleScore, artistScore, albumScore)

    return ScoreResult(
      score = finalScore,
      titleScore = titleScore,
      artistScore = artistScore,
      albumScore = albumScore,
      durationMatch = true
    )
  }

  /**
   * 计算歌曲匹配评分
   * @param targetSong 目标歌曲
   * @param candidateTitle 候选歌曲标题
   * @param candidateArtist 候选歌曲艺术家
   * @param candidateAlbum 候选歌曲专辑（可选）
   * @param candidateDuration 候选歌曲时长（毫秒，可选）
   * @param keyword 搜索时的关键字
   * @param keyKind 关键字的类型
   * @return 评分结果
   */
  fun calculateSongScoreWithKeyKind(
    targetSong: Song,
    candidateTitle: String?,
    candidateArtist: String?,
    candidateAlbum: String? = null,
    candidateDuration: Long? = null,
    keyword: String,
    keyKind: SearchKeyUtil.KeyKind
  ): ScoreResult {
    if (targetSong is Song.Local) {
      if (!checkDurationMatch(targetSong.duration, candidateDuration)) {
        return ScoreResult(score = 0f, durationMatch = false)
      }
    }
    return if (keyKind == SearchKeyUtil.KeyKind.FILE_NAME) {
      val s1 = textDifference(keyword, candidateTitle ?: "")
      val s2 = textDifference(keyword, "${candidateArtist ?: ""} - ${candidateTitle ?: ""}")
      val s3 = textDifference(keyword, "${candidateTitle ?: ""} - ${candidateArtist ?: ""}")
      val score = (maxOf(s1, s2, s3) * 100f)
      ScoreResult(
        score = score,
        titleScore = score,
        artistScore = null,
        albumScore = null,
        durationMatch = true
      )
    } else {
      calculateSongScore(
        targetSong,
        candidateTitle,
        candidateArtist,
        candidateAlbum,
        candidateDuration
      )
    }
  }

  fun calculateAlbumScore(
    targetAlbum: Album,
    candidateAlbum: String?,
    candidateArtist: String? = null,
  ): ScoreResult {
    val albumScore = calculateAlbumScore(targetAlbum.album, candidateAlbum)
    val artScore = calculateArtistScore(targetAlbum.artist, candidateArtist)

    val score = when {
      albumScore != null && artScore != null -> albumScore * 0.7f + artScore * 0.3f
      albumScore != null -> albumScore
      artScore != null -> artScore * 0.6f
      else -> 0f
    }

    return ScoreResult(
      score = score,
      artistScore = artScore,
      albumScore = albumScore,
      durationMatch = true
    )
  }

  fun calculateArtistScore(
    targetArtist: Artist,
    candidateArtist: String?,
  ): ScoreResult {
    val artScore = calculateArtistScore(targetArtist.artist, candidateArtist)
    return ScoreResult(score = artScore ?: 0f, artistScore = artScore)
  }

  /**
   * 检查时长匹配
   */
  private fun checkDurationMatch(localDuration: Long?, remoteDuration: Long?): Boolean {
    if (localDuration == null || remoteDuration == null) return true
    if (localDuration <= 0 || remoteDuration <= 0) return true

    return abs(localDuration - remoteDuration) <= DURATION_TOLERANCE_MS
  }

  /**
   * 计算标题得分
   */
  private fun calculateTitleScore(localTitle: String?, remoteTitle: String?): Float {
    if (localTitle.isNullOrBlank() || remoteTitle.isNullOrBlank()) return 0f

    val title1 = unifiedSymbol(localTitle.lowercase())
    val title2 = unifiedSymbol(remoteTitle.lowercase())

    if (title1 == title2) return 100f

    // 基础文本相似度
    val baseScore = textSimilarity(title1, title2) * 100

    // 查找共同前缀
    val commonPrefix = findCommonPrefix(title1, title2)
    if (commonPrefix.isEmpty() || commonPrefix == title1 || commonPrefix == title2) {
      return baseScore
    }

    // 处理标题标签（如版本信息等）
    val suffix1 = title1.substring(commonPrefix.length)
    val suffix2 = title2.substring(commonPrefix.length)

    val tags1 = extractTitleTags(suffix1)
    val tags2 = extractTitleTags(suffix2)

    // 计算标签相似度
    val tagSimilarity = calculateTagSimilarity(tags1, tags2)
    val prefixWeight =
      commonPrefix.length.toFloat() / ((suffix1.length + suffix2.length) / 2 + commonPrefix.length)

    val enhancedScore =
      100 * prefixWeight + max(textSimilarity(suffix1, suffix2) * 100, 0f) * (1 - prefixWeight)

    return max(enhancedScore * 0.7f + tagSimilarity, baseScore)
  }

  /**
   * 计算艺术家得分
   */
  private fun calculateArtistScore(localArtist: String?, remoteArtist: String?): Float? {
    // 本地无歌手信息，不参与评分
    if (localArtist.isNullOrBlank()) return null
    // 本地有但网络无，判为0分
    if (remoteArtist.isNullOrBlank()) return 0f

    val artist1 = unifiedSymbol(localArtist.lowercase())
    val artist2 = unifiedSymbol(remoteArtist.lowercase())

    if (artist1 == artist2) return 100f

    // 分割艺术家名称
    val artists1 = splitArtistNames(artist1)
    val artists2 = splitArtistNames(artist2)

    // 计算最大匹配相似度
    return calculateListMaxSimilarity(artists1, artists2) * 100
  }

  /**
   * 计算专辑得分
   */
  private fun calculateAlbumScore(localAlbum: String?, remoteAlbum: String?): Float? {
    // 本地无专辑信息，不参与评分
    if (localAlbum.isNullOrBlank()) return null
    // 本地有但网络无（或无效），判为0分
    if (remoteAlbum.isNullOrBlank()) return 0f

    val album1 = unifiedSymbol(localAlbum.lowercase())
    val album2 = unifiedSymbol(remoteAlbum.lowercase())

    return max(textSimilarity(album1, album2) * 100, 0f)
  }

  /**
   * 计算最终综合得分
   */
  private fun calculateFinalScore(
    titleScore: Float,
    artistScore: Float?,
    albumScore: Float?
  ): Float {
    // 固定权重避免因 max 过早饱和导致区分度丢失
    var finalScore = when {
      artistScore != null && albumScore != null -> titleScore * 0.5f + artistScore * 0.35f + albumScore * 0.15f
      artistScore != null -> titleScore * 0.6f + artistScore * 0.4f
      albumScore != null -> titleScore * 0.75f + albumScore * 0.25f
      else -> titleScore
    }

    // 标题得分过低时的惩罚
    if (titleScore < 30) {
      finalScore = max(0f, finalScore - 35)
    }

    return finalScore
  }

  /**
   * 统一符号处理
   */
  private fun unifiedSymbol(text: String): String {
    return text.trim()
      .replace("（", "(")
      .replace("）", ")")
      .replace("：", ":")
      .replace("！", "!")
      .replace("？", "?")
      .replace("／", "/")
      .replace("＆", "&")
      .replace("＊", "*")
      .replace("＠", "@")
      .replace("＃", "#")
      .replace("＄", "$")
      .replace("％", "%")
      .replace("＼", "\\")
      .replace("｜", "|")
      .replace("＝", "=")
      .replace("＋", "+")
      .replace("－", "-")
      .replace("＜", "<")
      .replace("＞", ">")
      .replace("［", "[")
      .replace("］", "]")
      .replace("｛", "{")
      .replace("｝", "}")
      .replace(Regex("\\s+"), " ")
  }

  /**
   * 计算文本相似度
   */
  private fun textSimilarity(text1: String, text2: String): Float {
    if (text1 == text2) return 1.0f

    val longer = if (text1.length > text2.length) text1 else text2
    val shorter = if (text1.length > text2.length) text2 else text1

    if (longer.isEmpty()) return 1.0f

    return (longer.length - editDistance(longer, shorter)) / longer.length.toFloat()
  }

  private fun textDifference(text1: String, text2: String): Float {
    val a = text1.replace("\\s+".toRegex(), " ")
    val b = text2.replace("\\s+".toRegex(), " ")
    if (a == b) return 1.0f
    val lcs = lcsLength(a, b)
    val denom = a.length + b.length
    if (denom == 0) return 1.0f
    return (2f * lcs) / denom.toFloat()
  }

  private fun lcsLength(s1: String, s2: String): Int {
    val m = s1.length
    val n = s2.length
    val dp = IntArray(n + 1)
    for (i in 1..m) {
      var prev = 0
      for (j in 1..n) {
        val tmp = dp[j]
        dp[j] = if (s1[i - 1] == s2[j - 1]) prev + 1 else maxOf(dp[j], dp[j - 1])
        prev = tmp
      }
    }
    return dp[n]
  }

  /**
   * 计算编辑距离
   */
  private fun editDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
      for (j in 1..s2.length) {
        dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
          dp[i - 1][j - 1]
        } else {
          1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
        }
      }
    }

    return dp[s1.length][s2.length]
  }

  /**
   * 查找共同前缀
   */
  private fun findCommonPrefix(s1: String, s2: String): String {
    val minLength = minOf(s1.length, s2.length)
    for (i in 0 until minLength) {
      if (s1[i] != s2[i]) {
        return s1.substring(0, i)
      }
    }
    return s1.substring(0, minLength)
  }

  /**
   * 提取标题标签
   */
  private fun extractTitleTags(suffix: String): List<String> {
    val tagPattern =
      Regex("""[-<(\[～]([～\]^)>-]*)[～\]^)>-]|(\w+ ?(?:(?:solo |size )?ver(?:sion)?\.?|size|style|mix(?:ed)?|edit(?:ed)?|版|solo))|(纯音乐|inst\.?(?:rumental)|off ?vocal(?: ?[Vv]er.)?)""")
    return tagPattern.findAll(suffix)
      .map { it.value.trim() }
      .filter { it.isNotEmpty() }
      .toList()
  }

  /**
   * 计算标签相似度
   */
  private fun calculateTagSimilarity(tags1: List<String>, tags2: List<String>): Float {
    if (tags1.isEmpty() && tags2.isEmpty()) return 30f
    if (tags1.isEmpty() || tags2.isEmpty()) return 0f

    var matchCount = 0
    val normalizedTags1 = tags1.map { normalizeTag(it) }
    val normalizedTags2 = tags2.map { normalizeTag(it) }

    for (tag1 in normalizedTags1) {
      if (tag1 in normalizedTags2) {
        matchCount++
      }
    }

    return (matchCount.toFloat() / maxOf(tags1.size, tags2.size)) * 30f
  }

  /**
   * 标准化标签
   */
  private fun normalizeTag(tag: String): String {
    return tag.lowercase()
      .replace(Regex("ver(?:sion)?\\.?"), "ver")
      .replace(Regex("伴奏|纯音乐|inst\\.?(?:rumental)|off ?vocal(?: ?[Vv]er.)?"), "inst")
      .replace("mixed", "mix")
      .replace("edited", "edit")
      .replace(Regex("(solo|mix|edit|style|size) ver"), "$1")
      .replace(Regex("(?:tv|anime) ?(?:サイズ|size)?(?: ?edit)?(?: ?ver)?"), "tv size")
  }

  /**
   * 分割艺术家名称
   */
  private fun splitArtistNames(artist: String): List<String> {
    return artist.split(Regex("[,、/\\\\&]"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  }

  /**
   * 计算列表最大相似度
   */
  private fun calculateListMaxSimilarity(list1: List<String>, list2: List<String>): Float {
    if (list1.isEmpty() || list2.isEmpty()) return 0f

    val similarities = mutableListOf<Triple<Int, Int, Float>>()

    for (i in list1.indices) {
      for (j in list2.indices) {
        similarities.add(Triple(i, j, textSimilarity(list1[i], list2[j])))
      }
    }

    similarities.sortByDescending { it.third }

    val usedI = mutableSetOf<Int>()
    val usedJ = mutableSetOf<Int>()
    var totalScore = 0f

    for ((i, j, score) in similarities) {
      if (i !in usedI && j !in usedJ) {
        usedI.add(i)
        usedJ.add(j)
        totalScore += score

        if (usedI.size == list1.size || usedJ.size == list2.size) {
          break
        }
      }
    }

    return totalScore / maxOf(list1.size, list2.size)
  }
}

