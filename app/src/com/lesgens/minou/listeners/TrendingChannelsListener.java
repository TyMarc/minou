package com.lesgens.minou.listeners;

import java.util.ArrayList;

import com.lesgens.minou.models.Channel;

public interface TrendingChannelsListener {
	public void onTrendingChannelsFetched(ArrayList<Channel> topics);
	public void onTrendingChannelsError(Throwable throwable);
}
