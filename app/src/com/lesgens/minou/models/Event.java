package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.UUID;

public abstract class Event {
	
	
	private UUID id;
	private Timestamp timestamp;
	private IDestination destination;
	private User sourceUser;
	private String realName;
	private String fakeName;
	
	public Event(UUID id, Timestamp timestamp, IDestination destination, User user, String realName, String fakeName) {
		this.id = id;
		this.timestamp = timestamp;
		this.destination = destination;
		this.sourceUser = user;
		this.realName = realName;
		this.fakeName = fakeName;
	}
	
	public UUID getId(){
		return id;
	}
	
	public Timestamp getTimestamp(){
		return timestamp;
	}
	
	public IDestination getDestination(){
		return destination;
	}
	
	public User getUser(){
		return sourceUser;
	}
	
	public String getRealName() {
		return realName;
	}
	
	public String getFakeName() {
		return fakeName;
	}
	
}
