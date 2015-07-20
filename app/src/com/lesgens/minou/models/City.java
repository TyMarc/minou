package com.lesgens.minou.models;

import rx.Observable;
import ws.wamp.jawampa.PubSubData;


public class City extends Channel {

	public City(String namespace, Observable<PubSubData> subscription) {
		super(namespace, subscription);
	}

}
