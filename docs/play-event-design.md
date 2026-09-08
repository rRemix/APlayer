# 播放事件功能设计（统一文档 · 已合并 v2 升级）

> 本文档是统一的“播放事件 + 年度报告”设计文档，已合并 v2。v1 为初版，v2 见第 10 章，跨端说明见第 11 章。跨平台协议以本文档为准。

## 1. 目标

新增本地播放事件记录能力，为年度听歌报告提供可靠的原始数据，同时保留现有 `History` 表和最近播放功能。Room 只是 Android 端的存储方式，事件从第一版开始遵循跨平台协议，可导出为 JSON/JSONL 并被电脑端直接导入。

## 2. 数据模型

新增 Room Entity：`PlayEvent`。

| 字段 | 说明 |
|---|---|
| `schemaVersion` | 事件协议版本 |
| `eventId` | 全局唯一事件 ID，用于同步去重 |
| `deviceId` | 产生事件的设备 ID |
| `eventType` | 第一版固定为 `playback` |
| `canonicalId` | 跨平台稳定歌曲 ID |
| `audioId` | Android 本地歌曲 ID，仅作本地映射 |
| `startedAt` | 开始播放时间，内部可用 Long |
| `endedAt` | 播放结束时间 |
| `durationMs` | 歌曲总时长 |
| `listenedMs` | 实际累计收听时长 |
| `listenRatio` | 收听比例 |
| `playScore` | 统计使用的收听积分 |
| `completed` | 是否完整播放 |
| `source` | 播放来源 |
| `endReason` | 结束原因 |
| `titleSnapshot` | 播放时的歌曲名 |
| `artistSnapshot` | 播放时的歌手 |
| `albumSnapshot` | 播放时的专辑 |
| `genreSnapshot` | 播放时的流派，可选 |
| `sourceUri` | 歌曲来源 URI，可选 |
| `contentHash` | 音频内容哈希，可选 |
| `pathHint` | 相对路径提示，可选 |

歌曲实体或仓库还应维护 `addedAt`、`firstPlayedAt`、`lastPlayedAt`。远程歌曲可能没有稳定本地 ID，因此事件中必须保留 `canonicalId` 和歌曲信息快照。

## 3. 跨平台协议

```json
{
  "schemaVersion": 1,
  "eventId": "01JABCD1234567890",
  "deviceId": "android-xxxxxxxx",
  "eventType": "playback",
  "startedAt": "2026-08-28T08:30:00Z",
  "endedAt": "2026-08-28T08:33:45Z",
  "durationMs": 245000,
  "listenedMs": 225000,
  "listenRatio": 0.9184,
  "playScore": 0.9184,
  "completed": true,
  "source": "SEARCH_CLICK",
  "endReason": "NATURAL_END",
  "track": {
    "canonicalId": "sha256:...",
    "title": "歌曲名",
    "artist": "歌手",
    "album": "专辑",
    "durationMs": 245000
  }
}
```

规范要求：

- `canonicalId` 是跨平台主标识，不能使用 Android `audioId`。
- 本地文件优先使用内容哈希或规范化元数据哈希；在线来源优先使用来源系统稳定 ID。
- `sourceUri`、`contentHash`、`pathHint` 是辅助匹配字段。
- 导出协议统一使用 UTC ISO 8601，例如 `2026-08-28T08:30:00Z`。
- `listenRatio`、`playScore`、`completed` 和日期分组字段属于派生值，原始依据是播放时间和收听时长。
- `eventId`、`deviceId` 和 `schemaVersion` 必须存在。

优先支持 JSONL，每行一条完整事件，例如 `play-events-2026.jsonl`。同步时按 `eventId` 合并，重复事件直接忽略；事件不可变，修正应产生新事件或更高协议版本。

预留事件类型：`playback`、`song_added`、`song_removed`、`metadata_changed`。第一版只实现 `playback`。

## 4. 播放会话生命周期

