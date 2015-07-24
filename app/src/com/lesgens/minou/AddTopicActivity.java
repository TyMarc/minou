package com.lesgens.minou;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.ChannelsTrendingAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.TrendingChannelsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.ChannelTrending;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

public class AddTopicActivity extends MinouActivity implements OnClickListener, TextWatcher, TrendingChannelsListener{
	private ChannelsTrendingAdapter adapter;
	private Channel currentCity;

	public static void show(final Context context) {
		Intent i = new Intent(context, AddTopicActivity.class);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		currentCity = Controller.getInstance().getChannelsContainer().getChannelByName(Controller.getInstance().getGeolocation().getStateNameSpace());

		setContentView(R.layout.add_a_channel);

		((TextView) findViewById(R.id.channel_name)).setText(Utils.capitalizeFirstLetters(Utils.capitalizeFirstLetters(currentCity.getName())));

		((EditText) findViewById(R.id.editText)).addTextChangedListener(this);
		InputFilter filter = new InputFilter() { 
			@Override
			public CharSequence filter(CharSequence source, int start, int end, 
					Spanned dest, int dstart, int dend) { 
				for (int i = start; i < end; i++) { 
					if (!Character.isLetterOrDigit(source.charAt(i)) && source.charAt(i) != ' ') { 
						return ""; 
					}
				} 
				return null; 
			}
		};

		findViewById(R.id.back_btn).setOnClickListener(this);
		((EditText) findViewById(R.id.editText)).setFilters(new InputFilter[]{filter}); 

		findViewById(R.id.currently_written).setOnClickListener(this);
		findViewById(R.id.search_bar).requestFocus();


		Server.getTrendingTopics(currentCity, this);
	}


	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.currently_written){
			final String text = ((TextView) v).getText().toString().trim();
			if(!text.isEmpty()){
				final String channelName = Utils.getNormalizedString(currentCity.getNamespace() + "." + text);
				if(!Controller.getInstance().getChannelsContainer().isContainSubscription(channelName)){
					DatabaseHelper.getInstance().addPublicChannel(channelName);
					Server.subscribeToChannel(this, channelName);
					finish();
				}
			}
		} else if(v.getId() == R.id.back_btn){
			onBackPressed();
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		((TextView) findViewById(R.id.currently_written)).setText(arg0.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void onTrendingChannelsFetched(ArrayList<ChannelTrending> topics) {
		findViewById(R.id.progress_trending).setVisibility(View.GONE);
		if(topics.size() > 0) {
			adapter = new ChannelsTrendingAdapter(this, topics);
			((ListView) findViewById(R.id.list_view)).setAdapter(adapter);
			findViewById(android.R.id.empty).setVisibility(View.GONE);
		} else{
			findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onTrendingChannelsError(Throwable throwable) {
		findViewById(R.id.progress_trending).setVisibility(View.GONE);
		findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
		Log.i("AddAChannelActivity", "Error when fetching trending channels: " + throwable.getMessage());
	}


}
