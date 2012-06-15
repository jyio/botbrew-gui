package com.botbrew.basil;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;

class ArrayListFragment extends SherlockListFragment {
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setEmptyText("No stored commands");
		setListAdapter(new ArrayAdapter<String>(
			getActivity(),
			android.R.layout.simple_list_item_1,
			new String[] {"# dpkg --configure --pending"}
		));
	}
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final String item = (String)l.getAdapter().getItem(position);
		final Bundle b = new Bundle();
		if(item.startsWith("#")) {
			b.putBoolean("superuser",true);
			b.putCharSequence("command",item.substring(1));
		} else {
			b.putBoolean("superuser",false);
			b.putCharSequence("command",item);
		}
		final DialogFragment frag = new TerminalDialogFragment();
		frag.setArguments(b);
		frag.show(getActivity().getSupportFragmentManager(),null);
	}
}

public class TerminalActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getSupportFragmentManager().findFragmentById(android.R.id.content) == null) {
			ArrayListFragment list = new ArrayListFragment();
			getSupportFragmentManager().beginTransaction().add(android.R.id.content,list).commit();
		}
	}
}