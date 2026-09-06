# Faculty Workbench · 大学教师工作台

> 一款面向大学教师的 Android 工作台应用，集今日任务、待办清单、教学课表、科研管理与个人中心于一体，帮助教师高效管理日常教学与科研事务。

[![Release](https://img.shields.io/github/v/release/wx0373163/faculty-workbench)](https://github.com/wx0373163/faculty-workbench/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-purple.svg)](https://kotlinlang.org/)
[![Min SDK](https://img.shields.io/badge/min%20SDK-26-orange.svg)](https://developer.android.com/studio/releases/platforms)
[![Target SDK](https://img.shields.io/badge/target%20SDK-34-green.svg)](https://developer.android.com/studio/releases/platforms)
[![Version](https://img.shields.io/badge/version-1.2-blue.svg)]()

---

## ✨ 应用概览

应用底部导航包含六个模块：**今日任务 · 待办 · 教学 · 科研 · 课表 · 我的**。其中「待办」「教学」「科研」中录入的事项会自动同步显示在「今日任务」中，实现一处录入、多处联动。

---

## 📱 功能特性

### 1. 今日任务

「今日任务」是全应用的**汇总视图**，所有任务均来自「待办」「教学」「科研」三个模块，本页面不提供独立添加功能。

- **自动汇总**：待办、科研事项一经录入即自动出现在今日任务；当天课程自动展示
- **今日课程**：顶部展示当天课程卡片，点击可跳转课表
- **三态流转**：待办 → 进行中 → 已完成，勾选完成时同步更新来源事项
- **筛选标签**：全部 / 待办 / 进行中 / 已完成，标签实时显示各状态数量
- **双向联动**：在今日任务中勾选完成或删除，会自动同步回待办/科研来源

### 2. 待办

- **待办管理**：新增、编辑、删除待办事项
- **分类筛选**：按分类（默认 / 工作 / 生活 / 学习 / 其他）筛选
- **状态筛选**：全部 / 待办 / 已完成，实时显示数量
- **进度概览**：顶部展示完成进度卡片（已完成 / 总数 + 进度条）
- **截止日期**：日历选择器选择截止日期，列表中以 `yyyy-MM-dd` 格式显示
- **清除已完成**：一键清除所有已完成待办
- **数据导入导出**：支持导出为 JSON 文件，支持合并追加 / 替换全部两种导入模式

### 3. 教学

- **课程管理**：维护课程信息（课程名称、任课班级）
- **排课信息**：设置星期、节次、上课地点、授课周次
- **课表展示**：按周视图网格展示课程安排
- **自动同步**：当天课程自动显示在「今日任务」顶部

### 4. 科研

- **科研事项管理**：新增、编辑、删除科研待办
- **分类筛选**：按分类（论文 / 项目 / 实验 / 申报 / 其他）筛选
- **状态筛选**：全部 / 待办 / 进行中 / 已完成，实时显示数量
- **进度跟踪**：进度条实时展示科研任务进展（0-100%）
- **截止日期**：日历选择器选择截止日期
- **清除已完成**：一键清除所有已完成科研待办
- **自动同步**：科研事项自动同步至「今日任务」

### 5. 课表

- **周视图网格**：以周为单位的课表网格视图，直观展示每日课程安排
- **课程信息**：显示课程名称、班级、地点、节次
- **编辑功能**：支持课程的新增、编辑、删除

### 6. 我的（个人中心）

- **版本信息**：显示当前应用版本
- **主题切换**：深色 / 浅色 / 跟随系统
- **学期设置**：设置当前学期起止周次
- **数据管理**：
  - 导出 JSON 备份
  - 导入 JSON 备份
  - 同步到系统日历
  - 导出数据库文件 (.db)
- **日历同步开关**：开启后任务自动同步到系统日历
- **每日提醒**：设置每日提醒时间

---

## 🛠 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | [Kotlin](https://kotlinlang.org/) 2.0.20 |
| UI 框架 | [Jetpack Compose](https://developer.android.com/jetpack/compose) |
| 本地数据库 | [Room](https://developer.android.com/training/data-storage/room) + [KSP](https://kotlinlang.org/docs/ksp-overview.html) |
| 设计系统 | [Material 3](https://m3.material.io/) (Material You) |
| 导航 | [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) |
| 数据存储 | [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) |
| 序列化 | org.json（备份导入导出） |
| 构建工具 | [Gradle](https://gradle.org/) 8.14.3 + AGP 8.5.2 |
| 最低 SDK | 26 (Android 8.0) |
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
    │   │   ├── backup/           # 数据备份、序列化与校验
    │   │   ├── dao/              # Room DAO 接口
    │   │   ├── db/               # Room 数据库
    │   │   ├── entity/           # 数据实体（Task/Todo/Course/ResearchTask）
    │   │   └── repository/       # 数据仓库（含跨表同步逻辑）
    │   ├── receiver/             # 广播接收器（开机、每日提醒）
    │   ├── ui/
    │   │   ├── navigation/       # 导航图与底部导航栏
    │   │   ├── today/            # 今日任务（汇总视图）
    │   │   ├── todo/             # 待办清单
    │   │   ├── teaching/         # 教学管理
    │   │   ├── research/         # 科研管理
    │   │   ├── schedule/         # 日程课表
    │   │   ├── profile/          # 个人中心
    │   │   └── theme/            # 主题与配色
    │   └── util/                 # 工具类（日历同步、提醒）
    └── res/                      # 资源文件
```

---

## 🚀 快速开始

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK Platform 34
- Gradle 8.14.3

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

### 构建 Debug APK

```bash
# 生成 Debug APK
./gradlew assembleDebug

# 产物位置
app/build/outputs/apk/debug/app-debug.apk
```

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
| version | `1.2` |
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
    implementation("com.example.facultyworkbench:faculty-workbench:1.2@apk")
}
```

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

发布配置位于 `app/build.gradle.kts` 的 `afterEvaluate { publishing { ... } }` 块中。

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
- 数据备份文件会导出到应用外部存储，请注意保护个人数据
- 数据库版本升级时使用 `fallbackToDestructiveMigration`，升级后旧数据会被清空

---

## 📄 许可证

MIT License — 详见 [LICENSE](LICENSE) 文件。
