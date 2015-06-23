package com.lesgens.minou.adapters;

import android.content.Context;
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

public class RootAdapter extends BaseExpandableListAdapter {

	private static final String TAG = "RootAdapter";

	private Channel root;

	private final LayoutInflater inflater;

	public class Entry {
		public final CustExpListview cls;
		public final SecondLevelAdapter sadpt;

		public Entry(CustExpListview cls, SecondLevelAdapter sadpt) {
			this.cls = cls;
			this.sadpt = sadpt;
		}
	}

	public Entry[] lsfirst;

	public RootAdapter(Context context, Channel root, ExpandableListView.OnGroupClickListener grpLst,
			ExpandableListView.OnChildClickListener childLst, ExpandableListView.OnGroupExpandListener grpExpLst) {
		this.root = root;
		this.inflater = LayoutInflater.from(context);

		lsfirst = new Entry[root.getChannels().size()];
		Log.i(TAG, "Number of children=" + root.getChannels().size());
		for (int i = 0; i < root.getChannels().size(); i++) {
			final CustExpListview celv = new CustExpListview(context);
			SecondLevelAdapter adp = new SecondLevelAdapter(root.getChannels().get(i),context);
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
		return root.getChannels().get(0).getChannels().get(groupPosition).getChannels().get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
			View convertView, ViewGroup parent) {
		// second level list
		return lsfirst[groupPosition].cls;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return root.getChannels().size();
	}

	@Override
	public Channel getGroup(int groupPosition) {
		return root.getChannels().get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return root.getChannels().size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
			ViewGroup parent) {

		// first level

		View layout = convertView;
		GroupViewHolder holder;
		final Channel item = (Channel) getGroup(groupPosition);

		if (layout == null) {
			layout = inflater.inflate(R.layout.public_channel_expandable_item, parent, false);
			holder = new GroupViewHolder();
			holder.title = (TextView) layout.findViewById(R.id.name);
			layout.setTag(holder);
		} else {
			holder = (GroupViewHolder) layout.getTag();
		}

		holder.title.setText(Utils.capitalizeFirstLetters(item.getName()));

		return layout;
	}

	private static class GroupViewHolder {
		TextView title;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
