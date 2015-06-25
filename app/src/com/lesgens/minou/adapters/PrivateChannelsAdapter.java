package com.lesgens.minou.adapters;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;

public class PrivateChannelsAdapter extends ArrayAdapter<User>{
	private Context mContext;
	private LayoutInflater mInflater = null;
	private SimpleDateFormat sdfMessage = new SimpleDateFormat("HH:mm");
	private SimpleDateFormat sdfDaySameWeek = new SimpleDateFormat("EEEE");
	private SimpleDateFormat sdfDaySameYear = new SimpleDateFormat("dd MMM");
	private SimpleDateFormat sdfDayAnotherYear = new SimpleDateFormat("dd MMM yyyy");
	private Date sameWeek;
	private Calendar sameYear;

	private ArrayList<User> users;

	public PrivateChannelsAdapter(Context context, ArrayList<User> users) {  
		super(context,R.layout.private_channel_item, users);
		mContext = context;     
		sameYear = Calendar.getInstance();
		sameYear.add(Calendar.DAY_OF_MONTH, -7);
		sameWeek = sameYear.getTime();
		sameYear.add(Calendar.DAY_OF_MONTH, +7);
		sameYear.set(Calendar.DAY_OF_YEAR, 0);
		this.users = users;     
	}
	
	static class ViewHolder {
	    public TextView name;
	    public ImageView avatar;
	    public TextView lastMessage;
	    public TextView timeLastMessage;
	    public TextView dayLastMessage;
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
			rowView = getInflater().inflate(R.layout.private_channel_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.lastMessage = (TextView) rowView.findViewById(R.id.last_message);
			holder.avatar = (ImageView) rowView.findViewById(R.id.avatar);
			holder.timeLastMessage = (TextView) rowView.findViewById(R.id.last_message_time);
			holder.dayLastMessage = (TextView) rowView.findViewById(R.id.last_message_day);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		User user = users.get(position);
		ViewHolder holder = (ViewHolder) rowView.getTag();
		final String channelName = user.getUsername();
		
		holder.name.setText(channelName);
		
		if(user.getAvatar() != null){
			holder.avatar.setImageBitmap(user.getAvatar());
		}
		
		Message lastMessage = DatabaseHelper.getInstance().getLastMessage(user);
		if(lastMessage != null){
			if(lastMessage.getMessage() != null){
				holder.lastMessage.setText(lastMessage.getMessage());
			} else{
				holder.lastMessage.setText(R.string.picture);
			}
			holder.timeLastMessage.setText(sdfMessage.format(lastMessage.getTimestamp()));
			holder.dayLastMessage.setText(getTimeText(lastMessage.getTimestamp()));
		}

		return rowView;
	}
	
	public String getTimeText(Timestamp time){
		if(sameWeek.before(time)){
			return sdfDaySameWeek.format(time);
		} else if(sameYear.before(time)){
			return sdfDaySameYear.format(time);
		} else{
			return sdfDayAnotherYear.format(time);
		}
	}
	
}
