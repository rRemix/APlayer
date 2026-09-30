@file:OptIn(ExperimentalMaterial3Api::class)

package remix.myplayer.ui.screen.report

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.viewmodel.AnnualReportViewModel
import remix.myplayer.viewmodel.annualReportViewModel

@Composable
fun AnnualReportScreen() {
  val viewModel = annualReportViewModel
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LaunchedEffect(Unit) {
    viewModel.load()
  }

  LaunchedEffect(state.exportIntent) {
    val intent = state.exportIntent
    if (intent != null) {
      context.startActivity(Intent.createChooser(intent, null))
      viewModel.consumeExportIntent()
    }
  }

  BackHandler { viewModel.consumeExportIntent() }

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.annual_report), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    val report = state.report
    if (report == null) {
      Column(
        modifier = Modifier
          .padding(contentPadding)
          .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        TextSecondary(text = stringResource(R.string.no_play_stat_data), fontSize = 16.sp)
      }
      return@Scaffold
    }

    LazyColumn(modifier = Modifier.padding(contentPadding)) {
      item {
        if (state.years.size > 1) {
          YearSelector(
            years = state.years,
            selected = state.year ?: report.year,
            onSelect = viewModel::selectYear
          )
        }
        Spacer(Modifier.height(8.dp))
        MetricCard(report)
        Spacer(Modifier.height(8.dp))
        TopSongsCard(report)
        Spacer(Modifier.height(8.dp))
        TopArtistsCard(report)
        Spacer(Modifier.height(8.dp))
        TopAlbumsCard(report)
        Spacer(Modifier.height(8.dp))
        DistributionCard(report)
        Spacer(Modifier.height(8.dp))
        SourceCard(report)
        Spacer(Modifier.height(8.dp))
        ExportRow(
          onExport = viewModel::exportJsonl,
          onClear = viewModel::clear
        )
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun YearSelector(years: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
  LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
    items(years) { year ->
      val isSelected = year == selected
      TextPrimary(
        text = year.toString(),
        fontSize = 16.sp,
        color = if (isSelected) LocalTheme.current.primary else LocalTheme.current.textSecondary,
        modifier = Modifier
          .padding(8.dp)
          .clickable { onSelect(year) }
      )
    }
  }
}

@Composable
private fun MetricCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_plays), report.plays.toString())
      MetricItem(stringResource(R.string.stat_listen_score), "%.1f".format(report.listenScore))
      MetricItem(stringResource(R.string.stat_completed), report.completedPlays.toString())
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_listen_ms), formatTime(report.listenMs))
      MetricItem(stringResource(R.string.stat_days), report.listenedDays.toString())
      MetricItem(stringResource(R.string.stat_first_listened), report.firstListenedSongs.toString())
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      MetricItem(stringResource(R.string.stat_songs), report.distinctSongs.toString())
      MetricItem(stringResource(R.string.stat_artists), report.distinctArtists.toString())
      MetricItem(stringResource(R.string.stat_albums), report.distinctAlbums.toString())
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    TextPrimary(text = value, fontSize = 20.sp)
    TextSecondary(text = label, fontSize = 12.sp)
  }
}

@Composable
private fun TopSongsCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_songs)) {
    report.topSongs.forEachIndexed { index, item ->
      RankRow(index + 1, item.title, item.artist, item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun TopArtistsCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_artists)) {
    report.topArtists.forEachIndexed { index, item ->
      RankRow(index + 1, item.name, "", item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun TopAlbumsCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_top_albums)) {
    report.topAlbums.forEachIndexed { index, item ->
      RankRow(index + 1, item.name, "", item.listenedMs, item.plays)
    }
  }
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, listenMs: Long, plays: Int) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    TextPrimary(text = "$rank", fontSize = 16.sp, color = LocalTheme.current.secondary)
    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
      TextPrimary(text = title, fontSize = 15.sp)
      if (subtitle.isNotBlank()) {
        TextSecondary(text = subtitle, fontSize = 12.sp)
      }
    }
    Column(horizontalAlignment = Alignment.End) {
      TextPrimary(text = formatTime(listenMs), fontSize = 13.sp)
      TextSecondary(text = "$plays x", fontSize = 12.sp)
    }
  }
}

@Composable
private fun DistributionCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_months)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      report.monthDistribution.forEach { m ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          TextPrimary(text = m.month.toString(), fontSize = 14.sp)
          TextSecondary(text = m.plays.toString(), fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun SourceCard(report: remix.myplayer.data.model.report.AnnualReport) {
  SectionCard(title = stringResource(R.string.stat_sources)) {
    report.sourceBreakdown.forEach { s ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TextPrimary(text = s.source, fontSize = 14.sp)
        TextSecondary(text = s.plays.toString(), fontSize = 13.sp)
      }
    }
  }
}

@Composable
private fun ExportRow(onExport: () -> Unit, onClear: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    SectionButton(text = stringResource(R.string.export_jsonl), onClick = onExport)
    SectionButton(text = stringResource(R.string.clear_play_stats), onClick = onClear)
  }
}

@Composable
private fun SectionButton(text: String, onClick: () -> Unit) {
  androidx.compose.material3.Text(
    text = text,
    color = LocalTheme.current.primary,
    fontSize = 14.sp,
    modifier = Modifier
      .padding(vertical = 12.dp)
      .clickable { onClick() }
  )
}

@Composable
private fun SectionCard(title: String? = null, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    if (title != null) {
      TextPrimary(text = title, fontSize = 16.sp)
      Spacer(Modifier.height(6.dp))
    }
    content()
  }
}

private fun formatTime(ms: Long): String {
  val minutes = ms / 60000.0
  return if (minutes < 60) {
    "%.1f min".format(minutes)
  } else {
    "%.1f h".format(minutes / 60.0)
  }
}
