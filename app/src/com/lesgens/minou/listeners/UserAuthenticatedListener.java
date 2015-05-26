package com.lesgens.minou.listeners;

public interface UserAuthenticatedListener {
	public void onUserAuthenticated();
	public void onUserNetworkErrorAuthentication();
	public void onUserServerErrorAuthentication();
}