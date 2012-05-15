package com.botbrew.basil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BootstrapDownloadActivity extends SherlockFragmentActivity {
	private ProgressBar vProgress;
	private boolean mLocked = false;
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bootstrap_download_activity);
		ActionBar actionbar = getSupportActionBar();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(true);
		final CharSequence codename = getResources().getText(R.string.app_codename);
		((Button)findViewById(R.id.retry)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(BootstrapDownloadActivity.this,BootstrapReadyActivity.class));
				finish();
			}
		});
		vProgress = (ProgressBar)findViewById(R.id.progress);
		vProgress.setIndeterminate(false);
		(new AsyncTask<Void,Integer,Boolean>() {
			@Override
			protected Boolean doInBackground(final Void... params) {
				mLocked = true;
				try {
					final URL src = new URL("http://repo.botbrew.com/"+codename+"/bootstrap/pkg.zip");
					final File dst = new File(getCacheDir(),"pkg.zip");
					publishProgress(0);
					fetch(src,dst);
					return true;
				} catch(MalformedURLException ex) {
				} catch(IOException ex) {
				}
				return false;
			}
			@Override
			protected void onProgressUpdate(final Integer... progress) {
				vProgress.setProgress(progress[0]);
			}
			@Override
			protected void onCancelled(Boolean result) {
				findViewById(R.id.fail).setVisibility(View.VISIBLE);
				findViewById(R.id.retry).setVisibility(View.VISIBLE);
				mLocked = false;
			}
			@Override
			protected void onPostExecute(Boolean result) {
				if(!result) {
					onCancelled(result);
					return;
				}
				startActivity(new Intent(BootstrapDownloadActivity.this,BootstrapInstallActivity.class).putExtra("file",getIntent().getStringExtra("file")));
				finish();
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
	}
	@Override
	public void onBackPressed() {
		if(!mLocked) super.onBackPressed();
	}
}