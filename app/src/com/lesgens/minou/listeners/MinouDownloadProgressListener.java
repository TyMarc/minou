package com.lesgens.minou.listeners;

import java.io.File;

import com.amazonaws.event.ProgressListener;

public abstract class MinouDownloadProgressListener implements ProgressListener {
	protected File fileDownload;	

	public void setFileDownload(File file) {
		fileDownload = file;
	}

}
