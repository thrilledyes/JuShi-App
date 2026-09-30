package com.jushi.demo.main.ui.imports;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.database.db.DBManager;
import com.jushi.demo.main.ui.timetable.WeekUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OnlineTimetableImportActivity extends BaseActivity {
    public static final String EXTRA_UNIVERSITY_NAME = "university_name";
    public static final String EXTRA_LOGIN_URL = "login_url";

    private static final String DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    + "AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/125.0.0.0 Safari/537.36";

    private static final String SYSU_TIMETABLE_EXPORT_PAGE_URL =
            "https://jwxt.sysu.edu.cn/jwxt/mk/schedule-web/#/studentTimeTabPrint"
                    + "?code=jwxsd_xskbcx"
                    + "&resourceName=%25E8%25AF%25BE%25E8%25A1%25A8%25E6%259F%25A5%25E8%25AF%25A2";

    private static final String SYSU_TIMETABLE_OUTPUT_URL =
            "https://jwxt.sysu.edu.cn/jwxt/timetable-search/stuTimeTabPrint/output";

    private static final String DIRECT_OUTPUT_FETCH_JS =
            "(function(){"
                    + "function fail(msg){try{window.JushiExportBridge.onExportError(String(msg||'下载失败'));}catch(e){}}"
                    + "function currentAcadYear(){"
                    + "try{"
                    + "var el=document.querySelector('[name=acadYear],#acadYear');"
                    + "if(el&&el.value){return String(el.value).trim();}"
                    + "var text=document.body?document.body.innerText:'';"
                    + "var m=text.match(/(20\\d{2})\\s*学年度?\\s*第?\\s*([一二12])\\s*学期/);"
                    + "if(m){var sem=(m[2]==='一'||m[2]==='1')?'1':'2';return m[1]+'-'+sem;}"
                    + "}catch(e){}"
                    + "var d=new Date();var y=d.getFullYear();var month=d.getMonth()+1;"
                    + "return month>=8?y+'-1':(y-1)+'-2';"
                    + "}"
                    + "function sendBlob(blob){"
                    + "try{"
                    + "var reader=new FileReader();"
                    + "reader.onload=function(){try{window.JushiExportBridge.onExportFile(String(reader.result||''));}catch(e){}};"
                    + "reader.onerror=function(){fail('读取导出文件失败');};"
                    + "reader.readAsDataURL(blob);"
                    + "}catch(e){fail(e.message||e);}"
                    + "}"
                    + "var body='acadYear='+encodeURIComponent(currentAcadYear())+'&submitFlag=1&containKey=1%2C2%2C3%2C4%2C5&_t='+Date.now();"
                    + "fetch('" + SYSU_TIMETABLE_OUTPUT_URL + "',{"
                    + "method:'POST',"
                    + "credentials:'include',"
                    + "headers:{'Accept':'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8','Content-Type':'application/x-www-form-urlencoded'},"
                    + "body:body"
                    + "}).then(function(resp){"
                    + "if(!resp.ok){return resp.text().then(function(){fail('服务器返回 '+resp.status);});}"
                    + "var ct=(resp.headers.get('content-type')||'').toLowerCase();"
                    + "if(ct.indexOf('text/html')>=0){return resp.text().then(function(){fail('服务器返回了网页而不是课表文件');});}"
                    + "return resp.blob().then(function(blob){"
                    + "if(!blob||blob.size===0){fail('导出的课表文件为空');return;}"
                    + "sendBlob(blob);"
                    + "});"
                    + "}).catch(function(e){fail(e&&e.message?e.message:e);});"
                    + "return 'started';"
                    + "})();";

    private WebView webView;
    private Button importButton;
    private String universityName;
    private String loginUrl;
    private String defaultUserAgent;
    private boolean desktopMode = false;
    private boolean waitingForExportPage = false;
    private boolean exportConfirmShown = false;
    private boolean exportFileReceived = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        universityName = getIntent().getStringExtra(EXTRA_UNIVERSITY_NAME);
        loginUrl = getIntent().getStringExtra(EXTRA_LOGIN_URL);
        if (loginUrl == null || loginUrl.trim().isEmpty()) {
            loginUrl = "https://jwxt.sysu.edu.cn/";
        }
        setContentView(buildRoot());
        webView.loadUrl(loginUrl);
    }

    private LinearLayout buildRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF5F6F8);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(12), 0);
        bar.setBackgroundColor(0xFF1565C0);

        ImageButton back = new ImageButton(this);
        back.setImageResource(android.R.drawable.ic_media_previous);
        back.setColorFilter(0xFFFFFFFF);
        back.setBackgroundColor(0x00000000);
        back.setOnClickListener(v -> finish());
        bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = new TextView(this);
        title.setText((universityName == null ? "联网" : universityName) + "导入课表");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        TextView hint = new TextView(this);
        hint.setText("请先在手机版教务系统登录。登录后点击自动导入，应用会临时打开电脑端课表导出页并导入完整课表。");
        hint.setTextColor(0xFF5F6670);
        hint.setTextSize(13);
        hint.setPadding(dp(12), dp(8), dp(12), dp(8));
        root.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        defaultUserAgent = settings.getUserAgentString();
        applyUserAgent();
        webView.addJavascriptInterface(new ExportBridge(), "JushiExportBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                maybeShowExportConfirm(url);
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);

        importButton = new Button(this);
        importButton.setText("自动导入");
        importButton.setAllCaps(false);
        importButton.setTextSize(12);
        importButton.setOnClickListener(v -> startOnlineExportImport());
        actions.addView(importButton, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        ));

        root.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        return root;
    }

    private void applyUserAgent() {
        if (webView == null) {
            return;
        }
        webView.getSettings().setUserAgentString(desktopMode ? DESKTOP_USER_AGENT : defaultUserAgent);
    }

    private void startOnlineExportImport() {
        waitingForExportPage = true;
        exportConfirmShown = false;
        exportFileReceived = false;
        importButton.setEnabled(false);
        importButton.setText("正在打开课表页...");
        desktopMode = true;
        applyUserAgent();
        webView.loadUrl(SYSU_TIMETABLE_EXPORT_PAGE_URL);
    }

    private void maybeShowExportConfirm(String url) {
        if (!waitingForExportPage || exportConfirmShown || !isSysuTimetableExportPage(url)) {
            return;
        }
        exportConfirmShown = true;
        importButton.setEnabled(true);
        importButton.setText("自动导入");

        new AlertDialog.Builder(this)
                .setTitle("确认自动导入")
                .setMessage("将从教务系统下载完整课表文件，并直接导入聚事。")
                .setPositiveButton("确定", (dialog, which) -> downloadKnownSysuTimetableOutput())
                .setNegativeButton("取消", (dialog, which) -> {
                    waitingForExportPage = false;
                    exportConfirmShown = false;
                })
                .show();
    }

    private boolean isSysuTimetableExportPage(String url) {
        return url != null
                && url.contains("jwxt.sysu.edu.cn")
                && url.contains("/schedule-web/")
                && url.contains("studentTimeTabPrint");
    }

    private void downloadKnownSysuTimetableOutput() {
        waitingForExportPage = false;
        exportFileReceived = false;
        importButton.setEnabled(false);
        importButton.setText("正在下载课表...");
        webView.evaluateJavascript(DIRECT_OUTPUT_FETCH_JS, value -> scheduleExportTimeout());
    }

    private void scheduleExportTimeout() {
        handler.postDelayed(() -> {
            if (!exportFileReceived
                    && importButton != null
                    && "正在下载课表...".contentEquals(importButton.getText())) {
                resetImportButton();
                Toast.makeText(this, "没有收到教务系统导出的课表文件，请稍后重试", Toast.LENGTH_LONG).show();
            }
        }, 25000);
    }

    private void resetImportButton() {
        importButton.setEnabled(true);
        importButton.setText("自动导入");
    }

    private class ExportBridge {
        @JavascriptInterface
        public void onExportFile(String dataUrl) {
            if (exportFileReceived) {
                return;
            }
            exportFileReceived = true;
            runOnUiThread(() -> {
                importButton.setEnabled(false);
                importButton.setText("正在解析课表...");
            });

            new Thread(() -> {
                try {
                    byte[] fileBytes = decodeDataUrl(dataUrl);
                    DocxParser.ParseResult result = DocxParser.parse(fileBytes);
                    runOnUiThread(() -> handleExportParseResult(result));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        exportFileReceived = false;
                        resetImportButton();
                        Toast.makeText(OnlineTimetableImportActivity.this,
                                "解析导出文件失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }

        @JavascriptInterface
        public void onExportError(String message) {
            if (exportFileReceived) {
                return;
            }
            runOnUiThread(() -> {
                resetImportButton();
                Toast.makeText(OnlineTimetableImportActivity.this,
                        "下载课表失败：" + (message == null ? "未知错误" : message),
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private byte[] decodeDataUrl(String dataUrl) throws Exception {
        if (dataUrl == null || dataUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("导出文件为空");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("导出文件格式不正确");
        }
        String meta = dataUrl.substring(0, comma).toLowerCase(Locale.ROOT);
        String data = dataUrl.substring(comma + 1);
        if (meta.contains(";base64")) {
            return Base64.decode(data, Base64.DEFAULT);
        }
        String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8.name());
        return decoded.getBytes(StandardCharsets.UTF_8);
    }

    private void handleExportParseResult(DocxParser.ParseResult result) {
        if (result != null && result.isSuccess()
                && result.courses != null && !result.courses.isEmpty()) {
            maybeConfirmSemesterStart(result.courses, result.academicYear, result.semester);
            return;
        }

        resetImportButton();
        String message = result == null || result.error == null || result.error.trim().isEmpty()
                ? "未能识别导出的课表文件"
                : result.error;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void maybeConfirmSemesterStart(List<Course> courses, int academicYear, int semester) {
        if (academicYear <= 0 || semester <= 0) {
            showImportConfirm(courses, academicYear, semester);
            return;
        }

        long estimatedStart = WeekUtils.estimateSemesterStart(academicYear, semester);
        String semLabel = semester == 1 ? "第一学期" : "第二学期";
        String dateStr = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
                .format(new Date(estimatedStart));
        String dayOfWeek = new SimpleDateFormat("EEEE", Locale.getDefault())
                .format(new Date(estimatedStart));
        String msg = academicYear + "学年度 " + semLabel + "\n\n"
                + "预计开学日期:\n" + dateStr + " (" + dayOfWeek + ")\n\n"
                + "日期正确吗？";

        new AlertDialog.Builder(this)
                .setTitle("检测到学期信息")
                .setMessage(msg)
                .setPositiveButton("确认", (dialog, which) -> {
                    WeekUtils.setSemesterStart(this, estimatedStart);
                    WeekUtils.setAcademicYear(this, academicYear);
                    WeekUtils.setSemester(this, semester);
                    showImportConfirm(courses, academicYear, semester);
                })
                .setNegativeButton("手动调整", (dialog, which) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(estimatedStart);
                    new android.app.DatePickerDialog(this,
                            (view, year, month, dayOfMonth) -> {
                                Calendar selected = Calendar.getInstance();
                                selected.set(year, month, dayOfMonth, 0, 0, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                WeekUtils.setSemesterStart(this, selected.getTimeInMillis());
                                WeekUtils.setAcademicYear(this, academicYear);
                                WeekUtils.setSemester(this, semester);
                                showImportConfirm(courses, academicYear, semester);
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH))
                            .show();
                })
                .setCancelable(false)
                .show();
    }

    private void showImportConfirm(List<Course> courses, int academicYear, int semester) {
        String message = buildPreview(courses);
        if (academicYear > 0 && semester > 0) {
            String semesterLabel = semester == 1 ? "第一学期" : "第二学期";
            message = academicYear + "学年度 " + semesterLabel + "\n\n" + message;
        }
        new AlertDialog.Builder(this)
                .setTitle("识别到 " + courses.size() + " 门课程")
                .setMessage(message)
                .setPositiveButton("导入", (dialog, which) -> saveCourses(courses, academicYear, semester))
                .setNegativeButton("取消", (dialog, which) -> resetImportButton())
                .show();
    }

    private String buildPreview(List<Course> courses) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(courses.size(), 10);
        for (int i = 0; i < count; i++) {
            Course course = courses.get(i);
            sb.append(course.getCourseName())
                    .append("  ")
                    .append(WeekUtils.DAY_NAMES[course.getDayOfWeek()])
                    .append(" ")
                    .append(course.getStartPeriod())
                    .append("-")
                    .append(course.getEndPeriod())
                    .append("节");
            if (course.getWeekRange() != null && !course.getWeekRange().isEmpty()) {
                sb.append("  第").append(course.getWeekRange()).append("周");
            }
            sb.append('\n');
        }
        if (courses.size() > count) {
            sb.append("...");
        }
        return sb.toString();
    }

    private void saveCourses(List<Course> courses, int academicYear, int semester) {
        importButton.setEnabled(false);
        new Thread(() -> {
            DBManager db = new DBManager(this);
            try {
                db.deleteAllCourses();
                db.insertCourses(courses);
                db.ensureCourseAssistantTodoMessage();
                if (academicYear > 0 && semester > 0) {
                    WeekUtils.setAcademicYear(this, academicYear);
                    WeekUtils.setSemester(this, semester);
                }
            } finally {
                db.close();
            }
            runOnUiThread(() -> {
                Toast.makeText(this, "已导入 " + courses.size() + " 门课程", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
