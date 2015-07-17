package com.lesgens.minou.network;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lesgens.minou.controllers.Controller;

public class FileManagerS3 {
	private static String BUCKET_NAME = "minou";
	private static FileManagerS3 instance;
	private static CognitoCachingCredentialsProvider credentialsProvider;
	private static TransferManager transferManager;
	
	private FileManagerS3(final Context context){
		// Initialize the Amazon Cognito credentials provider
		credentialsProvider = new CognitoCachingCredentialsProvider(
		    context, // Context
		    "us-east-1:4a74daf6-217e-4da1-9a7c-45ceba58636c", // Identity Pool ID
		    Regions.US_EAST_1 // Region
		);
		Map<String, String> logins = new HashMap<String, String>();
		logins.put("graph.facebook.com", Controller.getInstance().getToken());
		credentialsProvider.setLogins(logins);
		
		transferManager = new TransferManager(credentialsProvider);
	}
	
	public static void init(final Context context){
		if(instance == null){
			instance = new FileManagerS3(context);
		}
	}
	
	public static FileManagerS3 getInstance(){
		return instance;
	}
	
	
	public void uploadPicture(final String filename, final byte[] bytes, final ProgressListener listener){
		InputStream inputStream = new ByteArrayInputStream(bytes); 
		ObjectMetadata meta = new ObjectMetadata();
		meta.setContentLength(bytes.length);
		PutObjectRequest por = new PutObjectRequest(BUCKET_NAME, filename, inputStream, meta);
		Upload upload = transferManager.upload(por);
		upload.addProgressListener(listener);
	}

}
