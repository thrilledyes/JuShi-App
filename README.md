# 聚事 JuShi

简体中文 | [English](README_EN.md)

一款 Android 校园助手：把教务系统课表与散落在微信、QQ、学习通等平台的群消息汇聚到一个应用里。

| 项目 | 值 |
|------|-----|
| 应用 ID | `com.jushi.demo.uc01` |
| 命名空间 | `com.jushi.demo.main` |
| 语言 / 构建 | Java 17 · Gradle Wrapper 8.7 · AGP 8.5.2 |
| SDK | minSdk 24 / targetSdk 36 / compileSdk 36 |
| 主要依赖 | AppCompat 1.7.0 · Material 1.12.0 · RecyclerView 1.3.2 · Room 2.6.1 |

## 核心功能

- **课表导入**：系统文件选择器选取教务系统导出的 `.doc` / `.docx`，自动识别 Flat OPC XML 与 ZIP 两种 Word 格式，解析表格并还原跨节次合并单元格；导入前可预览并逐条编辑课程名、教师、地点、星期、节次、周次。
- **课表展示**：自定义 Canvas 网格，7 天 × 11 节次；跨节次课程合并为一张彩色卡片；支持周次切换（1–30 周）、按周次范围过滤、点击查看课程详情；无数据时引导导入。
- **消息源接入**：多选微信 / 企业微信 / QQ / 学习通 / 超算习堂 / 雨课堂，并为每个来源填写群聊名或课程名，生成监听规则。
- **后台消息采集**：`NotificationListenerService` 在系统通知到达时解析消息（来源识别、群名清洗、正文提取），按规则过滤后以 JSON 追加落盘，最多保留 500 条。
- **外观设置**：字体大小（0.9 / 1.0 / 1.1）、全局加粗、主题（跟随系统 / 浅色 / 深色），保存后即时生效。
- **待办、消息**：入口已就绪，内容待实现。

## 软件架构

单 Activity + 多 Fragment，按功能分包，UI 层直接调用数据层与采集层。

```
MainActivity（底部 4 Tab + 导入菜单）
├─ ui.timetable   课表：TimetableFragment → TimetableGridView(Canvas) / PeriodLabelView / WeekUtils
├─ ui.imports     导入：SelectSourceActivity → ConfigureTargetsActivity（消息源规则）
│                       ImportTimetableActivity → DocxParser（课表解析）
├─ ui.settings    设置：SettingsFragment → General / Authority / Profile SettingsActivity
│                       UiPreferences（持久化）+ UiContextWrapper（字体缩放）
├─ ui.todo · ui.message          占位
├─ utils          采集：BackgroundNotificationListenerService → NotificationMessageParser
│                       → NotificationRuleStore（规则）→ FilteredNotificationJsonStore（落盘）
└─ data           持久化：Room —— Course 实体 / CourseDao / CourseDatabase

BaseActivity：注入字体缩放、应用主题模式、递归遍历 View 树统一字重
```

**数据流**

| 链路 | 路径 |
|------|------|
| 课表导入 | SAF 选文件 → `DocxParser.parse()` → 预览/编辑 → `CourseDao.deleteAll() + insertAll()` → Room |
| 课表展示 | `CourseDao.getAllCourses()` → `WeekUtils` 按周过滤 → `TimetableGridView` 绘制 |
| 消息采集 | 系统通知 → 解析为 `MessageModel` → 规则匹配 → 追加写入 `filtered_notifications.json` |
| 界面偏好 | `UiPreferences`(SharedPreferences) → `UiContextWrapper` / `BaseActivity` 全局生效 |

**存储**

| 数据 | 位置 |
|------|------|
| 课程 | Room `jushi_courses.db` → 表 `courses` |
| 界面偏好 | SharedPreferences `jushi_ui_prefs` |
| 学期起始日 / 当前周 | SharedPreferences `jushi_timetable_prefs` |
| 消息源选择 | SharedPreferences `jushi_demo_prefs` |
| 监听规则 | `filesDir/notification_rules.json`（`{sources:[{packageName,groupName}]}`） |
| 采集结果 | `filesDir/filtered_notifications.json`（最近 500 条） |

## 目录结构

```
JuShi-App/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/jushi/demo/main/
│   │   ├── MainActivity.java        # 主入口：4 Tab + 导入菜单
│   │   ├── BaseActivity.java        # 字体缩放 / 主题 / 全局字重
│   │   ├── data/                    # Room：Course、CourseDao、CourseDatabase
│   │   ├── ui/timetable/            # 课表展示
│   │   ├── ui/imports/              # 课表导入、消息源与监听目标配置
│   │   ├── ui/settings/             # 设置页与偏好持久化
│   │   ├── ui/todo/ · ui/message/   # 占位 Tab
│   │   └── utils/                   # 通知监听、解析、规则、落盘
│   └── res/                         # layout / menu / values / values-night
├── gradle/wrapper · gradlew(.bat)   # Gradle Wrapper 8.7
├── settings.gradle · build.gradle · gradle.properties
└── README.md · README_EN.md
```

## 构建与部署

**环境**：JDK 17+、Android SDK（Platform 36 + Build Tools 34）；配置 `ANDROID_HOME`，或在项目根目录创建 `local.properties` 写入 `sdk.dir=<SDK 路径>`。

```bash
./gradlew assembleDebug     # Windows: gradlew.bat assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

./gradlew installDebug      # 安装到已连接的设备或模拟器
```

Android Studio 用户直接打开项目根目录，等待 Gradle 同步后点击 Run 即可。消息采集功能需在**系统设置 → 通知使用权**中授权「聚事」。

**验证状态**：`assembleDebug` 构建通过，产出 `app-debug.apk`；核心逻辑闭环验证（课表解析、周次与节次计算、通知解析→规则过滤→落盘）共 52 项断言全部通过。

## 已知限制

- 待办、消息 Tab 为空壳；个人资料、权限设置页为静态占位。
- 应用内未内置通知使用权引导页，需手动在系统设置中开启。
- 学期起始日期默认硬编码为 2026-02-16，暂无设置入口。
- 课表导入为全量替换（先清空再写入），不支持增量合并。
