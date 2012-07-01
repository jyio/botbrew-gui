package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

@ReportsCrashes(
	formKey="dEVFcXVwU01rcE9EcHVrWktILXNvX3c6MQ",
	mode = ReportingInteractionMode.NOTIFICATION,
	resNotifTickerText = R.string.crash_notif_ticker_text,
	resNotifTitle = R.string.crash_notif_title,
	resNotifText = R.string.crash_notif_text,
	resDialogText = R.string.crash_dialog_text,
	resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
	resDialogOkToast = R.string.crash_dialog_ok_toast
)
public class BotBrewApp extends Application {
	public static final String TAG = "BotBrew";
	public static final String default_root = "/data/botbrew-basil";
	public BootstrapActivity.DialogState mBootstrapDialogState = BootstrapActivity.DialogState.NONE;
	@Override
	public void onCreate() {
		ACRA.init(this);
		super.onCreate();
	}
	public String root() {
		return PreferenceManager.getDefaultSharedPreferences(this).getString("var_root",default_root);
	}
	public void root(final String s) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(!pref.getString("var_root",default_root).equals(s)) {
			unmount(s);
			final SharedPreferences.Editor editor = pref.edit();
			editor.putString("var_root",s);
			editor.remove("var_dbChecksumCache");
			editor.commit();
		}
	}
	public void root(final File f) {
		try {
			root(f.getCanonicalPath());
		} catch(IOException ex) {
			root(f.getAbsolutePath());
		}
	}
	public boolean isInstalled(final File path) {
		if(!path.isDirectory()) return false;
		final File path_init = new File(path,"init");
		if(path_init.isFile()) return checkInstall(path,false);
		final File path_img = new File(path,"fs.img");
		if(path_img.isFile()) return checkInstall(path,true);
		return false;
	}
	public boolean isInstalled() {
		return isInstalled(new File(root()));
	}
	public boolean checkInstall(final File path, final boolean remount) {
		if(!path.isDirectory()) return false;
		final File path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so"));
		final File path_init = new File(path,"init");
		final File path_img = new File(path,"fs.img");
		Shell sh;
		try {
			if((remount)||(!path_init.isFile())) {
				sh = Shell.Pipe.getRootShell().redirect();
				sh.exec("'"+path_init_src.getAbsolutePath()+"' --target '"+path.getAbsolutePath()+"' --unmount");
				sh.stdin().close();
				sinkOutput(sh);
				if(sh.waitFor() != 0) return false;
				if(path_init.isFile()) {
					sh = Shell.Pipe.getRootShell().redirect();
					sh.botbrew(path_init_src.getAbsolutePath(),path.getAbsolutePath(),"/system/bin/sh -c ''");
					sh.stdin().close();
					sinkOutput(sh);
					return sh.waitFor() == 0;
				} else if(path_img.isFile()) {
					sh = Shell.Pipe.getRootShell().redirect();
					sh.botbrew(path_init_src.getAbsolutePath(),path_img.getAbsolutePath(),"/system/bin/sh -c ''");
					sh.stdin().close();
					sinkOutput(sh);
					return sh.waitFor() == 0;
				} else return false;
			}
			sh = Shell.Pipe.getRootShell().redirect();
			if(remount) sh.botbrew(path_init_src.getAbsolutePath(),path.getAbsolutePath(),"/system/bin/sh -c 'rm -rf /var/run /tmp /var/lock /botbrew/tmp; ln -s ../run /var/run; ln -s run/tmp /tmp; ln -s ../run/lock /var/lock; ln -s run/tmp /botbrew/tmp'");
			else sh.botbrew(path_init_src.getAbsolutePath(),path.getAbsolutePath(),"/system/bin/sh -c ''");
			sh.stdin().close();
			sinkOutput(sh);
			return sh.waitFor() == 0;
		} catch(IOException ex) {
			Log.v(TAG,"IOException");
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException");
		}
		return false;
	}
	public boolean nativeInstall(final File path) {
		try {
			final MountFs.MountEntry mntent = MountFs.find(path);
			if((mntent != null)&&("vfat".equals(mntent.fs_vfstype))) return checkInstall(path,true);
		} catch(FileNotFoundException ex) {}
		try {
			final Shell.Pipe sh = Shell.Pipe.getRootShell().redirect();
			final OutputStream sh_stdin = sh.stdin();
			final String path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so")).getAbsolutePath();
			final String path_init = (new File(path,"init")).getAbsolutePath();
			sh_stdin.write(("cp '"+path_init_src+"' '"+path_init+"'\n").getBytes());
			sh_stdin.write(("chmod 4755 '"+path_init+"'\n").getBytes());
			sh_stdin.close();
			sinkOutput(sh);
			if(sh.waitFor() == 0) checkInstall(path,true);
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return false;
	}
	public boolean clean() {
		try {
			final Shell sh = Shell.Pipe.getRootShell().redirect();
			sh.botbrew(root(),"apt-get clean");
			sh.stdin().close();
			sinkOutput(sh);
			return (sh.waitFor() == 0);
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return false;
	}
	public File textEdit(final File path) {
		try {
			final String root = root();
			final File tmp = File.createTempFile("editor-"+path.getName().replace('.','-'),".tmp",getCacheDir());
			final Shell sh = Shell.Pipe.getRootShell();
			sh.botbrew(false,root,"cp '"+path+"' '"+tmp+"'");
			sh.botbrew(root,"chmod 0777 '"+tmp+"'");
			sinkOutput(sh);
			sinkError(sh);
			sh.waitFor();
			return tmp;
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return null;
	}
	public boolean textCommit(final File path, final File tmp) {
		try {
			final String root = root();
			final Shell sh = Shell.Pipe.getRootShell();
			sh.botbrew(false,root,"cp '"+tmp+"' '"+path+"'");
			sh.botbrew(root,"rm '"+tmp+"'");
			sinkOutput(sh);
			sinkError(sh);
			return (sh.waitFor() == 0);
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return false;
	}
	public boolean unmount(final String path) {
		stopService(new Intent(this,SupervisorService.class));
		final File path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so"));
		Shell sh;
		try {
			sh = Shell.Pipe.getRootShell().redirect();
			sh.exec("'"+path_init_src.getCanonicalPath()+"' --target '"+path+"' --unmount");
			sh.stdin().close();
			sinkOutput(sh);
			return sh.waitFor() == 0;
		} catch(IOException ex) {
			Log.v(TAG,"IOException");
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException");
		}
		return false;
	}
	public boolean unmount(final File path) {
		if(!path.isDirectory()) return false;
		try {
			return unmount(path.getCanonicalPath());
		} catch(IOException ex) {
			return unmount(path.getAbsolutePath());
		}
	}
	public boolean unmount() {
		return unmount(root());
	}
	public boolean isOnline() {
		NetworkInfo ni = ((ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
		if((ni != null)&&(ni.isConnected())) return true;
		else return false;
	}
	public int getScreenWidthDp() {
		if(Build.VERSION.SDK_INT >= 13) return getResources().getConfiguration().screenWidthDp;
		else {
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			return Math.round(metrics.density*metrics.widthPixels);
		}
	}
	public boolean isWide() {
		return getScreenWidthDp() >= 800;
	}
	public static long getByteCount(String path) {
		StatFs stat = new StatFs(path);
		return ((long)stat.getBlockCount())*stat.getBlockSize();
	}
	public static long getFreeBytes(String path) {
		StatFs stat = new StatFs(path);
		return ((long)stat.getAvailableBlocks())*stat.getBlockSize();
	}
	public long checksumSource() {
		final String root = root();
		long total = (new File(root,"etc/apt/sources.list")).lastModified();
		final File[] items = (new File(root,"etc/apt/sources.list.d")).listFiles();
		if(items != null) for(File item: items) total += item.lastModified();
		return total;
	}
	public long checksumCache() {
		final String root = root();
		long total = (new File(root,"var/lib/dpkg/status")).lastModified();
		final File[] items = (new File(root,"var/lib/apt/lists")).listFiles();
		if(items != null) for(File item: items) total += item.lastModified();
		return total;
	}
	public boolean hasKeyboard() {
		Configuration c = getResources().getConfiguration();
		return (c.keyboard == Configuration.KEYBOARD_QWERTY)&&(c.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);
	}
	public void doShowSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}
	public void doToggleSoftKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(0,0);
	}
	public static void sinkOutput(final Shell sh) throws IOException {
		String line;
		final BufferedReader p_stdout = new BufferedReader(new InputStreamReader(sh.stdout()));
		while((line = p_stdout.readLine()) != null) Log.v(TAG,"[STDOUT] "+line);
	}
	public static void sinkOutput(final Process p) throws IOException {
		String line;
		final BufferedReader p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while((line = p_stdout.readLine()) != null) Log.v(TAG,"[STDOUT] "+line);
	}
	public static void sinkError(final Shell sh) throws IOException {
		String line;
		final BufferedReader p_stderr = new BufferedReader(new InputStreamReader(sh.stderr()));
		while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
	}
	public static void sinkError(final Process p) throws IOException {
		String line;
		final BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
	}
}
