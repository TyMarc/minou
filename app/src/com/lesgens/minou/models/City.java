package com.lesgens.minou.models;

import rx.Observable;
import ws.wamp.jawampa.PubSubData;


public class City extends Channel {
	private String flagUrl;

	public City(String namespace, Observable<PubSubData> subscription) {
		super(namespace, subscription);
	}
	
	public void setFlagUrl(final String flagUrl) {
		this.flagUrl = flagUrl;
	}
	
	public String getFlagUrl(){
		return flagUrl;
	}

}
