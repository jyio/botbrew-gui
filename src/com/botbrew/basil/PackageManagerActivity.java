package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class PackageManagerActivity extends SherlockFragmentActivity {
	protected static enum TransactionType {
		DPKG_INSTALL,
		APTCACHE_SHOW,
		APTGET_INSTALL,
		APTGET_REINSTALL,
		APTGET_UPGRADE,
		APTGET_DISTUPGRADE,
		APTGET_REMOVE,
		APTGET_AUTOREMOVE
	}
	private BotBrewApp mApp;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_manager_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		final Intent intent = getIntent();
		final String command = intent.getStringExtra("command");
		final String pkg = intent.getStringExtra("package");
		if("installdeb".equals(command)) doDpkgInstall(pkg);
		else if("install".equals(command)) doAptGet(TransactionType.APTGET_INSTALL,pkg);
		else if("reinstall".equals(command)) doAptGet(TransactionType.APTGET_REINSTALL,pkg);
		else if("remove".equals(command)) doAptGet(TransactionType.APTGET_REMOVE,pkg);
		else if("autoremove".equals(command)) doAptGet(TransactionType.APTGET_AUTOREMOVE,pkg);
		else if("upgrade".equals(command)) doAptGet(TransactionType.APTGET_DISTUPGRADE,pkg);
		else finish();
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
	public void setViewFrame(Object obj) {
		final ViewGroup layout = (ViewGroup)findViewById(R.id.viewframe);
		layout.removeAllViews();
		if(obj instanceof TermSession) {
			final EmulatorView emulator = new EmulatorView(this,(TermSession)obj,getResources().getDisplayMetrics());
			emulator.setTextSize(16);
			emulator.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onSingleTapUp(MotionEvent e) {
					mApp.doToggleSoftKeyboard();
					return true;
				}
			});
			layout.addView(emulator);
		};
	}
	public void doAptGet(final TransactionType what, final CharSequence... pkg) {
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		dpm.config(pref);
		DebianPackageManager dpm2;
		Shell sh = null;
		try {
			switch(what) {
				case APTCACHE_SHOW:
					sh = Shell.Term.getUserShell();
					sh.botbrew(root,dpm.aptcache_show(pkg));
					break;
				case APTGET_INSTALL:
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_install(pkg));
					break;
				case APTGET_REINSTALL:
					dpm2 = new DebianPackageManager(dpm);
					dpm2.config(DebianPackageManager.Config.APT_Get_ReInstall,"1");
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm2.aptget_install(pkg));
					break;
				case APTGET_UPGRADE:
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_upgrade(pkg));
					break;
				case APTGET_DISTUPGRADE:
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_distupgrade(pkg));
					break;
				case APTGET_REMOVE:
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_remove(pkg));
					break;
				case APTGET_AUTOREMOVE:
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_autoremove(pkg));
					break;
			}
			if(sh == null) return;
			mLocked = true;
			final InputStream sh_stdout = sh.stdout();
			while(sh_stdout.read() != '\n');
			while(sh_stdout.read() != '\n');
			final TermSession term = new TermSession();
			term.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
			term.setTermOut(sh.stdin());
			term.setTermIn(sh.stdout());
			setViewFrame(term);
			final Shell fsh = sh;
			(new AsyncTask<Void,Void,Integer>() {
				@Override
				protected Integer doInBackground(final Void... ign) {
					try {
						return fsh.waitFor();
					} catch(InterruptedException ex) {
						return -1;
					}
				}
				@Override
				protected void onCancelled(Integer result) {
					//mProxy.onFail();
					final TextView vResult = (TextView)findViewById(R.id.result);
					vResult.setText("fail (press back)");
					vResult.setVisibility(View.VISIBLE);
					mLocked = false;
				}
				@Override
				protected void onPostExecute(Integer result) {
					if(result.intValue() != 0) {
						onCancelled(result);
						return;
					}
					//mProxy.onSuccess();
					final TextView vResult = (TextView)findViewById(R.id.result);
					vResult.setText("win (press back)");
					vResult.setVisibility(View.VISIBLE);
					mLocked = false;
				}
			}).execute();
		} catch(IOException ex) {
		}
	}
	public void doDpkgInstall(final CharSequence... pkg) {
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		dpm.config(pref);
		mLocked = true;
		final TermSession term0 = new TermSession();
		final TermSession term1 = new TermSession();
		(new AsyncTask<Void,TermSession,Integer>() {
			@Override
			protected Integer doInBackground(final Void... ign) {
				try {
					Shell sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.dpkg_install(pkg));
					InputStream sh_stdout = sh.stdout();
					while(sh_stdout.read() != '\n');
					while(sh_stdout.read() != '\n');
					term0.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
					term0.setTermOut(sh.stdin());
					term0.setTermIn(sh.stdout());
					publishProgress(term0);
					if(sh.waitFor() == 0) return 0;
					sh = Shell.Term.getRootShell();
					dpm.config(DebianPackageManager.Config.APT_Get_FixBroken,"1");
					sh.botbrew(root,dpm.aptget_install());
					sh_stdout = sh.stdout();
					while(sh_stdout.read() != '\n');
					term1.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
					term1.setTermOut(sh.stdin());
					term1.setTermIn(sh.stdout());
					publishProgress(term1);
					return sh.waitFor();
				} catch (IOException e) {
					return -1;
				} catch(InterruptedException ex) {
					return -1;
				}
			}
			@Override
			protected void onProgressUpdate(TermSession... progress) {
				setViewFrame(progress[progress.length-1]);
			}
			@Override
			protected void onCancelled(Integer result) {
				final TextView vResult = (TextView)findViewById(R.id.result);
				vResult.setText("fail (press back)");
				vResult.setVisibility(View.VISIBLE);
				mLocked = false;
			}
			@Override
			protected void onPostExecute(Integer result) {
				if(result.intValue() != 0) {
					onCancelled(result);
					return;
				}
				final TextView vResult = (TextView)findViewById(R.id.result);
				vResult.setText("win (press back)");
				vResult.setVisibility(View.VISIBLE);
				mLocked = false;
			}
		}).execute();
	}
}