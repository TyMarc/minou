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
import com.lesgens.minou.models.ContactPicker;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;
import com.lesgens.minou.views.CheckableImageView;

public class ContactPickerAdapter extends ArrayAdapter<ContactPicker>{
	private Context mContext;
	private LayoutInflater mInflater = null;

	private ArrayList<ContactPicker> contacts;

	public ContactPickerAdapter(Context context, ArrayList<ContactPicker> contacts) {  
		super(context,R.layout.contact_picker_item, contacts);
		mContext = context;
		this.contacts = contacts;     
	}
	
	static class ViewHolder {
	    public TextView name;
	    public ImageView avatar;
	    public CheckableImageView checkbox;
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
			rowView = getInflater().inflate(R.layout.contact_picker_item, parent, false);
			
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) rowView.findViewById(R.id.name);
			holder.avatar = (ImageView) rowView.findViewById(R.id.avatar);
			holder.checkbox = (CheckableImageView) rowView.findViewById(R.id.checkbox);
			rowView.setTag(holder);
		} else{
			rowView = convertView;
		}
		
		String userId = contacts.get(position).getUserId();
		User user = DatabaseHelper.getInstance().getUser(userId);
		ViewHolder holder = (ViewHolder) rowView.getTag();
		final String channelName = user.getUsername();
		
		holder.name.setText(channelName);
		
		if(user.getAvatar() != null){
			holder.avatar.setImageBitmap(Utils.cropToCircle(user.getAvatar()));
		}
		
		
		holder.checkbox.setChecked(contacts.get(position).isSelected());

		return rowView;
	}

	public void unCheckAll() {
		for(ContactPicker c : contacts){
			c.setSelected(false);
		}
	}

	public void checkContact(int position) {
		contacts.get(position).setSelected(!contacts.get(position).isSelected());
	}
	
}
