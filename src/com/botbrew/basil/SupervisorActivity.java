package com.botbrew.basil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class SupervisorActivity extends SherlockFragmentActivity {
	private class RefreshTimer implements Runnable {
		private final Handler handler = new Handler();
		@Override
		public void run() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					servicelistfragment.refresh();
					supervisorState();
					setIP();
					setTime();
					register(4000);
				}
			});
		}
		public void register() {
			unregister();
			handler.post(this);
		}
		public void register(long delay) {
			unregister();
			handler.postDelayed(this,delay);
		}
		public void unregister() {
			handler.removeCallbacks(this);
		}
	}
	private Menu mMenu;
	private ToggleButton togglerefresh;
	private ToggleButton toggleboot;
	private TextView textiplist;
	private ServiceListFragment servicelistfragment;
	private SharedPreferences pref;
	private RefreshTimer mRefreshTimer = new RefreshTimer();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.supervisor_activity);
	//	pref = getSharedPreferences("BotBrew",0);
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		pref = PreferenceManager.getDefaultSharedPreferences(this);
		togglerefresh = (ToggleButton)findViewById(R.id.togglerefresh);
		toggleboot = (ToggleButton)findViewById(R.id.toggleboot);
		textiplist = (TextView)findViewById(R.id.textiplist);
		servicelistfragment = (ServiceListFragment)getSupportFragmentManager().findFragmentById(R.id.servicelist);
		togglerefresh.setChecked(true);
		togglerefresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ToggleButton toggle = (ToggleButton)v;
				if(toggle.isChecked()) mRefreshTimer.register();
				else mRefreshTimer.unregister();
			}
		});
		toggleboot.setChecked(pref.getBoolean("boot_supervisor",false));
		toggleboot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = pref.edit();
				editor.putBoolean("boot_supervisor",((ToggleButton)v).isChecked());
				editor.commit();
			}
		});
		setIP();
		setTime();
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		onNewIntent(getIntent());
	}
	@Override
	public void onNewIntent(final Intent intent) {
		setIntent(intent);
		supervisorState();
	}
	@Override
	protected void onResume() {
		super.onResume();
		if(togglerefresh.isChecked()) mRefreshTimer.register(4000);
	}
	@Override
	protected void onPause() {
		mRefreshTimer.unregister();
		super.onPause();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.supervisor,menu);
		supervisorState();
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				startActivity((new Intent(this,Main.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
			case R.id.menu_start:
				supervisorStart();
				return true;
			case R.id.menu_stop:
				supervisorStop();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void startActivity(Intent intent) {
		try {
			super.startActivity(intent);
		} catch(SecurityException ex) {}
	}
	protected void supervisorStart() {
		startService(new Intent(SupervisorActivity.this,SupervisorService.class));
		supervisorState(true);
	}
	protected void supervisorStop() {
		stopService(new Intent(SupervisorActivity.this,SupervisorService.class));
		supervisorState(false);
	}
	protected void supervisorState() {
		supervisorState(SupervisorService.isRunning());
	}
	protected void supervisorState(boolean enabled) {
		try {
			if(enabled) {
				mMenu.findItem(R.id.menu_start).setVisible(false);
				mMenu.findItem(R.id.menu_stop).setVisible(true);
			} else {
				mMenu.findItem(R.id.menu_start).setVisible(true);
				mMenu.findItem(R.id.menu_stop).setVisible(false);
			}
		} catch(NullPointerException ex) {}
	}
	public void setIP() {
		ArrayList<String> iplist = getLocalIpAddresses();
		if(iplist.isEmpty()) textiplist.setText("WARNING | no network connection");
		else {
			StringBuilder sb = new StringBuilder("IP");
			for(String ip: iplist) {
				sb.append(" | ");
				sb.append(ip);
			}
			textiplist.setText(sb.toString());
		}
	}
	public void setTime() {
		togglerefresh.setText("Updated\n"+android.text.format.DateFormat.format("kk:mm:ss",new Date()));
	}
	public ArrayList<String> getLocalIpAddresses() {
		ArrayList<String> iplist = new ArrayList<String>();
		try {
			NetworkInterface iface;
			Enumeration<InetAddress> ea;
			InetAddress addr;
			for(Enumeration<NetworkInterface> ei = NetworkInterface.getNetworkInterfaces(); ei.hasMoreElements();) {
				iface = ei.nextElement();
				for(ea = iface.getInetAddresses(); ea.hasMoreElements();) {
					addr = ea.nextElement();
					if((!addr.isLoopbackAddress())&&(addr.getAddress().length == 4)) iplist.add(addr.getHostAddress().toString());
				}
			}
		} catch(SocketException ex) {
			Log.wtf(BotBrewApp.TAG,ex);
		}
		return iplist;
	}
}