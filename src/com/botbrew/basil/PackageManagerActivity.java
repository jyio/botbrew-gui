package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class PackageManagerActivity extends SherlockFragmentActivity {
	protected static enum TransactionType {
		APTGET_INSTALL,
		APTGET_REINSTALL,
		APTGET_UPGRADE,
		APTGET_DISTUPGRADE,
		APTGET_REMOVE,
		APTGET_AUTOREMOVE
	}
	private BotBrewApp mApp;
	private ActionBar mActionBar;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.package_manager_activity);
		mActionBar = getSupportActionBar();
		mActionBar.setHomeButtonEnabled(true);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setDisplayUseLogoEnabled(true);
		mApp = (BotBrewApp)getApplicationContext();
		final Intent intent = getIntent();
		final CharSequence command = intent.getCharSequenceExtra("command");
		final CharSequence pkg = intent.getCharSequenceExtra("package");
		if("info".equals(command)) doInfo(pkg);
		else if("install".equals(command)) doAptGet(TransactionType.APTGET_INSTALL,pkg);
		else if("reinstall".equals(command)) doAptGet(TransactionType.APTGET_REINSTALL,pkg);
		else if("remove".equals(command)) doAptGet(TransactionType.APTGET_REMOVE,pkg);
		else if("autoremove".equals(command)) doAptGet(TransactionType.APTGET_AUTOREMOVE,pkg);
		else if("upgrade".equals(command)) doAptGet(TransactionType.APTGET_DISTUPGRADE,pkg);
		else if("installdeb".equals(command)) doDpkgInstall(pkg);
		else finish();
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case android.R.id.home:
				if(!mLocked) startActivity((new Intent(this,Main.class)).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
	public void setViewFrame(Object obj) {
		final ViewGroup layout = (ViewGroup)findViewById(R.id.viewframe);
		layout.removeAllViews();
		if(obj instanceof TermSession) {
			final EmulatorView emulator = new EmulatorView(this,(TermSession)obj,getResources().getDisplayMetrics());
			emulator.setTextSize(16);
			emulator.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onSingleTapUp(MotionEvent e) {
					mApp.doToggleSoftKeyboard();
					return true;
				}
			});
			layout.addView(emulator);
		} else if(obj instanceof CharSequence) {
			final ScrollView wrapper = new ScrollView(this);
			final TextView tv = new TextView(this,null,android.R.attr.textAppearanceMedium);
			tv.setTypeface(Typeface.MONOSPACE);
			tv.setText((CharSequence)obj);
			wrapper.addView(tv);
			layout.addView(wrapper);
		}
	}
	protected Button buttonOnClick(final Button v, final View.OnClickListener l) {
		v.setVisibility(View.VISIBLE);
		v.setOnClickListener(l);
		return v;
	}
	public void doInfo(final CharSequence pkg) {
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		(new AsyncTask<Void,CharSequence,Integer>() {
			@Override
			protected void onPreExecute() {
				mLocked = true;
			}
			@Override
			protected Integer doInBackground(final Void... ign) {
				try {
					final Shell sh = Shell.Pipe.getUserShell().redirect();
					sh.botbrew(root,dpm.aptcache_show(pkg));
					sh.stdin().close();
					String line;
					final StringBuilder sb = new StringBuilder();
					BufferedReader reader = new BufferedReader(new InputStreamReader(sh.stdout()));
					while((line = reader.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}
					reader.close();
					publishProgress(sb.toString());
					return sh.waitFor();
				} catch(IOException ex) {
				} catch(InterruptedException ex) {
				}
				return -1;
			}
			@Override
			protected void onProgressUpdate(CharSequence... progress) {
				setViewFrame(progress[progress.length-1]);
			}
			@Override
			protected void onCancelled(Integer result) {
				buttonOnClick((Button)findViewById(R.id.retry),new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
						startActivity(getIntent());
					}
				});
				mLocked = false;
			}
			@Override
			protected void onPostExecute(Integer result) {
				if(result.intValue() != 0) {
					onCancelled(result);
					return;
				}
				mActionBar.setTitle("Package "+pkg);
				// TODO: multiarch
				Cursor cursor = getContentResolver().query(
					PackageCacheProvider.ContentUri.CACHE_BASE.uri,
					new String[] {DatabaseOpenHelper.C_NAME+" AS _id",DatabaseOpenHelper.C_INSTALLED,DatabaseOpenHelper.C_UPGRADABLE},
					DatabaseOpenHelper.C_NAME+"=?",new String[] {pkg.toString()},null
				);
				if(cursor.getCount() < 1) return;
				cursor.moveToFirst();
				final String installed = cursor.getString(1);
				final String upgradable = cursor.getString(2);
				cursor.close();
				final Intent intent = (new Intent(PackageManagerActivity.this,PackageManagerActivity.class)).putExtra("package",pkg);
				if("".equals(installed)) {
					buttonOnClick((Button)findViewById(R.id.install),new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
							startActivity(intent.putExtra("command","install"));
						}
					});
				} else {
					if("".equals(upgradable)) {
						buttonOnClick((Button)findViewById(R.id.reinstall),new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								finish();
								startActivity(intent.putExtra("command","reinstall"));
							}
						});
					} else {
						buttonOnClick((Button)findViewById(R.id.upgrade),new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								finish();
								startActivity(intent.putExtra("command","upgrade"));
							}
						});
					}
					buttonOnClick((Button)findViewById(R.id.remove),new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
							startActivity(intent.putExtra("command","remove"));
						}
					});
					buttonOnClick((Button)findViewById(R.id.autoremove),new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
							startActivity(intent.putExtra("command","autoremove"));
						}
					});
				}
				mLocked = false;
			}
		}).execute();
	}
	public void doAptGet(final TransactionType what, final CharSequence... pkg) {
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		Shell sh = null;
		try {
			// TODO: multiple package names in title
			switch(what) {
				case APTGET_INSTALL:
					mActionBar.setTitle("Install "+pkg[0]);
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_install(pkg));
					break;
				case APTGET_REINSTALL:
					mActionBar.setTitle("Reinstall "+pkg[0]);
					dpm.config(DebianPackageManager.Config.APT_Get_ReInstall,"1");
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_install(pkg));
					break;
				case APTGET_UPGRADE:
					mActionBar.setTitle("Upgrade "+pkg[0]);
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_upgrade(pkg));
					break;
				case APTGET_DISTUPGRADE:
					mActionBar.setTitle("Dist-Upgrade "+pkg[0]);
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_distupgrade(pkg));
					break;
				case APTGET_REMOVE:
					mActionBar.setTitle("Remove "+pkg[0]);
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_remove(pkg));
					break;
				case APTGET_AUTOREMOVE:
					mActionBar.setTitle("Autoremove "+pkg[0]);
					sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.aptget_autoremove(pkg));
					break;
			}
			if(sh == null) return;
			mLocked = true;
			final InputStream sh_stdout = sh.stdout();
			while(sh_stdout.read() != '\n');
			while(sh_stdout.read() != '\n');
			final TermSession term = new TermSession();
			term.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
			term.setTermOut(sh.stdin());
			term.setTermIn(sh.stdout());
			setViewFrame(term);
			final Shell fsh = sh;
			(new AsyncTask<Void,Void,Integer>() {
				@Override
				protected Integer doInBackground(final Void... ign) {
					try {
						return fsh.waitFor();
					} catch(InterruptedException ex) {
						return -1;
					}
				}
				@Override
				protected void onCancelled(Integer result) {
					buttonOnClick((Button)findViewById(R.id.retry),new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
							startActivity(getIntent());
						}
					});
					mLocked = false;
				}
				@Override
				protected void onPostExecute(Integer result) {
					if(result.intValue() != 0) {
						onCancelled(result);
						return;
					}
					buttonOnClick((Button)findViewById(R.id.ok),new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							finish();
						}
					});
					mLocked = false;
				}
			}).execute();
		} catch(IOException ex) {
		}
	}
	public void doDpkgInstall(final CharSequence... pkg) {
		PreferenceManager.setDefaultValues(this,R.xml.preference,false);
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		final String root = (new File(pref.getString("var_root",BotBrewApp.default_root))).getAbsolutePath();
		final DebianPackageManager dpm = new DebianPackageManager(root);
		mLocked = true;
		final TermSession term0 = new TermSession();
		final TermSession term1 = new TermSession();
		// TODO: multiple file names in title
		mActionBar.setTitle("Install "+(new File(pkg[0].toString())).getName());
		(new AsyncTask<Void,TermSession,Integer>() {
			@Override
			protected Integer doInBackground(final Void... ign) {
				try {
					Shell sh = Shell.Term.getRootShell();
					sh.botbrew(root,dpm.dpkg_install(pkg));
					InputStream sh_stdout = sh.stdout();
					while(sh_stdout.read() != '\n');
					while(sh_stdout.read() != '\n');
					term0.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
					term0.setTermOut(sh.stdin());
					term0.setTermIn(sh.stdout());
					publishProgress(term0);
					if(sh.waitFor() == 0) return 0;
					sh = Shell.Term.getRootShell();
					dpm.config(DebianPackageManager.Config.APT_Get_FixBroken,"1");
					sh.botbrew(root,dpm.aptget_install());
					sh_stdout = sh.stdout();
					while(sh_stdout.read() != '\n');
					term1.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
					term1.setTermOut(sh.stdin());
					term1.setTermIn(sh.stdout());
					publishProgress(term1);
					return sh.waitFor();
				} catch (IOException e) {
					return -1;
				} catch(InterruptedException ex) {
					return -1;
				}
			}
			@Override
			protected void onProgressUpdate(TermSession... progress) {
				setViewFrame(progress[progress.length-1]);
			}
			@Override
			protected void onCancelled(Integer result) {
				buttonOnClick((Button)findViewById(R.id.retry),new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
						startActivity(getIntent());
					}
				});
				mLocked = false;
			}
			@Override
			protected void onPostExecute(Integer result) {
				if(result.intValue() != 0) {
					onCancelled(result);
					return;
				}
				buttonOnClick((Button)findViewById(R.id.ok),new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						finish();
					}
				});
				mLocked = false;
			}
		}).execute();
	}
}