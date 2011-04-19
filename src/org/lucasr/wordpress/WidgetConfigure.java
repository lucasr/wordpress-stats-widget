package org.lucasr.wordpress;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class WidgetConfigure extends Activity {
	public static final String PREFS_NAME = "org.lucasr.wordpress.WidgetProvider";
	public static final String PREFS_KEY_APP_KEY = "app_key_";
	public static final String PREFS_KEY_BLOG_HOST = "blog_host_";

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private Button loginButton;
	private Button cancelButton;
	private EditText usernameEntry;
	private EditText passwordEntry;

	public WidgetConfigure() {
        super();
    }

	private void startLogin() {
    	final Context context = WidgetConfigure.this;

    	// Hide any error messages here

    	String username = usernameEntry.getText().toString();
    	String password = passwordEntry.getText().toString();

    	new BlogInfoTask(context, this, appWidgetId, username, password).execute();
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
            public void onClick(View v) {
                finish();
            }
        });

        loginButton = (Button) findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
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
}