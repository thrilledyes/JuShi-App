# JuShi (聚事)

[简体中文](README.md) | English

An Android campus assistant that brings your university timetable and group messages scattered across WeChat, QQ, XueXiTong and similar platforms into one app.

| Item | Value |
|------|-------|
| Application ID | `com.jushi.demo.uc01` |
| Namespace | `com.jushi.demo.main` |
| Language / Build | Java 17 · Gradle Wrapper 8.7 · AGP 8.5.2 |
| SDK | minSdk 24 / targetSdk 36 / compileSdk 36 |
| Key deps | AppCompat 1.7.0 · Material 1.12.0 · RecyclerView 1.3.2 · Room 2.6.1 |

## Features

- **Timetable import**: pick a `.doc` / `.docx` exported from the academic system; both Flat OPC XML and ZIP (OOXML) layouts are detected automatically. Tables are parsed incl. vertically merged cells that span periods. Results can be previewed and edited (name, teacher, room, weekday, periods, week range) before saving.
- **Timetable view**: a custom Canvas grid with 7 days × 11 periods; multi-period courses render as one colored card; week navigation (weeks 1–30), week-range filtering, tap for course details, and an empty state that guides to import.
- **Message sources**: pick from WeChat / WeCom / QQ / XueXiTong / EasyHPC / YuKeTang and set the group or course name for each, which becomes a capture rule.
- **Background capture**: a `NotificationListenerService` parses incoming notifications (source mapping, group-name cleanup, body extraction), filters them by rule, and appends them to a JSON file (rolling window of 500 messages).
- **Appearance**: font scale (0.9 / 1.0 / 1.1), global bold, and theme (follow system / light / dark), applied immediately.
- **Todo / Message tabs**: navigation entries exist; content is not implemented yet.

## Architecture

Single Activity with multiple fragments, packages split by feature; the UI layer talks to the data and capture layers directly.

```
MainActivity (4 bottom tabs + import menu)
├─ ui.timetable   TimetableFragment → TimetableGridView(Canvas) / PeriodLabelView / WeekUtils
├─ ui.imports     SelectSourceActivity → ConfigureTargetsActivity (source rules)
│                 ImportTimetableActivity → DocxParser (timetable parsing)
├─ ui.settings    SettingsFragment → General / Authority / Profile SettingsActivity
│                 UiPreferences (persistence) + UiContextWrapper (font scale)
├─ ui.todo · ui.message          placeholders
├─ utils          BackgroundNotificationListenerService → NotificationMessageParser
│                 → NotificationRuleStore (rules) → FilteredNotificationJsonStore (sink)
└─ data           Room: Course entity / CourseDao / CourseDatabase

BaseActivity: injects font scale, applies theme mode, walks the View tree for global weight
```

**Flows**

| Flow | Path |
|------|------|
| Import | SAF picker → `DocxParser.parse()` → preview/edit → `CourseDao.deleteAll() + insertAll()` → Room |
| Display | `CourseDao.getAllCourses()` → `WeekUtils` week filter → `TimetableGridView` draw |
| Capture | system notification → `MessageModel` → rule match → append `filtered_notifications.json` |
| Preferences | `UiPreferences` (SharedPreferences) → `UiContextWrapper` / `BaseActivity` |

**Storage**

| Data | Location |
|------|----------|
| Courses | Room `jushi_courses.db` → table `courses` |
| UI prefs | SharedPreferences `jushi_ui_prefs` |
| Semester start / current week | SharedPreferences `jushi_timetable_prefs` |
| Source selection | SharedPreferences `jushi_demo_prefs` |
| Capture rules | `filesDir/notification_rules.json` (`{sources:[{packageName,groupName}]}`) |
| Captured messages | `filesDir/filtered_notifications.json` (latest 500) |

## Project layout

```
JuShi-App/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/jushi/demo/main/
│   │   ├── MainActivity.java        # entry: 4 tabs + import menu
│   │   ├── BaseActivity.java        # font scale / theme / global weight
│   │   ├── data/                    # Room: Course, CourseDao, CourseDatabase
│   │   ├── ui/timetable/            # timetable rendering
│   │   ├── ui/imports/              # timetable import, source & target config
│   │   ├── ui/settings/             # settings screens & preference store
│   │   ├── ui/todo/ · ui/message/   # placeholder tabs
│   │   └── utils/                   # notification listening, parsing, rules, sink
│   └── res/                         # layout / menu / values / values-night
├── gradle/wrapper · gradlew(.bat)   # Gradle Wrapper 8.7
├── settings.gradle · build.gradle · gradle.properties
└── README.md · README_EN.md
```

## Build & deploy

**Requirements**: JDK 17+ and Android SDK (Platform 36 + Build Tools 34). Set `ANDROID_HOME`, or create `local.properties` in the project root with `sdk.dir=<SDK path>`.

```bash
./gradlew assembleDebug     # Windows: gradlew.bat assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk

./gradlew installDebug      # install to a connected device or emulator
```

Android Studio users can simply open the project root, wait for Gradle sync, and hit Run. Message capture requires granting notification access to JuShi in **Settings → Notification access**.

**Verification**: `assembleDebug` succeeds and produces `app-debug.apk`; core logic is covered by a closed-loop check (timetable parsing, week/period calculation, notification parse → rule filter → persistence) with 52 assertions, all passing.

## Known limitations

- Todo and Message tabs are placeholders; Profile and Authority screens are static stubs.
- No in-app onboarding for notification access — it must be granted manually in system settings.
- Semester start date is hardcoded to 2026-02-16 with no settings entry yet.
- Import replaces the whole timetable (delete-then-insert); incremental merge is not supported.
