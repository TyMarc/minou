package com.lesgens.minou.models;

import com.lesgens.minou.utils.Utils;

public class Geolocation {
	private String city;
	private String state;
	private String country;
	private Channel cityChannel;
	private Channel stateChannel;
	private Channel countryChannel;

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
	
	public Channel getCityChannel(){
		return cityChannel;
	}
	
	public Channel getStateChannel(){
		return stateChannel;
	}
	
	public Channel getCountryChannel(){
		return countryChannel;
	}
	
	public void setCityChannel(final Channel channel){
		cityChannel = channel;
	}
	
	public void setStateChannel(final Channel channel){
		stateChannel = channel;
	}
	
	public void setCountryChannel(final Channel channel){
		countryChannel = channel;
	}
	
	public String getCityNameSpace(){
		return Utils.getNormalizedString(getStateNameSpace() + "." + (city != null ? city : "montreal"));
	}
	
	public String getStateNameSpace(){
		return Utils.getNormalizedString(getCountryNameSpace() + "." + (state != null ? state : "quebec"));
	}
	
	public String getCountryNameSpace(){
		return Utils.getNormalizedString(Channel.BASE_CHANNEL + "worldwide." + (country != null ? country : "canada"));
	}

}