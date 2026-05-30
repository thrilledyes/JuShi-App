# 聚事 (JuShi) — Android 项目结构文档

## 项目概览

| 属性 | 值 |
|------|-----|
| 项目名 | **聚事** (JuShiUc01Demo) |
| 包名 | `com.jushi.demo.uc01` |
| 命名空间 | `com.jushi.demo.main` |
| compileSdk / targetSdk | **36** |
| minSdk | **24** |
| 语言 | **Java**（纯原生 Android，无 Kotlin） |
| 构建工具 | Gradle 8.5.2 + AGP 8.5.2 |
| Maven 仓库 | 阿里云镜像加速 |

---

## 环境配置

### 1. 系统要求

| 项目 | 要求 |
|------|------|
| JDK | 17（项目使用 JavaVersion.VERSION_17） |
| Android SDK | compileSdk 36，需安装 SDK Platform 36 |
| Android Studio | 推荐最新稳定版（内置 Gradle Wrapper，无需单独安装 Gradle） |
| 操作系统 | Windows / macOS / Linux 均可 |

### 2. ANDROID_HOME 环境变量配置

Gradle 构建依赖此变量定位 Android SDK 路径。

**Windows（PowerShell，管理员权限）：**

```powershell
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "你的SDK路径", "User")
```

默认路径通常是 `C:\Users\<用户名>\AppData\Local\Android\Sdk`，自定义安装的请替换为实际路径。

**macOS / Linux（添加到 `~/.bashrc` 或 `~/.zshrc`）：**

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

然后执行 `source ~/.bashrc` 或 `source ~/.zshrc` 生效。

设置后需**重启终端**才能生效，可用以下命令验证：

```bash
echo $ANDROID_HOME        # Linux/macOS
echo %ANDROID_HOME%       # Windows CMD
echo $env:ANDROID_HOME    # Windows PowerShell
```

### 3. local.properties 说明

- `local.properties` 已加入 `.gitignore`，不会被提交到版本控制
- 如果 `ANDROID_HOME` 环境变量已正确设置，Gradle 会自动使用该路径，无需手动创建 `local.properties`
- 如需手动指定，可创建 `local.properties` 并写入 `sdk.dir=你的SDK路径`（Windows 路径需转义反斜杠，如 `sdk.dir=C\:\\Android\\Sdk`）

---

## 构建与运行

本项目使用 **Android Studio** 进行构建和运行，所有操作均为可视化界面操作。

### 1. 打开项目

1. 启动 Android Studio
2. 选择 `Open`，浏览到项目根目录（`E:/Macrosoft/`）
3. 等待 Gradle 同步完成（首次同步需要下载依赖，耗时较长）

### 2. 运行应用

1. 连接 Android 真机（开启开发者选项和 USB 调试）或启动 Android 模拟器
2. 在 Android Studio 顶部工具栏选择目标设备
3. 点击 **Run** 按钮（绿色三角形，或快捷键 `Shift+F10`）
4. 应用将自动编译、安装并启动

### 3. 构建 APK

