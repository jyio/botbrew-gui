package com.botbrew.basil;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class ControllerService extends Service {
	private static final String TAG = "BBController";
	private static boolean isRunning = false;
	protected class StateHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if(mState != null) mState.sendMessage(Message.obtain(msg));
		}
	}
	protected class ReadyState extends EnumHandler<MessageType> {
		ReadyState() {
			super(MessageType.class);
		}
		@Override
		public void handleMessage(MessageType msg) {
			switch(msg) {
				case MSG_REGISTER_CLIENT:
				//	mClients.add(msg.replyTo);
					break;
				case MSG_UNREGISTER_CLIENT:
				//	mClients.remove(msg.replyTo);
					break;
				case MSG_SET_VALUE:
				//	incrementby = msg.arg1;
					break;
			}
		}
	}
	//protected final LocalBinder<ControllerService> mBinder = new LocalBinder<ControllerService>(this);
	protected final Messenger mMessenger = new Messenger(new StateHandler());
	protected Handler mState = null;
	@Override
	public void onCreate() {
		super.onCreate();
		mState = new ReadyState();
		isRunning = true;
		Log.v(TAG,"onCreate()");
	}
	@Override
	public void onDestroy() {
		Log.v(TAG,"onDestroy()");
		isRunning = false;
		super.onDestroy();
	}
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	@Override
	public boolean onUnbind(Intent intent) {
		return false;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG,"onStartCommand("+intent+","+flags+","+startId+")");
		return START_STICKY; // run until explicitly stopped.
	}
	public static boolean isRunning() {
		return isRunning;
	}
}
