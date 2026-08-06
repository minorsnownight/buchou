# 状态模型与统计口径

## 1. 事实来源

所有展示状态从事实数据推导，UI 不单独保存"当前连续天数""今天是否无烟"等派生值。

核心事实：

- `SmokingProfile`：消费估算基线
- `QuitJourney`：本次初始化后的戒烟旅程起点
- `DailyCheckIn`：某个自然日的无烟确认
- `SmokingEvent`：每一次吸烟事件
- `QuitReason`：最多五条用户理由
- `AchievementUnlock`：永久保留的成就解锁记录
- `ReminderPreferences`：提醒配置
- `HomeModulePreferences`：可选首页模块的显示状态和顺序

自然日数据必须保存记录发生时的时区。事件时间保存 UTC Instant。

## 2. 关键不变量

1. 一个自然日最多有一条 `DailyCheckIn`。
2. 一个自然日可以有多条 `SmokingEvent`。
3. 当天只要存在至少一条 `SmokingEvent`，日期状态就是 `SMOKED`。
4. 没有吸烟事件且存在无烟打卡时，日期状态是 `SMOKE_FREE`。
5. 两者都不存在时，日期状态是 `UNRECORDED`。
6. `SMOKE_FREE` 可以更正为 `SMOKED`。
7. 已存在吸烟事件的日期不能更正为 `SMOKE_FREE`。
8. 漏打卡不会打断连续不抽时长。
9. 成就解锁记录不可因后续吸烟而删除。
10. UI 不得绕过 domain 层直接修改上述状态。

日期状态推导：

```text
hasSmokingEvent -> SMOKED
else hasSmokeFreeCheckIn -> SMOKE_FREE
else -> UNRECORDED
```

## 3. 连续不抽时长

`currentStreakStartedAt`：

```text
latestSmokingEvent.occurredAt ?: quitJourney.startedAt
```

`currentSmokeFreeDuration`：

```text
max(now - currentStreakStartedAt, 0)
```

首页主数字显示完整自然日数量，辅助文字显示小时和分钟。它不依赖连续打卡。

## 4. 日期与更正规则

### 今天没抽

- 立即写入或更新今日 `DailyCheckIn`。
- 烟瘾强度可为空，补充后更新同一记录。
- 已存在吸烟事件时拒绝写入，并返回明确业务错误。

### 今天抽了

- 每次创建独立 `SmokingEvent`。
- 支数必须大于 0。
- 烟瘾、诱因和备注可为空。
- 若当天此前是无烟，日期状态自动变成吸烟，不需要删除无烟打卡事实。
- 当前连续时长从最新吸烟事件重新计算。

### 今日更正

- 无烟改吸烟：新增吸烟事件。
- 修改吸烟详情：只允许编辑今天的吸烟事件。
- 吸烟改无烟：不允许。
- 历史日期：只读。

## 5. 累计统计

统计范围从当前 `QuitJourney.startedAt` 到现在。执行"重新开始戒烟"后，新旅程从零统计；永久成就不参与累计计算。

### 5.1 预期吸烟支数

```text
expected = profile.cigarettesPerDay * elapsedHours / 24
```

使用小时比例避免未满一天时整日跳变。

### 5.2 实际吸烟支数

```text
actual = sum(smokingEvents.cigaretteCount)
```

### 5.3 少抽支数

```text
avoided = max(expected - actual, 0)
```

展示取向下整数，详情可保留一位小数。修改个人基线后对全部旅程重新计算。

### 5.4 节省金额

仅当 `cigarettesPerPack` 和 `pricePerPack` 都有效时计算：

```text
saved = avoided / cigarettesPerPack * pricePerPack
```

缺少参数时首页显示"完善个人基线后计算"，不显示 0 元。

## 6. 记录统计

### 打卡无烟率

```text
smokeFreeRate = smokeFreeDays / recordedDays
recordedDays = smokeFreeDays + smokedDays
```

没有记录时显示"暂无数据"，不显示 0%。

### 记录完整度

```text
completeness = recordedDays / eligibleElapsedDays
```

`eligibleElapsedDays` 只包含当前统计窗口内、从旅程开始日期到今天的自然日，不含未来日期。

### 每日吸烟支数

- `SMOKED`：显示当日所有事件支数之和。
- `SMOKE_FREE`：显示明确的 0。
- `UNRECORDED`：显示缺失，不绘制为 0。

### 烟瘾趋势

- 只连接存在烟瘾数值的记录点。
- 缺失值不使用 0 填充。
- 同日多个吸烟事件使用最高烟瘾强度；无吸烟事件时使用无烟打卡强度。

### 最长连续不抽时长

由旅程开始、吸烟事件时间点和当前时间构成区间，取最长区间。漏打卡不切断区间。

## 7. 成就

成就基于连续不抽时长，节点：

- 1 天
- 3 天
- 7 天
- 14 天
- 30 天
- 90 天
- 180 天
- 365 天

当条件首次满足时写入 `AchievementUnlock(id, unlockedAt)`。展示由该记录决定，不根据当前连续时长反向上锁。

## 8. 提醒状态

提醒调度的最早日期：

```text
todayRecorded ? tomorrow : today
```

以下操作都必须使用相同规则重新调度：

- 开启提醒
- 修改提醒时间
- 修改重复星期
- 系统重启
- 时区或时间变化
- 精确闹钟权限恢复

闹钟触发时必须再次读取日期状态。只要状态不是 `UNRECORDED`，就直接安排下一个启用日，不启动响铃服务。

设置页必须展示领域层计算出的 `nextReminderAt`，不能只展示用户配置时间。

## 9. 数据管理语义

### 重新开始戒烟

删除：

- `QuitJourney`（重新创建，起点为当前时间）
- `DailyCheckIn`
- `SmokingEvent`

保留：

- `SmokingProfile`（个人基线）
- `QuitReason`（戒烟理由）
- `AchievementUnlock`（成就记录）
- `ReminderPreferences`（提醒设置）

完成后取消所有闹钟，不进入引导页。

### 重置全部数据

删除：

- `SmokingProfile`
- `QuitJourney`
- `DailyCheckIn`
- `SmokingEvent`
- `QuitReason`
- `AchievementUnlock`
- 提醒配置和首页模块偏好

完成后取消所有闹钟并进入引导页。

## 10. WebDAV 同步

WebDAV 同步已实现，支持坚果云等标准 WebDAV 服务。

### 同步内容

同步文件保存以下事实数据：

- `SmokingProfile`（个人基线，含货币代码）
- `QuitJourney`（戒烟旅程）
- `DailyCheckIn`（打卡记录）
- `SmokingEvent`（吸烟事件）
- `QuitReason`（戒烟理由）
- `AchievementUnlock`（成就解锁）

不同步偏好设置（主题、语言、提醒、货币、首页模块配置等），这些由设备本地管理。

### 同步机制

- 上传：导出全部事实数据为 JSON，PUT 到 WebDAV 服务器的 `buchou/backup.json` 路径。
- 下载：从 WebDAV 服务器获取 `buchou/backup.json`，清空本地事实数据后导入。
- 下载会覆盖本地事实数据，偏好设置不受影响。
- 同步失败不会修改本地数据。
- 凭据保存在 SharedPreferences 中，不进入数据库、日志或同步文件。
