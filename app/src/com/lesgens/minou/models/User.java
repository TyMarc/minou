package com.lesgens.minou.models;

import android.graphics.Bitmap;

public class User extends Channel{
	private Bitmap avatar;
	private String tokenId;
	private String name;
	
	public User(String name, String namespace, Bitmap avatar, String tokenId){
		super(namespace, null);
		this.name = name;
		this.avatar = avatar;
		this.tokenId = tokenId;
	}
	
	public Bitmap getAvatar(){
		return avatar;
	}

	public String getId() {
		return tokenId;
	}
	
	public String getUsername(){
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof User){
			if(((User) o).getId() != null && tokenId != null){
				if(((User) o).getId().toLowerCase().equals(tokenId.toLowerCase())){
					return true;
				}
			}
		}
		return false;
	}
	
}
