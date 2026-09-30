package remix.myplayer.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import remix.myplayer.data.db.room.entity.PlayEvent
import remix.myplayer.data.model.report.AnnualReport
import remix.myplayer.data.model.report.PlayEventExport
import remix.myplayer.data.model.report.TrackExport
import remix.myplayer.repo.PlayEventRepository
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AnnualReportViewModel @Inject constructor(
  private val playEventRepository: PlayEventRepository,
  @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _state = MutableStateFlow(ReportUiState())
  val state = _state.asStateFlow()

  fun load() {
    viewModelScope.launch {
      val years = playEventRepository.availableYears()
      val year = _state.value.year.takeIf { it in years } ?: (years.firstOrNull() ?: currentYear())
      val report = playEventRepository.annualReport(year)
      _state.value = ReportUiState(
        years = years,
        year = year,
        report = report,
        loading = false
      )
    }
  }

  fun selectYear(year: Int) {
    if (_state.value.year == year) return
    _state.value = _state.value.copy(year = year, loading = true)
    viewModelScope.launch {
      _state.value = _state.value.copy(
        report = playEventRepository.annualReport(year),
        loading = false
      )
    }
  }

  fun clear() {
    viewModelScope.launch {
      playEventRepository.clear()
      load()
    }
  }

  fun exportJsonl() {
    viewModelScope.launch {
      val year = _state.value.year ?: return@launch
      val events = playEventRepository.eventsOf(year)
      if (events.isEmpty()) return@launch
      val intent = withContext(Dispatchers.IO) {
        writeAndShare(events)
      }
      _state.value = _state.value.copy(
        exportIntent = intent,
        exportRequestId = _state.value.exportRequestId + 1
      )
    }
  }

  fun consumeExportIntent() {
    _state.value = _state.value.copy(exportIntent = null)
  }

  private suspend fun writeAndShare(events: List<PlayEvent>): Intent? = withContext(Dispatchers.IO) {
    val year = _state.value.year ?: return@withContext null
    val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "play_event")
    dir.mkdirs()
    val file = File(dir, "play-events-%d.jsonl".format(year))
    val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    file.bufferedWriter().use { writer ->
      events.forEach { event ->
        writer.write(json.encodeToString(PlayEventExport.serializer(), event.toExport()))
        writer.newLine()
      }
    }
    try {
      val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
      Intent(Intent.ACTION_SEND)
        .setType("application/jsonl")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (e: Exception) {
      null
    }
  }

  private fun PlayEvent.toExport(): PlayEventExport {
    return PlayEventExport(
      schemaVersion = schemaVersion,
      eventId = eventId,
      deviceId = deviceId,
      eventType = eventType,
      startedAt = formatIso(startedAt),
      endedAt = formatIso(endedAt),
      durationMs = durationMs,
      listenedMs = listenedMs,
      listenRatio = listenRatio,
      playScore = playScore,
      completed = completed,
      source = source,
      endReason = endReason,
      track = TrackExport(
        canonicalId = canonicalId,
        title = titleSnapshot,
        artist = artistSnapshot,
        album = albumSnapshot,
        durationMs = durationMs
      ),
      songId = songId,
      artistId = artistId,
      albumId = albumId,
      genreId = genreId,
      playlistId = playlistId,
      mediaType = mediaType,
      sessionId = sessionId,
      gapBeforeMs = gapBeforeMs,
      gapAfterMs = gapAfterMs,
      loopCount = loopCount,
      outputDevice = outputDevice,
      isForeground = isForeground,
      decoder = decoder
    )
  }

  private fun formatIso(epochMs: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(epochMs)
  }

  private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}

data class ReportUiState(
  val years: List<Int> = emptyList(),
  val year: Int? = null,
  val report: AnnualReport? = null,
  val loading: Boolean = true,
  val exportIntent: Intent? = null,
  val exportRequestId: Int = 0
)
