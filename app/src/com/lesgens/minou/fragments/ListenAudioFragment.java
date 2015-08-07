package com.lesgens.minou.fragments;

import java.io.IOException;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import com.lesgens.minou.R;

public class ListenAudioFragment extends DialogFragment implements OnClickListener, OnCompletionListener {
	private static final String TAG = "ListenAudioFragment";
	private String filepath;
	private MediaPlayer mPlayer = null;
	private Handler handler;
	private ProgressBar progressBar;

	public static ListenAudioFragment newInstance(final String filepath) {
		return new ListenAudioFragment(filepath);
	}

	public ListenAudioFragment(final String filepath){
		this.filepath = filepath;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		// request a window without the title
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.listen_audio, container, false);
		
		handler = new Handler(getActivity().getMainLooper());
		
		progressBar = (ProgressBar) v.findViewById(R.id.progress_audio);

		onPlay(true);
		
		return v;
	}

	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(filepath);
			mPlayer.setOnCompletionListener(this);
			mPlayer.prepare();
			mPlayer.start();
			handler.post(progressUpdate);
		} catch (IOException e) {
			Log.e(TAG, "prepare() failed");
		}
	}

	private void stopPlaying() {
		handler.removeCallbacks(progressUpdate);
		mPlayer.release();
		mPlayer = null;
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
	
	private Runnable progressUpdate = new Runnable(){

		@Override
		public void run() {
			if(mPlayer != null) {
				int perc = (int) (((float)mPlayer.getCurrentPosition() / (float)mPlayer.getDuration()) * 100f);
				progressBar.setProgress(perc);
				
				handler.postDelayed(progressUpdate, 50);
			}
			
		}};

	@Override
	public void onCompletion(MediaPlayer mp) {
		handler.removeCallbacks(progressUpdate);
		dismiss();
	}
}