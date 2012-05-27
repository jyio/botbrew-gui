package com.botbrew.basil;

import android.content.Context;
import android.content.Intent;

public enum IntentType {
	APP_EXIT(),
	APP_RESTART(),
	BOOTSTRAP_STATE_DOWNLOAD(),
	BOOTSTRAP_STATE_INSTALL(),
	BOOTSTRAP_STATE_DONE(),
	SESSION_STARTED(),
	SESSION_STOPPED(),
	TEST();
	public final String action;
	IntentType() {
		action = "botbrew.intent.action."+this;
	}
	public boolean equals(String s) {
		return action.equals(s);
	}
	public boolean equals(Intent i) {
		return action.equals(i.getAction());
	}
	public boolean strequals(String s) {
		return action.equals(s);
	}
	public Intent intent() {
		return new Intent(action);
	}
	public Intent intent(Intent o) {
		return new Intent(o).setAction(action);
	}
	public Intent intent(Context ctx, Class<?> cls) {
		return new Intent(ctx,cls).setAction(action);
	}
}
