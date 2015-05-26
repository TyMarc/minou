package com.lesgens.minou.models;

public class Trend {
	public String city;
	public int numberOfParticipants;
	
	public Trend(String city, int numberOfParticipants){
		this.city = city;
		this.numberOfParticipants = numberOfParticipants;
	}
	
	public String getCity(){
		return city;
	}
	
	public int getNumberOfParticipants(){
		return numberOfParticipants;
	}
}
