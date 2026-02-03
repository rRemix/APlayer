package remix.myplayer.data.model.smb

data class SmbFile(
    val name: String,
    val isDirectory: Boolean,
    val path: String,
    val size: Long,
    val lastModified: Long
) {

    val isAudio: Boolean
        get() {
            val lower = name.lowercase()
            return lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".wav") || lower.endsWith(
                ".m4a"
            ) || lower.endsWith(".ogg")
        }
}
