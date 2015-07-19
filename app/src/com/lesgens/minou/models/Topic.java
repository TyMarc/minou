package com.lesgens.minou.models;

import rx.Observable;
import ws.wamp.jawampa.PubSubData;
import android.graphics.Bitmap;


public class Topic extends Channel {
	private Bitmap image;
	private String imageUrl;
	private String desc;
	private String parentName;

	public Topic(String namespace, Observable<PubSubData> subscription) {
		super(namespace, subscription);
		desc = "Come and talk about " + getName();
		imageUrl = "golf.jpg";
	}
	
	public void setImageUrl(final String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	public void setParentName(final String parentName) {
		this.parentName = parentName;
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public String getImageUrl(){
		return imageUrl;
	}
	
	public void setDescription(final String desc) {
		this.desc = desc;
	}
	
	public String getDescription() {
		return desc;
	}
	
	public void setImage(final Bitmap image) {
		this.image = image;
	}
	
	public Bitmap getImage() {
		return image;
	}

}
