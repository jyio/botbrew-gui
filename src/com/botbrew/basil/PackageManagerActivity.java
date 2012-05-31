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
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class PackageManagerActivity extends SherlockFragmentActivity {
	private static interface ProcessProxy {
		boolean setup(String root, DebianPackageManager dpm);
		FileDescriptor getSubprocess() throws IOException;
		int waitFor();
		void hangupProcessGroup();
		void onSuccess();
		void onFail();
	}
	private class DebProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write((root+"/init -- dpkg --install "+pkg+"\n").getBytes());
			// TODO: cleanup
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_install("-f")+"\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private class InstallProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_install(pkg)+"\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private class ReinstallProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_install(pkg)+" -oapt::get::reinstall=1\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private class RemoveProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_remove(pkg)+"\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private class AutoremoveProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_autoremove(pkg)+"\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private class DistupgradeProxy implements ProcessProxy {
		private String root;
		private DebianPackageManager dpm;
		private String pkg;
		private int mPID;
		@Override
		public boolean setup(final String root, final DebianPackageManager dpm) {
			this.root = root;
			this.dpm = dpm;
			pkg = getIntent().getStringExtra("package");
			return pkg != null;
		}
		@Override
		public FileDescriptor getSubprocess() throws IOException {
			int[] pid = new int[] {0};
			FileDescriptor fd = Exec.createSubprocess(BotBrewApp.rootshell,new String[] {BotBrewApp.rootshell},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_distupgrade(pkg)+"\n").getBytes());
			return fd;
		}
		@Override
		public int waitFor() {
			return Exec.waitFor(mPID);
		}
		@Override
		public void hangupProcessGroup() {
			Exec.hangupProcessGroup(mPID);
		}
		@Override
		public void onSuccess() {
		}
		@Override
		public void onFail() {
		}
	}
	private BotBrewApp mApp;
	private ProcessProxy mProxy;
	private FileDescriptor mFD;
	private FileOutputStream mFDstdin;
	private FileInputStream mFDstdout;
	private TermSession mTermSession;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_manager_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		dpm.config(pref);
		final Intent intent = getIntent();
		final String command = intent.getStringExtra("command");
		if("installdeb".equals(command)) mProxy = new DebProxy();
		else if("install".equals(command)) mProxy = new InstallProxy();
		else if("reinstall".equals(command)) mProxy = new ReinstallProxy();
		else if("remove".equals(command)) mProxy = new RemoveProxy();
		else if("autoremove".equals(command)) mProxy = new AutoremoveProxy();
		else if("upgrade".equals(command)) mProxy = new DistupgradeProxy();
		if((mProxy == null)||(!mProxy.setup(root,dpm))) {
			finish();
			return;
		}
		try {
			mFD = mProxy.getSubprocess();
			mFDstdin = new FileOutputStream(mFD);
			mFDstdout = new FileInputStream(mFD);
			ColorScheme scheme = new ColorScheme(7,0xffffffff,0,0xff000000);
			mTermSession = new TermSession();
			mTermSession.setColorScheme(scheme);
			mTermSession.setTermOut(mFDstdin);
			mTermSession.setTermIn(mFDstdout);
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
					return mProxy.waitFor();
				}
				@Override
				protected void onCancelled(Integer result) {
					mProxy.onFail();
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
					mProxy.onSuccess();
					final TextView vResult = (TextView)findViewById(R.id.result);
					vResult.setText("win (press back)");
					vResult.setVisibility(View.VISIBLE);
					mLocked = false;
				}
			}).execute();
		} catch(IOException ex) {
			mProxy.hangupProcessGroup();
			Exec.close(mFD);
			mProxy.onFail();
		}
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