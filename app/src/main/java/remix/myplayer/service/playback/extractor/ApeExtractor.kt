package remix.myplayer.service.playback.extractor

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.ParsableByteArray
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.Extractor
import androidx.media3.extractor.ExtractorInput
import androidx.media3.extractor.ExtractorOutput
import androidx.media3.extractor.PositionHolder
import androidx.media3.extractor.SeekMap
import androidx.media3.extractor.SeekPoint
import androidx.media3.extractor.TrackOutput
import java.io.EOFException
import java.io.IOException

/**
 * Media3 extractor for Monkey's Audio (APE).
 *
 * The packet layout intentionally mirrors FFmpeg's libavformat/ape.c:
 * each compressed APE frame is prefixed with little-endian nblocks and skip (8 bytes total),
 * and the codec initialization data is version + compressionType + formatFlags (6 bytes).
 */
@OptIn(UnstableApi::class)
class ApeExtractor : Extractor {
  private lateinit var extractorOutput: ExtractorOutput
  private lateinit var trackOutput: TrackOutput

  private var headerParsed = false
  private var currentFrame = 0
  private var pendingSeekTimeUs = C.TIME_UNSET

  private var fileVersion = 0
  private var compressionType = 0
  private var formatFlags = 0
  private var blocksPerFrame = 0L
  private var finalFrameBlocks = 0L
  private var totalFrames = 0
  private var bitsPerSample = 0
  private var channels = 0
  private var sampleRate = 0
  private var wavTailLength = 0L

  private var frames: Array<ApeFrame> = emptyArray()
  private var durationUs = C.TIME_UNSET

  override fun sniff(input: ExtractorInput): Boolean {
    input.resetPeekPosition()
    return try {
      var skipped = 0L
      val header = ByteArray(10)
      while (skipped <= MAX_SNIFF_JUNK_BYTES) {
        if (!input.peekFully(header, 0, header.size, true)) return false
        if (isId3Header(header)) {
          val tagSize = synchsafeInt(header, 6)
          val footerSize = if ((header[5].toInt() and 0x10) != 0) 10 else 0
          val remaining = tagSize.toLong() + footerSize
          if (remaining < 0 || skipped + 10L + remaining > MAX_SNIFF_JUNK_BYTES) return false
          advancePeekFully(input, remaining)
          skipped += 10L + remaining
          continue
        }

        val version = littleEndianU16(header, 4)
        return isApeMagic(header) && version in APE_MIN_VERSION..APE_MAX_VERSION
      }
      false
    } catch (_: EOFException) {
      false
    } finally {
      input.resetPeekPosition()
    }
  }

  override fun init(output: ExtractorOutput) {
    extractorOutput = output
    trackOutput = output.track(0, C.TRACK_TYPE_AUDIO)
    output.endTracks()
  }

  override fun read(input: ExtractorInput, seekPosition: PositionHolder): Int {
    if (!headerParsed) {
      parseHeader(input)
      headerParsed = true
      if (pendingSeekTimeUs != C.TIME_UNSET) {
        currentFrame = findFrameByTimeUs(pendingSeekTimeUs)
        pendingSeekTimeUs = C.TIME_UNSET
      }
    }

    if (currentFrame >= frames.size) return Extractor.RESULT_END_OF_INPUT

    val frame = frames[currentFrame]
    if (input.position < frame.position) {
      skipFullyLong(input, frame.position - input.position)
    } else if (input.position != frame.position) {
      seekPosition.position = frame.position
      return Extractor.RESULT_SEEK
    }

    val prefixBytes = ByteArray(APE_PACKET_PREFIX_SIZE)
    writeLittleEndianU32(prefixBytes, 0, frame.blocks)
    writeLittleEndianU32(prefixBytes, 4, frame.skip.toLong())
    trackOutput.sampleData(ParsableByteArray(prefixBytes), prefixBytes.size)

    var remaining = frame.size.toInt()
    var payloadBytesRead = 0
    while (remaining > 0) {
      val read = trackOutput.sampleData(input, remaining, true)
      if (read == C.RESULT_END_OF_INPUT) break
      if (read <= 0) break
      payloadBytesRead += read
      remaining -= read
    }

    if (payloadBytesRead == 0 && frame.size > 0) {
      return Extractor.RESULT_END_OF_INPUT
    }

    trackOutput.sampleMetadata(
      frame.timeUs,
      C.BUFFER_FLAG_KEY_FRAME,
      APE_PACKET_PREFIX_SIZE + payloadBytesRead,
      0,
      null
    )
    currentFrame++
    return Extractor.RESULT_CONTINUE
  }

