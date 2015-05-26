package com.lesgens.minou.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.TypedValue;

public class Utils {
	
	public static final String MINOU_IMAGE_BASE = "BLINDR_IMAGE_BASE:";

	public static int dpInPixels(Context context, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources()
				.getDisplayMetrics());
	}
	
	public static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
	        boolean filter) {
	    float ratio = Math.min(
	            (float) maxImageSize / realImage.getWidth(),
	            (float) maxImageSize / realImage.getHeight());
	    int width = Math.round((float) ratio * realImage.getWidth());
	    int height = Math.round((float) ratio * realImage.getHeight());

	    Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
	            height, filter);
	    return newBitmap;
	}
	
	public static byte[] getByteArrayFromString(String str){
		String[] splitted = str.split(",");
		
		byte[] byteArray = new byte[splitted.length];
		
		for(int i = 0; i < splitted.length; i++){
			byteArray[i] = Byte.valueOf(splitted[i]);
		}
		
		return byteArray;
	}

}