1. 播放器进入 `STATE_READY` 且开始播放时创建内存中的播放会话。
2. 播放、暂停、恢复时累计真实播放时间。
3. 切歌、播放结束、播放错误、服务停止或应用退出时结束会话。
4. 通过播放位置变化计算 `listenedMs`，不能直接使用最终播放位置，避免拖动进度条被误判为听完。
5. `listenedMs / durationMs < 0.1` 时丢弃事件。
6. 达到有效阈值后保存事件，并计算 `playScore`。

暂停后继续播放属于同一事件。循环播放时，每一轮单独记录。总时长未知时只保存实际收听时长，不计算比例。

## 5. 统计规则

```text
listenRatio < 0.1          -> 忽略
0.1 <= ratio < 1           -> playScore = ratio
ratio >= 0.9 或自然结束    -> completed = true
```

“有效播放次数”是有效事件数量；“收听积分”是所有有效事件的 `playScore` 之和；“完整播放数”是 `completed = true` 的事件数量。

## 6. 播放来源

创建播放队列或调用播放入口时，将来源传递到 `MusicService` / `ExoPlayback`：

- `SEARCH_CLICK`
- `LIBRARY_CLICK`
- `PLAYLIST_CLICK`
- `QUEUE_AUTO`
- `RESUME`
- `EXTERNAL_INTENT`

## 7. 数据库与同步

- 提升 `AppDatabase` 版本并增加迁移。
- 新增 `PlayEventDao`，支持按年份、歌曲、歌手、专辑、来源和日期范围查询。
- 对 `startedAt`、`canonicalId`、`audioId`、`source` 建立索引。
- 统计页面只查询聚合结果，避免一次加载全部事件。
- 保留现有 `History`，避免影响历史页面和最近播放逻辑。
- 增加 JSONL 导出；导入和跨设备同步可作为后续阶段。

## 8. 实现阶段

### 第一阶段：采集

- 新增 Entity、DAO、Repository 和数据库迁移。
- 生成稳定的 `deviceId`，并为事件生成全局唯一 `eventId`。
- 按协议保存 `canonicalId`、UTC 时间和歌曲快照。
- 在 `MusicService` 接入播放会话计时和主动播放来源。

### 第二阶段：统计

- 增加年度聚合查询。
- 计算播放次数、收听积分、完整播放数、时长、天数和时段分布。
- 增加新增歌曲和首次收听统计。

### 第三阶段：界面与互操作

- 增加年度报告入口和年份选择。
- 展示核心数字、排行和播放来源对比。
- 增加关闭统计、清除统计和 JSONL 导出能力。
- 电脑端支持按 `eventId` 幂等导入。

## 9. 测试重点

- 播放不足 10% 时不会产生事件。
- 播放 50% 时产生 `playScore = 0.5`。
- 播放完成时只产生一个完整事件。
- 暂停恢复不会重复计算时间。
- 拖动进度条不会虚增 `listenedMs`。
- 随机播放和主动点击的 `source` 正确。
- 数据库升级不会影响既有 `History` 数据。
- 导出的 JSONL 能被电脑端逐行读取，并按 `eventId` 幂等合并。

---

## 10. 版本 2 数据层升级（已落地，含本次扩充）

> 本节为 v2 升级。**只动数据层**：存储结构、采集字段、导出协议、可登记的事件类型；报告层可视化/分享等后续再做。

### 10.1 新增字段（全部可选，向前兼容）

| 字段 | 类型 | 用途 |
|---|---|---|
| `songId` | String? | 播放器内部歌曲 ID |
| `artistId` | String? | 歌手稳定 ID |
| `albumId` | String? | 专辑稳定 ID |
| `genreId` | String? | 流派稳定 ID |
| `playlistId` | String? | 来源歌单 ID |
| `mediaType` | String? | `local`/`webdav`/`smb`/`online` |
| `sessionId` | String? | 连续听歌会话 ID（长时间间隔后重新生成） |
| `gapBeforeMs` | Long? | 距上一首结束的间隔 |
| `gapAfterMs` | Long? | 距下一首开始的间隔（可由相邻事件推导） |
| `loopCount` | Int | 单曲循环第几轮（默认 1） |
| `outputDevice` | String? | 输出设备（耳机/外放/蓝牙；可选） |
| `isForeground` | Boolean? | 是否前台播放（可选） |
| `decoder` | String? | 解码器/音质（可选） |

