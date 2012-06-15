package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.botbrew.basil.Shell.Pipe;

class RepairListLoader extends AsyncTaskLoader<ArrayList<String>> {
	private ArrayList<String> mData;
	private BotBrewApp mApplication;
	public RepairListLoader(Context ctx) {
		super(ctx);
		mApplication = (BotBrewApp)ctx.getApplicationContext();
	}
	@Override
	public void onStartLoading() {
		if(mData != null) deliverResult(mData);	// deliver loaded data
		else forceLoad(); // start AsyncTask
	}
	@Override
	public void deliverResult(ArrayList<String> data) {
		mData = data;		// cache that stuff
		if(isStarted()) super.deliverResult(mData);	// deliver if loader wasn't stopped or canceled
	}
	@Override
	public void onReset() {
		cancelLoad();	// cancel old task
		mData = null;
	}
	@Override
	public void onCanceled(ArrayList<String> data) {
		cancelLoad();	// cancel old task
		mData = null;
	}
	@Override
	public ArrayList<String> loadInBackground() {	// called from AsyncTask
		ArrayList<String> data = new ArrayList<String>();
		try {
			final Shell.Pipe sh = (Pipe)Shell.Pipe.getUserShell().botbrew(mApplication.root(),"reinstdb broken");
			sh.stdin().close();
			final BufferedReader p_stdout = new BufferedReader(new InputStreamReader(sh.stdout()));
			String line;
			while((line = p_stdout.readLine()) != null) {
				line = line.trim();
				if((line.length() > 0)&&(line.indexOf(' ') < 0)) data.add(line);
			}
			p_stdout.close();
			BotBrewApp.sinkError(sh);
			sh.waitFor();
		} catch(IOException ex) {
		} catch(InterruptedException ex) {
		}
		return data;
	}
}

public class RepairListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<ArrayList<String>>, TitleFragment {
	private static final int LOADER_ID = 0x03;
	private ArrayAdapter<String> adapter;
	@Override
	public String getTitle() {
		return "Repairable";
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//setEmptyText("No packages");
		adapter = new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1);
		registerForContextMenu(getListView());
		setListAdapter(adapter);
		//setListShown(false);	// progress indicator
		getLoaderManager().initLoader(LOADER_ID,null,this);
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) { 
		final View view = inflater.inflate(R.layout.repair_list_fragment,null);
		return view;
	}
	@Override
	public void onResume() {
		super.onResume();
		refresh();
	}
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		startActivity((new Intent(getActivity(),PackageManagerActivity.class)).putExtra("package",(String)adapter.getItem(position)));
	}
	// LoaderManager.LoaderCallbacks<Cursor> methods
	@Override
	public Loader<ArrayList<String>> onCreateLoader(int id, Bundle args) {
		return new RepairListLoader(getActivity());
	}
	@Override
	public void onLoadFinished(Loader<ArrayList<String>> loader, ArrayList<String> data) {
		adapter.clear();
		for(String item: data) adapter.add(item);
		//if(isResumed()) setListShown(true);
		//else setListShownNoAnimation(true);
	}
	@Override
	public void onLoaderReset(Loader<ArrayList<String>> loader) {
		adapter.clear();
	}
	public void refresh() {
		getLoaderManager().restartLoader(LOADER_ID,null,this);
	}
}