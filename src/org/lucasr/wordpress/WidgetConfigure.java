package org.lucasr.wordpress;

import java.util.HashMap;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WidgetConfigure extends Activity {
    public static final String PREFS_NAME = "org.lucasr.wordpress.WidgetProvider";
    public static final String PREFS_KEY_APP_KEY = "app_key_";
    public static final String PREFS_KEY_BLOG_ID = "blog_id_";
    public static final String PREFS_KEY_BLOG_HOST = "blog_host_";
    public static final String PREFS_KEY_VIEW_COUNTS = "view_counts_";
    public static final String PREFS_KEY_LAST_UPDATE = "last_update_";

    private final Context context;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    private Button loginButton;
    private Button cancelButton;
    private EditText usernameEntry;
    private EditText passwordEntry;

    public WidgetConfigure() {
        super();

        context = WidgetConfigure.this;
    }

    private void startLogin() {
        new BlogInfoTask().execute();
    }

    private void updateLoginButtonFromEntries() {
        boolean enableLoginButton = usernameEntry.getText().length() != 0 &&
                                    passwordEntry.getText().length() != 0;

        loginButton.setEnabled(enableLoginButton);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Default to canceled, just in case
        setResult(RESULT_CANCELED);

        // Set initial layout for the activity
        setContentView(R.layout.wordpress_configure);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                                        AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If we can't find an app widget id, just finish straight away
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLogin();
            }
        });

        usernameEntry = (EditText) findViewById(R.id.username_entry);
        passwordEntry = (EditText) findViewById(R.id.password_entry);

        TextWatcher entryWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonFromEntries();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        usernameEntry.addTextChangedListener(entryWatcher);
        passwordEntry.addTextChangedListener(entryWatcher);
    }

    final class BlogInfoTask extends AsyncTask<Void, Void, HashMap<String, String>> {
        private final String username;
        private final String password;

        private final View errorLayout;
        private final TextView errorText;
        private int errorMessage;

        private final View progressLayout;
        private final View cancelButton;
        private final View loginButton;

        public BlogInfoTask() {
            super();

            username = usernameEntry.getText().toString();
            password = passwordEntry.getText().toString();

            progressLayout = findViewById(R.id.progress_layout);
            errorLayout = findViewById(R.id.error_layout);

            errorText = (TextView) findViewById(R.id.error_text);

            cancelButton = findViewById(R.id.cancel_button);
            loginButton = findViewById(R.id.login_button);
        }

        private void saveBlogInfo(String apiKey, String blogId, String blogHost) {
            SharedPreferences.Editor prefs =
                context.getSharedPreferences(WidgetConfigure.PREFS_NAME, 0).edit();

            prefs.putString(WidgetConfigure.PREFS_KEY_APP_KEY + appWidgetId, apiKey);
            prefs.putString(WidgetConfigure.PREFS_KEY_BLOG_ID + appWidgetId, blogId);
            prefs.putString(WidgetConfigure.PREFS_KEY_BLOG_HOST + appWidgetId, blogHost);

            prefs.commit();
        }

        private void updateAppWidget(String apiKey, String blogId, String blogHost) {
            // Fetch latest stats and update the widget accordingly
            new StatsTask(context, appWidgetId, apiKey, blogId, blogHost).execute();
        }

        private void updateResultAndFinish() {
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

            setResult(Activity.RESULT_OK, resultValue);
            finish();
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
            String blogHost = null;

            if (blogInfo != null) {
                apiKey = blogInfo.get("apiKey");
                blogId = blogInfo.get("blogId");
                blogHost = blogInfo.get("blogHost");
            }

            if (apiKey == null || blogId == null || blogHost == null) {
                progressLayout.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);

                errorText.setText(errorMessage);

                cancelButton.setEnabled(true);
                loginButton.setEnabled(true);
            } else {
                saveBlogInfo(apiKey, blogId, blogHost);
                updateAppWidget(apiKey, blogId, blogHost);
                updateResultAndFinish();
            }
        }
    }
}