package com.buwizz.buwizzdemo.controls;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class DrivePad extends View implements View.OnTouchListener {

	private MotionListener motionListener;

	private float startY;

	public DrivePad(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		setOnTouchListener(this);
	}

	public DrivePad(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		setOnTouchListener(this);
	}

	public DrivePad(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		setOnTouchListener(this);
	}

	public void setMotionListener(MotionListener motionListener) {
		this.motionListener = motionListener;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		float y = event.getY();

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				startY = y;
				break;
			case MotionEvent.ACTION_UP:
				resetPosition();
				break;
			case MotionEvent.ACTION_CANCEL:
				resetPosition();
				break;
			case MotionEvent.ACTION_MOVE:
				notify(y);
				break;
			default:
				break;
		}

		return true;
	}

	private void notify(float y) {
		float height = getHeight();

		float minY = 200;
		float maxY = height - 200;

		float range = Math.min(startY - minY, maxY - startY);
		double relY = (startY - y) / range;

		if (relY > 1) relY = 1;
		else if (relY < -1) relY = -1;

		dispatchMotion(0, relY);
	}

	private void resetPosition() {
		dispatchMotion(0, 0);
	}

	private void dispatchMotion(double x, double y) {
		if (motionListener != null) {
			motionListener.onMotion(x, y);
		}
	}


	public interface MotionListener {

		void onMotion(double x, double y);
	}
}
