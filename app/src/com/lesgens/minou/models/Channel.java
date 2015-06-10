package com.lesgens.minou.models;

import java.util.ArrayList;

import rx.Observable;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.PubSubData;

public class Channel {
	public static final String BASE_CHANNEL = "minou.";
	private String name;
	private String namespace;
	private Observable<PubSubData> subscription;
	private ArrayList<Channel> channels;

	public Channel(String namespace, Observable<PubSubData> subscription) {
		channels = new ArrayList<Channel>();
		this.name = namespace.substring(namespace.lastIndexOf("."));
		this.namespace = namespace;
		this.subscription = subscription;
	}

	public Observable<PubSubData> getSubscription(){
		return subscription;
	}
	
	public void setSubscription(final Observable<PubSubData> subscription){
		this.subscription = subscription;
	}
	
	public String getNamespace(){
		return namespace;
	}
	
	public ArrayList<Channel> getChannels(){
		return channels;
	}

	public boolean isContainSubscription(final String channelName){
		if(namespace.startsWith(channelName)) return true;
		
		for(Channel channel : channels){
			return channel.isContainSubscription(channelName);
		}

		return false;
	}

	public void addSubscription(final Channel channel){
		if(channel.getNamespace().startsWith(namespace)){
			
		}
		channels.add(channel);
	}

	public void closeSubscriptions(){
		for(Channel channel : channels){
			channel.closeSubscriptions();
		}

		subscription.unsubscribeOn(Schedulers.immediate());
	}

	public String getName(){
		return name;
	}

	public String getId(){
		return name;
	}

	public Channel getChannelByName(String channel) {
		if(namespace.equals(channel))
			return this;
		for(Channel c : channels){
			return c.getChannelByName(channel);
		}
		
		return null;
	}
}
