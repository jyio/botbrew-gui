package com.botbrew.basil;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SupervisorService extends Service {
	public class LocalBinder extends Binder {
		public SupervisorService getService() {
			return SupervisorService.this;
		}
	}
	private class SupervisorProcess implements Runnable {
		private int startId;
		public SupervisorProcess(int startId) {
			this.startId = startId;
		}
		@Override
		public void run() {
			final BotBrewApp app = (BotBrewApp)getApplicationContext();
			final String root = app.root();
			if(!app.isInstalled()) {
				Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): cannot start supervisor");
				return;
			}
			Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): supervisor started");
			Shell.Pipe sh = null;
			try {
				sh = Shell.Pipe.getRootShell();
				sh.botbrew(root,"runsvdir -P /etc/service 'log: ................................................................................................................................................................................................................................................................'");
				sh.stdin().close();
				sh.waitFor();
			} catch(IOException ex) {
			} catch(InterruptedException ex) {
			} finally {
				boolean exited = true;
				if(sh != null) {
					Process p = sh.proc;
					try {
						p.exitValue();
					} catch(IllegalThreadStateException ex0) {
						try {	// the process exists, so send SIGHUP
							Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): sending SIGHUP to runsvdir...");
							Field f = p.getClass().getDeclaredField("id");
							f.setAccessible(true);
							f.get(p);
							sh = Shell.Pipe.getRootShell();
							sh.exec("kill -1 "+f.get(p));
							sh.stdin().close();
							if(sh.waitFor() == 0) {
								p.waitFor();
								exited = false;
							}
						} catch(NoSuchFieldException ex1) {
							Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): NoSuchFieldException");
						} catch(IllegalAccessException ex1) {
							Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): IllegalAccessException");
						} catch(IOException ex1) {
							Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): IOException");
						} catch(InterruptedException ex1) {
							Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): InterruptedException");
						}
					}
				}
				if(exited) {	// the process does not exist, so clean up offline
					try {
						Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): sending SIGTERM to runsv...");
						sh = Shell.Pipe.getRootShell();
						sh.botbrew(false,root,"killall -1 runsvdir || true\nkillall -15 runsv || true");
						String[] enabled = (new File(root,"etc/service")).list();
						if(enabled != null) for(String filename: enabled) sh.botbrew(false,root,"sv exit "+filename+" || true");
						sh.stdin().close();
						sh.waitFor();
					} catch(IOException ex) {
					} catch(InterruptedException ex) {
					} finally {
						stopSelfResult(startId);
						Log.v(BotBrewApp.TAG,"SupervisorProcess.run(): supervisor stopped");
					}
				}
			}
		}
	}
	private static boolean mRunning = false;
	private static final int ID_RUNNING = 1;
	private final IBinder mBinder = new LocalBinder();
	private SupervisorProcess mSupervisorProcess;
	private Thread mSupervisorThread;
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	@Override
	public void onStart(Intent intent, int startId){
		super.onStart(intent,startId);
		mRunning = true;
		Notification notification = new Notification(R.drawable.ic_launcher,"BotBrew Supervisor started",System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(),"BotBrew Supervisor running","tap to manage",PendingIntent.getActivity(this,0,new Intent(this,SupervisorActivity.class),0));
		startForeground(ID_RUNNING,notification);
		mSupervisorProcess = new SupervisorProcess(startId);
		mSupervisorThread = new Thread(mSupervisorProcess);
		mSupervisorThread.start();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent,startId);
		return START_STICKY;
	}
	@Override
	public void onDestroy() {
		if(mSupervisorThread != null) {
			mSupervisorThread.interrupt();
			mSupervisorThread = null;
			mSupervisorProcess = null;
		}
		mRunning = false;
		super.onDestroy();
	}
	public static boolean isRunning() {
		return mRunning;
	}
}