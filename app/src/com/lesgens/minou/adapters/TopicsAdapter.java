package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.utils.Utils;

public class TopicsAdapter extends ArrayAdapter<Topic>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<Topic> channels;

	public TopicsAdapter(Context context, ArrayList<Topic> chatValue) {  
		super(context,R.layout.public_channel_item, chatValue);
		mContext = context;     
		channels = chatValue;
	}
	
	static class ViewHolder {
	    public TextView name;
	    public TextView desc;
	    public TextView usersConnected;
	    public TextView cityName;
	    public ImageView image;
	    public ImageView imageCity;
	  }

	private LayoutInflater getInflater(){
		if(mInflater == null)
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return mInflater;       
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		
		Topic topic = channels.get(position);

		if(convertView == null){
			rowView = getInflater().inflate(R.layout.public_channel_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.usersConnected = (TextView) rowView.findViewById(R.id.users_connected);
			holder.cityName = (TextView) rowView.findViewById(R.id.city);
			holder.desc = (TextView) rowView.findViewById(R.id.description);
			holder.image = (ImageView) rowView.findViewById(R.id.image);
			holder.imageCity = (ImageView) rowView.findViewById(R.id.image_city);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
	
		String channelName = Utils.capitalizeFirstLetters(topic.getName());
		
		holder.name.setText(channelName);
		holder.usersConnected.setText(getContext().getResources().getQuantityString(R.plurals.users_connected, topic.getCount(), topic.getCount()));
		holder.desc.setText(topic.getDescription());
		holder.cityName.setText(Utils.capitalizeFirstLetters(topic.getParentName()));

		return rowView;
	}
}