1. 菜单栏选择 `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
2. 构建完成后，点击右下角通知中的 `locate` 可直接打开产物目录
3. APK 位于 `app/build/outputs/apk/debug/` 或 `app/build/outputs/apk/release/`

### 4. 常见问题

- **Gradle 同步失败**：检查 `ANDROID_HOME` 环境变量是否正确设置，网络是否能访问阿里云 Maven 镜像
- **SDK 找不到**：确认 SDK Platform 36 已安装，通过 `File` → `Settings` → `Languages & Frameworks` → `Android SDK` 检查
- **JDK 版本不匹配**：项目要求 JDK 17，通过 `File` → `Project Structure` → `SDK Location` 确认

---

## 目录结构

```
E:/Macrosoft/
├── build.gradle              # 根构建脚本（AGP 插件声明）
├── settings.gradle           # 项目名 & 模块注册 & Maven 仓库配置
├── gradle.properties         # JVM 参数 & AndroidX 开关
├── local.properties          # 本地 SDK 路径
├── gradlew / gradlew.bat     # Gradle Wrapper
├── README.md                 # 项目说明
├── 修改日志.txt               # 修改日志
├── 用户界面设计.docx           # UI 设计文档（6.5MB）
│
├── app/                      # ★ 主模块（唯一有代码的模块）
│   ├── build.gradle          # SDK 版本、依赖配置
│   ├── proguard-rules.pro    # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/jushi/demo/main/
│       │   ├── BaseActivity.java
│       │   ├── MainActivity.java
│       │   ├── data/                  # 数据层（新增）
│       │   │   ├── entity/Course.java          # Room 实体
│       │   │   ├── CourseDao.java              # Room DAO
│       │   │   └── CourseDatabase.java         # Room 数据库单例
│       │   └── ui/
│       │       ├── imports/          # 导入功能模块
│       │       │   ├── DocxParser.java         # DOCX 课表解析器（新增）
│       │       │   ├── ImportTimetableActivity.java
│       │       │   ├── SelectSourceActivity.java
│       │       │   ├── ConfigureTargetsActivity.java
│       │       │   ├── SourceItem.java
│       │       │   └── SourceAdapter.java
│       │       ├── message/          # 消息 Tab
│       │       ├── settings/         # 设置功能模块
│       │       ├── timetable/        # 课表 Tab
│       │       │   ├── TimetableFragment.java
│       │       │   ├── TimetableGridView.java  # 自定义课表网格视图（新增）
│       │       │   └── WeekUtils.java          # 周次/日期工具类（新增）
│       │       └── todo/             # 待办 Tab
│       └── res/
│           ├── color/                # 颜色选择器
│           ├── layout/               # 13 个布局文件
│           ├── menu/                 # 底部导航 & 导入菜单
│           ├── values/               # 字符串、颜色、日间主题
│           └── values-night/         # 夜间主题 & 颜色
│
├── core/                     # 空模块（预留）
│   └── coreREADME.md
│
└── data/                     # 空模块（预留）
    └── dataREADME.md
