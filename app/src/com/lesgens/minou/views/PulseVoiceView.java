package com.lesgens.minou.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.lesgens.minou.R;
import com.lesgens.minou.utils.Utils;

public class PulseVoiceView extends View{
	private Paint amplitudePaint;
	private float amplitude;
	private float minRadius;

	public PulseVoiceView(Context context) {
		super(context);
		init();
	}
	
	public PulseVoiceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	public PulseVoiceView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	public PulseVoiceView(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}
	
	private void init(){
		amplitudePaint = new Paint();
		amplitudePaint.setColor(getContext().getResources().getColor(R.color.red_70));
        amplitudePaint.setStyle(Paint.Style.STROKE);
        amplitudePaint.setStrokeWidth(Utils.dpInPixels(getContext(), 15));
		amplitude = 0;
		minRadius = Utils.dpInPixels(getContext(), 30);
		Log.i("PulseVoiceView", "width=" + getWidth() + " height=" + getHeight());
	}
	
	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}
	
	 @Override
     protected void onDraw(Canvas canvas) {
		 float radius = Math.max(minRadius, (((float)getWidth() / 2.0f) * amplitude) - (amplitudePaint.getStrokeWidth() / 2));
         canvas.drawCircle(getWidth() / 2, getHeight() / 2, radius, amplitudePaint);
     }

}
