package com.lesgens.minou.models;

public class Channel implements IDestination {
	private String name;

	public Channel(String name) {
		this.name = name;
	}

	public String getName(){
		return name;
	}

	public String getId(){
		return name;
	}
}
