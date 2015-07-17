package com.lesgens.minou.listeners;

import java.io.File;
import java.io.IOException;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

public class MinouProgressListener implements ProgressListener {
	private String channelNamespace;
	private Message message;
	private File fileDownload;
	
	public MinouProgressListener(final Message message, final String channelNamespace){
		this.message = message;
		this.channelNamespace = channelNamespace;
	}	
	
	@Override
	public void progressChanged(ProgressEvent event) {
		Log.i("MinouProgressListener", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			Server.publishPicture(message, channelNamespace);
			message.setStatus(message.isIncoming() ? SendingStatus.RECEIVED : SendingStatus.SENT);
			if(fileDownload != null){
				try {
					message.setData(Utils.read(fileDownload));
					fileDownload.delete();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			DatabaseHelper.getInstance().updateMessageData(message);
		} else if(event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
			message.setStatus(SendingStatus.FAILED);
			DatabaseHelper.getInstance().updateMessageData(message);
		}
	}

	public void setFileDownload(File file) {
		fileDownload = file;
	}

}
