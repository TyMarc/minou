package com.lesgens.minou.models;

import com.lesgens.minou.R;
import com.lesgens.minou.application.MinouApplication;

import rx.Observable;
import ws.wamp.jawampa.PubSubData;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class Topic extends Channel {
	private Bitmap image;
	private String imageUrl;
	private String desc;
	private String parentName;
	private int[] DEFAULT_DRAWABLES = new int[]{R.drawable.default_topic};

	public Topic(String namespace, Observable<PubSubData> subscription) {
		super(namespace, subscription);
		desc = "Come and talk about " + getName();
		imageUrl = "golf.jpg";
		image = BitmapFactory.decodeResource(MinouApplication.getCurrentActivity().getResources(), DEFAULT_DRAWABLES[(int)(Math.random()*DEFAULT_DRAWABLES.length)]);
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
