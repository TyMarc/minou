package com.lesgens.minou.models;

import com.lesgens.minou.utils.Utils;

public class Geolocation {
	private String city;
	private String state;
	private String country;
	private City cityChannel;
	private City stateChannel;
	private City countryChannel;

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
	
	public City getCityChannel(){
		return cityChannel;
	}
	
	public City getStateChannel(){
		return stateChannel;
	}
	
	public City getCountryChannel(){
		return countryChannel;
	}
	
	public void setCityChannel(final City channel){
		cityChannel = channel;
	}
	
	public void setStateChannel(final City channel){
		stateChannel = channel;
	}
	
	public void setCountryChannel(final City channel){
		countryChannel = channel;
	}
	
	public String getCityNameSpace(){
		return Utils.getNormalizedString(getStateNameSpace() + "." + (city != null ? city : "montreal"));
	}
	
	public String getStateNameSpace(){
		return Utils.getNormalizedString(getCountryNameSpace() + "." + (state != null ? state : "quebec"));
	}
	
	public String getCountryNameSpace(){
		return Utils.getNormalizedString(Channel.WORLDWIDE_CHANNEL + "." + (country != null ? country : "canada"));
	}

}