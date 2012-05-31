package com.botbrew.basil;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class DebInstallDialogFragment extends SherlockDialogFragment {
	public DebInstallDialogFragment() {
	}
	public static DebInstallDialogFragment create(final Uri data) {
		final DebInstallDialogFragment inst = new DebInstallDialogFragment();
		final Bundle args = new Bundle();
		args.putParcelable("data",data);
		inst.setArguments(args);
		return inst;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.deb_install_dialog_fragment,container);
		final Uri data = (Uri)getArguments().getParcelable("data");
		final String path = data.getPath();
		((TextView)view.findViewById(R.id.path)).setText(path);
		((Button)view.findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
		});
		((Button)view.findViewById(R.id.proceed)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity((new Intent(getActivity(),PackageManagerActivity.class)).putExtra("package",path).putExtra("command","installdeb"));
				getDialog().dismiss();
			}
		});
		getDialog().setTitle("Install "+data.getLastPathSegment());
		return view;
	}
}
