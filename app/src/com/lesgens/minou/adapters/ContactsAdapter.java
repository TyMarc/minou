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
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class ContactsAdapter extends ArrayAdapter<String>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<String> usersId;

	public ContactsAdapter(Context context, ArrayList<String> usersId) {  
		super(context,R.layout.contact_item, usersId);
		mContext = context;
		this.usersId = usersId;     
	}
	
	static class ViewHolder {
	    public TextView name;
	    public ImageView avatar;
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
			rowView = getInflater().inflate(R.layout.contact_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.avatar = (ImageView) rowView.findViewById(R.id.avatar);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		String userId = usersId.get(position);
		User user = DatabaseHelper.getInstance().getUser(userId);
		ViewHolder holder = (ViewHolder) rowView.getTag();
		final String channelName = user.getUsername();
		
		holder.name.setText(channelName);
		
		if(user.getAvatar() != null){
			holder.avatar.setImageBitmap(Utils.cropToCircle(user.getAvatar()));
		}

		return rowView;
	}

	public ArrayList<String> getItems() {
		return usersId;
	}
	
}
