package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StatFs;
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
	public static File root;
	public static String rootshell = (new File("/system/bin/su")).exists()?"/system/bin/su":"/system/xbin/su";
	public boolean isInstalled(final File path) {
		if(!path.isDirectory()) return false;
		final File path_init = new File(path,"init");
		if(path_init.isFile()) return checkInstall(path,false);
		final File path_img = new File(path,"fs.img");
		if(path_img.isFile()) return checkInstall(path,true);
		return false;
	}
	public boolean checkInstall(final File path, final boolean remount) {
		if(!path.isDirectory()) return false;
		final File path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so"));
		final File path_init = new File(path,"init");
		final File path_img = new File(path,"fs.img");
		Process p;
		OutputStream p_stdin;
		try {
			if((remount)||(!path_init.isFile())) {
				p = Runtime.getRuntime().exec(new String[] {rootshell});
				p_stdin = p.getOutputStream();
				p_stdin.write(("exec '"+path_init_src.getAbsolutePath()+"' --target '"+path.getAbsolutePath()+"' --unmount").getBytes());
				p_stdin.close();
				sinkError(p);
				if(p.waitFor() != 0) return false;
				if(path_init.isFile()) {
					p = Runtime.getRuntime().exec(new String[] {rootshell});
					p_stdin = p.getOutputStream();
					p_stdin.write(("exec '"+path_init_src.getAbsolutePath()+"' --target '"+path.getAbsolutePath()+"' -- /system/bin/sh -c ''").getBytes());
					p_stdin.close();
					sinkError(p);
					return p.waitFor() == 0;
				} else if(path_img.isFile()) {
					p = Runtime.getRuntime().exec(new String[] {rootshell});
					p_stdin = p.getOutputStream();
					p_stdin.write(("exec '"+path_init_src.getAbsolutePath()+"' --target '"+path_img.getAbsolutePath()+"' -- /system/bin/sh -c ''").getBytes());
					p_stdin.close();
					sinkError(p);
					return p.waitFor() == 0;
				} else return false;
			}
			p = Runtime.getRuntime().exec(new String[] {rootshell});
			p_stdin = p.getOutputStream();
			if(remount) p_stdin.write(("exec '"+path_init.getAbsolutePath()+"' -- /system/bin/sh -c 'rm -rf /var/run /tmp /var/lock /botbrew/tmp; ln -s ../run /var/run; ln -s run/tmp /tmp; ln -s ../run/lock /var/lock; ln -s run/tmp /botbrew/tmp'").getBytes());
			else p_stdin.write(("exec '"+path_init.getAbsolutePath()+"' -- /system/bin/sh -c ''").getBytes());
			p_stdin.close();
			sinkError(p);
			return p.waitFor() == 0;
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
			final Shell.Pipe sh = Shell.Pipe.getRootShell();
			final OutputStream p_stdin = sh.stdin();
			final String path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so")).getAbsolutePath();
			final String path_init = (new File(path,"init")).getAbsolutePath();
			p_stdin.write(("cp '"+path_init_src+"' '"+path_init+"'\n").getBytes());
			p_stdin.write(("chmod 4755 '"+path_init+"'\n").getBytes());
			p_stdin.close();
			sinkError(sh.proc);
			if(sh.waitFor() == 0) checkInstall(path,true);
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return false;
	}
	public boolean clean() {
		try {
			final Process p = Runtime.getRuntime().exec(new String[] {rootshell});
			final OutputStream p_stdin = p.getOutputStream();
			p_stdin.write(("exec '"+root.getAbsolutePath()+"/init' -- apt-get clean").getBytes());
			p_stdin.close();
			sinkError(p);
			return (p.waitFor() == 0);
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return false;
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
	public static long checksumSource() {
		long total = (new File(root,"etc/apt/sources.list")).lastModified();
		final File[] items = (new File(root,"etc/apt/sources.list.d")).listFiles();
		if(items != null) for(File item: items) total += item.lastModified();
		return total;
	}
	public static long checksumCache() {
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
	public static void sinkOutput(final Process p) throws IOException {
		String line;
		final BufferedReader p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while((line = p_stdout.readLine()) != null) Log.v(TAG,"[STDOUT] "+line);
	}
	public static void sinkError(final Process p) throws IOException {
		String line;
		final BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
	}
}
