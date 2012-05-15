package com.botbrew.basil;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

public class CheckableDelegate implements Checkable {
	public static class RelativeLayout extends android.widget.RelativeLayout implements Checkable {
		private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
		private final CheckableDelegate mCheckableDelegate = new CheckableDelegate();
		public RelativeLayout(Context context, AttributeSet attrs, int defStyle) {
			super(context,attrs,defStyle);
		}
		public RelativeLayout(Context context, AttributeSet attrs) {
			super(context,attrs);
		}
		public RelativeLayout(Context context, int checkableId) {
			super(context);
		}
		@Override
		public boolean isChecked() {
			return mCheckableDelegate.isChecked();
		}
		@Override
		public void setChecked(boolean isChecked) {
			mCheckableDelegate.setChecked(isChecked);
		}
		@Override
		public void toggle() {
			mCheckableDelegate.toggle();
		}
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			mCheckableDelegate.addCheckableChildren(this);
		}
		@Override
		protected int[] onCreateDrawableState(int extraSpace) {
			final int[] drawableState = super.onCreateDrawableState(extraSpace+1);
			if(isChecked()) mergeDrawableStates(drawableState,CHECKED_STATE_SET);
			return drawableState;
		}
		@Override
		public boolean performClick() {
			toggle();
			return super.performClick();
		}
	}
	public static class TwoLineListItem extends android.widget.TwoLineListItem implements Checkable {
		private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
		private final CheckableDelegate mCheckableDelegate = new CheckableDelegate();
		public TwoLineListItem(Context context, AttributeSet attrs, int defStyle) {
			super(context,attrs,defStyle);
		}
		public TwoLineListItem(Context context, AttributeSet attrs) {
			super(context,attrs);
		}
		public TwoLineListItem(Context context, int checkableId) {
			super(context);
		}
		@Override
		public boolean isChecked() {
			return mCheckableDelegate.isChecked();
		}
		@Override
		public void setChecked(boolean isChecked) {
			mCheckableDelegate.setChecked(isChecked);
		}
		@Override
		public void toggle() {
			mCheckableDelegate.toggle();
		}
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			mCheckableDelegate.addCheckableChildren(this);
		}
		@Override
		protected int[] onCreateDrawableState(int extraSpace) {
			final int[] drawableState = super.onCreateDrawableState(extraSpace+1);
			if(isChecked()) mergeDrawableStates(drawableState,CHECKED_STATE_SET);
			return drawableState;
		}
		@Override
		public boolean performClick() {
			toggle();
			return super.performClick();
		}
	}
	public static class CheckBox extends android.widget.CheckBox {
		public CheckBox(Context context, AttributeSet attrs, int defStyle) {
			super(context,attrs,defStyle);
		}
		public CheckBox(Context context, AttributeSet attrs) {
			super(context,attrs);
		}
		public CheckBox(Context context) {
			super(context);
		}
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return false;
		}
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyPreIme(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyShortcut(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			return false;
		}
	}
	public static class ToggleButton extends android.widget.ToggleButton {
		public ToggleButton(Context context, AttributeSet attrs, int defStyle) {
			super(context,attrs,defStyle);
		}
		public ToggleButton(Context context, AttributeSet attrs) {
			super(context,attrs);
		}
		public ToggleButton(Context context) {
			super(context);
		}
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return false;
		}
		@Override
		public boolean onKeyDown(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyPreIme(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyShortcut(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			return false;
		}
		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			return false;
		}
	}
	private boolean isChecked = false;
	private List<Checkable> checkableViews = new ArrayList<Checkable>(1);
	@Override
	public boolean isChecked() {
		return isChecked;
	}
	@Override
	public void setChecked(final boolean isChecked) {
		this.isChecked = isChecked;
		for(Checkable c: checkableViews) c.setChecked(isChecked);
	}
	@Override
	public void toggle() {
		isChecked = !isChecked;
		for(Checkable c: checkableViews) c.toggle();
	}
	public void addCheckableChildren(final View v) {
		if(v instanceof ViewGroup) {
			final ViewGroup g = (ViewGroup)v;
			for(int i = g.getChildCount()-1; i >= 0; i--) addCheckable(g.getChildAt(i));
		}
	}
	protected void addCheckable(final View v) {
		if(v instanceof Checkable) this.checkableViews.add((Checkable)v);
		if(v instanceof ViewGroup) {
			final ViewGroup g = (ViewGroup)v;
			for(int i = g.getChildCount()-1; i >= 0; i--) addCheckable(g.getChildAt(i));
		}
	}
}