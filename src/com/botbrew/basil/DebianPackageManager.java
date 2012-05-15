package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.util.Log;

public class DebianPackageManager {
	private static final String TAG = "BB_PM";
	public static enum Config {
		APT_Architectures("APT::Architectures"),
		APT_IgnoreHold("APT::Ignore-Hold","0"),
		APT_CleanInstalled("APT::Clean-Installed","1"),
		APT_ForceLoopBreak("APT::Force-LoopBreak","0"),
		APT_CacheLimit("APT::CacheLimit"),
		APT_BuildEssential("APT::BuildEssential"),
		APT_Get_DownloadOnly("APT::Get::Download-Only","0"),
		APT_Get_FixBroken("APT::Get::Fix-Broken","0"),
		APT_Get_FixMissing("APT::Get::Fix-Missing","0"),
		APT_Get_Download("APT::Get::Download","1"),
		APT_Get_Simulate("APT::Get::Simulate","0"),
		APT_Get_AssumeYes("APT::Get::Assume-Yes","0"),
		APT_Get_ShowUpgraded("APT::Get::Show-Upgraded","1"),
		APT_Get_ShowVersions("APT::Get::Show-Versions","0"),
		APT_Get_Upgrade("APT::Get::Upgrade","1"),
		APT_Get_ForceYes("APT::Get::Force-Yes","0"),
		APT_Get_PrintURIs("APT::Get::Print-URIs","0"),
		APT_Get_ReInstall("APT::Get::ReInstall","0"),
		APT_Get_ListCleanup("APT::Get::List-Cleanup","1"),
		APT_DefaultRelease("APT::Default-Release"),
		APT_Get_TrivialOnly("APT::Get::Trivial-Only","0"),
		APT_Get_Remove("APT::Get::Remove","0"),
		APT_Get_Purge("APT::Get::Purge","0"),
		APT_Get_AllowUnauthenticated("APT::Get::AllowUnauthenticated","0"),
		DPkg_Options("DPkg::Options::");
		public final String name;
		public final String defval;
		Config(final String s) {
			this(s,"");
		}
		Config(final String s, final String d) {
			name = s.toLowerCase();
			defval = d;
		}
	}
	private static Runtime runtime = Runtime.getRuntime();
	protected final EnumMap<Config,String> mConfig = new EnumMap<Config,String>(Config.class);
	public final String root;
	public boolean redirect = false;
	public String shell = "/system/bin/sh";
	public DebianPackageManager(final String root) {
		this.root = root;
	}
	public DebianPackageManager(final DebianPackageManager copy) {
		root = copy.root;
		redirect = copy.redirect;
		shell = copy.shell;
		config(copy.mConfig);
	}
	public void config(final EnumMap<Config,String> config) {
		mConfig.clear();
		mConfig.putAll(config);
	}
	public String config(final Config key) {
		return mConfig.get(key);
	}
	public String config(final Config key, final String value) {
		return value==null?mConfig.remove(key):mConfig.put(key,value);
	}
	protected String mkConfigStr() {
		final StringBuilder sb = new StringBuilder();
		for(EnumMap.Entry<Config,String> entry: mConfig.entrySet()) {
			sb.append(" -o");
			sb.append(entry.getKey().name);
			sb.append("='");
			sb.append(entry.getValue());
			sb.append("'");
		}
		return sb.toString();
	}
	public String aptget_update() {
		return "apt-get"+mkConfigStr()+" update";
	}
	public String aptget_upgrade() {
		return "apt-get"+mkConfigStr()+" upgrade";
	}
	public String aptget_upgrade(final String pkg) {
		return "apt-get"+mkConfigStr()+" upgrade "+pkg;
	}
	public String aptget_distupgrade() {
		return "apt-get"+mkConfigStr()+" dist-upgrade";
	}
	public String aptget_distupgrade(final String pkg) {
		return "apt-get"+mkConfigStr()+" dist-upgrade "+pkg;
	}
	public String aptget_install(final String pkg) {
		return "apt-get"+mkConfigStr()+" install "+pkg;
	}
	public String aptget_remove(final String pkg) {
		return "apt-get"+mkConfigStr()+" remove "+pkg;
	}
	public String aptget_autoremove(final String pkg) {
		return "apt-get"+mkConfigStr()+" autoremove "+pkg;
	}
	public String aptget_clean() {
		return "apt-get"+mkConfigStr()+" clean";
	}
	public String aptget_autoclean() {
		return "apt-get"+mkConfigStr()+" autoclean";
	}
	public String aptcache_show(final String pkg) {
		return "apt-cache"+mkConfigStr()+" show '"+pkg+"'";
	}
	public String aptcache_search() {
		return "apt-cache"+mkConfigStr()+" search ''";
	}
	public String aptcache_search(final String q) {
		return "apt-cache"+mkConfigStr()+" search '"+q+"'";
	}
	public String dpkgquery(final String q) {
		return "dpkg-query "+q;
	}
	public boolean pm_update() {
		try {
			String line;
			// installed packages
			Process p = exec(true,aptget_update());
			p.getOutputStream().close();
			BufferedReader p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while((line = p_stdout.readLine()) != null) Log.v(TAG,"[STDOUT] "+line);
			BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
			if(p.waitFor() != 0) return false;
			return true;
		} catch(IOException e) {
			Log.v(TAG,"IOException: cannot refresh database");
			return false;
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException: cannot refresh database");
			return false;
		}
	}
	public boolean pm_refresh(final ContentResolver cr) {
		// TODO: check upgradable status
		final ArrayList<ContentValues> values = new ArrayList<ContentValues>();
		try {
			ContentValues cv;
			Matcher matcher;
			String line;
			// installed packages
			final HashMap<String,String> installed = new HashMap<String,String>();
			Process p = exec(false,dpkgquery("--show --showformat='${status} ${package} ${version}\\n'"));
			p.getOutputStream().close();
			BufferedReader p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final Pattern re_name_version = Pattern.compile("^install ok installed (\\S+) (\\S+)$");
			while((line = p_stdout.readLine()) != null) {
				matcher = re_name_version.matcher(line);
				if(matcher.find()) installed.put(matcher.group(1),matcher.group(2));
			}
			BufferedReader p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
			if(p.waitFor() != 0) return false;
			// available packages
			p = exec(false,aptcache_search());
			p.getOutputStream().close();
			p_stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final Pattern re_name_summary = Pattern.compile("^(\\S+) - (.*)$");
			while((line = p_stdout.readLine()) != null) {
				matcher = re_name_summary.matcher(line);
				if(!matcher.find()) continue;
				line = matcher.group(1);
				cv = new ContentValues();
				cv.put(DatabaseOpenHelper.C_NAME,line);
				cv.put(DatabaseOpenHelper.C_SUMMARY,matcher.group(2));
				line = installed.get(line);
				cv.put(DatabaseOpenHelper.C_INSTALLED,line!=null?line:"");
				cv.put(DatabaseOpenHelper.C_UPGRADABLE,"");
				values.add(cv);
			}
			p_stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			while((line = p_stderr.readLine()) != null) Log.v(TAG,"[STDERR] "+line);
			if(p.waitFor() != 0) return false;
		} catch(IOException e) {
			Log.v(TAG,"IOException: cannot refresh database");
			return false;
		} catch(InterruptedException ex) {
			Log.v(TAG,"InterruptedException: cannot refresh database");
			return false;
		}
		final ContentValues[] a = new ContentValues[values.size()];
		values.toArray(a);
		cr.bulkInsert(PackageCacheProvider.ContentUri.CACHE_BASE.uri,a);
		return true;
	}
	protected Process exec(final boolean superuser, final String command) throws IOException {
		final StringBuilder sb = new StringBuilder();
		sb.append("exec ");
		sb.append(root);
		sb.append("/init -- ");
		sb.append(command);
		if(redirect) sb.append(" 2>&1");
		return runtime.exec(new String[] {superuser?"/system/xbin/su":shell,"-c",sb.toString()});
	}
}
