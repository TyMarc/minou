package com.lesgens.minou.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.location.Address;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.models.Channel;

public class Utils {

	public static final String MINOU_IMAGE_BASE = "MINOU_IMAGE_BASE:";
	public static final int MAX_IMAGE_DIMEN = 1600;
	private static final String TAG = "Utils";

	public static String getFullPrivateChannel(String userId){
		final long myId = Long.parseLong(Controller.getInstance().getId());
		final long otherId = Long.parseLong(userId);
		return Channel.BASE_PRIVATE_CHANNEL + "." + String.valueOf(Math.min(myId, otherId)) + "." + String.valueOf(Math.max(myId, otherId));

	}

	public static void copyFile(File source, File dest)
			throws IOException {
		InputStream input = null;
		OutputStream output = null;
		try {
			input = new FileInputStream(source);
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			if(input != null){
				input.close();
			}
			if(output != null){
				output.close();
			}
		}
	}

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

		image = putWhiteBackground(image);

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		image.compress(CompressFormat.JPEG, 70, bos);
		image.recycle();

		Log.i(TAG, "Scaled down image size= " + bos.size()/1024 + "kb");

		return bos.toByteArray();
	}

	private static Bitmap putWhiteBackground(final Bitmap image) {
		Bitmap newBitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), image.getConfig());
		Canvas canvas = new Canvas(newBitmap);
		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(image, 0, 0, null);
		image.recycle();

		return newBitmap;
	}

	public static String getNormalizedString(final String str){
		String fullChannelName = str.toLowerCase().replace("-", "_").replace(" ", "_");
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

	public static String capitalizeFirstLetter(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
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
		return namespace.substring(namespace.lastIndexOf(".") + 1).replace("_", " ");
	}

	public static Address getFromLocation(double lat, double lng) {

		String address = String.format(Locale.ENGLISH, "http://maps.googleapis.com/maps/api/geocode/json?latlng=%1$f,%2$f&sensor=false&language=en_CA", lat, lng);
		HttpGet httpGet = new HttpGet(address);
		HttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(AllClientPNames.USER_AGENT, "Mozilla/5.0 (Java) Gecko/20081007 java-geocoder");
		client.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, 5 * 1000);
		client.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, 25 * 1000);
		HttpResponse response;

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String json = EntityUtils.toString(entity, "UTF-8");

			JSONObject jsonObject = new JSONObject(json);

			if ("OK".equalsIgnoreCase(jsonObject.getString("status"))) {
				JSONArray results = jsonObject.getJSONArray("results");
				if (results.length() > 0) {
					JSONObject result = results.getJSONObject(0);
					Address addr = new Address(Locale.getDefault());

					JSONArray components = result.getJSONArray("address_components");
					String streetNumber = "";
					String route = "";
					for (int a = 0; a < components.length(); a++) {
						JSONObject component = components.getJSONObject(a);
						JSONArray types = component.getJSONArray("types");
						for (int j = 0; j < types.length(); j++) {
							String type = types.getString(j);
							if (type.equals("locality")) {
								addr.setLocality(component.getString("long_name"));
							} else if (type.equals("street_number")) {
								streetNumber = component.getString("long_name");
							} else if (type.equals("route")) {
								route = component.getString("long_name");
							}
						}
					}
					addr.setAddressLine(0, route + " " + streetNumber);

					addr.setLatitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lat"));
					addr.setLongitude(result.getJSONObject("geometry").getJSONObject("location").getDouble("lng"));
					return addr;

				}
			}


		} catch (ClientProtocolException e) {
			Log.e("Utils", "Error calling Google geocode webservice.", e);
		} catch (IOException e) {
			Log.e("Utils", "Error calling Google geocode webservice.", e);
		} catch (JSONException e) {
			Log.e("Utils", "Error parsing Google geocode webservice response.", e);
		}

		return null;
	}

	public static byte[] read(String absolutePath) throws IOException {
		return read(new File(absolutePath));
	}

	public static byte[] read(File file) throws IOException {
		ByteArrayOutputStream ous = null;
		InputStream ios = null;
		try {
			byte[] buffer = new byte[4096];
			ous = new ByteArrayOutputStream();
			ios = new FileInputStream(file);
			int read = 0;
			while ( (read = ios.read(buffer)) != -1 ) {
				ous.write(buffer, 0, read);
			}
		} finally { 
			try {
				if ( ous != null ) 
					ous.close();
			} catch ( IOException e) {
			}

			try {
				if ( ios != null ) 
					ios.close();
			} catch ( IOException e) {
			}
		}
		return ous.toByteArray();
	}

	public static int getStatusBarHeight(final Context context){
		int result = 0;
		int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		} 
		return result;
	}

	@SuppressWarnings("deprecation")
	public static String getRealPathFromURI(Activity activity, Uri contentUri) {
		String[] proj = { MediaStore.Images.Media.DATA };
		Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(column_index);
	}

}
