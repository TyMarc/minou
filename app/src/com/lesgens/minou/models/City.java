package com.lesgens.minou.models;

public class City extends Channel{
	private String state;
	private String country;

	public City(String city, String state, String country) {
		super(city);
		this.state = state;
		this.country = country;
	}
	
	public String getState(){
		return state;
	}
	
	public String getCountry(){
		return country;
	}

}
