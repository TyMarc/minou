package com.lesgens.minou.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import android.os.AsyncTask;
import android.os.Environment;

import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lesgens.minou.listeners.MinouDownloadProgressListener;

public class FileManagerS3 {
	private static String BUCKET_NAME = "minou";
	private static FileManagerS3 instance;
	private static TransferManager transferManager;
	
	private FileManagerS3(){
		transferManager = new TransferManager(new com.amazonaws.auth.AnonymousAWSCredentials());
	}
	
	public static FileManagerS3 getInstance(){
		if(instance == null){
			instance = new FileManagerS3();
		}
		
		return instance;
	}
	
	public void uploadFile(final String filename, final byte[] bytes, final ProgressListener listener){
		InputStream inputStream = new ByteArrayInputStream(bytes); 
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(bytes.length);
		PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, filename, inputStream, meta);
		Upload upload = transferManager.upload(por);
		upload.addProgressListener(listener);
	}
	
	public void downloadFile(final String filename, final MinouDownloadProgressListener listener){
		AsyncTask<Void, Void, Integer> request = new AsyncTask<Void, Void, Integer>() {

			@Override
			protected Integer doInBackground(Void... arg0) {
				GetObjectRequest gor = new GetObjectRequest(BUCKET_NAME, filename);
				File file = new File(Environment.getExternalStorageDirectory() + "/" + filename);
				listener.setFileDownload(file);
				Download download = transferManager.download(gor, file);
				download.addProgressListener(listener);
				return 1;
			}

			@Override
			protected void onPostExecute(Integer v) {
				super.onPostExecute(v);


			}


		};
		request.execute();
		
	}

}
