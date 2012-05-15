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
		if(!path_init.isFile()) return false;
		try {
			String line;
			Process p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
			OutputStream p_stdin = p.getOutputStream();
			p_stdin.write(("busybox mount -o loop '"+path_img+"' '"+path+"'").getBytes());
			p_stdin.close();
			BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
			if(p.waitFor() != 0) return false;
			if((path_init.isFile())&&(checkInstall(path_init))) return true;
			p = Runtime.getRuntime().exec(new String[] {"/system/xbin/su"});
			p_stdin = p.getOutputStream();
			p_stdin.write(("busybox umount '"+path+"'").getBytes());
			p_stdin.close();
			p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
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
			BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
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
}
