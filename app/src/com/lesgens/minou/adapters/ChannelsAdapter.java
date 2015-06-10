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

public class ChannelsAdapter extends ArrayAdapter<Channel>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<Channel> channels;

	public ChannelsAdapter(Context context, ArrayList<Channel> chatValue) {  
		super(context,R.layout.chat_even, chatValue);
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

		if(convertView == null){ // Only inflating if necessary is great for performance
			rowView = getInflater().inflate(R.layout.channel_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
		final String channelName = channels.get(position).getName();
		
		holder.name.setText(channelName);

		return rowView;
	}
}
