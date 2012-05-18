package com.botbrew.basil;

import java.io.File;
import java.util.List;
import java.util.Vector;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

class PagerAdapter extends FragmentPagerAdapter implements TitleProvider {
	private final List<Fragment> fragmentlist;
	PagerAdapter(FragmentManager fm, List<Fragment> fragmentlist) {
		super(fm);
		this.fragmentlist = fragmentlist;
	}
	@Override
	public Fragment getItem(int position) {
		return fragmentlist.get(position);
	}
	@Override
	public int getCount() {
		return fragmentlist.size();
	}
	@Override
	public String getTitle(int position) {
		return ((TitleFragment)fragmentlist.get(position)).getTitle();
	}
}

public class Main extends SherlockFragmentActivity {
	private static final String TAG = "BBMain";
	private final Messenger mLocalMessenger = new Messenger(new EnumHandler<MessageType>(MessageType.class) {
		@Override
		public void handleMessage(MessageType msg) {
			switch(msg) {
				case MSG_SET_VALUE:
					break;
			}
		}
	});
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mRemoteMessenger = new Messenger(service);
			Log.v(TAG,"onServiceConnected("+className+")");
			try {
				Message msg = Message.obtain(null,MessageType.MSG_REGISTER_CLIENT.ordinal());
				msg.replyTo = mLocalMessenger;
				mRemoteMessenger.send(msg);
			} catch (RemoteException e) {
				// In this case the service has crashed before we could even do anything with it
			}
		}
		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been unexpectedly disconnected - process crashed.
			mRemoteMessenger = null;
			Log.v(TAG,"onServiceDisconnected("+className+")");
		}
	};
	private Messenger mRemoteMessenger = null;
	private BotBrewApp mApplication;
	private PagerAdapter mPagerAdapter;
	private ViewPager mPager;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mApplication = (BotBrewApp)getApplicationContext();
		List<Fragment> fragmentlist = new Vector<Fragment>();
		fragmentlist.add(new PackageListFragment.ListAvailable());
		fragmentlist.add(new PackageListFragment.ListInstalled());
		fragmentlist.add(new PackageListFragment.ListUpgradable());
		//fragmentlist.add(new RepairListFragment());
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager(),fragmentlist);
		mPager = (ViewPager)findViewById(R.id.mainpager);
		mPager.setAdapter(mPagerAdapter);
		// initialize indicator
		TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.mainindicator);
		indicator.setViewPager(mPager);
		/*indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				Fragment item = mPagerAdapter.getItem(mPager.getCurrentItem());
				if(item instanceof RepairListFragment) try {
					((RepairListFragment)item).refresh();
				} catch(IllegalStateException ex) {}
			}
		});*/
		ActionBar actionbar = getSupportActionBar();
		actionbar.setTitle("");
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		onNewIntent(getIntent());
	}
	@Override
	public void onNewIntent(final Intent intent) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		BotBrewApp.root = new File(pref.getString("var_root",BotBrewApp.default_root));
		if(!mApplication.isInstalled(BotBrewApp.root)) {
			startActivity((new Intent(this,BootstrapReadyActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			finish();
			return;
		}
		setIntent(intent);
		String action = intent.getAction();
		if(Intent.ACTION_VIEW.equals(action)) {
			Uri data = intent.getData();
			if(data != null) {
				if("content".equals(data.getScheme())) {
				//	List<String> path = data.getPathSegments();
				//	OpkgService.requestOpkgInfo(this,path.get(path.size()-1));
				} else {	// maybe android.intent.category.BROWSABLE
				//	verifyChecksum();
				//	OpkgService.requestOpkgInstallURL(this,data);
				}
			}
		} else if(Intent.ACTION_SEARCH.equals(action)) {
		//	verifyChecksum();
		//	OpkgService.requestOpkgInfo(this,intent.getStringExtra(SearchManager.QUERY));
		} else {
		//	verifyChecksum();
		}
		if(Intent.ACTION_MAIN.equals(action)) {
			boolean firstrun = false;
			try {
				long version = getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_META_DATA).versionCode;
				firstrun = pref.getLong("var_lastversion",0) < version;
				if(firstrun) {
					SharedPreferences.Editor editor = pref.edit();
					editor.putLong("var_lastversion",version);
					editor.commit();
				}
			} catch(PackageManager.NameNotFoundException ex) {}	// wtf
			if((firstrun)||(pref.getBoolean("interface_launch_webactivity",false))) {
				if(mApplication.isOnline()) startActivity(new Intent(this,WebActivity.class));
			}
		}
	}
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater menuInflater = getSupportMenuInflater();
		menuInflater.inflate(R.menu.main,menu);
		return super.onCreateOptionsMenu(menu);
	}
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				startActivity(new Intent(this,WebActivity.class));
				return true;
			case R.id.menu_refresh:
				onRefreshRequested(true);
				return true;
			case R.id.menu_search:
				onSearchRequested();
				return true;
			case R.id.menu_control:
				startActivity(new Intent(this,ControlActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onResume() {
		super.onResume();
		bindService(new Intent(this,ControllerService.class),mConnection,BIND_AUTO_CREATE);
		if(BotBrewApp.root != null) verifyChecksum();
	}
	@Override
	public void onPause() {
		unbindService(mConnection);
		mRemoteMessenger = null;
		super.onPause();
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
	protected void onRefreshRequested(final boolean update) {
		if(BotBrewApp.root == null) return;
		final ProgressDialog pd = ProgressDialog.show(this,"Please wait...","Updating the package cache...");
		pd.setCancelable(false);
		(new AsyncTask<Void,Void,Boolean>() {
			@Override
			protected Boolean doInBackground(final Void... ign) {
				mLocked = true;
				Log.v(TAG,"-> onUpdateRequested("+update+")");
				final DebianPackageManager dpm = new DebianPackageManager(BotBrewApp.root.getAbsolutePath());
				if(update) dpm.pm_update();
				final boolean result = dpm.pm_refresh(getContentResolver());
				Log.v(TAG,"<- onUpdateRequested("+update+")");
				return result;
			}
			@Override
			protected void onCancelled(Boolean result) {
				mLocked = false;
				pd.dismiss();
			}
			@Override
			protected void onPostExecute(Boolean result) {
				mLocked = false;
				pd.dismiss();
			}
		}).execute();
	}
	protected boolean verifyChecksum() {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final long dbchecksum = BotBrewApp.getChecksum();
		if(dbchecksum != pref.getLong("var_dbchecksum",-1)) {
			SharedPreferences.Editor editor = pref.edit();
			editor.putLong("var_dbchecksum",dbchecksum);
			editor.commit();
			onRefreshRequested(false);
			return true;
		} else return false;
	}
}
