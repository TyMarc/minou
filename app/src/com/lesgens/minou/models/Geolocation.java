package com.lesgens.minou.models;

public class Geolocation {
	private String city;
	private String state;
	private String country;

	public Geolocation(String city, String state, String country) {
		this.city = city;
		this.state = state;
		this.country = country;
	}
	
	public String getCity(){
		return city;
	}
	
	public String getState(){
		return state;
	}
	
	public String getCountry(){
		return country;
	}
	
	public String getCityNameSpace(){
		return getStateNameSpace() + "." + (city != null ? city : "montreal");
	}
	
	public String getStateNameSpace(){
		return getCountryNameSpace() + "." + (state != null ? state : "quebec");
	}
	
	public String getCountryNameSpace(){
		return Channel.BASE_CHANNEL + "worldwide." + (country != null ? country : "canada");
	}

}