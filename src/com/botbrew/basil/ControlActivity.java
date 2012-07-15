package com.botbrew.basil;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class ControlActivity extends SherlockPreferenceActivity implements
SharedPreferences.OnSharedPreferenceChangeListener {
	private boolean mChanged = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.control_activity);
		addPreferencesFromResource(R.xml.preference);
		((Button)findViewById(R.id.control_bootstrap)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity((new Intent(ControlActivity.this,BootstrapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
				finish();
			}
		});
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.control,menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				startActivity((new Intent(this,Main.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_restart:
				((BotBrewApp)getApplicationContext()).unmount();
				startActivity(IntentType.APP_RESTART.intent(this,Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_exit:
				((BotBrewApp)getApplicationContext()).unmount();
				startActivity(IntentType.APP_EXIT.intent(this,Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onPause() {
		if(mChanged) {
			mChanged = false;
			DebianPackageManager.pm_writeconf(this);
		}
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}
	@Override
	public void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		mChanged = true;
	}
}