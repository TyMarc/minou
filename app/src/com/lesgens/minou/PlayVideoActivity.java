package com.lesgens.minou;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

public class PlayVideoActivity extends MinouActivity implements OnClickListener, 
			SurfaceHolder.Callback, OnPreparedListener, OnCompletionListener{
	private String dataPath;
	private MediaPlayer mediaPlayer;
	private SurfaceHolder vidHolder;
	private SurfaceView vidSurface;

	public static void show(final Context context, String dataPath){
		Intent i = new Intent(context, PlayVideoActivity.class);
		i.putExtra("dataPath", dataPath);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.play_video);

		dataPath = getIntent().getStringExtra("dataPath");

		vidSurface = (SurfaceView) findViewById(R.id.video_view);
		vidHolder = vidSurface.getHolder();
		vidHolder.addCallback(this);

		findViewById(R.id.container).setOnClickListener(this);
	}
	
	@Override
	public void onBackPressed(){
		mediaPlayer.stop();
		finish();
	}

	@Override
	public void onStart(){
		super.onStart();
		slideIn();
	}

	private void slideIn(){
		final Animation animBackground = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
		findViewById(R.id.container).startAnimation(animBackground);
	}

	public void slideOut(){

		final Animation animBackground = AnimationUtils.loadAnimation(this, R.anim.alpha_out);
		animBackground.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				finish();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}});
		findViewById(R.id.container).startAnimation(animBackground);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.container) {
			mediaPlayer.stop();
			slideOut();
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDisplay(vidHolder);
			mediaPlayer.setDataSource(dataPath);
			mediaPlayer.prepare();
			mediaPlayer.setOnPreparedListener(this);
			mediaPlayer.setOnCompletionListener(this);
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		} 
		catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		mediaPlayer.start();
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		mediaPlayer.stop();
		finish();
	}

}
