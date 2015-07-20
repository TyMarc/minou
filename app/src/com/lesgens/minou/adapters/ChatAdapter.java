package com.lesgens.minou.adapters;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
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
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class ChatAdapter extends ArrayAdapter<Message> implements StickyListHeadersAdapter{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<Message> messages;
	private SimpleDateFormat sdfMessage = new SimpleDateFormat("HH:mm");
	private SimpleDateFormat sdfDaySameWeek = new SimpleDateFormat("EEEE");
	private SimpleDateFormat sdfDaySameYear = new SimpleDateFormat("dd MMM");
	private SimpleDateFormat sdfDayAnotherYear = new SimpleDateFormat("dd MMM yyyy");
	private static SimpleDateFormat sdfDateForDays = new SimpleDateFormat("dd.MM.yyyy");
	private Date sameWeek;
	private Calendar sameYear;
	private boolean isPrivate;
	private boolean isLoadImages;
	private HashMap<String, Bitmap> thumbnailsCache;

	public ChatAdapter(Context context, ArrayList<Message> chatValue, boolean isPrivate) {  
		super(context,-1, chatValue);
		mContext = context;     
		messages = chatValue;
		sameYear = Calendar.getInstance();
		sameYear.add(Calendar.DAY_OF_MONTH, -7);
		sameWeek = sameYear.getTime();
		sameYear.add(Calendar.DAY_OF_MONTH, +7);
		sameYear.set(Calendar.DAY_OF_YEAR, 0);
		this.isPrivate = isPrivate;
		thumbnailsCache = new HashMap<String, Bitmap>();
		isLoadImages = true;
	}

	static class ViewHolder {
		public TextView name;
		public ImageView avatar;
		public TextView message;
		public TextView time;
		public TextView timePicture;
		public ImageView picture;
		public ImageView videoPlay;
	}

	static class HeaderViewHolder {
		public TextView day;
	}


	private LayoutInflater getInflater(){
		if(mInflater == null)
			mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		return mInflater;       
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView;
		Message message = messages.get(position);

		android.util.Log.i("ChannelChatAdapter", "isIncoming="+message.isIncoming() + " message=" + message.getContent() + " data path=" + message.getDataPath());
		if(!message.isIncoming()){
			rowView = getInflater().inflate(R.layout.chat_odd, parent, false);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = null;
			viewHolder.avatar = null;
			viewHolder.message = (TextView) rowView.findViewById(R.id.message);
			viewHolder.time = (TextView) rowView.findViewById(R.id.time);
			viewHolder.timePicture = (TextView) rowView.findViewById(R.id.time_picture);
			viewHolder.picture = (ImageView) rowView.findViewById(R.id.picture);
			viewHolder.videoPlay = (ImageView) rowView.findViewById(R.id.video_play);
			rowView.setTag(viewHolder);
		} else{
			rowView = getInflater().inflate(R.layout.chat_even, parent, false);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.name);
			viewHolder.avatar = (ImageView) rowView.findViewById(R.id.avatar);
			viewHolder.message = (TextView) rowView.findViewById(R.id.message);
			viewHolder.time = (TextView) rowView.findViewById(R.id.time);
			viewHolder.timePicture = (TextView) rowView.findViewById(R.id.time_picture);
			viewHolder.picture = (ImageView) rowView.findViewById(R.id.picture);
			viewHolder.videoPlay = (ImageView) rowView.findViewById(R.id.video_play);

			rowView.setTag(viewHolder);

		}

		ViewHolder holder = (ViewHolder) rowView.getTag();

		if(message.getThumbnail() != null){
			holder.message.setVisibility(View.GONE);
			holder.time.setVisibility(View.GONE);
			holder.timePicture.setVisibility(View.VISIBLE);
			holder.timePicture.setText(sdfMessage.format(message.getTimestamp()));
			holder.picture.setVisibility(View.VISIBLE);
			Bitmap bitmap = null;
			if(isLoadImages) {
				if(thumbnailsCache.containsKey(message.getId().toString())){
					bitmap = thumbnailsCache.get(message.getId().toString());
				} else{
					bitmap = BitmapFactory.decodeByteArray(message.getThumbnail(), 0, message.getThumbnail().length);
					thumbnailsCache.put(message.getId().toString(), bitmap);
				}
				if(message.getMsgType() == MessageType.VIDEO) {
					holder.videoPlay.setVisibility(View.VISIBLE);
				} else{
					holder.videoPlay.setVisibility(View.GONE);
				}
				holder.picture.setImageBitmap(bitmap);
			} else{
				if(message.getMsgType() == MessageType.IMAGE){
					holder.picture.setImageDrawable(mContext.getResources().getDrawable(R.drawable.image_thumb));
				} else if(message.getMsgType() == MessageType.VIDEO) {
					holder.picture.setImageDrawable(mContext.getResources().getDrawable(R.drawable.video_thumb));
				}
				holder.videoPlay.setVisibility(View.GONE);
			}
			setImdn(message.getStatus(), holder.timePicture);
		} else{
			holder.message.setVisibility(View.VISIBLE);
			holder.time.setVisibility(View.VISIBLE);
			holder.timePicture.setVisibility(View.GONE);
			holder.videoPlay.setVisibility(View.GONE);
			holder.message.setText(message.getContent());
			holder.time.setText(sdfMessage.format(message.getTimestamp()));
			setImdn(message.getStatus(), holder.time);
		}

		User user = DatabaseHelper.getInstance().getUser(message.getUserId());

		if(holder.name != null){
			if(!isPrivate){
				holder.name.setText(user.getUsername());
			} else{
				holder.name.setVisibility(View.GONE);
			}
		}

		if(holder.avatar != null){
			if(!isPrivate){
				holder.avatar.setImageBitmap(Utils.cropToCircle(user.getAvatar()));
			} else{
				holder.avatar.setVisibility(View.GONE);
			}
		}

		return rowView;
	}

	private void setImdn(final SendingStatus status, TextView time){
		switch(status){
		case FAILED:
			time.setCompoundDrawablesWithIntrinsicBounds(R.drawable.failed, 0, 0, 0);
			break;
		case PENDING:
			time.setCompoundDrawablesWithIntrinsicBounds(R.drawable.pending, 0, 0, 0);
			break;
		default:
			time.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			break;
		}
	}

	public void addMessage(Message message){
		if(!messages.isEmpty()){
			if(!messages.contains(message)){
				super.add(message);
			}
		} else{
			super.add(message);
		}
	}

	@Override
	public View getHeaderView(int position, View convertView, ViewGroup parent) {
		HeaderViewHolder holder;
		if (convertView == null) {
			holder = new HeaderViewHolder();
			convertView = getInflater().inflate(R.layout.header, parent, false);
			holder.day = (TextView) convertView.findViewById(R.id.day);
			convertView.setTag(holder);
		} else {
			holder = (HeaderViewHolder) convertView.getTag();
		}
		//set header text as first char in name
		Timestamp time = messages.get(position).getTimestamp();
		String headerText = getHeaderText(time);

		holder.day.setText(headerText);

		return convertView;
	}

	public String getHeaderText(Timestamp time){
		if(sameWeek.before(time)){
			return sdfDaySameWeek.format(time);
		} else if(sameYear.before(time)){
			return sdfDaySameYear.format(time);
		} else{
			return sdfDayAnotherYear.format(time);
		}
	}

	@Override
	public long getHeaderId(int position) {
		return getDayCount(sdfDateForDays.format(messages.get(position).getTimestamp().getTime()), sdfDateForDays.format(sameYear.getTime()));
	}

	private long getDayCount(String start, String end) {
		long diff = -1;
		try {
			Date dateStart = sdfDateForDays.parse(start);
			Date dateEnd = sdfDateForDays.parse(end);

			//time is always 00:00:00 so rounding should help to ignore the missing hour when going from winter to summer time as well as the extra hour in the other direction
			diff = Math.round((dateEnd.getTime() - dateStart.getTime()) / (double) 86400000);
		} catch (Exception e) {
			//handle the exception according to your own situation
		}
		return diff;
	}

	public void setLoadImages(boolean b) {
		isLoadImages = b;
	}
}
