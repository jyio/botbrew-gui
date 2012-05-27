package com.botbrew.basil;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapRebaseActivity extends SherlockFragmentActivity {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootstrap_rebase_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(true);
		final String file = (new File(getIntent().getStringExtra("file"))).getAbsolutePath();
		final boolean loop = getIntent().getBooleanExtra("loop",false);
		((TextView)findViewById(R.id.location)).setText(file);
		((Button)findViewById(R.id.reinstall)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BootstrapRebaseActivity.this,BootstrapDownloadActivity.class).putExtra("file",file).putExtra("loop",loop));
				finish();
			}
		});
		((Button)findViewById(R.id.setdefault)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(BootstrapRebaseActivity.this);
				final SharedPreferences.Editor editor = pref.edit();
				editor.putString("var_root",file);
				editor.commit();
				startActivity(IntentType.APP_RESTART.intent(BootstrapRebaseActivity.this,Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				finish();
			}
		});
	}
}