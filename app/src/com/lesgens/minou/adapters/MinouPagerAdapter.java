package com.lesgens.minou.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public abstract class MinouPagerAdapter extends FragmentStatePagerAdapter {
	public MinouPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	public abstract Fragment getItem(int position);

	public abstract int getCount();
	
	public abstract int getDrawableId(int position);
}
