package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.UUID;

public abstract class Event {
	
	
	private UUID id;
	private Timestamp timestamp;
	private Channel channel;
	private User sourceUser;
	
	public Event(UUID id, Timestamp timestamp, Channel channel, User user) {
		this.id = id;
		this.timestamp = timestamp;
		this.channel = channel;
		this.sourceUser = user;
	}
	
	public UUID getId(){
		return id;
	}
	
	public Timestamp getTimestamp(){
		return timestamp;
	}
	
	public Channel getChannel(){
		return channel;
	}
	
	public User getUser(){
		return sourceUser;
	}
	
}
