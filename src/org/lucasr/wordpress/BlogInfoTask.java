package org.lucasr.wordpress;

import java.util.HashMap;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

final class BlogInfoTask extends AsyncTask<Void, Void, HashMap<String, String>> {
	private final Context context;
    private final String username;
    private final String password;

    private final Activity configureActivity;

    private final View errorLayout;
    private final TextView errorText;
    private int errorMessage;

    private final View progressLayout;
    private final View cancelButton;
    private final View loginButton;

	int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	public BlogInfoTask(Context context, Activity configureActivity,
			int appWidgetId, String username, String password) {
		super();

    	this.context = context;
    	this.configureActivity = configureActivity;
		this.appWidgetId = appWidgetId;
		this.username = username;
		this.password = password;

		progressLayout = configureActivity.findViewById(R.id.progress_layout);
		errorLayout = configureActivity.findViewById(R.id.error_layout);

		errorText = (TextView) configureActivity.findViewById(R.id.error_text);

		cancelButton = configureActivity.findViewById(R.id.cancel_button);
		loginButton = configureActivity.findViewById(R.id.login_button);
	}

	private void saveBlogInfo(String apiKey, String blogId) {
		SharedPreferences.Editor prefs =
			context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0).edit();

		prefs.putString(WidgetConfigure.PREFS_KEY_APP_KEY + appWidgetId, apiKey);
		prefs.putString(WidgetConfigure.PREFS_KEY_BLOG_HOST + appWidgetId, blogId);

		prefs.commit();
	}

	private void updateAppWidget(String apiKey, String blogId) {
		// Fetch latest stats and update the widget accordingly
		new StatsTask(context, appWidgetId, apiKey, blogId).execute();
	}

	private void updateResultAndFinish() {
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

    	configureActivity.setResult(Activity.RESULT_OK, resultValue);
    	configureActivity.finish();
	}

	@Override
	protected void onPreExecute() {
		Log.v("WordpressBlogInfoTask", "onPreExecute()");

		cancelButton.setEnabled(false);
		loginButton.setEnabled(false);

		progressLayout.setVisibility(View.VISIBLE);
		errorLayout.setVisibility(View.GONE);
	}

	@Override
	protected HashMap<String, String> doInBackground(Void... arg0) {
		Log.v("WordpressBlogInfoTask", "doInBackground()");

		BlogInfo blogInfo = new BlogInfo(username, password);

		try {
			return blogInfo.getInfo();
		} catch (NoAuthException nae) {
			errorMessage = R.string.login_error_auth;
			return null;
		} catch (NetworkException ne) {
			errorMessage = R.string.login_error_network;
			return null;
		}
	}

	@Override
	protected void onPostExecute(HashMap<String, String> blogInfo) {
		Log.v("WordpressBlogInfoTask", "onPostExecute()");

		String apiKey = null;
		String blogId = null;

		if (blogInfo != null) {
			apiKey = blogInfo.get("apiKey");
			blogId = blogInfo.get("blogId");
		}

		if (apiKey == null || blogId == null) {
			progressLayout.setVisibility(View.GONE);
			errorLayout.setVisibility(View.VISIBLE);

			errorText.setText(errorMessage);

			cancelButton.setEnabled(true);
			loginButton.setEnabled(true);
		} else {
			saveBlogInfo(apiKey, blogId);
			updateAppWidget(apiKey, blogId);
			updateResultAndFinish();
		}


	}
}