  override fun seek(position: Long, timeUs: Long) {
    if (!headerParsed || frames.isEmpty()) {
      currentFrame = 0
      pendingSeekTimeUs = timeUs
      return
    }

    currentFrame = if (position > 0) {
      findFrameByPosition(position)
    } else {
      findFrameByTimeUs(timeUs)
    }
  }

  override fun release() = Unit

  @Throws(IOException::class)
  private fun parseHeader(input: ExtractorInput) {
    val junkLength = skipLeadingId3(input)

    val magicAndVersion = ByteArray(6)
    input.readFully(magicAndVersion, 0, magicAndVersion.size)
    if (!isApeMagic(magicAndVersion)) throw IOException("Invalid APE magic")

    fileVersion = littleEndianU16(magicAndVersion, 4)
    if (fileVersion !in APE_MIN_VERSION..APE_MAX_VERSION) {
      throw IOException("Unsupported APE version: $fileVersion")
    }

    var descriptorLength = 0L
    var headerLength: Long
    var seekTableLength: Long
    var wavHeaderLength: Long

    if (fileVersion >= 3980) {
      readU16(input) // reserved/padding
      descriptorLength = readU32(input)
      headerLength = readU32(input)
      seekTableLength = readU32(input)
      wavHeaderLength = readU32(input)
      readU32(input) // audioDataLength low
      readU32(input) // audioDataLength high
      wavTailLength = readU32(input)
      skipFullyLong(input, 16) // MD5

      if (descriptorLength < MODERN_DESCRIPTOR_SIZE) {
        throw IOException("Invalid APE descriptor length: $descriptorLength")
      }
      skipFullyLong(input, descriptorLength - MODERN_DESCRIPTOR_SIZE)

      compressionType = readU16(input)
      formatFlags = readU16(input)
      blocksPerFrame = readU32(input)
      finalFrameBlocks = readU32(input)
      totalFrames = checkedFrameCount(readU32(input))
      bitsPerSample = readU16(input)
      channels = readU16(input)
      sampleRate = checkedPositiveInt(readU32(input), "sample rate")

      if (headerLength < MODERN_HEADER_SIZE) {
        throw IOException("Invalid APE header length: $headerLength")
      }
      skipFullyLong(input, headerLength - MODERN_HEADER_SIZE)
    } else {
      headerLength = LEGACY_HEADER_SIZE
      compressionType = readU16(input)
      formatFlags = readU16(input)
      channels = readU16(input)
      sampleRate = checkedPositiveInt(readU32(input), "sample rate")
      wavHeaderLength = readU32(input)
      wavTailLength = readU32(input)
      totalFrames = checkedFrameCount(readU32(input))
      finalFrameBlocks = readU32(input)

      if ((formatFlags and MAC_FORMAT_FLAG_HAS_PEAK_LEVEL) != 0) {
        skipFullyLong(input, 4)
        headerLength += 4
      }

      if ((formatFlags and MAC_FORMAT_FLAG_HAS_SEEK_ELEMENTS) != 0) {
        seekTableLength = readU32(input) * 4L
        headerLength += 4
      } else {
        seekTableLength = totalFrames * 4L
      }

      bitsPerSample = when {
        (formatFlags and MAC_FORMAT_FLAG_8_BIT) != 0 -> 8
        (formatFlags and MAC_FORMAT_FLAG_24_BIT) != 0 -> 24
        else -> 16
      }

      blocksPerFrame = when {
        fileVersion >= 3950 -> 73728L * 4L
        fileVersion >= 3900 || (fileVersion >= 3800 && compressionType >= 4000) -> 73728L
        else -> 9216L
      }

      if ((formatFlags and MAC_FORMAT_FLAG_CREATE_WAV_HEADER) == 0) {
        skipFullyLong(input, wavHeaderLength)
      }
    }

    validateHeader(seekTableLength)

    var firstFrame = junkLength + descriptorLength + headerLength + seekTableLength + wavHeaderLength
    if (fileVersion < 3810) firstFrame += totalFrames.toLong()

    val seekEntries = LongArray(totalFrames)
    for (i in 0 until totalFrames) seekEntries[i] = readU32(input)
    skipFullyLong(input, seekTableLength - totalFrames * 4L)

    val parsedFrames = Array(totalFrames) { index ->
      val position = if (index == 0) firstFrame else seekEntries[index] + junkLength
      ApeFrame(
        position = position,
        size = 0L,
        blocks = if (index == totalFrames - 1) finalFrameBlocks else blocksPerFrame,
        skip = 0,
        timeUs = samplesToUs(index * blocksPerFrame)
      )
    }

    for (i in 1 until totalFrames) {
      parsedFrames[i - 1].size = parsedFrames[i].position - parsedFrames[i - 1].position
      if (parsedFrames[i - 1].size <= 0) throw IOException("Invalid APE seek table")
      parsedFrames[i].skip = ((parsedFrames[i].position - parsedFrames[0].position) and 3L).toInt()
    }

    val sourceLength = input.length
    var finalSize = if (sourceLength != C.LENGTH_UNSET.toLong()) {
      sourceLength - parsedFrames.last().position - wavTailLength
    } else {
      -1L
    }
    if (finalSize > 0) finalSize -= finalSize and 3L
    if (finalSize <= 0) finalSize = finalFrameBlocks * 8L
    parsedFrames.last().size = finalSize

    for (frame in parsedFrames) {
      if (frame.skip != 0) {
        frame.position -= frame.skip.toLong()
        frame.size += frame.skip.toLong()
      }
      frame.size = (frame.size + 3L) and -4L
      if (frame.size <= 0 || frame.size > Int.MAX_VALUE - APE_PACKET_PREFIX_SIZE) {
        throw IOException("Invalid APE frame size: ${frame.size}")
      }
    }

    if (fileVersion < 3810) {
      for (i in 0 until totalFrames) {
        val bits = readU8(input)
        if (i > 0 && bits != 0) parsedFrames[i - 1].size += 4L
        parsedFrames[i].skip = (parsedFrames[i].skip shl 3) + bits
      }
    }

    frames = parsedFrames
    val totalSamples = finalFrameBlocks + if (totalFrames > 1) blocksPerFrame * (totalFrames - 1L) else 0L
    durationUs = samplesToUs(totalSamples)

    val extraData = ByteArray(APE_EXTRADATA_SIZE)
    writeLittleEndianU16(extraData, 0, fileVersion)
    writeLittleEndianU16(extraData, 2, compressionType)
    writeLittleEndianU16(extraData, 4, formatFlags)

    val maxInputSize = frames.maxOf { it.size.toInt() + APE_PACKET_PREFIX_SIZE }
    trackOutput.format(
      Format.Builder()
        .setSampleMimeType(MIME_TYPE_APE)
        .setCodecs("ape")
        .setChannelCount(channels)
        .setSampleRate(sampleRate)
        .setMaxInputSize(maxInputSize)
        .setInitializationData(listOf(extraData, byteArrayOf(bitsPerSample.toByte())))
        .build()
    )
    trackOutput.durationUs(durationUs)
    extractorOutput.seekMap(ApeSeekMap(frames, durationUs))
  }

