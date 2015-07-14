package com.lesgens.minou.models;

public class ContactPicker {
	private String userId;
	private boolean isSelected;
	
	public ContactPicker(String userId){
		this.userId = userId;
		isSelected = false;
	}
	
	public void setSelected(final boolean isSelected){
		this.isSelected = isSelected;
	}
	
	public boolean isSelected(){
		return isSelected;
	}
	
	public String getUserId(){
		return userId;
	}

}
