package com.lesgens.minou.listeners;

import java.io.File;
import java.io.IOException;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.utils.Utils;

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
				try {
					message.setData(Utils.read(fileDownload));
					fileDownload.delete();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
