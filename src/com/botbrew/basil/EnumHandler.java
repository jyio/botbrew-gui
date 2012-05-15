package com.botbrew.basil;

import android.os.Handler;
import android.os.Message;

abstract public class EnumHandler<M extends Enum<M>> extends Handler {
	private final M[] sTypes;
	EnumHandler(Class<M> cls) {
		sTypes = cls.getEnumConstants();
	}
	@Override
	public void handleMessage(Message msg) {
		M what;
		try {
			what = sTypes[msg.what];
		} catch(ArrayIndexOutOfBoundsException ex) {
			super.handleMessage(msg);
			return;
		}
		handleMessage(what);
	}
	abstract void handleMessage(M msg);
}