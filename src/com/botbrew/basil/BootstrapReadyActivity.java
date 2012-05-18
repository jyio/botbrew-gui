package com.botbrew.basil;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapReadyActivity extends SherlockFragmentActivity {
	private static final String STR_CUSTOM = "custom";
	private static final int REQ_BOOTSTRAP = 1;
	private Spinner vLocation;
	private final ArrayList<String> mLocations = new ArrayList<String>();
	private String mLocation = null;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootstrap_ready_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(true);
		vLocation = (Spinner)findViewById(R.id.location);
		mLocations.clear();
		mLocations.add("/data/"+getResources().getText(R.string.filename_default));
		mLocations.add("/sd-ext/"+getResources().getText(R.string.filename_default));
		mLocations.add("/cache/"+getResources().getText(R.string.filename_default));
		mLocations.add("/emmc/"+getResources().getText(R.string.filename_default));
		mLocations.add("/sdcard/"+getResources().getText(R.string.filename_default));
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
					startActivityForResult(new Intent(BootstrapReadyActivity.this,ExplorerActivity.class).putExtra("file","/"),REQ_BOOTSTRAP);
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
					final boolean loop = BotBrewApp.needsLoopMount(file);
					startActivity(new Intent(BootstrapReadyActivity.this,BootstrapDownloadActivity.class).putExtra("file",file).putExtra("loop",loop));
					finish();
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
}