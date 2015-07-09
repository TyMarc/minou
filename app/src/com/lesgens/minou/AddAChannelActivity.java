package com.lesgens.minou;

import java.util.ArrayList;

import android.app.Activity;
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

import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.TrendingChannelsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

public class AddAChannelActivity extends MinouActivity implements OnClickListener, TextWatcher, TrendingChannelsListener{
	private ChannelsAdapter adapter;
	private String currentNamespace;

	public static void show(final Activity activity, final String currentNamespace, final int requestCode) {
		Intent i = new Intent(activity, AddAChannelActivity.class);
		i.putExtra("currentNamespace", currentNamespace);
		activity.startActivityForResult(i, requestCode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.add_a_channel);

		currentNamespace = getIntent().getStringExtra("currentNamespace");
		if(currentNamespace == null) currentNamespace = Channel.BASE_CHANNEL;

		((TextView) findViewById(R.id.channel_name)).setText(Utils.capitalizeFirstLetters(Utils.getNameFromNamespace(currentNamespace)));

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


		Server.getTrendingTopics(Controller.getInstance().getCurrentChannel(), this);
	}


	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.currently_written){
			final String text = ((TextView) v).getText().toString().trim();
			if(!text.isEmpty()){
				final String channelName = Utils.getNormalizedString(currentNamespace + "." + text);
				if(!Controller.getInstance().getCurrentChannel().isContainSubscription(channelName)){
					DatabaseHelper.getInstance().addPublicChannel(channelName);
					Server.subscribeToChannel(this, channelName);
					Controller.getInstance().setCurrentChannel(channelName);
					setResult(RESULT_OK);
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
	public void onTrendingChannelsFetched(ArrayList<Channel> topics) {
		findViewById(R.id.progress_trending).setVisibility(View.GONE);
		adapter = new ChannelsAdapter(this, topics);
		((ListView) findViewById(R.id.list_view)).setAdapter(adapter);
	}

	@Override
	public void onTrendingChannelsError(Throwable throwable) {
		findViewById(R.id.progress_trending).setVisibility(View.GONE);
		Log.i("AddAChannelActivity", "Error when fetching trending channels: " + throwable.getMessage());
	}


}
