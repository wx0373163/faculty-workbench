# Faculty Workbench · 大学教师工作台

> 一款面向大学教师的 Android 工作台应用，集今日任务、教学课表、科研待办、日程管理与个人中心于一体。

[![Release](https://img.shields.io/github/v/release/wx0373163/faculty-workbench)](https://github.com/wx0373163/faculty-workbench/releases)
[![Maven Package](https://img.shields.io/badge/Maven-GitHub%20Packages-success.svg)](https://github.com/wx0373163/faculty-workbench/packages)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/min%20SDK-24-orange.svg)](https://developer.android.com/studio/releases/platforms)
[![Target SDK](https://img.shields.io/badge/target%20SDK-34-green.svg)](https://developer.android.com/studio/releases/platforms)

---

## 📱 功能特性

### 1. 今日任务
- **三态状态流转**：待办 → 进行中 → 已完成，支持状态切换
- **进度条可视化**：每个任务显示完成进度
- **筛选标签**：按状态快速筛选任务列表
- **增删改查**：支持任务的新增、编辑、删除

### 2. 教学管理
- **课程管理**：维护课程信息（课程名称、任课班级）
- **排课信息**：设置星期、节次、上课地点、授课周次
- **课表展示**：按周视图网格展示课程安排

### 3. 科研待办
- **三态状态**：待办 / 进行中 / 已完成
- **进度跟踪**：进度条实时展示科研任务进展
- **筛选功能**：按状态筛选科研任务

### 4. 日程课表
- **周视图网格**：以周为单位的课表网格视图
- **任务标记**：在课表中标记任务与课程
- **日历同步**：支持与系统日历同步

### 5. 我的（个人中心）
- 个人信息管理
- 主题切换（深色 / 浅色模式）
- 学期设置
- 数据备份与导入导出
- 日历同步开关
- 每日提醒设置
- 检查更新

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | [Kotlin](https://kotlinlang.org/) 1.9.0 |
| UI 框架 | [Jetpack Compose](https://developer.android.com/jetpack/compose) |
| 本地数据库 | [Room](https://developer.android.com/training/data-storage/room) |
| 设计系统 | [Material 3](https://m3.material.io/) (Material You) |
| 导航 | [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) |
| 数据存储 | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| 构建工具 | [Gradle](https://gradle.org/) 8.5 + AGP 8.1.0 |
| 最低 SDK | 24 (Android 7.0) |
| 目标 SDK | 34 (Android 14) |

---

## 📂 项目结构

```
app/
├── build.gradle.kts              # 模块构建配置
├── proguard-rules.pro            # 混淆规则
└── src/main/
    ├── AndroidManifest.xml       # 应用清单
    ├── java/com/example/facultyworkbench/
    │   ├── FacultyApp.kt         # Application 入口
    │   ├── MainActivity.kt       # 主 Activity
    │   ├── SettingsDataStore.kt  # DataStore 偏好存储
    │   ├── data/
    │   │   ├── backup/           # 数据备份与序列化
    │   │   ├── dao/              # Room DAO 接口
    │   │   ├── db/               # Room 数据库
    │   │   ├── entity/           # 数据实体
    │   │   └── repository/       # 数据仓库
    │   ├── receiver/             # 广播接收器（开机、每日提醒）
    │   ├── ui/
    │   │   ├── navigation/       # 导航图
    │   │   ├── today/            # 今日任务
    │   │   ├── teaching/         # 教学管理
    │   │   ├── research/         # 科研待办
    │   │   ├── schedule/         # 日程课表
    │   │   ├── profile/          # 个人中心
    │   │   └── theme/            # 主题与配色
    │   └── util/                 # 工具类（日历同步、提醒、更新检查）
    └── res/                      # 资源文件
```

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK Platform 34
- Gradle 8.5

### 编译运行

1. **克隆仓库**

   ```bash
   git clone https://github.com/wx0373163/faculty-workbench.git
   cd faculty-workbench
   ```

2. **在 Android Studio 中打开**

   ```
   File → Open → 选择项目根目录
   ```

3. **同步 Gradle**

   Android Studio 会自动下载依赖并同步项目。

4. **运行应用**

   连接 Android 设备或启动模拟器，点击 Run 按钮（▶）。

### 构建 Release APK

```bash
# 生成签名 Release APK
./gradlew assembleRelease

# 产物位置
app/build/outputs/apk/release/app-release.apk
```

---

## 📦 下载安装

### 方式一：GitHub Releases（APK 直装）

从 [Releases](https://github.com/wx0373163/faculty-workbench/releases) 页面下载最新的 `app-release.apk`，安装到 Android 设备（需开启"未知来源"安装权限）。

### 方式二：GitHub Packages（Maven 依赖）

应用同时发布到 GitHub Packages 的 Maven 仓库，可作为依赖引入：

| 属性 | 值 |
|------|-----|
| groupId | `com.example.facultyworkbench` |
| artifactId | `faculty-workbench` |
| version | `1.0.0` |
| packaging | `apk` |
| 仓库地址 | `https://maven.pkg.github.com/wx0373163/faculty-workbench` |

**引入依赖示例（`build.gradle.kts`）：**

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/wx0373163/faculty-workbench")
        credentials {
            username = "YOUR_GITHUB_USERNAME"
            password = "YOUR_GITHUB_TOKEN"   // 需 read:packages 权限
        }
    }
}

dependencies {
    implementation("com.example.facultyworkbench:faculty-workbench:1.0.0@apk")
}
```

> 如需下载 APK 文件，可通过 Maven 仓库地址直接获取：
> `https://maven.pkg.github.com/wx0373163/faculty-workbench/com/example/facultyworkbench/faculty-workbench/1.0.0/faculty-workbench-1.0.0.apk`

---

## 🚢 发布到 GitHub Packages

项目已配置 `maven-publish` 插件，可将 Release APK 发布到 GitHub Packages。

### 前置条件

- GitHub Personal Access Token，需包含 `write:packages` 和 `read:packages` 权限
- 已构建 Release APK（`./gradlew assembleRelease`）

### 发布步骤

设置环境变量后执行发布任务：

```bash
# Linux / macOS
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-token-with-write-packages-scope
./gradlew publishReleasePublicationToGitHubPackagesRepository

# Windows (PowerShell)
$env:GITHUB_ACTOR = "your-github-username"
$env:GITHUB_TOKEN = "your-token-with-write-packages-scope"
.\gradlew publishReleasePublicationToGitHubPackagesRepository
```

发布配置位于 `app/build.gradle.kts` 的 `afterEvaluate { publishing { ... } }` 块中，包含 groupId、artifactId、version 及 POM 元数据。

---

## 🔐 签名配置

Release 版本已配置正式签名密钥。密钥文件位于本地，**不会**提交到版本库（已在 `.gitignore` 中排除）。

如需自行签名，请在 `app/keystore/` 目录放置密钥文件，并在 `keystore.properties` 中配置：

```properties
storeFile=keystore/your-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

---

## ⚠️ 注意事项

- **请勿提交** `keystore.properties`、`*.jks`、`*.keystore` 等签名相关文件
- **请勿提交** `local.properties`（包含本机 SDK 路径）
- **请勿提交** `.trae/` 等 IDE 配置目录
- 数据备份文件会导出到应用外部存储，请注意保护个人数据

---

## 📄 许可证

MIT License — 详见 [LICENSE](LICENSE) 文件。
