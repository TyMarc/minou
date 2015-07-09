package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.City;
import com.lesgens.minou.utils.Utils;

public class ChannelsAdapter extends ArrayAdapter<Channel>{
	private Context mContext;
	private LayoutInflater mInflater = null;
	private int normalColor;
	private int lightColor;

	private ArrayList<Channel> channels;

	public ChannelsAdapter(Context context, ArrayList<Channel> chatValue) {  
		super(context,R.layout.public_channel_item, chatValue);
		mContext = context;     
		channels = chatValue;
		normalColor = context.getResources().getColor(R.color.main_color);
		lightColor = context.getResources().getColor(R.color.light_main_color);
	}
	
	static class ViewHolder {
	    public TextView name;
	  }

	private LayoutInflater getInflater(){
		if(mInflater == null)
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return mInflater;       
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		
		Channel channel = channels.get(position);

		if(convertView == null){
			rowView = getInflater().inflate(R.layout.public_channel_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
	
		String channelName = Utils.capitalizeFirstLetters(channel.getName().replace("_", " "));
		
		if(channel instanceof City){
			holder.name.setBackgroundColor(lightColor);
			holder.name.setTextColor(Color.WHITE);
			holder.name.setText(channelName);
		} else {
			holder.name.setTextColor(normalColor);
			holder.name.setText(channelName);
		}

		return rowView;
	}
}
