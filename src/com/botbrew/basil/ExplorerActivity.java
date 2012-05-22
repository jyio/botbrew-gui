package com.botbrew.basil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

// https://gist.github.com/1362410
public class ExplorerActivity extends SherlockFragmentActivity {
	private TextView mViewPathSelf;
	private TextView mViewPathParent;
	private ListView mViewListSelf;
	private ListView mViewListParent;
	private File mDirectory;
	private boolean mIsWide = false;
	private final Comparator<File> comparator = new Comparator<File>() {
		private final AlphanumComparator alnum = new AlphanumComparator();
		@Override
		public int compare(final File o1, final File o2) {
			final String s1 = o1.getName();
			final String s2 = o2.getName();
			int res = alnum.compare(s1.toLowerCase(),s2.toLowerCase());
			if(res == 0)res = alnum.compare(s1,s2);
			return res;
		}
	};
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mIsWide = ((BotBrewApp)getApplicationContext()).isWide();
		setContentView(mIsWide?R.layout.explorer_activity_wide:R.layout.explorer_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);
		initialize(getIntent().getStringExtra("file"));
		listFiles(mDirectory);
	}
	private void listFiles(final File file) {
		if((file.canRead())&&(file.isDirectory())) {
			mDirectory = file;
			mViewPathSelf.setText(file.getAbsolutePath());
			File parent = file.getParentFile();
			File[] files;
			ArrayList<String> filenames = new ArrayList<String>();
			if(mIsWide) {
				if(parent != null) {
					mViewPathParent.setText("siblings in "+parent.getAbsolutePath());
					files = parent.listFiles();
					Arrays.sort(files,comparator);
					for(File f: files) if(f.isDirectory()) filenames.add("↱ "+f.getName());
				} else mViewPathParent.setText("");
				mViewListParent.setAdapter(new ArrayAdapter<Object>(
					getApplicationContext(),
					android.R.layout.simple_list_item_1,
					filenames.toArray()
				));
				filenames.clear();
			}
			if(parent != null) filenames.add("⇧ ..");
			files = file.listFiles();
			Arrays.sort(files,comparator);
			for(File f: files) if(f.isDirectory()) filenames.add("⇨ "+f.getName());
			for(File f: files) if(!f.isDirectory()) filenames.add("◇ "+f.getName());
			mViewListSelf.setAdapter(new ArrayAdapter<Object>(
				getApplicationContext(),
				android.R.layout.simple_list_item_1,
				filenames.toArray()
			));
		} else if(file.isFile()) selectFile(file);
	}
	@Override
	public void onBackPressed() {
		setResultX(RESULT_CANCELED);
		finish();
		super.onBackPressed();
	}
	public void setResultX(int resultCode) {
		(getParent()==null?this:getParent()).setResult(resultCode);
	}
	public void setResultX(int resultCode, Intent data) {
		(getParent()==null?this:getParent()).setResult(resultCode,data);
	}
	private void initialize(final String startfile) {
		mDirectory = startfile==null?Environment.getExternalStorageDirectory():(new File(startfile));
		mViewPathSelf = (TextView)findViewById(R.id.path_self);
		mViewListSelf = (ListView)findViewById(R.id.list_self);
		registerForContextMenu(mViewListSelf);
		mViewListSelf.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final String name = ((TextView)view).getText().toString().substring(2);
				if("..".equals(name)) {
					File file = mDirectory.getParentFile();
					if(file != null) listFiles(file);
				} else listFiles(new File(mDirectory,name));
			}
		});
		if(mIsWide) {
			mViewPathParent = (TextView)findViewById(R.id.path_parent);
			mViewListParent = (ListView)findViewById(R.id.list_parent);
			registerForContextMenu(mViewListParent);
			mViewListParent.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					final String name = ((TextView)view).getText().toString().substring(2);
					listFiles(new File(mDirectory.getParentFile(),name));
				}
			});
		}
		final EditText vFile = (EditText)findViewById(R.id.file);
		((Button)findViewById(R.id.select)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String file = vFile.getText().toString().trim();
				selectFile(file.startsWith("/")?(new File(file)):(new File(mDirectory,file)));
			}
		});
	}
	private void selectFile(final File file) {
		setResultX(RESULT_OK,(new Intent()).putExtra("file",file.getAbsolutePath()));
		finish();
	}
	/*@Override
	public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenuInfo menuInfo) {
		final MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.context,menu);
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	@Override
	public boolean onContextItemSelected(final MenuItem item) {
		return super.onContextItemSelected(item);
	}*/
}