package com.lesgens.minou.listeners;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.lesgens.minou.network.Server;

public class MinouUploadAvatarProgressListener implements ProgressListener {
	private String avatarUrl;
	private byte[] avatar;
	private AvatarUploadListener listener;
	
	public MinouUploadAvatarProgressListener(final String avatarUrl, byte[] avatar, final AvatarUploadListener listener){
		this.avatarUrl = avatarUrl;
		this.listener = listener;
		this.avatar = avatar;
	}	
	
	@Override
	public void progressChanged(ProgressEvent event) {
		Log.i("MinouProgressListener", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			Server.changeAvatar(avatarUrl, avatar, listener);
		} else if(event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
			if(listener != null) {
				listener.onAvatarUploadError();
			}
		}
	}
}