### 10.2 新增事件类型 `song_added`

- `eventType = "song_added"`：表示歌曲进入报告数据（用于“年度新增歌曲”）。
- 字段：`canonicalId`、快照、`mediaType`、`artistId/albumId`、`startedAt=endedAt=addedAt`、`durationMs`；`listenedMs/ratio/playScore=0`、`completed=false`、`source`/`endReason` 空串。
- 去重：同一 `canonicalId` 只落一条。
- `addedAt` 优先取“入库时间”，取不到取“首次出现时间”。

### 10.3 采集规则（v2 补充）

- `sessionId`：两次听歌间隔 >30 分钟重新生成。
- 单曲循环：`loopCount` 自增，每轮单独记录。
- 首次播放某歌时补登记 `song_added`（按 `canonicalId` 去重）。
- `mediaType`：本地=local，smb 来源=smb，其余远程=webdav。

### 10.4 数据库与迁移

- Room 版本 **8 → 9**；迁移为逐列 `ALTER TABLE play_events ADD COLUMN`（新列可空；`loopCount` 为 `NOT NULL DEFAULT 0`），并新增 `eventType` 索引。
- `schemaVersion` 升为 **2**；旧 v1 事件仍可读，缺失字段按空/默认处理。
- **播放类聚合只统计 `eventType='playback'`**，避免 `song_added` 污染报告数字；新增 `countSongAdded`、`countAddedSongs`。

### 10.5 统计口径新增点（数据层已支持）

- 年度新增歌曲数：`eventType='song_added'` 且 `year=该年` 去重 `canonicalId`。
- 新增收听表现：新增歌曲在 `playback` 事件的次数/积分。
- 单曲循环王：`loopCount>1` 聚合。
- 连续听歌天数/最长连续：按 `day` 与 `sessionId` 推导。
- 深夜听歌：`hour \in [0,6)`。
- 跳过率/完整率：`endReason \in (SKIP_TO_NEXT, SKIP_TO_PREVIOUS)` / `completed=true`。
- 重复 vs 探索：有效事件数 / 去重 `canonicalId`；首次收听用 `song_added` 或 `MIN(startedAt)`。
- 年度第一首/最后一首：`MIN/MAX(startedAt)`。

### 10.6 导出（v2）

- `play-events-<year>.jsonl`；每行一个 JSON；时间 UTC ISO 8601；`schemaVersion=2`。
- 顶层可选字段：`songId/artistId/albumId/genreId/playlistId/mediaType/sessionId/gapBeforeMs/gapAfterMs/loopCount/outputDevice/isForeground/decoder`。
- 示例：`docs/samples/play-events-2026.sample.jsonl`（含一条 `song_added`）。

### 10.7 相关文档

- 跨平台协议规范：`docs/play-event-protocol-spec.md`
- 导出样例：`docs/samples/play-events-2026.sample.jsonl`

---

## 11. 跨端与实现说明

- 本文档是**唯一的设计/规范依据**。
- `deviceId` 前缀：Android=`android-`，PC=`pc-`；跨端可按 `deviceId` 归并。
- 存储：PC 端可用 SQLite 或 PostgreSQL；`eventId` 作唯一键，导入/合并按 `eventId` `ON CONFLICT DO NOTHING`；建议对 `canonicalId`、`year`、`source`、`startedAt` 建索引。
- `loopCount`、`gapAfterMs` 等可由相邻事件时间推导；`playlistId`/`outputDevice`/`isForeground`/`decoder` 为可选，PC 端可自行填充。
- **PC 端具体实现步骤（接入点、状态机、存储脚本）由后续基于 PC 端代码单独生成的文档提供**，不在此设计文档内。

