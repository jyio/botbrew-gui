package com.botbrew.basil;

import jackpal.androidterm.emulatorview.ColorScheme;
import jackpal.androidterm.emulatorview.EmulatorView;
import jackpal.androidterm.emulatorview.TermSession;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class TerminalDialogFragment extends SherlockDialogFragment {
	public TerminalDialogFragment() {
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.terminal_dialog_fragment,container);
		final BotBrewApp app = (BotBrewApp)getActivity().getApplicationContext();
		((Button)view.findViewById(R.id.close)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
		});
		final boolean superuser = getArguments().getBoolean("superuser",false);
		CharSequence command = getArguments().getCharSequence("command");
		if(command == null) command = Shell.usershell;
		final File init = (new File(new File(app.getCacheDir().getParent(),"lib"),"libinit.so"));
		try {
			final Shell.Term sh = superuser?Shell.Term.getRootShell():Shell.Term.getUserShell();
			final OutputStream sh_stdin = sh.stdin();
			final InputStream sh_stdout = sh.stdout();
			if(superuser) sh.botbrew(init.getCanonicalPath(),app.root(),command);
			else sh.botbrew(app.root(),command);
			sh_stdin.close();
			while(sh_stdout.read() != '\n');
			final TermSession termsession = new TermSession();
			termsession.setColorScheme(new ColorScheme(7,0xffffffff,0,0xff000000));
			termsession.setTermOut(sh_stdin);
			termsession.setTermIn(sh_stdout);
			final EmulatorView emulatorview = (EmulatorView)view.findViewById(R.id.emulator);
			emulatorview.attachSession(termsession);
			emulatorview.setDensity(getResources().getDisplayMetrics());
			emulatorview.setTextSize(16);
			emulatorview.setExtGestureListener(new GestureDetector.SimpleOnGestureListener() {
				@Override
				public boolean onSingleTapUp(MotionEvent e) {
					app.doToggleSoftKeyboard();
					return true;
				}
			});
			(new AsyncTask<Void,Void,Integer>() {
				@Override
				protected void onPreExecute() {
					getDialog().setCancelable(false);
				}
				@Override
				protected Integer doInBackground(final Void... ign) {
					try {
						return sh.waitFor();
					} catch(InterruptedException ex) {
					}
					return -1;
				}
				@Override
				protected void onCancelled(Integer result) {
					view.findViewById(R.id.close).setVisibility(View.VISIBLE);
					getDialog().setCancelable(true);
				}
				@Override
				protected void onPostExecute(Integer result) {
					if(result.intValue() != 0) {
						onCancelled(result);
						return;
					}
					view.findViewById(R.id.close).setVisibility(View.VISIBLE);
					getDialog().setCancelable(true);
				}
			}).execute();
		} catch(IOException ex) {
			view.findViewById(R.id.close).setVisibility(View.VISIBLE);
		}
		getDialog().setTitle("Terminal: "+command);
		getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		return view;
	}
}