```

---

## 模块说明

| 模块 | 状态 | 说明 |
|------|------|------|
| `app` | **活跃** | 所有代码和资源所在的主模块 |
| `core` | 空壳 | 仅含空 README，预留给核心工具类 |
| `data` | 空壳 | 仅含空 README，预留给远端数据层 |

---

## 关键文件及作用

### 构建配置

| 文件 | 作用 |
|------|------|
| `E:/Macrosoft/build.gradle` | 根构建脚本：声明 `com.android.application` 插件 v8.5.2，`apply false` |
| `E:/Macrosoft/app/build.gradle` | App 模块构建脚本：SDK 36、Java 17、依赖（AppCompat 1.7.0 + Material 1.12.0 + RecyclerView 1.3.2 + Room 2.6.1） |
| `E:/Macrosoft/settings.gradle` | 项目名 `JuShiUc01Demo`；阿里云 Maven 镜像（google、central、gradle-plugin）；只 include `:app` |
| `E:/Macrosoft/gradle.properties` | `org.gradle.jvmargs=-Xmx2048m`，启用 AndroidX |

### 核心 Java 文件

| 文件 | 行数 | 作用 |
|------|------|------|
| `app/.../BaseActivity.java` | 67 | **全局 Activity 基类**：`attachBaseContext` 注入字体缩放；`onStart` 应用主题模式；递归遍历 View 树统一加粗 |
| `app/.../MainActivity.java` | 106 | **应用主入口（LAUNCHER Activity）**：底部导航栏 4 个 Tab；Fragment 切换逻辑；导入菜单 |
| `app/.../settings/UiPreferences.java` | 48 | **设置持久化工具类**：SharedPreferences 读写；字体大小/加粗/主题模式 |
| `app/.../settings/UiContextWrapper.java` | 18 | **Context 包装器**：注入字体缩放到系统 Configuration |

### 数据层（新增）

| 文件 | 作用 |
|------|------|
| `data/entity/Course.java` | Room 实体：课程名、教师、地点、星期、起始节次、结束节次、周次范围、颜色 |
| `data/CourseDao.java` | Room DAO 接口：`getAllCourses()`、`getByDayOfWeek()`、`insertAll()`、`deleteAll()` |
| `data/CourseDatabase.java` | Room 数据库单例：`jushi_courses.db`，版本 1，fallbackToDestructiveMigration |

### 功能页面

#### 课表展示（已实现）

| 文件 | 作用 |
|------|------|
| `ui/timetable/TimetableFragment.java` | **课表 Tab**：从 Room 加载课程数据，按当前周过滤，展示周视图网格；周切换按钮；空态引导跳转导入 |
| `ui/timetable/TimetableGridView.java` | **自定义 Canvas 课表网格**：7 列（周日~周六）× 11 节次；跨节次课程合并为彩色大格子；点击课程弹出详情对话框 |
| `ui/timetable/WeekUtils.java` | 周次/日期工具：学期起始日管理、当前周切换、周范围过滤、时间段格式化 |

#### 导入流程（课表导入已实现）

| 文件 | 作用 |
|------|------|
| `ui/imports/SelectSourceActivity.java` | **步骤1 — 选择消息源**：6 个平台多选（微信/企微/QQ/学习通/超算习堂/雨课堂）；记忆上次选择 |
| `ui/imports/SourceItem.java` | 消息源数据模型 |
| `ui/imports/SourceAdapter.java` | RecyclerView 适配器 |
| `ui/imports/ConfigureTargetsActivity.java` | **步骤2 — 配置监听目标**：为选中的消息源动态生成输入框；聊天类提示"群聊名称"，学习类提示"课程名称" |
| `ui/imports/ImportTimetableActivity.java` | **课表导入**：SAF 文件选择器选取 .doc/.docx 文件 → 后台解析 → 预览确认 → Room 存储 |
| `ui/imports/DocxParser.java` | **DOCX 解析器**：支持 Flat OPC XML 和 ZIP 两种格式；解析 Word 表格提取课程数据；处理单元格纵向合并（vMerge）识别跨节次课程 |

#### 其他 Tab

| 文件 | 状态 | 说明 |
|------|------|------|
| `ui/todo/TodoFragment.java` | 空壳占位 | 仅 inflate 布局 |
| `ui/message/MessageFragment.java` | 空壳占位 | 仅 inflate 布局 |
| `ui/settings/SettingsFragment.java` | 功能就绪 | 三个入口按钮跳转到子设置页 |

#### 设置子页面

| 文件 | 状态 | 功能 |
|------|------|------|
| `ui/settings/GeneralSettingsActivity.java` | 功能完备 | 字体大小/粗细/主题，通过 `recreate()` 即时生效 |
| `ui/settings/AuthoritySettingsActivity.java` | 静态占位 | 仅渲染布局 |
| `ui/settings/ProfileSettingsActivity.java` | 静态占位 | 仅渲染布局 |

### 资源文件

| 路径 | 说明 |
|------|------|
| `res/layout/activity_main.xml` | 主界面布局 |
| `res/layout/activity_select_source.xml` | 消息源选择页 |
| `res/layout/activity_configure_targets.xml` | 监听目标配置页 |
| `res/layout/activity_import_timetable.xml` | 导入课表页：文件选择按钮 + 状态提示 + 预览列表 + 确认/重选按钮 |
| `res/layout/activity_settings_general.xml` | 通用设置页 |
| `res/layout/activity_settings_authority.xml` | 权限设置页 |
| `res/layout/activity_settings_profile.xml` | 个人资料页 |
| `res/layout/fragment_timetable.xml` | 课表布局：周选择器 + HorizontalScrollView + 网格视图 + 空态 |
| `res/layout/fragment_todo.xml` | 待办 Fragment 布局 |
| `res/layout/fragment_message.xml` | 消息 Fragment 布局 |
| `res/layout/fragment_settings.xml` | 设置 Fragment 布局 |
| `res/layout/item_source.xml` | 消息源列表项布局 |
| `res/menu/menu_bottom_nav.xml` | 底部导航菜单项定义 |
| `res/menu/menu_import_actions.xml` | 导入下拉菜单项定义 |
| `res/values/strings.xml` | 全部中文字符串 |
| `res/values/colors.xml` | 颜色定义 |
| `res/values/themes.xml` | 日间主题 |
| `res/values-night/themes.xml` | 夜间主题 |
| `res/color/bottom_nav_item_colors.xml` | 底部导航图标颜色选择器 |

---

## Activity 注册清单（AndroidManifest）

| Activity | exported | 说明 |
|----------|----------|------|
| `MainActivity` | `true` | **启动入口**，含 `MAIN` + `LAUNCHER` intent-filter |
| `GeneralSettingsActivity` | `false` | 通用设置 |
| `AuthoritySettingsActivity` | `false` | 权限设置（占位） |
| `ProfileSettingsActivity` | `false` | 个人资料（占位） |
| `ImportTimetableActivity` | `false` | 导入课表（DOCX 解析 + Room 存储） |
| `ConfigureTargetsActivity` | `false` | 配置监听目标 |
| `SelectSourceActivity` | `false` | 选择消息源 |

---

## 架构特点

1. **单 Activity + 多 Fragment**：`MainActivity` 承载 4 个 Tab 的 Fragment 切换
2. **全局 UI 定制**：`BaseActivity` 通过 `attachBaseContext` 注入字体缩放，递归遍历 View 树统一加粗/主题
3. **课表导入流程**：SAF 文件选择 → Flat OPC / ZIP DOCX 解析 → 表格识别 + vMerge 处理 → 预览确认 → Room 数据库持久化
4. **课表展示**：自定义 Canvas 视图，7 列（周日~周六）× 11 节次网格，跨节次课程合并为彩色大格子，周切换，点击查看详情
5. **消息源导入流程**：选择消息源 → 配置监听目标 → SharedPreferences 持久化
6. **数据持久化**：课表数据使用 Room 数据库；消息源和设置使用 SharedPreferences
7. **依赖**：AppCompat + Material + RecyclerView + Room 2.6.1

---

## DOCX 课程表格式说明

### 支持的文件格式

- **Flat OPC XML**（`.doc` 扩展名，单文件 XML）：教务系统导出的常见格式，`<?xml>` 开头
- **标准 DOCX**（`.docx` 扩展名，ZIP 包）：Office 标准格式，`PK` 开头

### 解析流程

```
用户选择 .doc/.docx 文件
  → 检测格式（XML 头 → Flat OPC；PK 头 → ZIP）
  → 提取 /word/document.xml
  → 遍历 <w:tbl> 表格
  → 识别表头行（含"星期日"~"星期六"）
  → 映射列索引 → dayOfWeek（1~7）
  → 每行解析节次号（"第N节" 或 "第N-M节"）
  → 处理 <w:vMerge> 纵向合并（restart/continue）识别跨节次课程
  → 提取单元格文本（周次/课程名/教师/地点）
  → 合并同天相邻同课程
  → 预览确认 → Room 存储
