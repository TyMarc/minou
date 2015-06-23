package com.lesgens.minou.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.utils.Utils;

public class SecondLevelAdapter extends BaseExpandableListAdapter {

	private static final String TAG = "SecondLevelAdapter";
	public Channel child;
	Context mContext;
	LayoutInflater inflater;

	public SecondLevelAdapter(Channel child, Context context) {
		this.child = child;
		this.mContext=context;
		inflater = LayoutInflater.from(mContext);
	}

	@Override
	public Channel getChild(int groupPosition, int childPosition) {
		return child.getChannels().get(groupPosition).getChannels().get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	// third level
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {
		View layout = convertView;
		final Channel item = (Channel) getChild(groupPosition, childPosition);


		ChildViewHolder holder;

		if (layout == null) {
			layout = inflater.inflate(R.layout.public_channel_item, parent, false);

			holder = new ChildViewHolder();
			holder.title = (TextView) layout.findViewById(R.id.name);
			layout.setTag(holder);
		} else {
			holder = (ChildViewHolder) layout.getTag();
		}

		holder.title.setText(Utils.capitalizeFirstLetters(item.getName()));

		return layout;

	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return child.getChannels().get(groupPosition).getChannels().size();
	}

	@Override
	public Channel getGroup(int groupPosition) {
		return child.getChannels().get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return child.getChannels().size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	// Second level
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {
		View layout = convertView;
		ViewHolder holder;

		final Channel item = (Channel) getGroup(groupPosition);

		if (layout == null) {
			layout = inflater.inflate(R.layout.public_channel_expandable_item, parent, false);
			holder = new ViewHolder();
			holder.title = (TextView) layout.findViewById(R.id.name);
			layout.setTag(holder);
		} else {
			holder = (ViewHolder) layout.getTag();
		}

		holder.title.setText(Utils.capitalizeFirstLetters(item.getName()));

		return layout;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		super.registerDataSetObserver(observer);
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		Log.d("SecondLevelAdapter", "Unregistering observer");
		if (observer != null) {
			super.unregisterDataSetObserver(observer);
		}
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	private static class ViewHolder {
		TextView title;
	}

	private static class ChildViewHolder {
		TextView title;
	}

}