  private fun validateHeader(seekTableLength: Long) {
    if (totalFrames <= 0) throw IOException("APE contains no frames")
    if (channels <= 0) throw IOException("Invalid APE channel count: $channels")
    if (sampleRate <= 0) throw IOException("Invalid APE sample rate: $sampleRate")
    if (blocksPerFrame <= 0 || finalFrameBlocks <= 0) throw IOException("Invalid APE block count")
    if (seekTableLength < totalFrames * 4L) {
      throw IOException("APE seek table is shorter than frame count")
    }
  }

  private fun findFrameByTimeUs(timeUs: Long): Int {
    if (frames.isEmpty()) return 0
    var low = 0
    var high = frames.lastIndex
    var result = 0
    while (low <= high) {
      val mid = (low + high) ushr 1
      if (frames[mid].timeUs <= timeUs) {
        result = mid
        low = mid + 1
      } else {
        high = mid - 1
      }
    }
    return result
  }

  private fun findFrameByPosition(position: Long): Int {
    if (frames.isEmpty()) return 0
    var low = 0
    var high = frames.lastIndex
    var result = 0
    while (low <= high) {
      val mid = (low + high) ushr 1
      if (frames[mid].position <= position) {
        result = mid
        low = mid + 1
      } else {
        high = mid - 1
      }
    }
    return result
  }

