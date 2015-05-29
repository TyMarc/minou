package com.lesgens.minou.adapters;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.ImageViewerActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.utils.Utils;

public class PrivateChatAdapter extends ArrayAdapter<Message> implements StickyListHeadersAdapter, OnClickListener {
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
	private Typeface tf;

	public PrivateChatAdapter(Context context, ArrayList<Message> chatValue) {  
		super(context,-1, chatValue);
		mContext = context;
		messages = chatValue;
		sameYear = Calendar.getInstance();
		sameYear.add(Calendar.DAY_OF_MONTH, -7);
		sameWeek = sameYear.getTime();
		sameYear.add(Calendar.DAY_OF_MONTH, +7);
		sameYear.set(Calendar.DAY_OF_YEAR, 0);
		tf = Typeface.createFromAsset(context.getAssets(), "fonts/Raleway_Thin.otf");
	}

	static class ViewHolder {
		public TextView message;
		public TextView time;
		public ImageView picture;
		public TextView timePicture;
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
		if(!message.isIncoming()){
			rowView = getInflater().inflate(R.layout.chat_odd, parent, false);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.message = (TextView) rowView.findViewById(R.id.message);
			viewHolder.time = (TextView) rowView.findViewById(R.id.time);
			viewHolder.timePicture = (TextView) rowView.findViewById(R.id.time_picture);
			viewHolder.picture = (ImageView) rowView.findViewById(R.id.picture);
			rowView.setTag(viewHolder);
		} else{
			rowView = getInflater().inflate(R.layout.chat_even, parent, false);

			ViewHolder viewHolder = new ViewHolder();
			viewHolder.message = (TextView) rowView.findViewById(R.id.message);
			viewHolder.time = (TextView) rowView.findViewById(R.id.time);
			viewHolder.timePicture = (TextView) rowView.findViewById(R.id.time_picture);
			viewHolder.picture = (ImageView) rowView.findViewById(R.id.picture);

			rowView.setTag(viewHolder);

		}

		// fill data
		ViewHolder holder = (ViewHolder) rowView.getTag();

		if(message.getMessage().startsWith(Utils.MINOU_IMAGE_BASE)){
			holder.message.setVisibility(View.GONE);
			holder.picture.setVisibility(View.VISIBLE);
			holder.time.setVisibility(View.GONE);
			holder.timePicture.setVisibility(View.VISIBLE);
			holder.timePicture.setText(sdfMessage.format(message.getTimestamp()));
			String encoded = message.getMessage().substring(Utils.MINOU_IMAGE_BASE.length());
			byte[] bytes;
			try{
				bytes = Base64.decode(encoded, Base64.DEFAULT);
				Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
				holder.picture.setImageBitmap(bitmap);
				holder.picture.setOnClickListener(this);
			} catch(Exception e){
				e.printStackTrace();
			}
			
		} else{
			holder.message.setVisibility(View.VISIBLE);
			holder.message.setText(message.getMessage());
			holder.time.setVisibility(View.VISIBLE);
			holder.timePicture.setVisibility(View.GONE);
			holder.picture.setVisibility(View.GONE);
			holder.time.setText(sdfMessage.format(message.getTimestamp()));
		}

		return rowView;
	}

	public boolean addMessage(Message message){
		if(!messages.isEmpty()){
			if(!messages.contains(message)){
				super.add(message);
				return true;
			}
			return false;
		} else{
			super.add(message);
			return true;
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

		holder.day.setTypeface(tf);
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

	@Override
	public void onClick(View v) {
		if(v instanceof ImageView){
			ImageView image = (ImageView) v;
			Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
			ImageViewerActivity.show(getContext(), bitmap);
		}
	}

}
