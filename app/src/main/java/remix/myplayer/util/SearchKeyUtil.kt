package remix.myplayer.util

import remix.myplayer.data.bean.mp3.Album
import remix.myplayer.data.bean.mp3.Artist
import remix.myplayer.data.bean.mp3.Song

/**
 * 统一的搜索关键词生成工具类
 * 用于歌词搜索和封面搜索
 */
object SearchKeyUtil {

    // 检查常见的无效值
    private val invalidValues = setOf(
        "unknown", "<unknown>", "未知歌曲", "未知艺术家", "未知专辑",
        "unknown artist", "unknown album", "unknown song",
        "null", "n/a", "na", "none", "无", "暂无",
        "track", "track 1", "track 01"
    )

    /**
     * 生成歌词搜索关键词列表，按优先级排序
     * 参考LDDC项目的多关键词策略
     */
    fun getSearchKeys(song: Song?): List<String> {
        if (song == null) return emptyList()

        val keywords = mutableListOf<String>()

        val isTitleAvailable = isValidInfo(song.title)
        val isArtistAvailable = isValidInfo(song.artist)
        val isAlbumAvailable = isValidInfo(song.album)

        // 优先级1: 艺术家 - 歌曲名
        if (isTitleAvailable && isArtistAvailable) {
            keywords.add("${song.artist} - ${song.title}")
        }

        // 优先级2: 仅歌曲名
        if (isTitleAvailable) {
            keywords.add(song.title)
        }

        // 优先级3: 专辑 - 歌曲名
        if (isTitleAvailable && isAlbumAvailable && !isArtistAvailable) {
            keywords.add("${song.album} - ${song.title}")
        }

        // 优先级4: 文件名
        if (keywords.isEmpty() && song.isLocal()) {
            val fileName = song.displayName
            if (fileName.isNotBlank()) {
                keywords.add(fileName)
            }
        }

        return keywords.distinct()
    }

    /**
     * 获取单个搜索关键词（最高优先级）
     */
    fun getSearchKey(song: Song?): String {
        return getSearchKeys(song).firstOrNull() ?: ""
    }

    /**
     * 生成网易云音乐搜索关键词（用于封面搜索）
     */
    fun getNetEaseSearchKey(model: Any): String {
        return when (model) {
            is Song -> getNetEaseSearchKey(model.title, model.album, model.artist, true)
            is Album -> getNetEaseSearchKey("", model.album, model.artist, true)
            is Artist -> getNetEaseSearchKey("", "", model.artist, false)
            else -> ""
        }
    }

    /**
     * 生成网易云音乐搜索关键词的核心逻辑
     */
    private fun getNetEaseSearchKey(title: String, album: String, artist: String, searchAlbum: Boolean): String {
        val isTitleAvailable = isValidInfo(title)
        val isAlbumAvailable = isValidInfo(album)
        val isArtistAvailable = isValidInfo(artist)

        if (searchAlbum) {
            // 歌曲名合法
            if (isTitleAvailable) {
                // 艺术家合法
                if (isArtistAvailable) {
                    return "$artist-$title"
                }
                // 专辑名合法
                if (isAlbumAvailable) {
                    return "$album-$title"
                }
            }
            // 根据专辑名字查询
            if (isAlbumAvailable && isArtistAvailable) {
                return "$artist-$album"
            }
        } else {
            if (isArtistAvailable) {
                return artist
            }
        }
        return ""
    }

    /**
     * 信息验证
     */
    private fun isValidInfo(info: String?): Boolean {
        if (info.isNullOrBlank()) return false

        val trimmed = info.trim()
        val lowerCase = trimmed.lowercase()

        if (lowerCase in invalidValues) return false

        // 检查是否只包含数字（通常是无效的标题）
        if (trimmed.all { it.isDigit() }) return false

        // 检查是否包含过多特殊字符
        val specialCharCount = trimmed.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        if (specialCharCount > trimmed.length * 0.5) return false

        return true
    }
}