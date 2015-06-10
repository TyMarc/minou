package com.lesgens.minou.models;

import java.util.ArrayList;

import rx.Observable;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.PubSubData;

public class Channel {
	public static final String BASE_CHANNEL = "minou.";
	public static final String WORLDWIDE_CHANNEL = BASE_CHANNEL + "worldwide";
	private String name;
	private String namespace;
	private Observable<PubSubData> subscription;
	private ArrayList<Channel> channels;

	public Channel(String namespace, Observable<PubSubData> subscription) {
		channels = new ArrayList<Channel>();
		this.name = namespace.substring(namespace.lastIndexOf(".")-1);
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
		boolean answer = false;
		if(namespace.equals(channelName)) answer = true;
		
		for(Channel channel : channels){
			answer = channel.isContainSubscription(channelName);
		}

		return answer;
	}

	public void addSubscription(final Channel channel){
		if(channel.getNamespace().substring(0,channel.getNamespace().lastIndexOf(".")).equals(namespace)){
			channels.add(channel);
		} else{
			for(Channel c : channels){
				c.addSubscription(channel);
			}
		}
	}

	public void closeSubscriptions(){
		for(Channel channel : channels){
			channel.closeSubscriptions();
		}

		if(subscription != null){
			subscription.unsubscribeOn(Schedulers.immediate());
		}
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
