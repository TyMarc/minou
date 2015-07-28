package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.ChannelTrending;
import com.lesgens.minou.utils.Utils;

public class ChannelsTrendingAdapter extends ArrayAdapter<ChannelTrending>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<ChannelTrending> channels;

	public ChannelsTrendingAdapter(Context context, ArrayList<ChannelTrending> chatValue) {  
		super(context,R.layout.topics_item, chatValue);
		mContext = context;     
		channels = chatValue;
	}

	static class ViewHolder {
		public TextView name;
		public TextView count;
	}

	private LayoutInflater getInflater(){
		if(mInflater == null)
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return mInflater;       
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;

		ChannelTrending trending = channels.get(position);

		if(convertView == null){
			rowView = getInflater().inflate(R.layout.trending_item, parent, false);

			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.count = (TextView) rowView.findViewById(R.id.count);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		
		String channelName = Utils.capitalizeFirstLetters(Utils.getNameFromNamespace(trending.getNamespace()));

		holder.name.setText(channelName);
		holder.count.setText(trending.getCount() + "");

		return rowView;
	}
}
