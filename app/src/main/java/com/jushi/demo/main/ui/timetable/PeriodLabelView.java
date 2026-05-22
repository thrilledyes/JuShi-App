package com.jushi.demo.main.ui.timetable;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

public class PeriodLabelView extends View {

    private static final float LABEL_WIDTH_DP = 70;
    private static final float HEADER_HEIGHT_DP = 48;
    private static final float ROW_HEIGHT_DP = 82;
    private static final float ROW_MIN_HEIGHT_DP = 48;

    private final TextPaint labelPaint;
    private final Paint gridPaint;
    private final Paint headerBgPaint;
    private final Paint labelBgPaint;
    private final Paint labelBgAltPaint;
    private final Paint surfacePaint;

    private final float density;
    private float labelWidth, headerHeight, rowHeight;
    private int totalWidth, totalHeight;

    public PeriodLabelView(Context context) {
        this(context, null);
    }

    public PeriodLabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;

        surfacePaint = new Paint();
        surfacePaint.setColor(Color.WHITE);

        headerBgPaint = new Paint();
        headerBgPaint.setColor(0xFF1565C0);

        labelBgPaint = new Paint();
        labelBgPaint.setColor(0xFFF5F5F5);

        labelBgAltPaint = new Paint();
        labelBgAltPaint.setColor(0xFFEEEEEE);

        labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF666666);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(0xFFE0E0E0);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        labelWidth = dpToPx(LABEL_WIDTH_DP);
        headerHeight = dpToPx(HEADER_HEIGHT_DP);

        totalWidth = (int) labelWidth;

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        float minRow = dpToPx(ROW_MIN_HEIGHT_DP);
        float defaultRow = dpToPx(ROW_HEIGHT_DP);

        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            float calcRow = (heightSize - headerHeight) / (float) WeekUtils.MAX_PERIODS;
            rowHeight = Math.max(minRow, Math.max(defaultRow, calcRow));
            totalHeight = (int) (headerHeight + WeekUtils.MAX_PERIODS * rowHeight);
        } else {
            rowHeight = defaultRow;
            totalHeight = (int) (headerHeight + WeekUtils.MAX_PERIODS * rowHeight);
        }

        setMeasuredDimension(totalWidth, totalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(0, 0, totalWidth, totalHeight, surfacePaint);

        // Corner cell — matches header color
        canvas.drawRect(0, 0, labelWidth, headerHeight, headerBgPaint);

        // Period labels
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

        // Right border
        canvas.drawLine(labelWidth, 0, labelWidth, totalHeight, gridPaint);

        // Horizontal grid lines
        for (int p = 0; p <= WeekUtils.MAX_PERIODS; p++) {
            float y = headerHeight + p * rowHeight;
            canvas.drawLine(0, y, totalWidth, y, gridPaint);
        }
    }

    private void drawTextCentered(Canvas canvas, String text, float cx, float cy, TextPaint paint) {
        String[] lines = text.split("\n");
        Paint.FontMetrics fm = paint.getFontMetrics();
        float lineHeight = (fm.descent - fm.ascent) + dpToPx(2);
        float totalH = lines.length * lineHeight;
        float startY = cy - totalH / 2 - fm.ascent;

        canvas.save();
        canvas.clipRect(dpToPx(2), cy - totalH / 2, labelWidth - dpToPx(2), cy + totalH / 2);

        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], cx, startY + i * lineHeight, paint);
        }

        canvas.restore();
    }

    private float dpToPx(float dp) {
        return dp * density;
    }
}
