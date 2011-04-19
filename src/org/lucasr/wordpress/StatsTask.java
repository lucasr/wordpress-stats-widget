package org.lucasr.wordpress;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

final class StatsTask extends AsyncTask<Void, Void, Integer> {
	private final Context context;
	private final String apiKey;
	private final String blogId;

	int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	public StatsTask(Context context, int appWidgetId, String apiKey,
			String blogId) {
		super();

		this.context = context;
		this.appWidgetId = appWidgetId;
		this.apiKey = apiKey;
		this.blogId = blogId;
	}

	@Override
	protected Integer doInBackground(Void... arg0) {
		Log.v("WordpressUpdateTask", "doInBackground()");

		Stats stats = new Stats(apiKey, blogId);

		try {
			return stats.getViewCount();
		} catch (NetworkException ne) {
			// Return -2 meaning that it's a connection error
			return -2;
		}
	}

	private RemoteViews createRemoveViews(Integer viewCount) {
		RemoteViews rv =
			new RemoteViews(context.getPackageName(), R.layout.wordpress_widget);

		if (viewCount == -2) {
			// Network-related error
			rv.setViewVisibility(R.id.counter_text, View.GONE);
			rv.setViewVisibility(R.id.description_text, View.VISIBLE);
			rv.setInt(R.id.description_text, "setText", R.string.network_error);
		} else if (viewCount == -1) {
			// General errors fetching the data
			rv.setViewVisibility(R.id.counter_text, View.GONE);
			rv.setViewVisibility(R.id.description_text, View.VISIBLE);
			rv.setInt(R.id.description_text, "setText", R.string.counter_error);
		} else if (viewCount == 0) {
			rv.setViewVisibility(R.id.counter_text, View.GONE);
			rv.setViewVisibility(R.id.description_text, View.VISIBLE);
			rv.setInt(R.id.description_text, "setText", R.string.counter_no_views);
		} else if (viewCount > 0) {
			rv.setViewVisibility(R.id.counter_text, View.VISIBLE);
			rv.setCharSequence(R.id.counter_text, "setText", Integer.toString(viewCount));
			rv.setViewVisibility(R.id.description_text, View.VISIBLE);
			rv.setInt(R.id.description_text, "setText", R.string.counter_description);
		}

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

	        rv.setOnClickPendingIntent(R.id.wp_logo, pendingIntent);
		} catch (Exception e) {
			Log.v("WordpressUpdateTask", "wordpress app not available");
		}

        return rv;
	}

	@Override
	protected void onPostExecute(Integer viewCount) {
		Log.v("WordpressUpdateTask", "onPostExecute()");

		RemoteViews rv = createRemoveViews(viewCount);
		AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rv);
	}
}