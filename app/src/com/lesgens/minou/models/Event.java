package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.UUID;

public abstract class Event {
	
	
	protected UUID id;
	private Timestamp timestamp;
	private String channelNamespace;
	private User sourceUser;
	
	public Event(UUID id, Timestamp timestamp, String channelNamespace, User user) {
		this.id = id;
		this.timestamp = timestamp;
		this.channelNamespace = channelNamespace;
		this.sourceUser = user;
	}
	
	public UUID getId(){
		return id;
	}
	
	public Timestamp getTimestamp(){
		return timestamp;
	}
	
	public String getChannelNamespace(){
		return channelNamespace;
	}
	
	public User getUser(){
		return sourceUser;
	}
	
}
