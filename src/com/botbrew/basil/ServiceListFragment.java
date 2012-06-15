package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockListFragment;

class ServiceListEntry implements Comparable {
	public static class Loader extends AsyncTaskLoader<ArrayList<ServiceListEntry>> {
		private ArrayList<ServiceListEntry> mData;
		private BotBrewApp mApplication;
		public Loader(Context ctx) {
			super(ctx);
			mApplication = (BotBrewApp)ctx.getApplicationContext();
		}
		@Override
		public void onStartLoading() {
			if(mData != null) deliverResult(mData);	// deliver loaded data
			else forceLoad(); // start AsyncTask
		}
		@Override
		public void deliverResult(ArrayList<ServiceListEntry> data) {
			mData = data;		// cache that stuff
			if(isStarted()) super.deliverResult(mData);	// deliver if loader wasn't stopped or canceled
		}
		@Override
		public void onReset() {
			cancelLoad();	// cancel old task
			mData = null;
		}
		@Override
		public void onCanceled(ArrayList<ServiceListEntry> data) {
			cancelLoad();	// cancel old task
			mData = null;
		}
		@Override
		public ArrayList<ServiceListEntry> loadInBackground() {	// called from AsyncTask
			final String root = mApplication.root();
			ArrayList<ServiceListEntry> data = new ArrayList<ServiceListEntry>();
			HashSet<String> done = new HashSet<String>();
			Pattern re_status = Pattern.compile("^([^\\:]+)\\: ([^\\:]+)");
			Matcher matcher;
			Shell.Pipe sh;
			String line;
			String name;
			String[] svcs = (new File(root,"etc/service")).list();
			if((svcs != null)&&(svcs.length > 0)) {
				try {
					final StringBuffer sb = new StringBuffer("sv status");
					for(String svc: svcs) {
						sb.append(" ");
						sb.append(svc);
					}
					sh = Shell.Pipe.getRootShell();
					sh.botbrew(root,sb.toString());
					sh.stdin().close();
					final BufferedReader p_stdout = new BufferedReader(new InputStreamReader(sh.stdout()));
					while((line = p_stdout.readLine()) != null) {
						matcher = re_status.matcher(line);
						if(matcher.find()) {
							name = matcher.group(2);
							done.add(name);
							data.add(new ServiceListEntry(name,matcher.group(1),line,true));
						}
					}
					p_stdout.close();
					BotBrewApp.sinkError(sh);
					sh.waitFor();
				} catch(IOException ex) {
				} catch(InterruptedException ex) {
				}
			}
			svcs = (new File(root,"etc/sv")).list();
			if(svcs != null) {
				String d_status = "off";
				String d_detail = "this service is disabled";
				for(String svc: svcs) {
					if((done.contains(svc))||("enabled".equals(svc))) continue;
					done.add(svc);
					data.add(new ServiceListEntry(svc,d_status,d_detail,false));
				}
			}
			Collections.sort(data);
			return data;
		}
	}
	public String name;
	public String status;
	public String detail;
	public boolean enabled;
	public ServiceListEntry(String name, String status, String detail, boolean enabled) {
		this.name = name;
		this.status = status;
		this.detail = detail;
		this.enabled = enabled;
	}
	@Override
	public int compareTo(Object o) {
		if(o instanceof ServiceListEntry) return name.compareTo(((ServiceListEntry)o).name);
		return 0;
	}
}

