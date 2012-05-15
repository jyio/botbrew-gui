package com.botbrew.basil;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity {
	private WebView view_home;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_activity);
		WindowManager.LayoutParams params = getWindow().getAttributes();
		params.width = WindowManager.LayoutParams.MATCH_PARENT;
		params.height = WindowManager.LayoutParams.MATCH_PARENT;
		getWindow().setAttributes(params);
		view_home = (WebView)findViewById(R.id.view_home);
		WebSettings ws = view_home.getSettings();
		// enable javascript
		ws.setJavaScriptEnabled(true);
		// enable database
		ws.setDatabaseEnabled(true);
		ws.setDomStorageEnabled(true);
		ws.setDatabasePath(getDatabasePath("webview").getAbsolutePath());
		// enable cache
		ws.setAppCacheMaxSize(1024*1024*4);
		ws.setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
		ws.setAllowFileAccess(true);
		ws.setAppCacheEnabled(true);
		if(((BotBrewApp)getApplicationContext()).isOnline()) ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
		else ws.setCacheMode(WebSettings.LOAD_DEFAULT);
		view_home.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				if(!url.startsWith("http://botbrew.com")) startActivity((new Intent(Intent.ACTION_VIEW)).setData(Uri.parse(url)));
				else view.loadUrl(url);
				return true;
			}
		});
		view_home.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
				quotaUpdater.updateQuota(estimatedSize*2);
			}
			@Override
			public void onReachedMaxAppCacheSize(long spaceNeeded, long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater) {
				quotaUpdater.updateQuota(spaceNeeded*2);
			}
		});
		view_home.loadUrl("http://botbrew.com/app/");
		onNewIntent(getIntent());
	}
	@Override
	public void onNewIntent(final Intent intent) {
		setIntent(intent);
	}
	@Override
	public void startActivity(Intent intent) {
		try {
			super.startActivity(intent);
		} catch(SecurityException ex) {}
	}
}