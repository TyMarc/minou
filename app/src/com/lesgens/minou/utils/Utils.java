package com.lesgens.minou.utils;

import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;

public class Utils {

	public static final String MINOU_IMAGE_BASE = "MINOU_IMAGE_BASE:";
	public static final int MAX_IMAGE_DIMEN = 800;
	private static final String TAG = "Utils";

	public static int dpInPixels(Context context, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources()
				.getDisplayMetrics());
	}

	private static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
			boolean filter) {
		float ratio = Math.min(
				(float) maxImageSize / realImage.getWidth(),
				(float) maxImageSize / realImage.getHeight());
		int width = Math.round((float) ratio * realImage.getWidth());
		int height = Math.round((float) ratio * realImage.getHeight());

		Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
				height, filter);
		realImage.recycle();
		return newBitmap;
	}

	public static byte[] prepareImageFT(final Context context, Bitmap image){
		if(Math.max(image.getWidth(), image.getHeight()) > MAX_IMAGE_DIMEN){
			image = scaleDown(image, MAX_IMAGE_DIMEN, true);
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 70, bos);
		image.recycle();
		
		Log.i(TAG, "Scaled down image size= " + bos.size()/1024 + "kb");
		
		return bos.toByteArray();
	}

	public static String getNormalizedString(final String str){
		String fullChannelName = str.toLowerCase().replace("-", "_");
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");

		return fullChannelName;
	}

	public static String transformArrayListToString(final ArrayList<String> array){
		String str = "";
		int i = 0;
		for(String s : array){
			if(i > 0){
				str += "/";
			}

			str += s;
			i++;
		}

		return str;
	}

	public static byte[] getByteArrayFromString(String str){
		String[] splitted = str.split(",");

		byte[] byteArray = new byte[splitted.length];

		for(int i = 0; i < splitted.length; i++){
			byteArray[i] = Byte.valueOf(splitted[i]);
		}

		return byteArray;
	}

	public static String capitalizeFirstLetters(String str){
		StringBuffer stringbf = new StringBuffer();
		Matcher m = Pattern.compile("([a-z])([a-z]*)",
				Pattern.CASE_INSENSITIVE).matcher(str);
		while (m.find()) {
			m.appendReplacement(stringbf, 
					m.group(1).toUpperCase() + m.group(2).toLowerCase());
		}
		return m.appendTail(stringbf).toString();
	}

	public static String authSignature(String authChallenge, String authSecret) throws SignatureException{
		try {
			Key sk = new SecretKeySpec(authSecret.getBytes(), HASH_ALGORITHM);
			Mac mac = Mac.getInstance(sk.getAlgorithm());
			mac.init(sk);
			final byte[] hmac = mac.doFinal(authChallenge.getBytes());
			return Base64.encodeToString(hmac,Base64.NO_WRAP);
		} catch (NoSuchAlgorithmException e1) {
			throw new SignatureException("error building signature, no such algorithm in device " + HASH_ALGORITHM);
		} catch (InvalidKeyException e) {
			throw new SignatureException("error building signature, invalid key " + HASH_ALGORITHM);
		}
	}

	private static final String HASH_ALGORITHM = "HmacSHA256";

	public static String getNameFromNamespace(String namespace) {
		return namespace.substring(namespace.lastIndexOf(".") + 1);
	}

}
