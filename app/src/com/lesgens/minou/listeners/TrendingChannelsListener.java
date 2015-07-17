package com.lesgens.minou.listeners;

import java.util.ArrayList;

import com.lesgens.minou.models.ChannelTrending;

public interface TrendingChannelsListener {
	public void onTrendingChannelsFetched(ArrayList<ChannelTrending> topics);
	public void onTrendingChannelsError(Throwable throwable);
}
