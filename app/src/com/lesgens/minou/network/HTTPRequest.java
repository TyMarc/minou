package com.lesgens.minou.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class HTTPRequest {
	public enum RequestType {
		POST, GET
	};

	public enum StatusCode {
		FOUND(200), NOT_FOUND(404), INTERNAL_ERROR(500), REQUEST_NOT_SENT(-1);
		public int value;

		private StatusCode(int value) {
			this.value = value;
		}

		private static final Map lookup = new HashMap();

		public int getStatusCodeValue() {
			return value;
		}

		static {
			for (StatusCode sc : StatusCode.values()) {
				lookup.put(sc.getStatusCodeValue(), sc);
			}
		}

		public static StatusCode get(int value) { // reverse lookup
			return (StatusCode) lookup.get(value);
		}
	};

	private String output;
	private StatusCode statusCode = StatusCode.REQUEST_NOT_SENT;

	public HTTPRequest(String URL, RequestType requestType) {
		this(URL, requestType, null);
	}

	public HTTPRequest(String URL, RequestType requestType, List<NameValuePair> data) {
		this(URL, requestType, data, null);
	}
	
	public HTTPRequest(String URL, RequestType requestType, List<NameValuePair> data, List<NameValuePair> headers) {
		performRequest(URL, requestType, data, headers);
	}

	private void performRequest(String URL, RequestType requestType, List<NameValuePair> data, List<NameValuePair> headers) {
		statusCode = StatusCode.FOUND;
		HttpClient httpClient = new DefaultHttpClient();
		HttpRequestBase httpRequest;
		HttpResponse httpResponse;
		HttpEntity httpEntity;
		InputStream is = null;
		int statusCode = -1;

		try {
			if (requestType == RequestType.GET) {
				if(data != null) {
					URL += "?"+URLEncodedUtils.format(data, "utf-8");
				}
				HttpGet httpGet = new HttpGet(URL);
				httpRequest = httpGet;
			} else { // assume POST
				HttpPost httpPost = new HttpPost(URL);
				httpPost.setEntity(new UrlEncodedFormEntity(data));
				httpRequest = httpPost;
			}
			
			if(headers != null){
				for(NameValuePair pair:headers){
					httpRequest.addHeader(pair.getName(), pair.getValue());
				}
			}

			httpResponse = httpClient.execute(httpRequest);
			httpEntity = httpResponse.getEntity();

			is = httpEntity.getContent();
			statusCode = httpResponse.getStatusLine().getStatusCode();
			this.statusCode = StatusCode.get(statusCode);
		} catch (HttpHostConnectException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} // assume server went on with request, analyse it
			// if it's 404, don't bother reading

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			output = sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getOutput() {
		return output;
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}
}
