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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class PackageManagerActivity extends SherlockFragmentActivity {
	private static interface ProcessProxy {
		boolean setup(String root, DebianPackageManager dpm);
		FileDescriptor getSubprocess() throws IOException;
		int waitFor();
		void hangupProcessGroup();
		void onSuccess();
		void onFail();
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
			FileDescriptor fd = Exec.createSubprocess("/system/xbin/su",new String[] {"/system/xbin/su"},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
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
			FileDescriptor fd = Exec.createSubprocess("/system/xbin/su",new String[] {"/system/xbin/su"},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
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
			FileDescriptor fd = Exec.createSubprocess("/system/xbin/su",new String[] {"/system/xbin/su"},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
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
	private class UpgradeProxy implements ProcessProxy {
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
			FileDescriptor fd = Exec.createSubprocess("/system/xbin/su",new String[] {"/system/xbin/su"},new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},pid);
			mPID = pid[0];
			FileOutputStream p_stdin = new FileOutputStream(fd);
			p_stdin.write(("exec "+root+"/init -- "+dpm.aptget_upgrade(pkg)+"\n").getBytes());
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
	public static final String arch_native = Build.CPU_ABI.toLowerCase();
	public static final String arch =
		"device-"+Build.DEVICE.toLowerCase()+
		",cpu-abi-"+Build.CPU_ABI.toLowerCase()+
		",cpu-abi2-"+Build.CPU_ABI2.toLowerCase()+
		",board-"+Build.BOARD.toLowerCase()+
		",android"+
		("armeabi".equals(arch_native)?",armel":(","+arch_native))+
		","+Build.DEVICE.toLowerCase()+		// TODO: remove after dpkg transition
		","+Build.CPU_ABI.toLowerCase()+	// TODO: remove after dpkg transition
		","+Build.CPU_ABI2.toLowerCase();	// TODO: remove after dpkg transition
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
		actionbar.setDisplayUseLogoEnabled(false);
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		for(DebianPackageManager.Config key: DebianPackageManager.Config.values()) {
			if(pref.contains(key.name)) try {
				dpm.config(key,pref.getBoolean(key.name,false)?"1":"0");
			} catch(ClassCastException ex0) {
				try {
					dpm.config(key,pref.getString(key.name,null));
				} catch(ClassCastException ex1) {}
			}
		}
		if(pref.getBoolean("debian_hack",false)) dpm.config(
			DebianPackageManager.Config.DPkg_Options,
			"--ignore-depends=libgcc1"
		);
		String var_pref = pref.getString("apt::architectures",null);
		dpm.config(
			DebianPackageManager.Config.APT_Architectures,
			(var_pref==null)||("".equals(var_pref))?arch:arch+","+var_pref
		);
		final Intent intent = getIntent();
		final String command = intent.getStringExtra("command");
		if("install".equals(command)) mProxy = new InstallProxy();
		else if("remove".equals(command)) mProxy = new RemoveProxy();
		else if("autoremove".equals(command)) mProxy = new AutoremoveProxy();
		else if("upgrade".equals(command)) mProxy = new UpgradeProxy();
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
					vResult.setText("fail");
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
					vResult.setText("win");
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
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
}