# 日记功能最新改进计划

## 当前结论

日记功能的数据层已经基本修稳：

- Room `mood DEFAULT` 与 schema 不一致的问题已修正。
- `dateKey` 唯一索引已加入实体、迁移和 schema。
- `migrate1To3`、`migrate2To3`、`migrate1To2` 迁移测试已补充。
- 日记搜索已支持正文、心情、日期。

当前主要问题集中在 UI 状态流转和测试隔离。

## 优先级计划

| 顺序 | 优先级 | 改进项 | 目的 |
|---|---|---|---|
| 1 | P0 | 修复日记保存/删除后的 UI 状态流转 | 解决 `diaryWorkflowCrudAndPreview` connected 测试失败 |
| 2 | P0 | 给 Compose UI 测试增加数据隔离 | 解决测试之间互相污染导致的旧测试失败 |
| 3 | P1 | 调整日记 CRUD 测试断言路径 | 让测试匹配真实用户流程，并覆盖保存、编辑、删除后的状态 |
| 4 | P1 | 增加日记列表点击进入编辑的回归测试 | 确认从日记中心点条目能直接编辑对应日期 |
| 5 | P2 | 增加日历日记标记的显示/消失测试 | 确认保存后有标记，删除后标记消失 |
| 6 | P2 | 增加日记搜索测试 | 覆盖正文、心情、日期三类搜索 |
| 7 | P3 | 清理测试中的固定日期和无用变量 | 降低后续测试不稳定概率 |

## 详细改进步骤

### 1. 修复日记保存/删除后的 UI 状态流转

当前问题：

从日期详情弹窗点击“写日记”后，会打开日记编辑弹窗，同时日期详情弹窗被关闭。保存日记后只关闭日记编辑弹窗，因此不会回到日期详情弹窗，也就看不到 `card_diary_preview`。

推荐改法：

- 从日期详情进入日记编辑时，记录这是从日期详情进入的。
- 保存或删除日记后，关闭日记编辑弹窗。
- 自动重新打开当天日期详情弹窗。

目标体验：

1. 用户打开某一天详情。
2. 点击写日记。
3. 保存后回到当天详情。
4. 详情里立刻显示日记预览。

这样比保存后直接回到日历主页更连贯。

### 2. 给 UI 测试增加数据隔离

当前问题：

`CalendarUiTest` 多个测试使用相同日期，例如 15、16、17 号。某个测试写入备注后，另一个测试再点击同一天，可能因为已有备注而自动打开详情弹窗，导致旧断言失败。

推荐改法：

- 在每个 UI 测试运行前清理 App 数据。
- 或者每个测试使用唯一日期，避免复用同一天。
- 更稳的做法是两者结合：清理数据 + 避免日期复用。

优先推荐：

```kotlin
@Before
fun resetAppData() {
    // 清理 DataStore / Room 测试数据
}
```

如果清理真实 App 数据比较麻烦，可以先把测试日期错开，作为短期修复。

### 3. 调整日记 CRUD 测试断言路径

当前 `diaryWorkflowCrudAndPreview` 的意图是对的，但断言依赖“保存后仍停留在日期详情”。如果第 1 步选择了保存后回到详情，那么测试可以保留这个方向。

建议测试覆盖：

- 打开日期详情。
- 点击空日记卡片。
- 输入日记并保存。
- 回到日期详情后看到 `card_diary_preview`。
- 点击预览进入编辑。
- 修改内容并保存。
- 回到日期详情后预览仍存在。
- 点击预览进入编辑。
- 删除日记。
- 回到日期详情后看到 `card_diary_empty`。

同时删除当前未使用变量：

```kotlin
val editedDiaryText = "今天是个好日子，又做了一次修改"
```

### 4. 增加日记列表点击进入编辑测试

当前已经把日记列表点击改成：

1. 关闭日记列表。
2. 跳到对应日期。
3. 打开日记编辑弹窗。

建议补一个测试确认这条路径：

- 先创建一篇日记。
- 打开日记中心。
- 点击对应日记条目。
- 断言 `sheet_diary_edit` 显示。
- 断言编辑框中是该日期日记内容。

### 5. 增加日历日记标记测试

建议覆盖：

- 保存日记后，对应日期格子出现日记标记。
- 删除日记后，对应日期格子的日记标记消失。

如果当前绿色点没有单独 test tag，可以考虑给日记标记加稳定 tag，例如：

```kotlin
Modifier.testTag("indicator_diary_$date")
```

### 6. 增加日记搜索测试

搜索现在支持：

- 正文 `content`
- 心情 `mood`
- 日期 `dateKey`

建议至少覆盖三条：

- 搜正文关键词能找到日记。
- 搜心情 emoji 能找到日记。
- 搜 `yyyy-MM` 或 `yyyy-MM-dd` 能找到日记。

### 7. 收尾清理

最后做小清理：

- 移除 UI 测试中的无用变量。
- 统一日记测试日期命名。
- 确认 `diary-feature-improvement-steps.MD` 不参与 App 打包逻辑。
- 如果继续扩展日记功能，再考虑删除确认、保存成功提示、备份导出等后续功能。

## 推荐验证顺序

先跑本地编译和 JVM 测试：

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:GRADLE_USER_HOME='C:\Users\qiuye\.gradle'
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

然后跑 connected 测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

目标结果：

- `testDebugUnitTest` 通过。
- `assembleDebug` 通过。
- `connectedDebugAndroidTest` 通过。
- 迁移测试继续全部通过。
- `diaryWorkflowCrudAndPreview` 不再失败。
- `clickDateSelectsAndDetailButtonOpensDaySheet` 不再被其他测试数据污染。

## 当前最小修复范围

第一轮只需要完成：

1. 保存/删除日记后回到当天详情。
2. UI 测试隔离或测试日期去冲突。
3. 修正 `diaryWorkflowCrudAndPreview`。
4. 重新跑 `connectedDebugAndroidTest`。

这四步完成后，日记功能就可以进入比较稳定的状态。
