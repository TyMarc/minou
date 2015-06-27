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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
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
	
	public static Bitmap cropToCircle(Bitmap bitmap){
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(),
        		bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2,
        		bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
	}
	
	public static Bitmap cropToSquare(Bitmap bitmap){
	    int width  = bitmap.getWidth();
	    int height = bitmap.getHeight();
	    int newWidth = (height > width) ? width : height;
	    int newHeight = (height > width)? height - ( height - width) : height;
	    int cropW = (width - height) / 2;
	    cropW = (cropW < 0)? 0: cropW;
	    int cropH = (height - width) / 2;
	    cropH = (cropH < 0)? 0: cropH;
	    Bitmap cropImg = Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newHeight);
	    bitmap.recycle();

	    return cropImg;
	}
	
	/**
	 * Rotate an image if required.
	 * @param img
	 * @param selectedImage
	 * @return 
	 */
	private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) {

	    // Detect rotation
	    int rotation = getRotation(context, selectedImage);
	    if(rotation!=0){
	        Matrix matrix = new Matrix();
	        matrix.postRotate(rotation);
	        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
	        img.recycle();
	        return rotatedImg;        
	    }else{
	        return img;
	    }
	}

	/**
	 * Get the rotation of the last image added.
	 * @param context
	 * @param selectedImage
	 * @return
	 */
	private static int getRotation(Context context, Uri selectedImage) {
	    int rotation =0;
	    ContentResolver content = context.getContentResolver();


	    Cursor mediaCursor = content.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	            new String[] { "orientation", "date_added" },null, null,"date_added desc");

	    if (mediaCursor != null && mediaCursor.getCount() !=0 ) {
	        while(mediaCursor.moveToNext()){
	            rotation = mediaCursor.getInt(0);
	            break;
	        }
	    }
	    mediaCursor.close();
	    return rotation;
	}

	public static byte[] prepareImageFT(final Context context, Bitmap image, Uri selectedImage){
		if(Math.max(image.getWidth(), image.getHeight()) > MAX_IMAGE_DIMEN){
			image = scaleDown(image, MAX_IMAGE_DIMEN, true);
		}

		image = rotateImageIfRequired(context, image, selectedImage);
		
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
