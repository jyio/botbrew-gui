package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapActivity extends SherlockFragmentActivity {
	public class DownloadDialogFragment extends SherlockDialogFragment {
		public DownloadDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_download_dialog_fragment,container);
			final CharSequence path = getArguments().getCharSequence("path");
			final boolean loop = getArguments().getBoolean("loop",false);
			final CharSequence codename = getResources().getText(R.string.app_codename);
			((Button)view.findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getDialog().dismiss();
					showDownload(path,loop);
				}
			});
			final String name = loop?"img.zip":"pkg.zip";
			final String src = "http://repo.botbrew.com/"+codename+"/bootstrap/"+name;
			((TextView)view.findViewById(R.id.url)).setText(src);
			final ProgressBar progressbar = (ProgressBar)view.findViewById(R.id.progress);
			progressbar.setIndeterminate(false);
			(new AsyncTask<Void,Integer,Boolean>() {
				@Override
				protected Boolean doInBackground(final Void... params) {
					getDialog().setCancelable(false);
					try {
						final File dst = new File(getCacheDir(),name);
						publishProgress(0);
						Log.v(BotBrewApp.TAG,"now downloading "+src+" to"+dst);
						fetch(new URL(src),dst);
						return true;
					} catch(MalformedURLException ex) {
						Log.wtf(BotBrewApp.TAG,ex);
					} catch(IOException ex) {
						Log.wtf(BotBrewApp.TAG,ex);
					}
					return false;
				}
				@Override
				protected void onProgressUpdate(final Integer... progress) {
					progressbar.setProgress(progress[0]);
				}
				@Override
				protected void onCancelled(Boolean result) {
					view.findViewById(R.id.fail).setVisibility(View.VISIBLE);
					view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
					getDialog().setCancelable(true);
				}
				@Override
				protected void onPostExecute(Boolean result) {
					if(!result) {
						onCancelled(result);
						return;
					}
					getDialog().dismiss();
					showInstall(path,loop);
				}
				protected void fetch(final URL fremote, final File flocal) throws IOException {
					try {
						flocal.delete();
					} catch(SecurityException ex) {}
					int count;
					URLConnection c = fremote.openConnection();
					c.connect();
					long length = c.getContentLength();
					BufferedInputStream remote = new BufferedInputStream(fremote.openStream());
					FileOutputStream local = new FileOutputStream(flocal);
					byte[] data = new byte[1024];
					long total = 0;
					while((count = remote.read(data)) != -1) {
						total += count;
						publishProgress((int)(100*total/length));
						local.write(data,0,count);
					}
					local.flush();
					local.close();
					remote.close();
				}
			}).execute();
			getDialog().setTitle("Downloading...");
			return view;
		}
	}
	public class InstallDialogFragment extends SherlockDialogFragment {
		public InstallDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_install_dialog_fragment,container);
			final String path = getArguments().getCharSequence("path").toString();
			final boolean loop = getArguments().getBoolean("loop",false);
			((Button)view.findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getDialog().dismiss();
					showInstall(path,loop);
				}
			});
			final File archive = new File(getCacheDir(),loop?"img.zip":"pkg.zip");
			final String cmd = archive.getAbsolutePath();
			try {
				((BotBrewApp)getApplicationContext()).unmount(BotBrewApp.root);
				final Shell.Term sh = Shell.Term.getRootShell();
				final OutputStream sh_stdin = sh.stdin();
				final TermSession termsession = new TermSession();
				termsession.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
				termsession.setTermOut(sh_stdin);
				termsession.setTermIn(sh.stdout());
				sh_stdin.write(("set -e\n").getBytes());
				sh_stdin.write(("chmod 0755 '"+cmd+"'\n").getBytes());
				for(String mkdir: mkdir_p(new File(path))) sh_stdin.write(("mkdir '"+mkdir+"'\n").getBytes());
				sh_stdin.write(("cd '"+path+"'\n").getBytes());
				sh_stdin.write(("'"+cmd+"' -n\n").getBytes());
				sh_stdin.write(("exit\n").getBytes());
				sh_stdin.close();
				final EmulatorView emulatorview = (EmulatorView)view.findViewById(R.id.emulator);
				emulatorview.attachSession(termsession);
				emulatorview.setDensity(getResources().getDisplayMetrics());
				emulatorview.setTextSize(16);
				final BotBrewApp app = (BotBrewApp)getApplicationContext();
				emulatorview.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						app.doToggleSoftKeyboard();
						return true;
					}
				});
				(new AsyncTask<Void,Void,Integer>() {
					@Override
					protected Integer doInBackground(final Void... ign) {
						getDialog().setCancelable(false);
						try {
							int res = sh.waitFor();
							archive.delete();
							((BotBrewApp)getApplicationContext()).nativeInstall(new File(path));
							return res;
						} catch(InterruptedException ex) {
						}
						return -1;
					}
					@Override
					protected void onCancelled(Integer result) {
						view.findViewById(R.id.fail).setVisibility(View.VISIBLE);
						view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
						getDialog().setCancelable(true);
					}
					@Override
					protected void onPostExecute(Integer result) {
						if(result.intValue() != 0) {
							onCancelled(result);
							return;
						}
						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
						SharedPreferences.Editor editor = pref.edit();
						editor.putString("var_root",path);
						editor.remove("var_dbChecksumCache");
						editor.commit();
						getDialog().dismiss();
						startActivity(new Intent(getActivity(),Main.class));
						getActivity().finish();
					}
				}).execute();
			} catch(IOException ex) {
				view.findViewById(R.id.fail).setVisibility(View.VISIBLE);
				view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
			}
			getDialog().setTitle("Installing...");
			getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			return view;
		}
	}
	public class RebaseDialogFragment extends SherlockDialogFragment {
		public RebaseDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_rebase_dialog_fragment,container);
			final String path = getArguments().getCharSequence("path").toString();
			final boolean loop = getArguments().getBoolean("loop",false);
			((TextView)view.findViewById(R.id.location)).setText(path);
			((Button)view.findViewById(R.id.reinstall)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					getDialog().dismiss();
					showDownload(path,loop);
				}
			});
			((Button)view.findViewById(R.id.setdefault)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final BotBrewApp app = (BotBrewApp)getApplicationContext();
					app.unmount(BotBrewApp.root);
					app.nativeInstall(new File(path));
					final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
					final SharedPreferences.Editor editor = pref.edit();
					editor.putString("var_root",path);
					editor.remove("var_dbChecksumCache");
					editor.commit();
					getDialog().dismiss();
					startActivity(IntentType.APP_RESTART.intent(getActivity(),Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
					getActivity().finish();
				}
			});
			getDialog().setTitle("Whoa there...");
			return view;
		}
	}
	private static final String STR_CUSTOM = "custom";
	private static final int REQ_BOOTSTRAP = 1;
	private Spinner vLocation;
	private final ArrayList<String> mLocations = new ArrayList<String>();
	private String mLocation = null;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootstrap_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(true);
		vLocation = (Spinner)findViewById(R.id.location);
		mLocations.clear();
		final String filename_default = getResources().getText(R.string.filename_default).toString();
		for(String prefix: new String[] {"/data","/sd-ext","/cache","/emmc","/sdcard","/usbdisk"}) try {
			if((new File(prefix)).isDirectory()) mLocations.add((new File(prefix,filename_default)).getCanonicalPath());
		} catch(IOException ex) {}
		mLocations.add(STR_CUSTOM);
		mLocation = mLocations.get(0);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mLocations);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		vLocation.setAdapter(adapter);
		vLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				final String file = (String)parent.getItemAtPosition(position);
				if(STR_CUSTOM.equals(file))
					startActivityForResult(new Intent(BootstrapActivity.this,ExplorerActivity.class).putExtra("file","/"),REQ_BOOTSTRAP);
				else mLocation = file;
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		((Button)findViewById(R.id.proceed)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String file = (String)vLocation.getSelectedItem();
				if(!STR_CUSTOM.equals(file)) {
					final File path = new File(file);
					boolean loop = false;
					try {
						final MountFs.MountEntry mntent = MountFs.find(path);
						if((mntent != null)&&("vfat".equals(mntent.fs_vfstype))) loop = true;
					} catch(FileNotFoundException ex) {}
					boolean rebase = (new File(path,"botbrew")).exists();
					if((!rebase)&&(loop)&&((new File(path,"fs.img")).exists())) rebase = true;
					try {
						if(rebase) showRebase(path.getCanonicalPath(),loop);
						else showDownload(path.getCanonicalPath(),loop);
					} catch(IOException ex) {
						if(rebase) showRebase(path.getAbsolutePath(),loop);
						else showDownload(path.getAbsolutePath(),loop);
					}
				}
			}
		});
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
			case REQ_BOOTSTRAP:
				if(resultCode == Activity.RESULT_OK) {
					final String file = data.getStringExtra("file");
					final int idx = mLocations.indexOf(file);
					if(idx < 0) {
						mLocations.add(0,file);
						((ArrayAdapter<String>)vLocation.getAdapter()).notifyDataSetChanged();
						vLocation.setSelection(0,true);
					} else vLocation.setSelection(idx,true);
				} else if(resultCode == Activity.RESULT_CANCELED) vLocation.setSelection(mLocations.indexOf(mLocation),true);
				break;
			default:
				super.onActivityResult(requestCode,resultCode,data);
		}
	}
	protected Bundle bundleArguments(final CharSequence path, final boolean loop) {
		final Bundle b = new Bundle();
		b.putCharSequence("path",path);
		b.putBoolean("loop",loop);
		return b;
	}
	protected void showDownload(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new DownloadDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		frag.show(getSupportFragmentManager(),null);
	}
	protected void showInstall(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new InstallDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		frag.show(getSupportFragmentManager(),null);
	}
	protected void showRebase(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new RebaseDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		frag.show(getSupportFragmentManager(),null);
	}
	public static List<String> mkdir_p(final File path) {
		if(path.exists()) return new ArrayList<String>();
		else {
			final List<String> paths = mkdir_p(path.getParentFile());
			paths.add(path.getAbsolutePath());
			return paths;
		}
	}
}