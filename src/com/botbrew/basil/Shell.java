package com.botbrew.basil;

import jackpal.androidterm.Exec;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Shell {
	public static class Pipe extends Shell {
		public final Process proc;
		public Pipe(final String... cmd) throws IOException {
			proc = Runtime.getRuntime().exec(cmd);
			stdin(proc.getOutputStream());
			stdout(proc.getInputStream());
			stderr(proc.getErrorStream());
		}
		public Pipe redirect() throws IOException {
			exec("2>&1");
			return this;
		}
		public void close() {
			if(in != null) try {
				in.close();
			} catch(IOException ex) {}
			if(out != null) try {
				out.close();
			} catch(IOException ex) {}
			if(err != null) try {
				err.close();
			} catch(IOException ex) {}
		}
		public int waitFor() throws InterruptedException {
			return proc.waitFor();
		}
		public static Pipe getUserShell() throws IOException {
			return new Pipe(usershell);
		}
		public static Pipe getRootShell() throws IOException {
			return new Pipe(rootshell,"--shell",usershell);
		}
	}
	public static class Term extends Shell {
		public final FileDescriptor fd;
		public final int pid;
		public Term(final String... cmd) throws IOException {
			final int[] processId = {0};
			fd = Exec.createSubprocess(cmd[0],cmd,new String[] {"PATH="+System.getenv("PATH"),"TERM=vt100"},processId);
			pid = processId[0];
			stdin(new FileOutputStream(fd));
			stdout(new FileInputStream(fd));
		}
		public void close() {
			Exec.close(fd);
		}
		public void hangup() {
			Exec.hangupProcessGroup(pid);
		}
		public int waitFor() throws InterruptedException {
			return Exec.waitFor(pid);
		}
		public static Term getUserShell() throws IOException {
			return new Term(usershell);
		}
		public static Term getRootShell() throws IOException {
			return new Term(rootshell,"--shell",usershell);
		}
	}
	public static String usershell = "/system/bin/sh";
	public static String rootshell = (new File("/system/bin/su")).exists()?"/system/bin/su":"/system/xbin/su";
	protected OutputStream in;
	protected InputStream out;
	protected InputStream err;
	abstract int waitFor() throws InterruptedException;
	public OutputStream stdin() {
		return in;
	}
	protected void stdin(final OutputStream in) {
		this.in = in;
	}
	public InputStream stdout() {
		return out;
	}
	protected void stdout(final InputStream out) {
		this.out = out;
	}
	public InputStream stderr() {
		return err;
	}
	protected void stderr(final InputStream err) {
		this.err = err;
	}
	public Shell exec(final CharSequence cmd) throws IOException {
		in.write(("exec "+cmd+"\n").getBytes());
		in.flush();
		return this;
	}
	/*public Shell botbrew(final CharSequence root) throws IOException {
		in.write(("exec "+root+"/init -- "+usershell+"\n").getBytes());
		in.flush();
		return this;
	}*/
	public Shell botbrew(final CharSequence root, final CharSequence cmd) throws IOException {
		return botbrew(true,root,cmd);
	}
	public Shell botbrew(final boolean exec, final CharSequence root, final CharSequence cmd) throws IOException {
		if(exec) in.write(("exec '"+root+"/init' -- "+cmd+"\n").getBytes());
		else in.write(("'"+root+"/init' -- "+cmd+"\n").getBytes());
		in.flush();
		return this;
	}
	public Shell botbrew(final CharSequence init, final CharSequence root, final CharSequence cmd) throws IOException {
		return botbrew(true,init,root,cmd);
	}
	public Shell botbrew(final boolean exec, final CharSequence init, final CharSequence root, final CharSequence cmd) throws IOException {
		if(exec) in.write(("exec '"+init+"' --target '"+root+"' -- "+cmd+"\n").getBytes());
		else in.write(("'"+init+"' --target '"+root+"' -- "+cmd+"\n").getBytes());
		in.flush();
		return this;
	}
}
