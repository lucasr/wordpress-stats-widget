package org.lucasr.wordpress;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        Log.v("WordpressWidget", "onUpdate");

        SharedPreferences prefs =
            context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0);

        for (int i = 0; i < appWidgetIds.length; i++) {
            Log.v("WordpressWidget", "starting update task for "
                    + appWidgetIds[i]);

            String apiKey = prefs.getString(WidgetConfigure.PREFS_KEY_APP_KEY
                    + appWidgetIds[i], null);
            String blogId = prefs.getString(WidgetConfigure.PREFS_KEY_BLOG_ID
                    + appWidgetIds[i], null);
            String blogHost = prefs.getString(WidgetConfigure.PREFS_KEY_BLOG_HOST
                    + appWidgetIds[i], null);

            // Run task to get stats from wp server
            new StatsTask(context, appWidgetIds[i], apiKey, blogId, blogHost).execute();
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.v("WordpressWidget", "onDeleted");

        SharedPreferences.Editor prefs =
            context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0).edit();

        for (int i = 0; i < appWidgetIds.length; i++) {
            Log.v("WordpressWidget", "removing prefs for " + appWidgetIds[i]);

            prefs.remove(WidgetConfigure.PREFS_KEY_APP_KEY + appWidgetIds[i]);
            prefs.remove(WidgetConfigure.PREFS_KEY_BLOG_ID + appWidgetIds[i]);
            prefs.remove(WidgetConfigure.PREFS_KEY_BLOG_HOST + appWidgetIds[i]);
            prefs.remove(WidgetConfigure.PREFS_KEY_VIEW_COUNTS + appWidgetIds[i]);
            prefs.remove(WidgetConfigure.PREFS_KEY_LAST_UPDATE + appWidgetIds[i]);
        }

        prefs.commit();
    }
}