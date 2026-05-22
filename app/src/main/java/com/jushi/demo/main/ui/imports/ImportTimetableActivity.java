package com.jushi.demo.main.ui.imports;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jushi.demo.main.BaseActivity;
import com.jushi.demo.main.R;
import com.jushi.demo.main.data.CourseDatabase;
import com.jushi.demo.main.data.entity.Course;
import com.jushi.demo.main.ui.timetable.WeekUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ImportTimetableActivity extends BaseActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1001;

    private TextView tvStatus;
    private Button btnPickFile;
    private View previewContainer;
    private RecyclerView rvPreview;
    private View actionButtons;
    private Button btnReselect;
    private Button btnConfirm;

    private List<Course> parsedCourses;
    private String fileName;
    private CoursePreviewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_timetable);

        tvStatus = findViewById(R.id.tvStatus);
        btnPickFile = findViewById(R.id.btnPickFile);
        previewContainer = findViewById(R.id.previewContainer);
        rvPreview = findViewById(R.id.rvPreview);
        actionButtons = findViewById(R.id.actionButtons);
        btnReselect = findViewById(R.id.btnReselect);
        btnConfirm = findViewById(R.id.btnConfirm);

        rvPreview.setLayoutManager(new LinearLayoutManager(this));

        btnPickFile.setOnClickListener(v -> openFilePicker());
        btnReselect.setOnClickListener(v -> resetToInitial());
        btnConfirm.setOnClickListener(v -> saveCourses());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/msword",
                "application/octet-stream",
                "*/*"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CODE_PICK_FILE || resultCode != RESULT_OK || data == null) return;

        Uri uri = data.getData();
        if (uri == null) return;

        fileName = getFileName(uri);
        tvStatus.setText(getString(R.string.import_timetable_parsing));
        btnPickFile.setEnabled(false);

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                is.close();
                byte[] fileBytes = bos.toByteArray();

                DocxParser.ParseResult result = DocxParser.parse(fileBytes);

                runOnUiThread(() -> {
                    btnPickFile.setEnabled(true);
                    if (result.isSuccess()) {
                        parsedCourses = result.courses;
                        showPreview();
                    } else {
                        tvStatus.setText(result.error);
                        Toast.makeText(this, result.error, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    btnPickFile.setEnabled(true);
                    String msg = getString(R.string.import_timetable_parse_error, e.getMessage());
                    tvStatus.setText(msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showPreview() {
        tvStatus.setText(getString(R.string.import_timetable_preview_count, parsedCourses.size(), fileName));
        previewContainer.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);

        adapter = new CoursePreviewAdapter(parsedCourses, position -> showEditDialog(position));
        rvPreview.setAdapter(adapter);
    }

    private void showEditDialog(int position) {
        Course course = parsedCourses.get(position);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_course, null);

        EditText etCourseName = dialogView.findViewById(R.id.etCourseName);
        EditText etTeacher = dialogView.findViewById(R.id.etTeacher);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        Spinner spDayOfWeek = dialogView.findViewById(R.id.spDayOfWeek);
        EditText etStartPeriod = dialogView.findViewById(R.id.etStartPeriod);
        EditText etEndPeriod = dialogView.findViewById(R.id.etEndPeriod);
        EditText etWeekRange = dialogView.findViewById(R.id.etWeekRange);

        // Day of week spinner: Monday-first, values match Course.dayOfWeek
        String[] dayLabels = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int[] dayValues = {2, 3, 4, 5, 6, 7, 1};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, dayLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDayOfWeek.setAdapter(spinnerAdapter);

        // Populate fields
        etCourseName.setText(course.getCourseName());
        etTeacher.setText(course.getTeacher());
        etLocation.setText(course.getLocation());

        int dow = course.getDayOfWeek();
        for (int i = 0; i < dayValues.length; i++) {
            if (dayValues[i] == dow) {
                spDayOfWeek.setSelection(i);
                break;
            }
        }

        etStartPeriod.setText(String.valueOf(course.getStartPeriod()));
        etEndPeriod.setText(String.valueOf(course.getEndPeriod()));
        etWeekRange.setText(course.getWeekRange());

        new AlertDialog.Builder(this)
                .setTitle("编辑课程")
                .setView(dialogView)
                .setPositiveButton("确定", (dialog, which) -> {
                    String name = etCourseName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "课程名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int startP, endP;
                    try {
                        startP = Integer.parseInt(etStartPeriod.getText().toString().trim());
                        endP = Integer.parseInt(etEndPeriod.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "节次请输入数字", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (startP < 1 || startP > WeekUtils.MAX_PERIODS
                            || endP < 1 || endP > WeekUtils.MAX_PERIODS) {
                        Toast.makeText(this, "节次范围: 1-" + WeekUtils.MAX_PERIODS, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    course.setCourseName(name);
                    course.setTeacher(etTeacher.getText().toString().trim());
                    course.setLocation(etLocation.getText().toString().trim());
                    course.setDayOfWeek(dayValues[spDayOfWeek.getSelectedItemPosition()]);
                    course.setStartPeriod(startP);
                    course.setEndPeriod(endP);
                    course.setWeekRange(etWeekRange.getText().toString().trim());

                    adapter.notifyItemChanged(position);
                    Toast.makeText(this, "已更新", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveCourses() {
        tvStatus.setText(getString(R.string.import_timetable_saving));
        btnConfirm.setEnabled(false);

        new Thread(() -> {
            CourseDatabase db = CourseDatabase.getInstance(this);
            db.courseDao().deleteAll();
            db.courseDao().insertAll(parsedCourses);

            runOnUiThread(() -> {
                Toast.makeText(this,
                        getString(R.string.import_timetable_success, parsedCourses.size()),
                        Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private void resetToInitial() {
        parsedCourses = null;
        fileName = null;
        tvStatus.setText(getString(R.string.import_timetable_hint));
        previewContainer.setVisibility(View.GONE);
        actionButtons.setVisibility(View.GONE);
    }

    private String getFileName(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
            }
        } catch (Exception ignored) {}
        return name != null ? name : "unknown";
    }

    interface OnCourseClickListener {
        void onCourseClick(int position);
    }

    private static class CoursePreviewAdapter extends RecyclerView.Adapter<CoursePreviewAdapter.ViewHolder> {

        private final List<Course> courses;
        private final OnCourseClickListener listener;

        CoursePreviewAdapter(List<Course> courses, OnCourseClickListener listener) {
            this.courses = courses;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            Context ctx = parent.getContext();
            LinearLayout card = new LinearLayout(ctx);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(40, 14, 40, 14);
            card.setBackgroundColor(0xFFF5F5F5);
            card.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));

            TextView line1 = new TextView(ctx);
            line1.setTextSize(15);
            line1.setTextColor(0xFF1565C0);
            line1.setTypeface(Typeface.DEFAULT_BOLD);
            card.addView(line1);

            TextView line2 = new TextView(ctx);
            line2.setTextSize(13);
            line2.setTextColor(0xFF666666);
            line2.setPadding(0, 4, 0, 0);
            card.addView(line2);

            return new ViewHolder(card, line1, line2);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Course c = courses.get(position);
            String timeRange = WeekUtils.getPeriodTimeText(c.getStartPeriod());
            if (c.getEndPeriod() != c.getStartPeriod()) {
                String endTime = WeekUtils.getPeriodTimeText(c.getEndPeriod());
                timeRange = timeRange.split("~")[0] + "~" + endTime.split("~")[1];
            }

            String line1 = WeekUtils.DAY_NAMES[c.getDayOfWeek()] + "  "
                    + c.getStartPeriod() + "-" + c.getEndPeriod() + "节"
                    + "  " + timeRange;
            holder.line1.setText(line1);

            StringBuilder sb = new StringBuilder(c.getCourseName());
            if (c.getTeacher() != null && !c.getTeacher().isEmpty())
                sb.append("  |  ").append(c.getTeacher());
            if (c.getLocation() != null && !c.getLocation().isEmpty())
                sb.append("  |  ").append(c.getLocation());
            sb.append("  |  第").append(c.getWeekRange()).append("周");
            holder.line2.setText(sb.toString());

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCourseClick(position);
            });
        }

        @Override
        public int getItemCount() { return courses.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView line1, line2;
            ViewHolder(View itemView, TextView l1, TextView l2) {
                super(itemView);
                line1 = l1; line2 = l2;
            }
        }
    }
}
