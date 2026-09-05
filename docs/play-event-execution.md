# 播放事件执行文档

> 本文档基于 `play-event-design.md`，把设计落地为 APlayer 具体代码改动，供实现与评审使用。
> 目标：让播放器在运行中自动记录一次次的“播放事件”，为年度听歌报告提供跨平台可导出的原始数据，同时不破坏现有 `History`/最近播放。

## 0. 现状分析与实现策略

- 播放核心在 `service/MusicService.kt`，底层播放实现是 `service/playback/ExoPlayback.kt`。
- ExoPlayback 已提供 `onIsPlayingChanged`、`onPositionChange`(每 100ms)、`onItemTransition`、`onEnded`、`onError` 等回调，足够支撑“会话计时”。
- 播放器用 `Playback.PlayerCallback` 把事件回调给 `MusicService`。因此会话记录器放在 `MusicService` 的成员层最合适，既能拿到歌曲快照，也能用 `PlayEventRepository` 落库。
- 播放来源（source）通过 `MusicService.EXTRA_PLAY_EVENT_SOURCE` 随 Intent 传递，在创建播放队列的入口写入；为减少改动面，为未显式标注的命令提供默认映射。

## 1. 数据模型（Phase 1 采集）

### 1.1 新增文件

| 文件 | 内容 |
|---|---|
| `data/model/audio/PlayEventSource.kt` | 播放来源枚举，含 `value:Int` 与 `fromValue()` |
| `data/model/audio/PlayEventEndReason.kt` | 会话结束原因枚举 |
| `data/db/room/entity/PlayEvent.kt` | Room 实体 `play_events` |
| `data/db/room/dao/PlayEventDao.kt` | 插入/查询/聚合/清除 |
| `repo/PlayEventRepository.kt` | 接口 + `PlayEventRepoImpl` |
| `service/playback/PlayEventRecorder.kt` | 会话计时与落库逻辑 |
| `util/DeviceIdProvider.kt` | 稳定 deviceId |
| `util/CanonicalIdProvider.kt` | canonicalId 生成 |

### 1.2 `PlayEvent` 字段

与设计一致，另加 `year` 便于按年索引：

```kotlin
@Entity(tableName = "play_events")
data class PlayEvent(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val schemaVersion: Int = 1,
  val eventId: String,          // 全局唯一，用于跨端去重
  val deviceId: String,
  val eventType: String = "playback",
  val canonicalId: String,      // 跨平台稳定主键
  val audioId: Long?,           // 仅本地映射
  val startedAt: Long,          // UTC epoch ms
  val endedAt: Long,
  val durationMs: Long,
  val listenedMs: Long,
  val listenRatio: Double,
  val playScore: Double,        // = min(listenedMs/durationMs, 1.0)
  val completed: Boolean,
  val source: String,           // PlayEventSource.name
  val endReason: String,        // PlayEventEndReason.name
  val titleSnapshot: String,
  val artistSnapshot: String,
  val albumSnapshot: String,
  val genreSnapshot: String?,
  val sourceUri: String?,
  val contentHash: String?,
  val pathHint: String?,
  val year: Int,                // 由 startedAt 派生，便于查询
  val month: Int,
  val day: Int,
  val hour: Int,
  val weekday: Int
)
```

### 1.3 迁移

- `AppDatabase.VERSION` 从 `7` 升到 `8`。
- `DbMigrations.migration7to8`：`CREATE TABLE IF NOT EXISTS `play_events`(...)`（字段与 Room 生成一致，保证 schema 校验通过）。
- `buildDatabase` 添加 `addMigrations(migration7to8)`。
- `@Database(entities=[...PlayEvent::class])`、`abstract fun playEventDao()`。

### 1.4 采集规则

- 会话开始：歌曲开始播放（`onIsPlayingChanged(true)`）时，若当前歌曲与上次不同则新建会话。
- 会话累计：在 `onPositionChange`（每 100ms）且正在播放时累加 `position` 增量；增量异常（>2500ms 或 <0）视为 seek，不累计，避免拖动虚增。
- 会话结束：切歌/自然结束/错误/暂停后切歌/服务停止时最终化。
- 丢弃：`listenedMs / durationMs < 0.1` 不落库。
- 完整：`ratio >= 0.9` 或自然结束 → `completed = true`。
- `playScore = min(ratio, 1.0)`。

## 2. 来源传递（source）

- `MusicService` 增加常量 `EXTRA_PLAY_EVENT_SOURCE`。
- `MusicService` 内持有一个 `PlayEventRecorder`，其 `pendingSource` 由入口方法设置。
- 映射（无显式 source 时的默认值）：
  - `PLAY_TEMP` → `SEARCH_CLICK`（显式标注例外：外部打开 → `EXTERNAL_INTENT`）
  - `PLAY_AT` → `LIBRARY_CLICK`
  - 歌单/我的收藏 → `PLAYLIST_CLICK`（在 `LibraryItemPopup` 显式标注）
  - 恢复上次播放 → `RESUME`
  - 队列自动切换 → `QUEUE_AUTO`
- 显式标注的入口：
  - `SearchScreen`：`PLAY_TEMP` 加 `EXTRA_PLAY_EVENT_SOURCE=SEARCH_CLICK`
  - `PlayFromUriUseCase`：`PLAY_TEMP` 加 `EXTRA_PLAY_EVENT_SOURCE=EXTERNAL_INTENT`
  - `LibraryItemPopup`、各 Library 页 `setPlayQueue` → `LIBRARY_CLICK`/`PLAYLIST_CLICK`
  - `MusicService.restorePlayList` → 设置 `RESUME`

## 3. 统计聚合（Phase 2）

`PlayEventRepository` 提供：

- `suspend fun annualReport(year: Int): AnnualReport`
- `suspend fun clear()`

`AnnualReport`：
- `plays`（有效事件数）、`listenScore`（∑playScore）、`completedPlays`、`listenMs`（∑listenedMs）、`listenedDays`、`distinctSongs/artists/albums`、`firstListenedSongs`
- `topSongs/topArtists/topAlbums`（List<TopItem>）
- `monthDistribution`、`hourDistribution`
- `sourceBreakdown`（source → 次数）
- 需 DAO 提供聚合 SQL（按 `year` 分组、按 `canonicalId`/snapshot 聚合、按 `listenedMs` 求和、按 `source` 计数）。

## 4. 界面与互操作（Phase 3）

- 设置新增：
  - `播放统计` 开关（`SettingPrefs.playEventEnabled`）
  - `清除播放统计数据`
  - `导出本年度 JSONL`
- 新增 `ui/screen/report/AnnualReportScreen.kt` 展示核心数字、Top 排行、来源对比。
- `AppNav` 增 `RouteAnnualReport`，`SettingScreen` 增加入口。
- JSONL 导出：把当年事件序列化为一行一条 JSON（含 `track` 对象），写入 `context.getExternalFilesDir` 下，并用 `FileProvider`/Intent 分享。字段按 `play-event-design.md` §3 协议。

## 5. 测试重点

对应设计 §9：<10% 不落库、50% score=0.5、自然结束只写一次、暂停恢复不重复计时、拖动不虚增、随机/主动 source 正确、库升级不影响 History、JSONL 可逐行读取。

## 6. 风险与后续

- canonicalId 第一版用“规范化元数据哈希 / URI”生成，稳定但抗重组；后续可换内容哈希。
- `addedAt` 来自 MediaStore `DATE_ADDED`，本版用“当年首次收听”代替新歌统计，属可接受近似。
- 统计页聚合查询只取结果，不加载全量事件。
