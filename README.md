# Process Reporter

适用于Mix-space状态函数的安卓客户端，通过root获取前台应用包名，定时上传在线信息和电量。

## 下载

预编译 APK 可从 [GitHub Releases](https://github.com/Elvish064/Process-Reporter/releases) 直接下载安装。

## 快速开始
1. **安装 APK**：下载并安装最新版本的 `app-debug.apk`。
2. **授予 Root 权限和后台运行权限**：首次打开应用时，授予 Root 权限以允许获取前台应用信息，并允许应用在后台运行。
3. **配置服务器地址和密匙**：在设置界面输入 Mix-space 的API地址和密匙。
4. **启用同步**：返回主界面，启用状态同步功能，应用将开始定时上报状态信息。

### 功能

| 功能 | 说明 |
|------|------|
| **状态上报** | 通过root获取包名，映射为文本后上报，10–50 秒间隔（默认 30 秒），上报在线状态和电池信息 |
| **电量上报** | 自动上报电池电量和充电状态 |
| **连接状态检测** | 每 5 秒测试服务器连接，顶栏实时显示连接状态 |
| **诊断日志** | APP 内 DebugLog 页面查看同步日志，方便排查问题 |

### 技术栈

- Kotlin + Jetpack Compose（Material 3）
- WorkManager — 后台定时同步，支持网络约束和指数退避
- DataStore — 持久化配置和同步状态
- EncryptedSharedPreferences — Token 加密存储

### 系统要求

- Android 8.0+ (API 26)

### 文件结构

```
agents/android-app/
├── app/
│   ├── build.gradle.kts              # 构建配置（SDK 版本、依赖）
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/monika/dashboard/
│           ├── MainActivity.kt        # 入口 + 导航
│           ├── DashboardApp.kt        # Application 类
│           ├── data/
│           │   ├── SettingsStore.kt    # DataStore 配置管理
│           │   └── DebugLog.kt        # 内存日志（UI 查看）
│           ├── health/
│           │   ├── HealthConnectManager.kt  # HC 权限、读取、特性检测
│           │   └── HealthSyncWorker.kt      # WorkManager 同步任务
│           ├── network/
│           │   └── ReportClient.kt    # HTTP 上报客户端
│           └── ui/screens/
│               ├── SetupScreen.kt     # 服务器配置
│               ├── StatusScreen.kt    # 状态总览 + 权限诊断
│               └── DebugLogScreen.kt  # 日志查看
├── BUILD.md                           # 构建指南
├── GUIDE.md                           # 代码指南（架构、流程、API）
├── build.gradle.kts                   # 项目级构建
├── gradle/                            # Gradle wrapper
└── settings.gradle.kts
```

## 构建

```bash
cd agents/android-app
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk
```

详见 [`BUILD.md`](BUILD.md)。

## 架构与代码指南

详见 [`GUIDE.md`](GUIDE.md)，包含：
- 心跳流程、连接检测流程流程
- 设计决策与架构说明
- 常见问题排查

## 许可证
本项目采用 [MIT 许可证](LICENSE)

---

## 致谢

[Mix-space](https://github.com/mx-space) - 提供状态函数平台和接口规范，参考使用了其图标素材

[live-dashboard](https://github.com/Monika-Dream/live-dashboard/tree/android-source) - 提供了安卓客户端的初始代码和设计思路，重构了 UI 和同步逻辑。