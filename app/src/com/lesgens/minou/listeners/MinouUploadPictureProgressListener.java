package com.lesgens.minou.listeners;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;

public class MinouUploadPictureProgressListener implements ProgressListener {
	private String channelNamespace;
	private Message message;
	
	public MinouUploadPictureProgressListener(final Message message, final String channelNamespace){
		this.message = message;
		this.channelNamespace = channelNamespace;
	}	
	
	@Override
	public void progressChanged(ProgressEvent event) {
		Log.i("MinouProgressListener", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			Server.publishPicture(message, channelNamespace);
			message.setStatus(message.isIncoming() ? SendingStatus.RECEIVED : SendingStatus.SENT);
			
			if(MinouApplication.getCurrentActivity() instanceof ChatActivity){
				((ChatActivity) MinouApplication.getCurrentActivity()).notifyAdapter();
			}
			
		} else if(event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
			message.setStatus(SendingStatus.FAILED);
			if(MinouApplication.getCurrentActivity() instanceof ChatActivity){
				((ChatActivity) MinouApplication.getCurrentActivity()).notifyAdapter();
			}
		}
	}
}