class ServiceListAdapter extends ArrayAdapter<ServiceListEntry> {
	private final LayoutInflater mInflater;
	private BotBrewApp mApplication;
	public ServiceListAdapter(Context context) {
		super(context,R.layout.service_list_item);
		mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mApplication = (BotBrewApp)context.getApplicationContext();
	}
	public void setData(ArrayList<ServiceListEntry> data) {
		clear();
		if(data != null) for(Iterator<ServiceListEntry> it = data.iterator(); it.hasNext();) add(it.next());
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if(convertView == null) view = mInflater.inflate(R.layout.service_list_item,parent, false);
		else view = convertView;
		final ServiceListEntry item = getItem(position);
		ToggleButton toggle = (ToggleButton)view.findViewById(R.id.service_toggle);
		toggle.setChecked(item.enabled);
		toggle.setText((item.enabled?(toggle.getTextOn()+"/"+item.status.toUpperCase()):toggle.getTextOff()));
		toggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean checked = ((ToggleButton)v).isChecked();
				(new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							final Shell.Pipe sh = Shell.Pipe.getRootShell().redirect();
							sh.botbrew(mApplication.root(),(checked?"svenable ":"svdisable ")+item.name);
							sh.stdin().close();
							BotBrewApp.sinkOutput(sh);
							sh.waitFor();
						} catch(IOException ex) {
						} catch(InterruptedException ex) {
						}
					}
				})).start();
			}
		});
		((TextView)view.findViewById(R.id.service_name)).setText(item.name);
		((TextView)view.findViewById(R.id.service_detail)).setText(item.detail);
		return view;
	}
}

public class ServiceListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<ArrayList<ServiceListEntry>> {
	private static final int LOADER_ID = 0x02;
	private ServiceListAdapter adapter;
	private BotBrewApp mApplication;
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApplication = (BotBrewApp)getActivity().getApplicationContext();
		setEmptyText("No services");
		adapter = new ServiceListAdapter(getActivity());
		registerForContextMenu(getListView());
		setListAdapter(adapter);
		setListShown(false);	// progress indicator
		getLoaderManager().initLoader(LOADER_ID,null,this);
	}
	/*@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}*/
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l,v,position,id);
		//getActivity().openContextMenu(l);
	}
	// LoaderManager.LoaderCallbacks<Cursor> methods
	@Override
	public Loader<ArrayList<ServiceListEntry>> onCreateLoader(int id, Bundle args) {
		return new ServiceListEntry.Loader(getActivity());
	}
	@Override
	public void onLoadFinished(Loader<ArrayList<ServiceListEntry>> loader, ArrayList<ServiceListEntry> data) {
		adapter.setData(data);
		if(isResumed()) setListShown(true);
		else setListShownNoAnimation(true);
	}
	@Override
	public void onLoaderReset(Loader<ArrayList<ServiceListEntry>> loader) {
		adapter.setData(null);
	}
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu,v,menuInfo);
		getActivity().getMenuInflater().inflate(R.menu.service_list_context,menu);
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		menu.setHeaderTitle("Service: "+((ServiceListEntry)adapter.getItem(info.position)).name);
	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		final String name = ((ServiceListEntry)adapter.getItem(info.position)).name;
		final String cmd;
		switch(item.getItemId()) {
			case R.id.menu_sv_up:
				cmd = "sv up ";
				break;
			case R.id.menu_sv_down:
				cmd = "sv down ";
				break;
			case R.id.menu_sv_once:
				cmd = "sv once ";
				break;
			case R.id.menu_sv_stop:
				cmd = "sv pause ";
				break;
			case R.id.menu_sv_cont:
				cmd = "sv cont ";
				break;
			case R.id.menu_sv_hup:
				cmd = "sv hup ";
				break;
			case R.id.menu_sv_alrm:
				cmd = "sv alarm ";
				break;
			case R.id.menu_sv_int:
				cmd = "sv interrupt ";
				break;
			case R.id.menu_sv_quit:
				cmd = "sv quit ";
				break;
			case R.id.menu_sv_usr1:
				cmd = "sv 1 ";
				break;
			case R.id.menu_sv_usr2:
				cmd = "sv 2 ";
				break;
			case R.id.menu_sv_term:
				cmd = "sv term ";
				break;
			case R.id.menu_sv_kill:
				cmd = "sv kill ";
				break;
			case R.id.menu_sv_exit:
				cmd = "sv exit ";
				break;
			default:
				return super.onContextItemSelected(item);
		}
		(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final Shell.Pipe sh = Shell.Pipe.getRootShell().redirect();
					sh.botbrew(mApplication.root(),cmd+name);
					sh.stdin().close();
					BotBrewApp.sinkOutput(sh);
					sh.waitFor();
				} catch(IOException ex) {
				} catch(InterruptedException ex) {
				}
			}
		})).start();
		return true;
	}
	public void refresh() {
		getLoaderManager().restartLoader(LOADER_ID,null,this);
	}
}