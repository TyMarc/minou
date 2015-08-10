package com.lesgens.minou.network;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.listeners.MinouDownloadProgressListener;

public class FileManagerS3 {
	private static String BUCKET_NAME = "df8f77d8886fd8e4e4bgv445df4ss";
	private static String ACCESS_KEY = "AKIAIQW6PNRNDD6ZW2BQ";
	private static String SECRET = "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";
	private static FileManagerS3 instance;
	private static TransferManager transferManager;
	
	private FileManagerS3(){
		
		transferManager = new TransferManager(new com.amazonaws.auth.BasicAWSCredentials(ACCESS_KEY, SECRET));
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
				try{
					GetObjectRequest gor = new GetObjectRequest(BUCKET_NAME, filename);
					MinouApplication.getInstance().getCacheDir().mkdirs();
					File file = new File(MinouApplication.getInstance().getCacheDir(), "/" + filename);
					listener.setFileDownload(file);
					Download download = transferManager.download(gor, file);
					download.addProgressListener(listener);
				} catch(Exception e) {
					e.printStackTrace();
					Log.i("FileManagerS3", " error while downloading " + filename);
				}
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
