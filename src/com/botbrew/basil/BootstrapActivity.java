package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapActivity extends SherlockFragmentActivity {
	public static class DialogState {
		public static enum DialogType {
			DIALOG_NONE,
			DIALOG_DOWNLOAD,
			DIALOG_INSTALL,
			DIALOG_REBASE,
			DIALOG_SETUP
		}
		public final DialogType dialog;
		public final CharSequence path;
		public final boolean loop;
		public static final DialogState NONE = new DialogState(DialogType.DIALOG_NONE);
		public DialogState() {
			this(DialogType.DIALOG_NONE,BotBrewApp.default_root,false);
		}
		public DialogState(final DialogType dialog) {
			this(dialog,BotBrewApp.default_root,false);
		}
		public DialogState(final DialogType dialog, final CharSequence path, final boolean loop) {
			this.dialog = dialog;
			this.path = path;
			this.loop = loop;
		}
	}
	public static class DownloadDialogFragment extends SherlockDialogFragment {
		public DownloadDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_download_dialog_fragment,container);
			final BootstrapActivity activity = (BootstrapActivity)getActivity();
			final Dialog dialog = getDialog();
			final CharSequence path = getArguments().getCharSequence("path");
			final boolean loop = getArguments().getBoolean("loop",false);
			final CharSequence codename = getResources().getText(R.string.app_codename);
			((Button)view.findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					activity.showDownload(path,loop);
				}
			});
			final String name = loop?"img.zip":"pkg.zip";
			final String src = "http://repo.botbrew.com/"+codename+"/bootstrap/"+name;
			((TextView)view.findViewById(R.id.url)).setText(src);
			final ProgressBar progressbar = (ProgressBar)view.findViewById(R.id.progress);
			progressbar.setIndeterminate(false);
			(new AsyncTask<Void,Integer,Boolean>() {
				@Override
				protected void onPreExecute() {
					setCancelable(false);
				}
				@Override
				protected Boolean doInBackground(final Void... params) {
					try {
						final File dst = new File(activity.getCacheDir(),name);
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
					setCancelable(true);
				}
				@Override
				protected void onPostExecute(Boolean result) {
					if(!result) {
						onCancelled(result);
						return;
					}
					dialog.dismiss();
					activity.showInstall(path,loop);
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
			dialog.setTitle("Downloading...");
			return view;
		}
	}
	public static class InstallDialogFragment extends SherlockDialogFragment {
		public InstallDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_install_dialog_fragment,container);
			final BootstrapActivity activity = (BootstrapActivity)getActivity();
			final Dialog dialog = getDialog();
			final String path = getArguments().getCharSequence("path").toString();
			final boolean loop = getArguments().getBoolean("loop",false);
			((Button)view.findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					activity.showInstall(path,loop);
				}
			});
			final File archive = new File(activity.getCacheDir(),loop?"img.zip":"pkg.zip");
			final String cmd = archive.getAbsolutePath();
			try {
				final BotBrewApp app = (BotBrewApp)activity.getApplicationContext();
				app.unmount();
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
				emulatorview.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
					@Override
					public boolean onSingleTapUp(MotionEvent e) {
						app.doToggleSoftKeyboard();
						return true;
					}
				});
				(new AsyncTask<Void,Void,Integer>() {
					@Override
					protected void onPreExecute() {
						setCancelable(false);
					}
					@Override
					protected Integer doInBackground(final Void... ign) {
						try {
							int res = sh.waitFor();
							archive.delete();
							app.nativeInstall(new File(path));
							return res;
						} catch(InterruptedException ex) {
						}
						return -1;
					}
					@Override
					protected void onCancelled(Integer result) {
						view.findViewById(R.id.fail).setVisibility(View.VISIBLE);
						view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
						setCancelable(true);
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
						DebianPackageManager.pm_writeconf(activity);
						dialog.dismiss();
						activity.showSetup();
					}
				}).execute();
			} catch(IOException ex) {
				view.findViewById(R.id.fail).setVisibility(View.VISIBLE);
				view.findViewById(R.id.retry).setVisibility(View.VISIBLE);
			}
			dialog.setTitle("Installing...");
			dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
			return view;
		}
	}
	public static class RebaseDialogFragment extends SherlockDialogFragment {
		public RebaseDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_rebase_dialog_fragment,container);
			final BootstrapActivity activity = (BootstrapActivity)getActivity();
			final Dialog dialog = getDialog();
			final String path = getArguments().getCharSequence("path").toString();
			final boolean loop = getArguments().getBoolean("loop",false);
			((TextView)view.findViewById(R.id.location)).setText(path);
			((Button)view.findViewById(R.id.reinstall)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					activity.showDownload(path,loop);
				}
			});
			((Button)view.findViewById(R.id.setdefault)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final BotBrewApp app = (BotBrewApp)activity.getApplicationContext();
					app.unmount();
					app.nativeInstall(new File(path));
					final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
					final SharedPreferences.Editor editor = pref.edit();
					editor.putString("var_root",path);
					editor.remove("var_dbChecksumCache");
					editor.commit();
					DebianPackageManager.pm_writeconf(activity);
					dialog.dismiss();
					activity.showSetup();
				}
			});
			dialog.setTitle("Whoa there...");
			return view;
		}
	}
	public static class SetupDialogFragment extends SherlockDialogFragment {
		static enum SetupScriptType {
			SETUP_BOTBREW_WRAPPER("use BotBrew on the command line",R.raw.setup_botbrew_wrapper),
			SETUP_DEBIAN_REPOSITORY(null,R.raw.setup_debian_repository),
			SETUP_DEBIAN_APT("use the dpkg/APT system",R.raw.setup_debian_apt,SETUP_DEBIAN_REPOSITORY),
			SETUP_DEBIAN_PYTHON("run Python programs",R.raw.setup_debian_python,SETUP_DEBIAN_APT),
			SETUP_DEBIAN_MINIMAL("install a minimal Debian",R.raw.setup_debian_minimal,SETUP_DEBIAN_APT);
			public final String name;
			public final int resource;
			public final LinkedHashSet<SetupScriptType> dependencies = new LinkedHashSet<SetupScriptType>();
			SetupScriptType(final String name, final int resource, final SetupScriptType... dependencies) {
				this.name = name;
				this.resource = resource;
				for(SetupScriptType d: dependencies) this.dependencies.add(d);
			}
			public LinkedHashSet<SetupScriptType> resolve() {
				final LinkedHashSet<SetupScriptType> res = new LinkedHashSet<SetupScriptType>();
				for(SetupScriptType d: dependencies) res.addAll(d.resolve());
				res.add(this);
				return res;
			}
		}
		public SetupDialogFragment() {
		}
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			final View view = inflater.inflate(R.layout.bootstrap_activity_setup_dialog_fragment,container);
			final BootstrapActivity activity = (BootstrapActivity)getActivity();
			final Dialog dialog = getDialog();
			((Button)view.findViewById(R.id.ok)).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					activity.showDone();
				}
			});
			final ArrayList<String> scriptlist = new ArrayList<String>();
			final HashMap<String,SetupScriptType> scriptmap = new HashMap<String,SetupScriptType>();
			for(SetupScriptType setupscript: SetupScriptType.values()) if(setupscript.name != null) {
				scriptlist.add(setupscript.name);
				scriptmap.put(setupscript.name,setupscript);
			}
			ListView listview = (ListView)view.findViewById(R.id.scriptlist);
			listview.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_list_item_1,scriptlist));
			listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				public void onItemClick(AdapterView<?> l, View v, int position, long id) {
					BufferedReader reader;
					String line;
					final SetupScriptType item = scriptmap.get((String)l.getAdapter().getItem(position));
					final StringBuilder sb = new StringBuilder("#!/bin/sh\n");
					for(SetupScriptType setupscript: item.resolve()) {
						reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(setupscript.resource)));
						try {
							while((line = reader.readLine()) != null) {
								sb.append(line);
								sb.append("\n");
							}
							reader.close();
						} catch(IOException ex) {
						}
					}
					final File temp = new File(getActivity().getCacheDir(),"setup.sh");
					try {
						temp.delete();
						final FileWriter tempwriter = new FileWriter(temp);
						tempwriter.write(sb.toString());
						tempwriter.close();
						temp.setExecutable(true);
					} catch(IOException ex) {
						return;
					}
					final Bundle b = new Bundle();
					b.putBoolean("superuser",true);
					b.putCharSequence("command",temp.getAbsolutePath());
					final DialogFragment frag = new TerminalDialogFragment();
					frag.setArguments(b);
					frag.show(getActivity().getSupportFragmentManager(),null);
				}
			});
			dialog.setTitle("Setup");
			return view;
		}
		@Override
		public void onCancel(DialogInterface dialog) {
			super.onCancel(dialog);
			((BootstrapActivity)getActivity()).showDone();
		}
	}
	private static final String STR_CUSTOM = "custom";
	private static final int REQ_BOOTSTRAP = 1;
	private BotBrewApp mApplication;
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
		mApplication = (BotBrewApp)getApplicationContext();
	}
	@Override
	public void onResume() {
		super.onResume();
		DialogState dialogstate = mApplication.mBootstrapDialogState;
		switch(dialogstate.dialog) {
			case DIALOG_DOWNLOAD:
				showDownload(dialogstate.path,dialogstate.loop);
				break;
			case DIALOG_INSTALL:
				showInstall(dialogstate.path,dialogstate.loop);
				break;
			case DIALOG_REBASE:
				showRebase(dialogstate.path,dialogstate.loop);
				break;
			case DIALOG_SETUP:
				showSetup();
				break;
		}
		mApplication.mBootstrapDialogState = DialogState.NONE;
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
	public void showDownload(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new DownloadDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		mApplication.mBootstrapDialogState = DialogState.NONE;
		try {
			frag.show(getSupportFragmentManager(),null);
		} catch(IllegalStateException ex) {
			mApplication.mBootstrapDialogState = new DialogState(DialogState.DialogType.DIALOG_DOWNLOAD,path,loop);
		}
	}
	public void showInstall(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new InstallDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		mApplication.mBootstrapDialogState = DialogState.NONE;
		try {
			frag.show(getSupportFragmentManager(),null);
		} catch(IllegalStateException ex) {
			mApplication.mBootstrapDialogState = new DialogState(DialogState.DialogType.DIALOG_INSTALL,path,loop);
		}
	}
	public void showRebase(final CharSequence path, final boolean loop) {
		final DialogFragment frag = new RebaseDialogFragment();
		frag.setArguments(bundleArguments(path,loop));
		mApplication.mBootstrapDialogState = DialogState.NONE;
		try {
			frag.show(getSupportFragmentManager(),null);
		} catch(IllegalStateException ex) {
			mApplication.mBootstrapDialogState = new DialogState(DialogState.DialogType.DIALOG_REBASE,path,loop);
		}
	}
	public void showSetup() {
		final DialogFragment frag = new SetupDialogFragment();
		mApplication.mBootstrapDialogState = DialogState.NONE;
		try {
			frag.show(getSupportFragmentManager(),null);
		} catch(IllegalStateException ex) {
			mApplication.mBootstrapDialogState = new DialogState(DialogState.DialogType.DIALOG_SETUP);
		}
	}
	public void showDone() {
		mApplication.mBootstrapDialogState = DialogState.NONE;
		startActivity(IntentType.APP_RESTART.intent(this,Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
		finish();
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