  private fun samplesToUs(samples: Long): Long =
    if (sampleRate > 0) samples * C.MICROS_PER_SECOND / sampleRate else 0L

  private data class ApeFrame(
    var position: Long,
    var size: Long,
    val blocks: Long,
    var skip: Int,
    val timeUs: Long,
  )

  private class ApeSeekMap(
    private val frames: Array<ApeFrame>,
    private val durationUs: Long,
  ) : SeekMap {
    override fun isSeekable(): Boolean = frames.isNotEmpty()

    override fun getDurationUs(): Long = durationUs

    override fun getSeekPoints(timeUs: Long): SeekMap.SeekPoints {
      if (frames.isEmpty()) return SeekMap.SeekPoints(SeekPoint.START)

      var low = 0
      var high = frames.lastIndex
      var floor = 0
      while (low <= high) {
        val mid = (low + high) ushr 1
        if (frames[mid].timeUs <= timeUs) {
          floor = mid
          low = mid + 1
        } else {
          high = mid - 1
        }
      }

      val first = SeekPoint(frames[floor].timeUs, frames[floor].position)
      if (first.timeUs >= timeUs || floor == frames.lastIndex) return SeekMap.SeekPoints(first)
      val next = frames[floor + 1]
      return SeekMap.SeekPoints(first, SeekPoint(next.timeUs, next.position))
    }
  }

