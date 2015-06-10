package com.lesgens.minou.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.TypedValue;

public class Utils {
	
	public static final String MINOU_IMAGE_BASE = "MINOU_IMAGE_BASE:";

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
	
	public static String hmacSha1(String value, String key)
	        throws UnsupportedEncodingException, NoSuchAlgorithmException,
	        InvalidKeyException {
	    String type = "HmacSHA1";
	    SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
	    Mac mac = Mac.getInstance(type);
	    mac.init(secret);
	    byte[] bytes = mac.doFinal(value.getBytes());
	    return bytesToHex(bytes);
	}

	public final static char[] hexArray = "0123456789abcdef".toCharArray();

	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for (int j = 0; j < bytes.length; j++) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
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

}
