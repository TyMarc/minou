package com.lesgens.minou.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.utils.Utils;

public class TopicsAdapter extends ArrayAdapter<Topic>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<Topic> topics;

	public TopicsAdapter(Context context, ArrayList<Topic> chatValue) {  
		super(context,R.layout.topics_item, chatValue);
		mContext = context;     
		topics = chatValue;
	}
	
	static class ViewHolder {
	    public TextView name;
	    public TextView desc;
	    public TextView usersConnected;
	    public TextView cityName;
	    public TextView unreadCounter;
	    public ImageView image;
	  }

	private LayoutInflater getInflater(){
		if(mInflater == null)
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return mInflater;       
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		
		Topic topic = topics.get(position);

		if(convertView == null){
			rowView = getInflater().inflate(R.layout.topics_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.usersConnected = (TextView) rowView.findViewById(R.id.users_connected);
			holder.cityName = (TextView) rowView.findViewById(R.id.city);
			holder.desc = (TextView) rowView.findViewById(R.id.description);
			holder.image = (ImageView) rowView.findViewById(R.id.image);
			holder.unreadCounter = (TextView) rowView.findViewById(R.id.unread_count); 
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		ViewHolder holder = (ViewHolder) rowView.getTag();
		int unreadCount = DatabaseHelper.getInstance().getUnreadCountForTopic(topic.getNamespace());
		
		if(unreadCount == 0) {
			holder.unreadCounter.setVisibility(View.GONE);
		} else {
			holder.unreadCounter.setVisibility(View.VISIBLE);
			holder.unreadCounter.setText(unreadCount + "");
		}
		String channelName = Utils.capitalizeFirstLetters(topic.getName());
		
		Bitmap image = topic.getImage();
		if(image == null) {
			image = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.default_topic);
		}
		holder.image.setImageBitmap(image);
		holder.name.setText(channelName);

		return rowView;
	}

	public ArrayList<Topic> getItems() {
		return topics;
	}
}
