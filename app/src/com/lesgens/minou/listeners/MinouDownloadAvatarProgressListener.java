package com.lesgens.minou.listeners;

import java.io.File;
import java.io.IOException;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.utils.Utils;

public class MinouDownloadAvatarProgressListener extends MinouDownloadProgressListener {
	private String userId;
	private String avatarUrl;
	
	public MinouDownloadAvatarProgressListener(final String userId, final String avatarUrl){
		this.userId = userId;
		this.avatarUrl = avatarUrl;
	}	
	
	@Override
	public void progressChanged(ProgressEvent event) {
		Log.i("MinouProgressListener", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			try {
				byte[] byteArray = Utils.read(fileDownload);
				DatabaseHelper.getInstance().updateAvatar(userId, avatarUrl, byteArray);
				if(userId.equals(Controller.getInstance().getId())) {
					Controller.getInstance().getMyself().setAvatar(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), byteArray, avatarUrl);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {

		}
	}

	public void setFileDownload(File file) {
		fileDownload = file;
	}

}
