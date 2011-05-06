package org.lucasr.wordpress;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class BarChart {
    private final int BAR_TYPE_TODAY = 0;
    private final int BAR_TYPE_REGULAR = 1;

    private final Vector<Integer> viewCounts;

    private final Resources resources;
    private final DisplayMetrics metrics;
    private final Bitmap bitmap;
    private final Canvas canvas;

    private float chartWidth;
    private float chartHeight;

    private float separatorWidth;
    private float separatorPosition;

    private float barWidth;
    private float barSpacing;
    private float maxBarHeight;
    private float barStrokeWidth;

    private float counterTextSize;
    private float dateTextSize;

    public BarChart(Context context, Vector<Integer> viewCounts) {
        this.viewCounts = viewCounts;

        resources = context.getResources();
        metrics = resources.getDisplayMetrics();

        initDimensions();

        bitmap = Bitmap.createBitmap((int) chartWidth,
                (int) chartHeight, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitmap);

        drawChart();
    }

    private void initDimensions() {
        chartWidth = getDimension(R.dimen.chart_width);
        chartHeight = getDimension(R.dimen.chart_height);

        separatorWidth = getDimension(R.dimen.bar_chart_separator_width);
        separatorPosition = getDimension(R.dimen.bar_chart_separator_position);

        barWidth = getDimension(R.dimen.bar_chart_bar_width);
        barSpacing = getDimension(R.dimen.bar_chart_bar_spacing);
        maxBarHeight = getDimension(R.dimen.bar_chart_max_bar_height);
        barStrokeWidth = getDimension(R.dimen.bar_chart_bar_stroke_width);

        counterTextSize = getDimension(R.dimen.bar_chart_counter_text_size);
        dateTextSize = getDimension(R.dimen.bar_chart_date_text_size);
    }

    private void drawChart() {
        drawSeparator();
        drawBars();
    }

    private void drawSeparator() {
        Paint linePaint = new Paint();

        linePaint.setColor(getColor(R.color.bar_chart_separator_color));
        linePaint.setStrokeWidth(separatorWidth);

        canvas.drawLine(0,
                        separatorPosition,
                        chartWidth,
                        separatorPosition,
                        linePaint);
    }

    private void drawBars() {
        int nCounts = viewCounts.size();
        int maxCount = 0;

        for (int i = 0; i < nCounts; i++) {
            int count = viewCounts.get(i);

            if (count > maxCount) {
                maxCount = count;
            }
        }

        float totalWidth = nCounts * barWidth +
                           (nCounts - 1) * barSpacing;

        float x = (chartWidth - totalWidth) / 2;

        for (int i = 0; i < nCounts; i++) {
            int count = viewCounts.get(i);

            float height = count * maxBarHeight / maxCount;

            int type = (i == nCounts - 1 ? BAR_TYPE_TODAY : BAR_TYPE_REGULAR);

            drawBar(x, height, type);
            drawCounter(x, height, count, type);
            drawDate(x, i);

            x += barWidth + barSpacing;
        }
    }

    private void drawBar(float x, float height, int type) {
        Paint barPaint = new Paint();

        barPaint.setStyle(Paint.Style.FILL);

        int barColor1;
        int barColor2;

        if (type == BAR_TYPE_TODAY) {
            barColor1 = getColor(R.color.bar_chart_bar_today_color1);
            barColor2 = getColor(R.color.bar_chart_bar_today_color2);
        } else {
            barColor1 = getColor(R.color.bar_chart_bar_color1);
            barColor2 = getColor(R.color.bar_chart_bar_color2);
        }

        LinearGradient gradient =
                new LinearGradient(x,
                                   separatorPosition - height,
                                   x,
                                   separatorPosition,
                                   barColor1,
                                   barColor2,
                                   TileMode.CLAMP);

        barPaint.setShader(gradient);

        canvas.drawRect(x,
                        separatorPosition - height,
                        x + barWidth,
                        separatorPosition,
                        barPaint);

        barPaint.setColor(getColor(R.color.bar_chart_bar_stroke_color));
        barPaint.setStrokeWidth(barStrokeWidth);
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setShader(null);

        canvas.drawRect(x,
                        separatorPosition - height,
                        x + barWidth,
                        separatorPosition,
                        barPaint);
    }

    private void drawDate(float x, int index) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d");
        int daysBefore = viewCounts.size() - index - 1;

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize(dateTextSize);
        datePaint.setColor(getColor(R.color.bar_chart_date_text_color));

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -daysBefore);

        String dateText;

        if (daysBefore > 0) {
            dateText = dateFormat.format(calendar.getTime());
        } else {
            dateText = getString(R.string.today);
            datePaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        canvas.drawText(dateText,
                        0, dateText.length(),
                        x + barWidth / 2,
                        separatorPosition +
                        fromDipToPixels(10),
                        datePaint);
    }

    private void drawCounter(float x, float height, int count, int type) {
        Paint counterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        counterPaint.setTextAlign(Paint.Align.CENTER);
        counterPaint.setTextSize(counterTextSize);
        counterPaint.setColor(getColor(R.color.bar_chart_counter_text_color));

        if (type == BAR_TYPE_TODAY) {
            counterPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        String counterText = Integer.toString(count);

        canvas.drawText(counterText,
                        0, counterText.length(),
                        x + barWidth / 2,
                        separatorPosition -
                        height - fromDipToPixels(3),
                        counterPaint);
    }

    private float getDimension(int id) {
        return resources.getDimension(id);
    }

    private int getColor(int id) {
        return resources.getColor(id);
    }

    private String getString(int id) {
        return resources.getString(id);
    }

    private float fromDipToPixels(float dip) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip,
                metrics);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }
}