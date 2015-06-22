package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.utils.Utils;

public class ChannelsAdapter extends ArrayAdapter<Channel>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<Channel> channels;

	public ChannelsAdapter(Context context, ArrayList<Channel> chatValue) {  
		super(context,R.layout.public_channel_item, chatValue);
		mContext = context;     
		channels = chatValue;
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
			if(channel.getChannels().size() > 0){
				rowView = getInflater().inflate(R.layout.public_channel_expandable_item, parent, false);
			} else{
				rowView = getInflater().inflate(R.layout.public_channel_item, parent, false);
			}
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
	
		String channelName = Utils.capitalizeFirstLetters(channel.getName().replace("_", " "));
				
		holder.name.setText(channelName);

		return rowView;
	}
}
