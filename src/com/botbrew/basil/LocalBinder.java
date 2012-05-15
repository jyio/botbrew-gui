package com.botbrew.basil;

import java.lang.ref.WeakReference;

import android.os.Binder;

// http://www.ozdroid.com/#!BLOG/2010/12/19/How_to_make_a_local_Service_and_bind_to_it_in_Android
public class LocalBinder<S> extends Binder {
	private final WeakReference<S> mService;
	public LocalBinder(S service) {
		mService = new WeakReference<S>(service);
	}
	public S getService() {
		return mService.get();
	}
}
