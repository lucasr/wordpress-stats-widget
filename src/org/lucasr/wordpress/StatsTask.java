package org.lucasr.wordpress;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

final class StatsTask extends AsyncTask<Void, Void, Vector<Integer>> {
    private final int NETWORK_ERROR = 0;
    private final int GENERIC_ERROR = 1;

    private final Context context;
    private final String apiKey;
    private final String blogId;
    private final String blogHost;

    int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public StatsTask(Context context, int appWidgetId, String apiKey,
            String blogId, String blogHost) {
        super();

        this.context = context;
        this.appWidgetId = appWidgetId;
        this.apiKey = apiKey;
        this.blogId = blogId;
        this.blogHost = blogHost;
    }

    @Override
    protected void onPreExecute() {
        if (apiKey == null || blogId == null) {
            this.cancel(true);
        }
    }

    @Override
    protected Vector<Integer> doInBackground(Void... arg0) {
        Log.v("WordpressUpdateTask", "doInBackground()");

        if (this.isCancelled()) {
            Log.v("WordpressUpdateTask", "cancelled, not running");
            return null;
        }

        Stats stats = new Stats(apiKey, blogId);

        try {
            return stats.getViewCounts();
        } catch (NetworkException ne) {
            return null;
        }
    }

    private void maybeSetAppIntent(RemoteViews rv) {
        try {
            /* This is not the most flexible way of launch a third party
             * app because we make explicit references to the foreign class
             * hierarchy and packages. Ideally, the Wordpress app would export
             * custom actions/categories for the intents
             */
            Context wpContext =
                context.createPackageContext("org.wordpress.android",
                                             Context.CONTEXT_IGNORE_SECURITY |
                                             Context.CONTEXT_INCLUDE_CODE);

            Class<?> wpClass =
                wpContext.getClassLoader().loadClass("org.wordpress.android.splashScreen");

            Intent wpIntent = new Intent(wpContext, wpClass);

            PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, wpIntent, 0);

            rv.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);
        } catch (Exception e) {
            Log.v("WordpressUpdateTask", "wordpress app not available");
        }
    }

    private void updateUrlText(RemoteViews rv) {
        rv.setCharSequence(R.id.url_text, "setText", Uri.parse(blogHost).getHost());
    }

    private void updateLastUpdateText(RemoteViews rv) {
        SharedPreferences prefs =
            context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0);

        String lastUpdateTime = prefs.getString(WidgetConfigure.PREFS_KEY_LAST_UPDATE
                + appWidgetId, null);

        if (lastUpdateTime != null) {
            String lastUpdateStr =
                context.getResources().getString(R.string.last_update);

            String lastUpdate = String.format("%s %s", lastUpdateStr, lastUpdateTime);
            rv.setCharSequence(R.id.last_update_text, "setText", lastUpdate);
        } else {
            rv.setViewVisibility(R.id.last_update_text, View.GONE);
        }
    }

    private RemoteViews createChartRemoteViews(Vector<Integer> viewCounts) {
        Log.v("WordpressUpdateTask", "createChartRemoteViews()");

        RemoteViews rv =
            new RemoteViews(context.getPackageName(), R.layout.wordpress_widget);

        BarChart barChart = new BarChart(context, viewCounts);
        rv.setBitmap(R.id.bar_chart, "setImageBitmap", barChart.getBitmap());

        updateUrlText(rv);
        updateLastUpdateText(rv);
        maybeSetAppIntent(rv);

        return rv;
    }

    private RemoteViews createErrorRemoteViews(int errorType) {
        Log.v("WordpressUpdateTask", "createErrorRemoteViews()");

        RemoteViews rv =
            new RemoteViews(context.getPackageName(), R.layout.wordpress_error);

        if (errorType == NETWORK_ERROR) {
            rv.setInt(R.id.title_text, "setText", R.string.network_error_title);
        } else {
            rv.setInt(R.id.title_text, "setText", R.string.generic_error_title);
        }

        updateUrlText(rv);
        updateLastUpdateText(rv);

        return rv;
    }

    private void saveUpdateInfo(Vector<Integer> viewCounts) {
        Log.v("WordpressUpdateTask", "saveViewCounts()");

        SharedPreferences.Editor prefs =
            context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0).edit();

        StringBuffer viewCountsBuf = new StringBuffer();

        for (int i = 0; i < viewCounts.size(); i++) {
            if (i > 0) {
                viewCountsBuf.append(',');
            }

            viewCountsBuf.append(viewCounts.get(i));
        }

        prefs.putString(WidgetConfigure.PREFS_KEY_VIEW_COUNTS + appWidgetId,
                        viewCountsBuf.toString());

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, H:mm");
        Date now = new Date();

        prefs.putString(WidgetConfigure.PREFS_KEY_LAST_UPDATE + appWidgetId,
                dateFormat.format(now));

        prefs.commit();
    }

    private Vector<Integer> getSavedViewCounts() {
        SharedPreferences prefs =
            context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0);

        String viewCountsStr = prefs.getString(WidgetConfigure.PREFS_KEY_VIEW_COUNTS
                + appWidgetId, null);

        Log.v("WordpressUpdateTask", "getSavedViewCounts() " + viewCountsStr);

        if (viewCountsStr == null) {
            return null;
        }

        Vector<Integer> viewCounts = new Vector<Integer>();
        String[] viewCountsArray = viewCountsStr.split(",");

        for (int i = 0; i < viewCountsArray.length; i++) {
            viewCounts.add(Integer.valueOf(viewCountsArray[i]));
        }

        return viewCounts;
    }

    private RemoteViews createRemoteViews() {
        Log.v("WordpressUpdateTask", "createRemoteViews()");

        Vector<Integer> viewCounts = getSavedViewCounts();

        if (viewCounts == null) {
            return createErrorRemoteViews(NETWORK_ERROR);
        } else if (viewCounts.size() > 0) {
            return createChartRemoteViews(viewCounts);
        } else {
            return createErrorRemoteViews(GENERIC_ERROR);
        }
    }

    @Override
    protected void onPostExecute(Vector<Integer> viewCounts) {
        Log.v("WordpressUpdateTask", "onPostExecute()");

        if (viewCounts != null && viewCounts.size() > 0) {
            saveUpdateInfo(viewCounts);
        }

        RemoteViews rv = createRemoteViews();
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rv);
    }
}