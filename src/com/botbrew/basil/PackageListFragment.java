package com.botbrew.basil;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.AlphabetIndexer;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public abstract class PackageListFragment extends SherlockListFragment implements LoaderManager.LoaderCallbacks<Cursor>, TitleFragment {
	public static class ListAvailable extends PackageListFragment {
		@Override
		public String getTitle() {
			return "Available";
		}
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(
				getActivity(),PackageCacheProvider.ContentUri.CACHE_BASE.uri,
				new String[] {
					"*",DatabaseOpenHelper.C_NAME+" AS _id",
					"(CASE WHEN "+DatabaseOpenHelper.C_UPGRADABLE+"='' THEN '' ELSE '⇪' END) AS status"
				},
				null,null,DatabaseOpenHelper.C_NAME
			);
		}
	}
	public static class ListInstalled extends PackageListFragment {
		@Override
		public String getTitle() {
			return "Installed";
		}
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(
				getActivity(),PackageCacheProvider.ContentUri.CACHE_BASE.uri,
				new String[] {
					"*",DatabaseOpenHelper.C_NAME+" AS _id",
					"(CASE WHEN "+DatabaseOpenHelper.C_UPGRADABLE+"='' THEN '' ELSE '⇪' END) AS status"
				},
				DatabaseOpenHelper.C_INSTALLED+"!=?",new String[] {""},DatabaseOpenHelper.C_NAME
			);
		}
	}
	public static class ListUpgradable extends PackageListFragment {
		@Override
		public String getTitle() {
			return "Upgradable";
		}
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			return new CursorLoader(
				getActivity(),PackageCacheProvider.ContentUri.CACHE_BASE.uri,
				new String[] {"*",DatabaseOpenHelper.C_NAME+" AS _id","'⇪' AS status"},
				DatabaseOpenHelper.C_UPGRADABLE+"!=?",new String[] {""},DatabaseOpenHelper.C_NAME
			);
		}
	}
	private static abstract class IndexedCursorAdapter extends SimpleCursorAdapter implements SectionIndexer {
		IndexedCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context,layout,c,from,to,flags);
		}
	}
	private static final int LOADER_ID = 0x01;
	private final AlphabetIndexer indexer = new AlphabetIndexer(null,0,"ABCDEFGHIJKLMNOPQRSTUVWXYZ");
	private SimpleCursorAdapter adapter;
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText("No packages");
		setListShown(false);
		getLoaderManager().initLoader(LOADER_ID,null,this);
		adapter = new IndexedCursorAdapter(
			getActivity().getApplicationContext(),
			R.layout.package_list_item,
			null,
			new String[] {
				DatabaseOpenHelper.C_NAME,
				DatabaseOpenHelper.C_INSTALLED,
				"status",
				DatabaseOpenHelper.C_SUMMARY
			},
			new int[] {
				R.id.package_name,
				R.id.package_version,
				R.id.package_status,
				R.id.package_summary
			},
			CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
		) {
			@Override
			public Object[] getSections() {
				return indexer.getSections();
			}
			@Override
			public int getPositionForSection(int section) {
				return indexer.getPositionForSection(section);
			}
			@Override
			public int getSectionForPosition(int position) {
				return indexer.getSectionForPosition(position);
			}
		};
		setListAdapter(adapter);
	}
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		startActivity((new Intent(getActivity(),PackageManagerActivity.class)).putExtra("command","info").putExtra("package",((TextView)v.findViewById(R.id.package_name)).getText()));
	}
	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		ListView l = getListView();
		l.setFastScrollEnabled(false);
		adapter.swapCursor(cursor);
		indexer.setCursor(cursor);
		if(isResumed()) setListShown(true);
		else setListShownNoAnimation(true);
		l.setFastScrollEnabled(true);
	}
	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
		indexer.setCursor(null);
	}
}