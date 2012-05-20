package com.botbrew.basil;

import jackpal.androidterm.Exec;
import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
	private FileDescriptor mFD;
	private FileOutputStream mFDstdin;
	private FileInputStream mFDstdout;
	private int mPID;
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
		int[] pid = new int[] {0};
		mFD = Exec.createSubprocess("/system/xbin/su",new String[] {"/system/xbin/su"},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
		mFDstdin = new FileOutputStream(mFD);
		mFDstdout = new FileInputStream(mFD);
		mPID = pid[0];
		ColorScheme scheme = new ColorScheme(7,0xffffffff,0,0xff000000);
		mTermSession = new TermSession();
		mTermSession.setColorScheme(scheme);
		mTermSession.setTermOut(mFDstdin);
		mTermSession.setTermIn(mFDstdout);
		try {
			mFDstdin.write(("set -e\n").getBytes());
			mFDstdin.write(("chmod 0755 '"+cmd+"'\n").getBytes());
			mFDstdin.write(("mkdir -p '"+file+"'\n").getBytes());
			mFDstdin.write(("cd '"+file+"'\n").getBytes());
			mFDstdin.write(("'"+cmd+"' -n\n").getBytes());
			mFDstdin.write(("rm '"+cmd+"'\n").getBytes());
			mFDstdin.write(("exit\n").getBytes());
			EmulatorView emulator = (EmulatorView)findViewById(R.id.emulator);
			emulator.attachSession(mTermSession);
			emulator.setDensity(getResources().getDisplayMetrics());
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
					return Exec.waitFor(mPID);
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
					startActivity(new Intent(BootstrapInstallActivity.this,Main.class));
					finish();
				}
			}).execute();
		} catch(IOException ex) {
			Exec.hangupProcessGroup(mPID);
			Exec.close(mFD);
			findViewById(R.id.fail).setVisibility(View.VISIBLE);
			findViewById(R.id.retry).setVisibility(View.VISIBLE);
		}
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
}