```

### 课表展示

- **横轴**：周日~周六 + 日期（如 `5/20`）
- **纵轴**：第1~11节 + 时间段（如 `8:00~8:45`）
- **课程格**：彩色圆角卡片，显示课程名（加粗）、教师、地点
- **跨节次**：纵向合并为一个大格子
- **交互**：点击课程弹出详情对话框；左右箭头切换周次

---

## 数据流

```
用户设置 (字体/主题)
  └─ UiPreferences (SharedPreferences)
       ├─ UiContextWrapper → Configuration.fontScale → 全局生效
       ├─ BaseActivity.onStart() → AppCompatDelegate.setDefaultNightMode() → 主题切换
       └─ BaseActivity.applyGlobalTypography() → 递归 setTypeface → 加粗生效

课表导入流程
  └─ ImportTimetableActivity
       └─ SAF 文件选择 → DocxParser.parse() → List<Course>
            └─ CourseDao.insertAll() → Room DB (jushi_courses.db)

课表展示
  └─ TimetableFragment
       └─ CourseDao.getAllCourses() → WeekUtils 过滤当前周
            └─ TimetableGridView.setCourses() → Canvas 绘制网格

消息源导入流程
  └─ SelectSourceActivity
       └─ 选择平台 → Intent 传递 → ConfigureTargetsActivity
            └─ 填写群聊/课程 → SharedPreferences "jushi_demo_prefs"
```
