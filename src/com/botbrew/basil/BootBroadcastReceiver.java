package com.botbrew.basil;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		PreferenceManager.setDefaultValues(context,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		if(pref.getBoolean("boot_initialize",true)) (new Thread(new Runnable() {
			@Override
			public void run() {
				final BotBrewApp app = (BotBrewApp)context.getApplicationContext();
				if((app.checkInstall(new File(app.root()),false))&&(pref.getBoolean("boot_supervisor",false))) context.startService(new Intent(context,SupervisorService.class));
			}
		})).start();
	}
}