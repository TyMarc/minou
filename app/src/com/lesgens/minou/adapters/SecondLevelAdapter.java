package com.lesgens.minou.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.lesgens.minou.R;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.utils.Utils;
import com.lesgens.minou.views.CustExpListview;

public class SecondLevelAdapter extends BaseExpandableListAdapter {

	private static final String TAG = "SecondLevelAdapter";
	public Channel child;
	Context mContext;
	LayoutInflater inflater;

	public class Entry {
		public final CustExpListview cls;
		public final SecondLevelAdapter sadpt;

		public Entry(CustExpListview cls, SecondLevelAdapter sadpt) {
			this.cls = cls;
			this.sadpt = sadpt;
		}
	}

	public Entry[] lsfirst;

	public SecondLevelAdapter(Channel child, Context context, ExpandableListView.OnGroupClickListener grpLst,
			ExpandableListView.OnChildClickListener childLst, ExpandableListView.OnGroupExpandListener grpExpLst) {
		this.child = child;
		this.mContext=context;
		inflater = LayoutInflater.from(mContext);

		lsfirst = new Entry[child.getChannels().size()];
		Log.i(TAG, "Number of children=" + child.getChannels().size());
		for (int i = 0; i < child.getChannels().size(); i++) {
			final CustExpListview celv = new CustExpListview(context);
			SecondLevelAdapter adp = new SecondLevelAdapter(child.getChannels().get(i),context, grpLst, childLst, grpExpLst);
			celv.setAdapter(adp);
			celv.setGroupIndicator(null);
			celv.setOnChildClickListener(childLst);
			celv.setOnGroupClickListener(grpLst);
			celv.setOnGroupExpandListener(grpExpLst);

			lsfirst[i] = new Entry(celv, adp);
		}
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

		if(item.getChannels().size() > 0){
			return lsfirst[groupPosition].cls;
		} else{

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