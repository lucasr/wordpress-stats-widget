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

    public BarChart(Context context, Vector<Integer> viewCounts) {
        this.viewCounts = viewCounts;

        resources = context.getResources();
        metrics = resources.getDisplayMetrics();

        bitmap = Bitmap.createBitmap((int) getDimension(R.dimen.chart_width),
                (int) getDimension(R.dimen.chart_height), Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitmap);

        drawChart();
    }

    private void drawChart() {
        drawSeparator();
        drawBars();
    }

    private void drawSeparator() {
        Paint linePaint = new Paint();

        linePaint.setColor(getColor(R.color.bar_chart_separator_color));
        linePaint.setStrokeWidth(getDimension(R.dimen.bar_chart_separator_width));

        canvas.drawLine(0,
                        getDimension(R.dimen.bar_chart_separator_position),
                        getDimension(R.dimen.chart_width),
                        getDimension(R.dimen.bar_chart_separator_position),
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

        float totalWidth = nCounts * getDimension(R.dimen.bar_chart_bar_width) +
                           (nCounts - 1) * getDimension(R.dimen.bar_chart_bar_spacing);

        float x = (getDimension(R.dimen.chart_width) - totalWidth) / 2;

        for (int i = 0; i < nCounts; i++) {
            int count = viewCounts.get(i);

            float height = count * getDimension(R.dimen.bar_chart_max_bar_height) / maxCount;

            int type = (i == nCounts - 1 ? BAR_TYPE_TODAY : BAR_TYPE_REGULAR);

            drawBar(x, height, type);
            drawCounter(x, height, count, type);
            drawDate(x, i);

            x += getDimension(R.dimen.bar_chart_bar_width) +
                 getDimension(R.dimen.bar_chart_bar_spacing);
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
                                   getDimension(R.dimen.bar_chart_separator_position) - height,
                                   x,
                                   getDimension(R.dimen.bar_chart_separator_position),
                                   barColor1,
                                   barColor2,
                                   TileMode.CLAMP);

        barPaint.setShader(gradient);

        canvas.drawRect(x,
                        getDimension(R.dimen.bar_chart_separator_position) - height,
                        x + getDimension(R.dimen.bar_chart_bar_width),
                        getDimension(R.dimen.bar_chart_separator_position),
                        barPaint);

        barPaint.setColor(getColor(R.color.bar_chart_bar_stroke_color));
        barPaint.setStrokeWidth(getDimension(R.dimen.bar_chart_bar_stroke_width));
        barPaint.setStyle(Paint.Style.STROKE);
        barPaint.setShader(null);

        canvas.drawRect(x,
                        getDimension(R.dimen.bar_chart_separator_position) - height,
                        x + getDimension(R.dimen.bar_chart_bar_width),
                        getDimension(R.dimen.bar_chart_separator_position),
                        barPaint);
    }

    private void drawDate(float x, int index) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d");
        int daysBefore = viewCounts.size() - index - 1;

        Paint datePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        datePaint.setTextAlign(Paint.Align.CENTER);
        datePaint.setTextSize(getDimension(R.dimen.bar_chart_date_text_size));
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
                        x + getDimension(R.dimen.bar_chart_bar_width) / 2,
                        getDimension(R.dimen.bar_chart_separator_position) +
                        fromDipToPixels(10),
                        datePaint);
    }

    private void drawCounter(float x, float height, int count, int type) {
        Paint counterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        counterPaint.setTextAlign(Paint.Align.CENTER);
        counterPaint.setTextSize(getDimension(R.dimen.bar_chart_counter_text_size));
        counterPaint.setColor(getColor(R.color.bar_chart_counter_text_color));

        if (type == BAR_TYPE_TODAY) {
            counterPaint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        String counterText = Integer.toString(count);

        canvas.drawText(counterText,
                        0, counterText.length(),
                        x + getDimension(R.dimen.bar_chart_bar_width) / 2,
                        getDimension(R.dimen.bar_chart_separator_position) -
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