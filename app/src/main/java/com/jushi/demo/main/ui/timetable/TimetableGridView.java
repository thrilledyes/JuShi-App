package com.jushi.demo.main.ui.timetable;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.jushi.demo.main.data.entity.Course;

import java.util.ArrayList;
import java.util.List;

public class TimetableGridView extends View {

    private static final float LABEL_WIDTH_DP = 70;
    private static final float HEADER_HEIGHT_DP = 48;
    private static final float CELL_MIN_WIDTH_DP = 96;
    private static final float ROW_HEIGHT_DP = 82;

    private final TextPaint headerPaint;
    private final TextPaint labelPaint;
    private final TextPaint courseNamePaint;
    private final TextPaint courseDetailPaint;
    private final Paint gridPaint;
    private final Paint headerBgPaint;
    private final Paint labelBgPaint;
    private final Paint labelBgAltPaint;
    private final Paint surfacePaint;

    private final float density;
    private final int displayWidthPx;
    private float labelWidth, headerHeight, cellWidth, rowHeight;
    private int totalWidth, totalHeight;

    private List<Course> courses = new ArrayList<>();
    private int currentWeek = 1;
    private long semesterStart;
    private String[] dateLabels = new String[7];
    private final Course[][] grid;

    // Track font metrics for proper clipping
    private final Paint.FontMetrics fmCourseName;
    private final Paint.FontMetrics fmCourseDetail;

    public TimetableGridView(Context context) {
        this(context, null);
    }

    public TimetableGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        displayWidthPx = context.getResources().getDisplayMetrics().widthPixels;

        surfacePaint = new Paint();
        surfacePaint.setColor(Color.WHITE);

        headerBgPaint = new Paint();
        headerBgPaint.setColor(0xFF1565C0);

        labelBgPaint = new Paint();
        labelBgPaint.setColor(0xFFF5F5F5);

        labelBgAltPaint = new Paint();
        labelBgAltPaint.setColor(0xFFEEEEEE);

        headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.WHITE);
        headerPaint.setTextSize(dpToPx(11));
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setTypeface(Typeface.DEFAULT_BOLD);

        labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF666666);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        courseNamePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        courseNamePaint.setColor(Color.WHITE);
        courseNamePaint.setTextSize(dpToPx(12));
        courseNamePaint.setTypeface(Typeface.DEFAULT_BOLD);
        fmCourseName = courseNamePaint.getFontMetrics();

        courseDetailPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        courseDetailPaint.setColor(0xEEFFFFFF);
        courseDetailPaint.setTextSize(dpToPx(9));
        fmCourseDetail = courseDetailPaint.getFontMetrics();

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);

        grid = new Course[WeekUtils.MAX_PERIODS][7];
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses != null ? courses : new ArrayList<>();
        buildGrid();
        requestLayout();
    }

    public void setCurrentWeek(int week, long semStart) {
        this.currentWeek = week;
        this.semesterStart = semStart;
        this.dateLabels = WeekUtils.getDateLabels(week, semStart);
        buildGrid();
        invalidate();
    }

    private void buildGrid() {
        for (int p = 0; p < WeekUtils.MAX_PERIODS; p++) {
            for (int d = 0; d < 7; d++) {
                grid[p][d] = null;
            }
        }
        for (Course c : courses) {
            if (WeekUtils.isActiveInWeek(c, currentWeek)) {
                int startP = c.getStartPeriod() - 1;
                int endP = c.getEndPeriod() - 1;
                int d = c.getDayOfWeek() - 1;
                if (startP < 0 || d < 0 || d >= 7) continue;
                endP = Math.min(endP, WeekUtils.MAX_PERIODS - 1);
                for (int p = startP; p <= endP; p++) {
                    grid[p][d] = c;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        labelWidth = dpToPx(LABEL_WIDTH_DP);
        headerHeight = dpToPx(HEADER_HEIGHT_DP);
        rowHeight = dpToPx(ROW_HEIGHT_DP);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.UNSPECIFIED || widthSize == 0) {
            widthSize = displayWidthPx;
        }

        float minCell = dpToPx(CELL_MIN_WIDTH_DP);
        float availWidth = widthSize - labelWidth;
        cellWidth = Math.max(minCell, availWidth / 7f);

        totalWidth = (int) (labelWidth + 7 * cellWidth);
        totalHeight = (int) (headerHeight + WeekUtils.MAX_PERIODS * rowHeight);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if ((heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST)
                && heightSize > totalHeight) {
            totalHeight = heightSize;
        }

        setMeasuredDimension(totalWidth, totalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, totalWidth, totalHeight, surfacePaint);

        // Corner cell
        canvas.drawRect(0, 0, labelWidth, headerHeight, headerBgPaint);

        // Header row: day names + dates
        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        for (int d = 0; d < 7; d++) {
            float left = labelWidth + d * cellWidth;
            canvas.drawRect(left, 0, left + cellWidth, headerHeight, headerBgPaint);

            String label = dayNames[d];
            if (dateLabels[d] != null) {
                label = dayNames[d] + "\n" + dateLabels[d];
            }
            float cx = left + cellWidth / 2;
            float cy = headerHeight / 2 - dpToPx(3);
            drawTextCentered(canvas, label, cx, cy, headerPaint);
        }

        // Period label column
        for (int p = 0; p < WeekUtils.MAX_PERIODS; p++) {
            float top = headerHeight + p * rowHeight;
            float bottom = top + rowHeight;

            Paint bg = p % 2 == 0 ? labelBgPaint : labelBgAltPaint;
            canvas.drawRect(0, top, labelWidth, bottom, bg);

            int period = p + 1;
            String periodLabel = period + "\n" + WeekUtils.getPeriodTimeText(period);
            float cx = labelWidth / 2;
            float cy = top + rowHeight / 2 - dpToPx(2);
            drawTextCentered(canvas, periodLabel, cx, cy, labelPaint);
        }

        // Grid lines
        for (int d = 0; d <= 7; d++) {
            float x = labelWidth + d * cellWidth;
            canvas.drawLine(x, 0, x, totalHeight, gridPaint);
        }
        for (int p = 0; p <= WeekUtils.MAX_PERIODS; p++) {
            float y = headerHeight + p * rowHeight;
            canvas.drawLine(0, y, totalWidth, y, gridPaint);
        }

        // Course cells — only draw at course start period
        for (int p = 0; p < WeekUtils.MAX_PERIODS; p++) {
            for (int d = 0; d < 7; d++) {
                Course c = grid[p][d];
                if (c != null && c.getStartPeriod() - 1 == p) {
                    drawCourseCell(canvas, c, d);
                }
            }
        }
    }

    private void drawCourseCell(Canvas canvas, Course course, int dayIdx) {
        int startP = course.getStartPeriod() - 1;
        int endP = Math.min(course.getEndPeriod() - 1, WeekUtils.MAX_PERIODS - 1);
        int span = endP - startP + 1;

        float left = labelWidth + dayIdx * cellWidth + dpToPx(2);
        float top = headerHeight + startP * rowHeight + dpToPx(2);
        float right = labelWidth + (dayIdx + 1) * cellWidth - dpToPx(2);
        float bottom = headerHeight + (startP + span) * rowHeight - dpToPx(2);

        RectF rect = new RectF(left, top, right, bottom);
        int bgColor = (course.getColor() & 0x00FFFFFF) | 0xCC000000;
        Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(bgColor);
        canvas.drawRoundRect(rect, dpToPx(4), dpToPx(4), cellPaint);

        float pad = dpToPx(5);
        float textLeft = left + pad;
        float maxWidth = right - textLeft - pad;
        float lineH = (fmCourseName.descent - fmCourseName.ascent) + dpToPx(1);
        float detailLineH = (fmCourseDetail.descent - fmCourseDetail.ascent) + dpToPx(1);

        // Start drawing from top, leaving padding
        float curY = top + dpToPx(5) - fmCourseName.ascent;
        float maxY = bottom - dpToPx(3);

        // Course name
        String name = course.getCourseName();
        if (name != null && !name.isEmpty() && curY < maxY) {
            canvas.save();
            canvas.clipRect(textLeft, top, right - pad, maxY);
            canvas.drawText(name, textLeft, curY, courseNamePaint);
            canvas.restore();
            curY += lineH + dpToPx(1);
        }

        // Teacher
        if (course.getTeacher() != null && !course.getTeacher().isEmpty() && curY < maxY) {
            canvas.save();
            canvas.clipRect(textLeft, top, right - pad, maxY);
            curY += -fmCourseDetail.ascent;
            canvas.drawText(course.getTeacher(), textLeft, curY, courseDetailPaint);
            canvas.restore();
            curY += detailLineH;
        }

        // Location
        if (course.getLocation() != null && !course.getLocation().isEmpty() && curY < maxY) {
            canvas.save();
            canvas.clipRect(textLeft, top, right - pad, maxY);
            curY += -fmCourseDetail.ascent;
            canvas.drawText(course.getLocation(), textLeft, curY, courseDetailPaint);
            canvas.restore();
        }
    }

    private void drawTextCentered(Canvas canvas, String text, float cx, float cy, TextPaint paint) {
        String[] lines = text.split("\n");
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineHeight = (fm.descent - fm.ascent) + dpToPx(2);
        float totalHeight = lines.length * lineHeight;
        float startY = cy - totalHeight / 2 - fm.ascent;

        for (int i = 0; i < lines.length; i++) {
            float w = paint.measureText(lines[i]);
            canvas.drawText(lines[i], cx - w / 2, startY + i * lineHeight, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        float x = event.getX();
        float y = event.getY();

        if (x < labelWidth || y < headerHeight) return true;

        int dayIdx = (int) ((x - labelWidth) / cellWidth);
        int periodIdx = (int) ((y - headerHeight) / rowHeight);

        if (dayIdx < 0 || dayIdx >= 7 || periodIdx < 0 || periodIdx >= WeekUtils.MAX_PERIODS)
            return true;

        Course c = grid[periodIdx][dayIdx];
        if (c != null) {
            showCourseDialog(c);
        }

        return true;
    }

    private void showCourseDialog(Course c) {
        StringBuilder msg = new StringBuilder();
        msg.append("课程: ").append(c.getCourseName()).append("\n");
        if (c.getTeacher() != null && !c.getTeacher().isEmpty()) {
            msg.append("教师: ").append(c.getTeacher()).append("\n");
        }
        if (c.getLocation() != null && !c.getLocation().isEmpty()) {
            msg.append("地点: ").append(c.getLocation()).append("\n");
        }
        msg.append("时间: ").append(WeekUtils.DAY_NAMES[c.getDayOfWeek()])
                .append(" 第").append(c.getStartPeriod());
        if (c.getEndPeriod() != c.getStartPeriod()) {
            msg.append("-").append(c.getEndPeriod());
        }
        msg.append("节 ").append(WeekUtils.getPeriodTimeText(c.getStartPeriod())).append("\n");
        msg.append("周次: 第").append(c.getWeekRange()).append("周");

        new AlertDialog.Builder(getContext())
                .setTitle(c.getCourseName())
                .setMessage(msg.toString())
                .setPositiveButton("确定", null)
                .show();
    }

    private float dpToPx(float dp) {
        return dp * density;
    }
}
