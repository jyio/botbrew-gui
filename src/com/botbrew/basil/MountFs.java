package com.botbrew.basil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MountFs {
	public static class MountEntry {
		protected final static Pattern re_mnt = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)$");
		public final String fs_spec;
		public final String fs_file;
		public final String fs_vfstype;
		public final String fs_mntops;
		public final int fs_freq;
		public final int fs_passno;
		protected final String str;
		public MountEntry(final String entry) {
			if(entry == null) throw new IllegalArgumentException();
			final Matcher matcher = re_mnt.matcher(entry);
			if(matcher.find()) {
				fs_spec = matcher.group(1);
				fs_file = matcher.group(2);
				fs_vfstype = matcher.group(3);
				fs_mntops = matcher.group(4);
				fs_freq = new Integer(matcher.group(5));
				fs_passno = new Integer(matcher.group(6));
				str = entry;
			} else throw new IllegalArgumentException(entry);
		}
		@Override
		public String toString() {
			return str;
		}
	}
	public static final File proc_self_mounts = new File("/proc/self/mounts");
	public final List<MountEntry> mounts = new ArrayList<MountEntry>();
	public MountFs() throws FileNotFoundException {
		this(proc_self_mounts);
	}
	public MountFs(final File mnttab) throws FileNotFoundException {
		final BufferedReader r = setmntent(mnttab);
		try {
			while(true) mounts.add(getmntent(r));
		} catch(IOException ex) {
		} catch(IllegalArgumentException ex) {
		} finally {
			try {
				endmntent(r);
			} catch(IOException ex) {
			}
		}
	}
	public static BufferedReader setmntent() throws FileNotFoundException {
		return setmntent(proc_self_mounts);
	}
	public static BufferedReader setmntent(final File mnttab) throws FileNotFoundException {
		return new BufferedReader(new FileReader(mnttab));
	}
	public static MountEntry getmntent(final BufferedReader reader) throws IOException {
		return new MountEntry(reader.readLine());
	}
	public static void endmntent(final BufferedReader reader) throws IOException {
		reader.close();
	}
	public static MountEntry find(final File path) throws FileNotFoundException {
		return find(proc_self_mounts,path);
	}
	public static MountEntry find(final File mnttab, final File path) throws FileNotFoundException {
		MountEntry res = null;
		int maxlen = -1;
		final BufferedReader r = setmntent(mnttab);
		try {
			final String path_str = path.getCanonicalPath();
			MountEntry mntent;
			while(true) {
				mntent = getmntent(r);
				if((path_str.startsWith(mntent.fs_file))&&(mntent.fs_file.length() >= maxlen)) {
					res = mntent;
					maxlen = mntent.fs_file.length();
				}
			}
		} catch(IOException ex) {
		} catch(IllegalArgumentException ex) {
		} finally {
			try {
				endmntent(r);
			} catch(IOException ex) {
			}
		}
		return res;
	}
}