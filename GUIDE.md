# Process Reporter Android App — 代码指南

> 更新：2026-04-25

## 构建与部署

- **最低 SDK**：见 `app/build.gradle.kts` → `minSdk` (26)
- **构建**：`./gradlew assembleDebug`（在 `agents/android-app/` 下执行）
- **APK 输出**：`app/build/outputs/apk/debug/app-debug.apk`
- **安装**：`adb install -r app/build/outputs/apk/debug/app-debug.apk`

## 设计决策

- **通过root的应用检测**：通过。 `"su", "-c", "dumpsys window | grep mCurrentFocus"`获取前台应用包名，并根据自定义的包名映射表转化为更直观的文本。
- **仅 WorkManager**：HeartbeatWorker 使用自调度 OneTimeWorkRequest 绕过 15 分钟最小周期。底层 AlarmManager 即使被冻结也能唤醒。

## 关键流程

### 心跳流程（可选）
1. 用户在 SetupScreen 点击「开始监听」→ `HeartbeatWorker.schedule(context, interval)`
2. HeartbeatWorker 延迟触发 → 读取电量信息
3. `ReportClient.reportApp()` POST 到 API地址，信息结构为：
```
{
    "key": "<API密匙>",
    "emoji": "<自定义的emoji>",
    "desc": "<包名映射后的文本>",
    "ttl": <心跳间隔>
  }
```

4. Worker 自调度下一个 OneTimeWorkRequest
5. 通过 AlarmManager 存活于小米进程冻结

### 连接状态流程
1. `MainActivity.DashboardTopBar()` 运行 `LaunchedEffect` 循环
2. 每 5 秒创建临时 `ReportClient`，调用 `testConnection()`（GET `/api/health`）
3. 更新状态 → TopAppBar 显示「已连接」(绿) 或「未连接」(灰)

## 常见问题

| 症状 | 原因 | 解决 |
|------|------|------|
| 「未连接」但服务器正常 | URL 缺少 `https://` 或 Token 为空 | 检查 SetupScreen 配置，确认已保存 |
| 耗电快 | 心跳间隔过低（如 10s） | 将间隔调整到 20-50s |
| Token 保存失败 | EncryptedSharedPreferences 不可用（旧设备） | SetupScreen 会显示警告，无解决方案 |
| 后台被杀 | OEM 电池优化 | StatusScreen → 忽略电池优化 + 厂商特殊设置 |

