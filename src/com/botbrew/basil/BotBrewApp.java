package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.File;
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
	private static final String TAG = "BotBrew";
	public static final String default_root = "/data/botbrew-basil";
	public static File root;
	public boolean isInstalled(final File path) {
		if(!path.isDirectory()) return false;
		final File path_init = new File(path,"init");
		if(path_init.isFile()) return checkInstall(path_init);
		final File path_img = new File(path,"fs.img");
		final File path_busybox = (new File(getCacheDir(),"busybox"));
		final File path_busybox_src = new File(path,"busybox");
		try {
			Process p;
			OutputStream p_stdin;
			boolean mounted = false;
			if(path_img.isFile()) {
				boolean busyboxcopy = false;
				if(!path_busybox.isFile()) {
					if(path_busybox_src.isFile()) busyboxcopy = true;
					else return false;
				}
				p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
				p_stdin = p.getOutputStream();
				p_stdin.write(("export PATH="+getCacheDir()+":${PATH}\n").getBytes());
				if(busyboxcopy) {
					p_stdin.write(("cp '"+path_busybox_src+"' '"+path_busybox+"'\n").getBytes());
					p_stdin.write(("chmod 0755 '"+path_busybox+"'\n").getBytes());
				}
				p_stdin.write(("busybox mount -o loop '"+path_img+"' '"+path+"'").getBytes());
				p_stdin.close();
				sinkError(p);
				if(p.waitFor() != 0) return false;
				mounted = true;
			}
			final String path_init_src = (new File(new File(getCacheDir().getParent(),"lib"),"libinit.so")).getAbsolutePath();
			p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
			p_stdin = p.getOutputStream();
			p_stdin.write(("cp '"+path_init_src+"' '"+path_init+"'\n").getBytes());
			p_stdin.write(("chmod 4755 '"+path_init+"'\n").getBytes());
			p_stdin.close();
			sinkError(p);
			if(p.waitFor() != 0) return false;
			if((path_init.isFile())&&(checkInstall(path_init))) return true;
			if(mounted) {
				p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
				p_stdin = p.getOutputStream();
				p_stdin.write(("export PATH="+getCacheDir()+":${PATH}\n").getBytes());
				p_stdin.write(("busybox umount '"+path+"'").getBytes());
				p_stdin.close();
				sinkError(p);
				if(p.waitFor() != 0) return false;
			}
		} catch(IOException ex) {
			Log.v(TAG,"IOException");
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException");
		}
		return false;
	}
	public boolean checkInstall(final File path_init) {
		return path_init.isFile();
		/*if(!path_init.isFile()) return false;
		try {
			String line;
			Process p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
			OutputStream p_stdin = p.getOutputStream();
			p_stdin.write((path_init.getAbsolutePath()+" -- /system/bin/sh -c ''").getBytes());
			p_stdin.close();
			sinkError(p);
			if(p.waitFor() == 0) return true;
		} catch(IOException ex) {
			Log.v(TAG,"IOException");
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException");
		}
		return false;*/
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
	public static boolean needsLoopMount(final String path) {
		if(
			(path.startsWith("/sdcard"))||
			(path.startsWith("/mnt/sdcard"))||
			(path.startsWith("/emmc"))||
			(path.startsWith("/mnt/emmc"))||
			(path.startsWith("/usbdisk"))||
			(path.startsWith("/mnt/usbdisk"))
		) return true;
		else return false;
	}
	public static long getChecksum() {
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
