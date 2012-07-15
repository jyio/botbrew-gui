package com.botbrew.basil;

import java.io.File;
import java.util.List;
import java.util.Vector;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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
	private BotBrewApp mApplication;
	private PagerAdapter mPagerAdapter;
	private ViewPager mPager;
	private TextView freespace;
	private Handler mHandler = new Handler();
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
		fragmentlist.add(new RepairListFragment());
		mPagerAdapter = new PagerAdapter(getSupportFragmentManager(),fragmentlist);
		mPager = (ViewPager)findViewById(R.id.mainpager);
		mPager.setAdapter(mPagerAdapter);
		// initialize indicator
		TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.mainindicator);
		indicator.setViewPager(mPager);
		indicator.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				Fragment item = mPagerAdapter.getItem(mPager.getCurrentItem());
				if(item instanceof RepairListFragment) try {
					((RepairListFragment)item).refresh();
				} catch(IllegalStateException ex) {}
			}
		});
		ActionBar actionbar = getSupportActionBar();
		actionbar.setTitle("");
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		freespace = (TextView)findViewById(R.id.freespace);
		onNewIntent(getIntent());
	}
	@Override
	public void onNewIntent(final Intent intent) {
		if(IntentType.APP_EXIT.equals(intent)) {
			finish();
			return;
		} else if(IntentType.APP_RESTART.equals(intent)) {
			finish();
			startActivity((new Intent(this,getClass())).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			return;
		}
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if(!mApplication.checkInstall(new File(mApplication.root()),false)) {
			startActivity((new Intent(this,BootstrapActivity.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			finish();
			return;
		}
		setIntent(intent);
		String action = intent.getAction();
		if(Intent.ACTION_VIEW.equals(action)) {
			Uri data = intent.getData();
			if(data != null) {
				if("content".equals(data.getScheme())) {
					List<String> path = data.getPathSegments();
					startActivity((new Intent(this,PackageManagerActivity.class)).putExtra("command","info").putExtra("package",path.get(path.size()-1)));
				} else {	// maybe android.intent.category.BROWSABLE
					if((new File(data.getPath())).exists()) DebInstallDialogFragment.create(data).show(getSupportFragmentManager(),null);
				}
			}
		} else if(Intent.ACTION_SEARCH.equals(action)) {
			startActivity((new Intent(this,PackageManagerActivity.class)).putExtra("command","info").putExtra("package",intent.getStringExtra(SearchManager.QUERY)));
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
					mApplication.nativeInstall(new File(mApplication.root()));
				}
			} catch(PackageManager.NameNotFoundException ex) {}	// wtf
			if((firstrun)||(pref.getBoolean("interface_launch_webactivity",false))) {
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if(mApplication.isOnline()) startActivity(new Intent(Main.this,WebActivity.class));
					}
				},0);
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
			case R.id.menu_supervisor:
				startActivity(new Intent(this,SupervisorActivity.class));
				return true;
			case R.id.menu_control:
				startActivity(new Intent(this,ControlActivity.class));
				return true;
			case R.id.menu_clean:
				Toast.makeText(this,mApplication.clean()?"Archives cleaned.":"Archives already clean.",Toast.LENGTH_SHORT).show();
				return true;
			case R.id.menu_run:
				startActivity(new Intent(this,TerminalActivity.class));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onResume() {
		super.onResume();
		try {
			final String path = mApplication.root();
			freespace.setText(Formatter.formatFileSize(this,BotBrewApp.getFreeBytes(path))+"/"+Formatter.formatFileSize(this,BotBrewApp.getByteCount(path))+" free in "+path);
		} catch(RuntimeException ex) {
			freespace.setText("not bootstrapped");
		}
		conditionalRefresh(true);
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
	protected void onRefreshRequested(final boolean update) {
		final ProgressDialog pd = ProgressDialog.show(this,"Please wait...","Updating the package cache...");
		pd.setCancelable(false);
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final DebianPackageManager dpm = new DebianPackageManager(mApplication.root());
		(new AsyncTask<Void,Void,Boolean>() {
			@Override
			protected void onPreExecute() {
				mLocked = true;
			}
			@Override
			protected Boolean doInBackground(final Void... ign) {
				Log.v(BotBrewApp.TAG,"-> Main.onUpdateRequested("+update+")");
				if(update) dpm.pm_update();
				final boolean result = dpm.pm_refresh(getContentResolver(),update);
				Log.v(BotBrewApp.TAG,"<- Main.onUpdateRequested("+update+")");
				return result;
			}
			@Override
			protected void onCancelled(Boolean result) {
				mLocked = false;
				try {
					pd.dismiss();
				} catch(IllegalArgumentException ex) {
					Log.wtf(BotBrewApp.TAG,ex);
				}
			}
			@Override
			protected void onPostExecute(Boolean result) {
				final SharedPreferences.Editor editor = pref.edit();
				editor.putLong("var_dbChecksumSource",mApplication.checksumSource());
				editor.putLong("var_dbChecksumCache",mApplication.checksumCache());
				editor.commit();
				mLocked = false;
				try {
					pd.dismiss();
				} catch(IllegalArgumentException ex) {
					Log.wtf(BotBrewApp.TAG,ex);
				}
			}
		}).execute();
	}
	protected boolean conditionalRefresh(final boolean update) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		if((update)&&(mApplication.checksumSource() != pref.getLong("var_dbChecksumSource",-1))) {
			onRefreshRequested(true);
			return true;
		}
		if(mApplication.checksumCache() != pref.getLong("var_dbChecksumCache",-1)) {
			onRefreshRequested(false);
			return true;
		}
		return false;
	}
}
