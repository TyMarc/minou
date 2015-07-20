package com.lesgens.minou.listeners;

import java.io.File;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;

public class MinouDownloadFileProgressListener extends MinouDownloadProgressListener {
	private Message message;
	
	public MinouDownloadFileProgressListener(final Message message){
		this.message = message;
	}	
	
	@Override
	public void progressChanged(ProgressEvent event) {
		Log.i("MinouProgressListener", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			message.setStatus(message.isIncoming() ? SendingStatus.RECEIVED : SendingStatus.SENT);
			if(fileDownload != null){
				message.setDataPath(fileDownload.getAbsolutePath());
				DatabaseHelper.getInstance().updateMessageData(message);
			}
			
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

	public void setFileDownload(File file) {
		fileDownload = file;
	}

}
