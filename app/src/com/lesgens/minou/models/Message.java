package com.lesgens.minou.models;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.listeners.MinouDownloadFileProgressListener;
import com.lesgens.minou.network.FileManagerS3;
import com.lesgens.minou.utils.Utils;



public class Message extends Event{
	private static final long MAX_TIMEOUT = 30000;
	private String content;
	private boolean isIncoming;
	private byte[] thumbnail;
	private String dataPath;
	private String userId;
	private SendingStatus status;
	private MessageType msgType;


	public Message(User user, String content, boolean isIncoming, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, content,  isIncoming, null, status, msgType);
	}

	public Message(User user, String content, String dataPath, boolean isIncoming, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, content,  isIncoming, dataPath, status, msgType);
	}

	public Message(User user, String content, String userName, String channelNamespace, boolean isIncoming, String dataPath, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), channelNamespace, user, content,  isIncoming, dataPath, status, msgType);
	}

	public Message(UUID id, Timestamp timestamp, String channelNamespace, User user, String content, boolean isIncoming, String dataPath, SendingStatus status, MessageType msgType) {
		super(id, timestamp, channelNamespace, user);
		userId = user.getId();
		this.content = content;
		this.isIncoming = isIncoming;
		this.status = status;
		this.msgType = msgType;
		this.dataPath = dataPath;
		
		if(status == SendingStatus.PENDING){
			if( (System.currentTimeMillis() - timestamp.getTime()) > MAX_TIMEOUT) {
				setStatus(SendingStatus.FAILED);
			}
		}
		
		if((msgType == MessageType.IMAGE || msgType == MessageType.VIDEO || msgType == MessageType.AUDIO) && dataPath == null){
			Log.i("Message", "downloading file");
			status = SendingStatus.PENDING;
			FileManagerS3.getInstance().downloadFile(content, new MinouDownloadFileProgressListener(this));
		}
	}

	public String getUserId(){
		return userId;
	}

	public boolean isIncoming(){
		return isIncoming;
	}

	public String getContent(){
		return content;
	}

	public MessageType getMsgType(){
		return msgType;
	}

	public byte[] getThumbnail(){
		if(thumbnail == null) {
			if(dataPath != null) {
				try {
					if(msgType == MessageType.IMAGE || msgType == MessageType.AUDIO) {
						byte[] data = Utils.read(new File(dataPath));
						thumbnail = data;
					} else if(msgType == MessageType.VIDEO) {
						MediaMetadataRetriever mmr = new MediaMetadataRetriever();
						mmr.setDataSource(dataPath);
						Bitmap image = mmr.getFrameAtTime(0);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						image.compress(CompressFormat.JPEG, 70, bos);
						image.recycle();
						thumbnail = bos.toByteArray();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return thumbnail;
	}

	public SendingStatus getStatus(){
		return status;
	}

	public void setStatus(SendingStatus status){
		this.status = status;
		DatabaseHelper.getInstance().updateMessageData(this);
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Message){
			Message other = (Message) o;
			if(other.getId().equals(id)) {
				return true;
			}
		}

		return false;
	}

	public void setDataPath(String absolutePath) {
		this.dataPath = absolutePath;
	}

	public String getDataPath() {
		return dataPath;
	}
}
