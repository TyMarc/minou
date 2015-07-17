package com.lesgens.minou.models;

public class ChannelTrending {
	private String namespace;
	private int count;
	
	public ChannelTrending(final String namespace, final int count){
		this.namespace = namespace;
		this.count = count;
	}
	
	public int getCount(){
		return count;
	}
	
	public String getNamespace(){
		return namespace;
	}

}
