package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.utils.Utils;

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

		if(convertView == null){
			rowView = getInflater().inflate(R.layout.channel_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
		
		Channel channel = channels.get(position);
		String channelName = Utils.capitalizeFirstLetters(channel.getName());
		if(channel.equals(Controller.getInstance().getCurrentChannel().getParent())){
			holder.name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.button_up, 0, 0, 0);
			holder.name.setTextColor(getContext().getResources().getColor(R.color.main_color));
		}
				
		holder.name.setText(channelName + " (" + channel.getNumberOfChildren() + ")");

		return rowView;
	}
}
