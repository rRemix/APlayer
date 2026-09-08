# APlayer 播放事件跨平台协议规范

> 供桌面/电脑端应用做同样改动时参考。Android 端已按此实现（Room + JSONL 导出）。
>
> **当前 schemaVersion 已升级到 2**（新增可选字段 + `song_added` 事件）。唯一设计文档见 `docs/play-event-design.md`（第 10/11 章）；PC 端具体实现步骤由后续基于 PC 代码生成的文档提供。
>
> **相关参考**：
> - 导出样例：`docs/samples/play-events-2026.sample.jsonl`（6 条合法 JSONL）

## 0. 总览

播放器在运行中**逐次**记录“播放事件”（每次有效收听一条），用于年度听歌报告。
- Android 端用 **Room** 表 `play_events` 本地存储，可导出 **JSONL**（一行一条事件）。
- 跨平台统一关键字段：`eventId`（用于去重）、`deviceId`、`schemaVersion`、`canonicalId`、`UTC ISO 8601`。
- 汇总统计从事件派生，事件本身是唯一事实来源。

## 1. 播放会话语义

- 会话开始：歌曲开始播放（或切到新歌且正在播放）。
- 累计：仅播放状态下按位置 100ms 递增累计 `listenedMs`；相邻位置增量 >2500ms 视为 seek 不计入；位置回退到接近开头视为单曲循环新开一轮。
- 结束：切歌/自然结束/错误/服务停止/单曲循环重置/暂停后切歌。
- 阈值：有效播放 `listenedMs/durationMs >= 0.1`（未知时长需 `listenedMs >= 1000ms`）；`listenRatio=min(ratio,1)`；`playScore=listenRatio`；`completed = duration>0 && (ratio>=0.9 || endReason==NATURAL_END)`。
- 每会话一条；暂停→恢复同一事件；拖动不虚增。

## 2. `source` 枚举

`SEARCH_CLICK` / `LIBRARY_CLICK` / `PLAYLIST_CLICK` / `QUEUE_AUTO` / `RESUME` / `EXTERNAL_INTENT`

队列首曲用入口来源，其后自动切换记 `QUEUE_AUTO`；单曲循环重开记 `QUEUE_AUTO`。

## 3. `endReason` 枚举

`NATURAL_END` / `SKIP_TO_NEXT` / `SKIP_TO_PREVIOUS` / `PLAYLIST_CHANGED` / `STOP` / `ERROR`

## 4. 事件字段（`PlayEvent`，表 `play_events`，schema v2）

`id`(本地主键)、`schemaVersion`(=2)、`eventId`、`deviceId`、`eventType`(`playback`/`song_added`)、`canonicalId`、`audioId?`、`startedAt/endedAt`(UTC ms)、`durationMs`、`listenedMs`、`listenRatio`、`playScore`、`completed`、`source`、`endReason`、`titleSnapshot`、`artistSnapshot`、`albumSnapshot`、`genreSnapshot?`、`sourceUri?`、`contentHash?`、`pathHint?`、`year/month/day/hour/weekday`，
以及 v2 可选：`songId?`、`artistId?`、`albumId?`、`genreId?`、`playlistId?`、`mediaType?`、`sessionId?`、`gapBeforeMs?`、`gapAfterMs?`、`loopCount`、`outputDevice?`、`isForeground?`、`decoder?`。

索引：`year`、`canonicalId`、`audioId`、`source`、`startedAt`、`eventType`。

## 5. 导出 JSONL

文件 `play-events-<year>.jsonl`；每行一个 JSON；UTF-8；`\n`；时间 UTC ISO 8601；`schemaVersion=2`。
示例见 `docs/samples/play-events-2026.sample.jsonl`。

## 6. 标识生成

- `eventId`=`evt-`+UUID去连字符；`deviceId`：Android=`android-`、PC=`pc-`+UUID去连字符（持久化）。
- `canonicalId`：本地=sha256(规范化元数据)，在线=sha256(来源URI)，前缀 `sha256:`。

## 7. 统计口径

见 `docs/play-event-design.md` 第 5 章、第 10.5 节。

## 8. 注意

- 统计本地化、不依赖账号；提供开关与清除入口。
- `eventType` 预留 `playback`/`song_added`/`song_removed`/`metadata_changed`；本期实现 playback + song_added。
- 同步按 `eventId` 幂等导入，事件不可变，修正产生新事件或更高 `schemaVersion`。
