package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class PackageStatusActivity extends SherlockFragmentActivity {
	private static final String TAG = "BBStatus";
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_status_activity);
		final ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		final String pkg = getIntent().getStringExtra("package").toLowerCase();
		if(pkg == null) {
			finish();
			return;
		}
		(new AsyncTask<Void,Integer,Boolean>() {
			final StringBuilder sb = new StringBuilder();
			@Override
			protected Boolean doInBackground(final Void... params) {
				mLocked = true;
				String line;
				final DebianPackageManager dpm = new DebianPackageManager(BotBrewApp.root.getAbsolutePath());
				try {
					Process p = dpm.exec(false,dpm.aptcache_show(pkg));
					p.getOutputStream().close();
					BufferedReader p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while((line = p_stdout.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}
					BotBrewApp.sinkError(p);
					return p.waitFor() == 0;
				} catch(IOException ex) {
					Log.v(TAG,"IOException");
					return false;
				} catch(InterruptedException ex) {
					Log.v(TAG,"InterruptedException");
					return false;
				}
			}
			@Override
			protected void onCancelled(Boolean result) {
				sb.append("\n\nError retrieving information about package ");
				sb.append(pkg);
				((TextView)findViewById(R.id.body)).setText(sb.toString());
				mLocked = false;
			}
			@Override
			protected void onPostExecute(Boolean result) {
				if(!result) {
					onCancelled(result);
					return;
				}
				actionbar.setTitle("Package "+pkg);
				((TextView)findViewById(R.id.body)).setText(sb.toString());
				// TODO: multiarch
				Cursor cursor = getContentResolver().query(
					PackageCacheProvider.ContentUri.CACHE_BASE.uri,
					new String[] {DatabaseOpenHelper.C_NAME+" AS _id",DatabaseOpenHelper.C_INSTALLED,DatabaseOpenHelper.C_UPGRADABLE},
					DatabaseOpenHelper.C_NAME+"=?",new String[] {pkg},null
				);
				if(cursor.getCount() < 1) return;
				cursor.moveToFirst();
				final String installed = cursor.getString(1);
				final String upgradable = cursor.getString(2);
				cursor.close();
				Button button;
				final Intent intent = (new Intent(PackageStatusActivity.this,PackageManagerActivity.class)).putExtra("package",pkg);
				if("".equals(installed)) {
					button = (Button)findViewById(R.id.install);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							startActivity(intent.putExtra("command","install"));
							finish();
						}
					});
					button.setVisibility(View.VISIBLE);
				} else {
					if("".equals(upgradable)) {
						button = (Button)findViewById(R.id.reinstall);
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								startActivity(intent.putExtra("command","reinstall"));
								finish();
							}
						});
						button.setVisibility(View.VISIBLE);
					} else {
						button = (Button)findViewById(R.id.upgrade);
						button.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								startActivity(intent.putExtra("command","upgrade"));
								finish();
							}
						});
						button.setVisibility(View.VISIBLE);
					}
					button = (Button)findViewById(R.id.remove);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							startActivity(intent.putExtra("command","remove"));
							finish();
						}
					});
					button.setVisibility(View.VISIBLE);
					button = (Button)findViewById(R.id.autoremove);
					button.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							startActivity(intent.putExtra("command","autoremove"));
							finish();
						}
					});
					button.setVisibility(View.VISIBLE);
				}
				mLocked = false;
			}
		}).execute();
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				if(!mLocked) startActivity((new Intent(this,Main.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
}