  companion object {
    const val MIME_TYPE_APE = "audio/ape"

    private const val APE_MIN_VERSION = 3800
    private const val APE_MAX_VERSION = 3990
    private const val APE_EXTRADATA_SIZE = 6
    private const val APE_PACKET_PREFIX_SIZE = 8
    private const val MODERN_DESCRIPTOR_SIZE = 52L
    private const val MODERN_HEADER_SIZE = 24L
    private const val LEGACY_HEADER_SIZE = 32L
    private const val MAX_SNIFF_JUNK_BYTES = 64L * 1024L * 1024L

    private const val MAC_FORMAT_FLAG_8_BIT = 1
    private const val MAC_FORMAT_FLAG_HAS_PEAK_LEVEL = 4
    private const val MAC_FORMAT_FLAG_24_BIT = 8
    private const val MAC_FORMAT_FLAG_HAS_SEEK_ELEMENTS = 16
    private const val MAC_FORMAT_FLAG_CREATE_WAV_HEADER = 32

    private fun isApeMagic(bytes: ByteArray): Boolean =
      bytes.size >= 4 &&
        bytes[0] == 'M'.code.toByte() &&
        bytes[1] == 'A'.code.toByte() &&
        bytes[2] == 'C'.code.toByte() &&
        bytes[3] == ' '.code.toByte()

    private fun isId3Header(bytes: ByteArray): Boolean =
      bytes.size >= 10 &&
        bytes[0] == 'I'.code.toByte() &&
        bytes[1] == 'D'.code.toByte() &&
        bytes[2] == '3'.code.toByte()

    private fun synchsafeInt(bytes: ByteArray, offset: Int): Int =
      ((bytes[offset].toInt() and 0x7f) shl 21) or
        ((bytes[offset + 1].toInt() and 0x7f) shl 14) or
        ((bytes[offset + 2].toInt() and 0x7f) shl 7) or
        (bytes[offset + 3].toInt() and 0x7f)

    private fun littleEndianU16(bytes: ByteArray, offset: Int): Int =
      (bytes[offset].toInt() and 0xff) or ((bytes[offset + 1].toInt() and 0xff) shl 8)

    private fun writeLittleEndianU16(bytes: ByteArray, offset: Int, value: Int) {
      bytes[offset] = value.toByte()
      bytes[offset + 1] = (value ushr 8).toByte()
    }

    private fun writeLittleEndianU32(bytes: ByteArray, offset: Int, value: Long) {
      bytes[offset] = value.toByte()
      bytes[offset + 1] = (value ushr 8).toByte()
      bytes[offset + 2] = (value ushr 16).toByte()
      bytes[offset + 3] = (value ushr 24).toByte()
    }

    @Throws(IOException::class)
    private fun skipLeadingId3(input: ExtractorInput): Long {
      var skipped = 0L
      val header = ByteArray(10)
      while (true) {
        input.resetPeekPosition()
        if (!input.peekFully(header, 0, header.size, true)) return skipped
        if (!isId3Header(header)) return skipped
        val payload = synchsafeInt(header, 6).toLong()
        val footer = if ((header[5].toInt() and 0x10) != 0) 10L else 0L
        val total = 10L + payload + footer
        skipFullyLong(input, total)
        skipped += total
      }
    }

    @Throws(IOException::class)
    private fun readU8(input: ExtractorInput): Int {
      val b = ByteArray(1)
      input.readFully(b, 0, 1)
      return b[0].toInt() and 0xff
    }

    @Throws(IOException::class)
    private fun readU16(input: ExtractorInput): Int {
      val b = ByteArray(2)
      input.readFully(b, 0, 2)
      return littleEndianU16(b, 0)
    }

    @Throws(IOException::class)
    private fun readU32(input: ExtractorInput): Long {
      val b = ByteArray(4)
      input.readFully(b, 0, 4)
      return (b[0].toLong() and 0xffL) or
        ((b[1].toLong() and 0xffL) shl 8) or
        ((b[2].toLong() and 0xffL) shl 16) or
        ((b[3].toLong() and 0xffL) shl 24)
    }

    @Throws(IOException::class)
    private fun checkedFrameCount(value: Long): Int {
      if (value <= 0 || value > Int.MAX_VALUE) throw IOException("Invalid APE frame count: $value")
      return value.toInt()
    }

    @Throws(IOException::class)
    private fun checkedPositiveInt(value: Long, name: String): Int {
      if (value <= 0 || value > Int.MAX_VALUE) throw IOException("Invalid APE $name: $value")
      return value.toInt()
    }

    @Throws(IOException::class)
    private fun skipFullyLong(input: ExtractorInput, length: Long) {
      if (length < 0) throw IOException("Negative skip length: $length")
      var remaining = length
      while (remaining > 0) {
        val step = minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
        input.skipFully(step)
        remaining -= step
      }
    }

    @Throws(IOException::class)
    private fun advancePeekFully(input: ExtractorInput, length: Long) {
      var remaining = length
      while (remaining > 0) {
        val step = minOf(remaining, Int.MAX_VALUE.toLong()).toInt()
        input.advancePeekPosition(step)
        remaining -= step
      }
    }
  }
}
