# 聚事 JuShi

聚事是一个面向课程、作业和待办管理的 Android 应用。当前版本以 `app` 为唯一业务模块，核心能力包括课表导入与展示、通知消息源监听、AI 作业信息抽取、超算习堂作业同步、待办管理和基础设置。

## 项目信息

| 项目 | 内容 |
| --- | --- |
| 应用名 | 聚事 |
| Gradle 项目名 | `JuShiUc01Demo` |
| Application ID | `com.jushi.demo.uc01` |
| Namespace | `com.jushi.demo.main` |
| 语言 | Java |
| minSdk | 24 |
| targetSdk / compileSdk | 36 |
| Android Gradle Plugin | 8.5.2 |
| Java 版本 | 17 |

## 主要功能

### 待办

- 展示自动识别的课程作业、DDL 和个人待办。
- 支持新增、编辑、删除、完成、置顶和提醒。
- 支持将待办写入系统日历。
- 支持对话式查看课程/任务消息。

### 课表

- 支持导入 `.doc` / `.docx` 课程表文件。
- 支持在线导入课表。
- 以周视图展示课程，支持切换周次。
- 支持课程提醒规则。

### 消息源监听

- 支持选择微信、企业微信、QQ、学习通作为通知消息源。
- 支持为消息源配置监听对象。
- 支持将监听对象绑定到本地课程，用于“下次课/下节课”等相对时间归一化。
- 通知监听服务会筛选疑似作业/DDL 消息，使用本地 UIE 模型抽取任务和时间。

### 超算习堂同步

- 支持输入超算习堂账号密码抓取作业。
- 抓取后按超算习堂课程来源分组，用户可选择要导入的来源。
- 每个来源可绑定到本地课程，也可以不绑定。
- 导入成功后自动回到主界面。
- 按来源记录上次成功同步时间，只展示上次同步后新创建的作业。
- 若作业创建时间字段无法解析，则不会被时间戳过滤，避免漏导；重复导入由 `message_hash` 去重兜底。

### 设置

- 支持字体大小、字体粗细和主题模式设置。
- 支持头像、个人资料和权限相关页面。

## 目录结构

```text
jushi_main_v2/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/uie_model/
│       │   ├── uie_mini.onnx
│       │   └── vocab.txt
│       ├── java/com/jushi/demo/main/
│       │   ├── ai/                 # UIE 模型推理
│       │   ├── chaosuan/           # 超算习堂抓取、归一化和导入
│       │   ├── data/entity/        # 课程实体
│       │   ├── database/           # SQLite schema、DAO 风格管理器和数据模型
│       │   ├── ui/imports/         # 课表导入、消息源配置、超算习堂导入
│       │   ├── ui/message/         # 消息列表和消息组页面
│       │   ├── ui/settings/        # 设置页面
│       │   ├── ui/timetable/       # 课表页面
│       │   ├── ui/todo/            # 待办页面
│       │   └── utils/              # 通知监听、提醒、时间归一化、日历导出
│       └── res/                    # 布局、菜单、主题、图标和字符串资源
├── gradle/                         # Gradle Wrapper 文件
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
└── README.md
```

## 核心模块

### 数据层

当前主数据层使用自定义 SQLite：

- `DBSchema` 定义表结构。
- `DBHelper` 管理数据库创建、升级和兼容性补列。
- `DBManager` 封装课程、通知规则、原始消息、标准化记录、待办和提醒规则的读写。

主要数据表：

- `courses`
- `notification_rule`
- `raw_message`
- `normalized_record`
- `raw_normalized_map`
- `todo_message`
- `course_reminder_rule`

### 通知识别链路

```text
系统通知
  -> BackgroundNotificationListenerService
  -> NotificationMessageParser
  -> notification_rule 匹配
  -> UIEPredictor 抽取任务和时间
  -> TimeNormalizer 归一化时间
  -> DBManager 写入 raw / normalized / todo
```

### 超算习堂导入链路

```text
EasyHpcImportActivity
  -> EasyHpcHomeworkClient 登录并抓取课程/作业
  -> EasyHpcHomeworkNormalizer 解析标题、描述、截止时间、创建时间
  -> 按课程来源过滤上次同步前的旧作业
  -> 用户选择来源并绑定本地课程
  -> EasyHpcHomeworkSyncer 写入数据库
```

### 课表导入链路

```text
ImportTimetableActivity
  -> DocxParser 解析 doc/docx
  -> 课程预览
  -> DBManager 写入 courses
  -> TimetableFragment / TimetableGridView 展示
```

## 权限说明

应用在 `AndroidManifest.xml` 中声明以下权限：

- `INTERNET`：超算习堂登录和作业抓取。
- `POST_NOTIFICATIONS`：通知相关能力。
- `BIND_NOTIFICATION_LISTENER_SERVICE`：通知监听服务。
- `READ_CALENDAR` / `WRITE_CALENDAR`：待办提醒导出到日历。
- `RECEIVE_BOOT_COMPLETED`：重启后恢复课程/待办提醒。
- `READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`：课表文件导入。
- `CAMERA`：头像或资料相关页面预留能力。

## 构建环境

需要准备：

- JDK 17
- Android SDK Platform 36
- Android Studio 最新稳定版
- 可访问 `maven.aliyun.com` 的网络环境

`settings.gradle` 已配置阿里云 Maven 镜像：

```gradle
maven { url "https://maven.aliyun.com/repository/google" }
maven { url "https://maven.aliyun.com/repository/central" }
maven { url "https://maven.aliyun.com/repository/gradle-plugin" }
```

## 构建与运行

### Android Studio

1. 打开 `jushi_main_v2` 项目根目录。
2. 等待 Gradle Sync 完成。
3. 连接真机或启动模拟器。
4. 点击 Run 运行 `app`。

### 命令行

Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

macOS / Linux:

```bash
./gradlew :app:assembleDebug
```

Debug APK 默认输出到：

```text
app/build/outputs/apk/debug/
```

## 本地配置

`local.properties` 用于指定 Android SDK 路径，例如：

```properties
sdk.dir=D\:\\develop\\Android\\Sdk
```

如果换到其他机器，请按本机 Android SDK 路径调整该文件，或配置 `ANDROID_HOME`。

## 资产说明

`app/src/main/assets/uie_model/` 下的模型文件是通知作业识别功能所需资源：

- `uie_mini.onnx`
- `vocab.txt`

这些文件会随 App 打包，不能删除。

## 清理说明

当前目录已移除以下非运行必要内容：

- Git / IDE / Gradle 生成缓存
- `app/build` 构建产物
- 顶层历史占位目录和设计文档

保留内容均与构建、运行或项目维护直接相关。
