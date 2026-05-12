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

## 目录结构

```
E:/Macrosoft/
├── build.gradle              # 根构建脚本（AGP 插件声明）
├── settings.gradle           # 项目名 & 模块注册 & Maven 仓库配置
├── gradle.properties         # JVM 参数 & AndroidX 开关
├── local.properties          # 本地 SDK 路径
├── gradlew / gradlew.bat     # Gradle Wrapper
├── README.md                 # 项目说明（空）
├── 修改日志.txt               # 修改日志（空）
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
│       │   └── ui/
│       │       ├── imports/          # 导入功能模块
│       │       ├── message/          # 消息 Tab
│       │       ├── settings/         # 设置功能模块
│       │       ├── timetable/        # 课表 Tab
│       │       └── todo/             # 待办 Tab
│       └── res/
│           ├── color/                # 颜色选择器
│           ├── layout/               # 10 个布局文件
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
| `data` | 空壳 | 仅含空 README，预留给数据层 |

---

## 关键文件及作用

### 构建配置

| 文件 | 作用 |
|------|------|
| `E:/Macrosoft/build.gradle` | 根构建脚本：声明 `com.android.application` 插件 v8.5.2，`apply false` |
| `E:/Macrosoft/app/build.gradle` | App 模块构建脚本：SDK 36、Java 17、依赖 (AppCompat 1.7.0 + Material 1.12.0 + RecyclerView 1.3.2) |
| `E:/Macrosoft/settings.gradle` | 项目名 `JuShiUc01Demo`；阿里云 Maven 镜像（google、central、gradle-plugin）；只 include `:app` |
| `E:/Macrosoft/gradle.properties` | `org.gradle.jvmargs=-Xmx2048m`，启用 AndroidX |

### 核心 Java 文件

| 文件 | 行数 | 作用 |
|------|------|------|
| `app/src/main/java/com/jushi/demo/main/BaseActivity.java` | 67 | **全局 Activity 基类**：<br>• `attachBaseContext` 注入字体缩放（通过 `UiContextWrapper`）<br>• `onStart` 应用主题模式（深色/浅色/跟随系统）<br>• `applyGlobalTypography()` 递归遍历 View 树统一加粗<br>• 用 Handler 延迟二次执行，确保动态生成的 View 也能生效 |
| `app/src/main/java/com/jushi/demo/main/MainActivity.java` | 106 | **应用主入口（LAUNCHER Activity）**：<br>• 底部导航栏 4 个 Tab（待办/课表/消息/设置）<br>• Fragment 切换逻辑<br>• 导入菜单（PopupMenu），跳转至消息源选择或课表导入 |
| `app/src/main/java/com/jushi/demo/main/ui/settings/UiPreferences.java` | 48 | **设置持久化工具类**：<br>• SharedPreferences 读写<br>• 三个设置项：`font_scale`（float）、`font_bold`（boolean）、`theme_mode`（int）<br>• 主题模式映射到 `AppCompatDelegate.MODE_NIGHT_*` |
| `app/src/main/java/com/jushi/demo/main/ui/settings/UiContextWrapper.java` | 18 | **Context 包装器**：<br>• 读取 `UiPreferences` 中的 `fontScale`<br>• 写入系统 `Configuration.fontScale`<br>• 实现全局字体缩放，无需每个页面单独处理 |

### 功能页面

#### 底部导航 Tab（Fragment）

| 文件 | 状态 | 说明 |
|------|------|------|
| `ui/todo/TodoFragment.java` | 空壳占位 | 仅 inflate 布局，无业务逻辑 |
| `ui/timetable/TimetableFragment.java` | 空壳占位 | 仅 inflate 布局，无业务逻辑 |
| `ui/message/MessageFragment.java` | 空壳占位 | 仅 inflate 布局，无业务逻辑 |
| `ui/settings/SettingsFragment.java` | 功能就绪 | 三个入口按钮跳转到子设置页 |

#### 导入流程（核心落地业务）

| 文件 | 作用 |
|------|------|
| `ui/imports/SelectSourceActivity.java` | **步骤1 — 选择消息源**：<br>• 展示 6 个平台供多选：微信、企业微信、QQ、学习通、超算习堂、雨课堂<br>• 记忆上次选择（SharedPreferences）<br>• 点击"保存"跳转到步骤2 |
| `ui/imports/SourceItem.java` | 消息源数据模型：id + name + selected |
| `ui/imports/SourceAdapter.java` | RecyclerView 适配器：CheckBox + 整行点击切换 |
| `ui/imports/ConfigureTargetsActivity.java` | **步骤2 — 配置监听目标**：<br>• 为每个选中的消息源动态生成输入框<br>• 聊天类（微信/企微/QQ）提示"群聊名称"<br>• 学习类（学习通等）提示"课程名称"<br>• 多行输入，每行一个目标<br>• 保存格式化数据到 SharedPreferences |
| `ui/imports/ImportTimetableActivity.java` | 导入课表页面 — 空壳占位 |

#### 设置子页面

| 文件 | 状态 | 功能 |
|------|------|------|
| `ui/settings/GeneralSettingsActivity.java` | **功能完备** | 字体大小（小/标准/大）、字体粗细（标准/加粗）、主题（系统/浅色/深色），通过 `recreate()` 即时生效 |
| `ui/settings/AuthoritySettingsActivity.java` | 静态占位 | 仅渲染布局 |
| `ui/settings/ProfileSettingsActivity.java` | 静态占位 | 仅渲染布局 |

### 资源文件

| 路径 | 说明 |
|------|------|
| `res/layout/activity_main.xml` | 主界面布局：顶部标题栏 + Fragment 容器 + 底部导航栏 |
| `res/layout/activity_select_source.xml` | 消息源选择页 |
| `res/layout/activity_configure_targets.xml` | 监听目标配置页 |
| `res/layout/activity_import_timetable.xml` | 导入课表页 |
| `res/layout/activity_settings_general.xml` | 通用设置页（RadioGroup × 3） |
| `res/layout/activity_settings_authority.xml` | 权限设置页 |
| `res/layout/activity_settings_profile.xml` | 个人资料页 |
| `res/layout/fragment_timetable.xml` | 课表 Fragment 布局 |
| `res/layout/fragment_todo.xml` | 待办 Fragment 布局 |
| `res/layout/fragment_message.xml` | 消息 Fragment 布局 |
| `res/layout/fragment_settings.xml` | 设置 Fragment 布局（三个入口 item） |
| `res/layout/item_source.xml` | 消息源列表项布局 |
| `res/menu/menu_bottom_nav.xml` | 底部导航菜单项定义 |
| `res/menu/menu_import_actions.xml` | 导入下拉菜单项定义 |
| `res/values/strings.xml` | 全部中文字符串（32 条） |
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
| `ImportTimetableActivity` | `false` | 导入课表（占位） |
| `ConfigureTargetsActivity` | `false` | 配置监听目标 |
| `SelectSourceActivity` | `false` | 选择消息源 |

---

## 架构特点

1. **单 Activity + 多 Fragment**：`MainActivity` 承载 4 个 Tab 的 Fragment 切换
2. **全局 UI 定制**：`BaseActivity` 通过 `attachBaseContext` 注入字体缩放，递归遍历 View 树统一加粗/主题
3. **导入流程**是唯一有实质逻辑的功能：`选择消息源 → 配置监听目标 → 保存到 SharedPreferences`
4. **3 个核心 Tab**（待办/课表/消息）目前仅渲染空布局，属于骨架占位
5. **通用设置功能完备**：字体/主题修改即时生效，通过 `recreate()` 重建 Activity 刷新
6. **数据持久化**全部使用 `SharedPreferences`，无数据库
7. **依赖极简**：仅 AppCompat + Material + RecyclerView 三个第三方库

---

## 数据流

```
用户设置 (字体/主题)
  └─ UiPreferences (SharedPreferences)
       ├─ UiContextWrapper → Configuration.fontScale → 全局生效
       ├─ BaseActivity.onStart() → AppCompatDelegate.setDefaultNightMode() → 主题切换
       └─ BaseActivity.applyGlobalTypography() → 递归 setTypeface → 加粗生效

导入流程
  └─ SelectSourceActivity
       └─ 选择平台 → Intent 传递 → ConfigureTargetsActivity
            └─ 填写群聊/课程 → SharedPreferences "jushi_demo_prefs"
```
