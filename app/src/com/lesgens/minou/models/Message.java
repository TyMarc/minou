package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.UUID;

import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.listeners.MinouDownloadFileProgressListener;
import com.lesgens.minou.network.FileManagerS3;



public class Message extends Event{

	private String content;
	private boolean isIncoming;
	private byte[] data;
	private String userId;
	private SendingStatus status;
	private MessageType msgType;
	

	public Message(User user, String content, boolean isIncoming, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, content,  isIncoming, null, status, msgType);
	}
	
	public Message(User user, String content, byte[] data, boolean isIncoming, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, content,  isIncoming, data, status, msgType);
	}
	
	public Message(User user, String content, String userName, Channel iDestination, boolean isIncoming, byte[] data, SendingStatus status, MessageType msgType){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), iDestination, user, content,  isIncoming, data, status, msgType);
	}
	
	public Message(UUID id, Timestamp timestamp, Channel channel, User user, String content, boolean isIncoming, byte[] data, SendingStatus status, MessageType msgType) {
		super(id, timestamp, channel, user);
		userId = user.getId();
		this.content = content;
		this.isIncoming = isIncoming;
		this.data = data;
		this.status = status;
		this.msgType = msgType;
		
		if((msgType == MessageType.IMAGE || msgType == MessageType.VIDEO) && data == null){
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

	public byte[] getData(){
		return data;
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

	public void setData(byte[] data) {
		this.data = data;
		DatabaseHelper.getInstance().updateMessageData(this);
	}
}
