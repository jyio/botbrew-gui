package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapInstallActivity extends SherlockFragmentActivity {
	private BotBrewApp mApp;
	private TermSession mTermSession;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootstrap_install_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(true);
		((Button)findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BootstrapInstallActivity.this,BootstrapReadyActivity.class));
				finish();
			}
		});
		final String file = (new File(getIntent().getStringExtra("file"))).getAbsolutePath();
		final boolean loop = getIntent().getBooleanExtra("loop",false);
		final File archive = new File(getCacheDir(),loop?"img.zip":"pkg.zip");
		final String cmd = archive.getAbsolutePath();
		try {
			final Shell.Term sh = Shell.Term.getRootShell();
			final OutputStream sh_stdin = sh.stdin();
			mTermSession = new TermSession();
			mTermSession.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
			mTermSession.setTermOut(sh_stdin);
			mTermSession.setTermIn(sh.stdout());
			sh_stdin.write(("set -e\n").getBytes());
			sh_stdin.write(("chmod 0755 '"+cmd+"'\n").getBytes());
			for(String mkdir: mkdir_p(new File(file))) sh_stdin.write(("mkdir '"+mkdir+"'\n").getBytes());
			sh_stdin.write(("cd '"+file+"'\n").getBytes());
			sh_stdin.write(("'"+cmd+"' -n\n").getBytes());
			sh_stdin.write(("exit\n").getBytes());
			sh_stdin.close();
			EmulatorView emulator = (EmulatorView)findViewById(R.id.emulator);
			emulator.attachSession(mTermSession);
			emulator.setDensity(getResources().getDisplayMetrics());
			emulator.setTextSize(16);
			mApp = (BotBrewApp)getApplicationContext();
			emulator.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onSingleTapUp(MotionEvent e) {
					mApp.doToggleSoftKeyboard();
					return true;
				}
			});
			(new AsyncTask<Void,Void,Integer>() {
				@Override
				protected Integer doInBackground(final Void... ign) {
					mLocked = true;
					try {
						int res = sh.waitFor();
						archive.delete();
						((BotBrewApp)getApplicationContext()).nativeInstall(new File(file));
						return res;
					} catch(InterruptedException ex) {
					}
					return -1;
				}
				@Override
				protected void onCancelled(Integer result) {
					findViewById(R.id.fail).setVisibility(View.VISIBLE);
					findViewById(R.id.retry).setVisibility(View.VISIBLE);
					mLocked = false;
				}
				@Override
				protected void onPostExecute(Integer result) {
					if(result.intValue() != 0) {
						onCancelled(result);
						return;
					}
					SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(BootstrapInstallActivity.this);
					SharedPreferences.Editor editor = pref.edit();
					editor.putString("var_root",file);
					editor.commit();
					startActivity((new Intent(BootstrapInstallActivity.this,Main.class)).putExtra("update",true));
					finish();
				}
			}).execute();
		} catch(IOException ex) {
			findViewById(R.id.fail).setVisibility(View.VISIBLE);
			findViewById(R.id.retry).setVisibility(View.VISIBLE);
		}
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
	public static List<String> mkdir_p(final File path) {
		if(path.exists()) return new ArrayList<String>();
		else {
			final List<String> paths = mkdir_p(path.getParentFile());
			paths.add(path.getAbsolutePath());
			return paths;
		}
	}
}