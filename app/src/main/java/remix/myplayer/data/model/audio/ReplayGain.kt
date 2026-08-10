package remix.myplayer.data.model.audio

/**
 * ReplayGain values read from standard tags
 * (ID3v2 TXXX frames, Vorbis comments, APE tags).
 */
data class ReplayGain(
  val trackGainDb: Float? = null,
  val albumGainDb: Float? = null,
  val trackPeak: Float? = null,
  val albumPeak: Float? = null
) {

  val hasGain: Boolean
    get() = trackGainDb != null || albumGainDb != null

  companion object {

    private const val TRACK_GAIN_KEY = "REPLAYGAIN_TRACK_GAIN"
    private const val ALBUM_GAIN_KEY = "REPLAYGAIN_ALBUM_GAIN"
    private const val TRACK_PEAK_KEY = "REPLAYGAIN_TRACK_PEAK"
    private const val ALBUM_PEAK_KEY = "REPLAYGAIN_ALBUM_PEAK"

    private val gainPattern = Regex("""\s*([-+]?\d+(?:\.\d+)?)\s*(?:dB)?""", RegexOption.IGNORE_CASE)
    private val peakPattern = Regex("""\s*([-+]?\d+(?:\.\d+)?)\s*""")

    fun fromPropertyMap(propertyMap: Map<String, Array<String>>): ReplayGain? {
      val upper = propertyMap.mapKeys { it.key.uppercase() }
      val trackGain = upper[TRACK_GAIN_KEY]?.firstOrNull()?.toFloatOrNull(gainPattern)
      val albumGain = upper[ALBUM_GAIN_KEY]?.firstOrNull()?.toFloatOrNull(gainPattern)
      val trackPeak = upper[TRACK_PEAK_KEY]?.firstOrNull()?.toFloatOrNull(peakPattern)
      val albumPeak = upper[ALBUM_PEAK_KEY]?.firstOrNull()?.toFloatOrNull(peakPattern)
      if (trackGain == null && albumGain == null) {
        return null
      }
      return ReplayGain(trackGain, albumGain, trackPeak, albumPeak)
    }

    private fun String.toFloatOrNull(pattern: Regex): Float? {
      val match = pattern.matchEntire(this) ?: return null
      return match.groupValues[1].toFloatOrNull()
    }
  }
}
