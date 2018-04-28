package com.umeng.push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.json.JSONObject;

public class PushClient {
	
	private static final CloseableHttpClient httpClient;
    static {
    	PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
    	connManager.setMaxTotal(200);
    	connManager.setDefaultMaxPerRoute(200);
        RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(5000)
        		.setConnectTimeout(5000).setSocketTimeout(5000).setStaleConnectionCheckEnabled(true).build();
        httpClient = HttpClients.custom().setConnectionManager(connManager)
        		.setDefaultRequestConfig(config).build();
    }
	// The user agent
	protected final String USER_AGENT = "Mozilla/5.0";
	
	// The host
	protected static final String host = "https://msgapi.umeng.com";
	
	// The upload path
	protected static final String uploadPath = "/upload";
	
	// The post path
	protected static final String postPath = "/api/send";

	public String send(UmengNotification msg) throws Exception {
		String timestamp = Integer.toString((int)(System.currentTimeMillis() / 1000));
		msg.setPredefinedKeyValue("timestamp", timestamp);
        String url = host + postPath;
        String postBody = msg.getPostBody();
        String sign = DigestUtils.md5Hex(("POST" + url + postBody + msg.getAppMasterSecret()).getBytes("utf8"));
        url = url + "?sign=" + sign;
        CloseableHttpResponse response = null;
        InputStream instream = null;
        HttpPost post = new HttpPost(url);
        post.addHeader("Connection", "Keep-Alive");
        post.setHeader("User-Agent", USER_AGENT);
        try {
	        StringEntity se = new StringEntity(postBody, ContentType.create("text/plain", "UTF-8"));
	        post.setEntity(se);
	        // Send the post request and get the response
	        response = httpClient.execute(post);
	        int status = response.getStatusLine().getStatusCode();
	        if(status != HttpStatus.SC_OK) {
	        	throw new RuntimeException("HttpClient,error status code :" + status);
	        }
	        instream = response.getEntity().getContent();
	        BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
	        StringBuffer result = new StringBuffer();
	        String line = "";
	        while ((line = rd.readLine()) != null) {
	            result.append(line);
	        }
	        return result.toString();
        }finally {
        	if (instream != null) {
        		try {
        			instream.close();
        		}catch (IOException e) {
                    e.printStackTrace();
                }
        	}
        	if(post != null) {
        		post.releaseConnection();
        	}
        }
    }

	// Upload file with device_tokens to Umeng
	public String uploadContents(String appkey,String appMasterSecret,String contents) throws Exception {
		// Construct the json string
		JSONObject uploadJson = new JSONObject();
		uploadJson.put("appkey", appkey);
		String timestamp = Integer.toString((int)(System.currentTimeMillis() / 1000));
		uploadJson.put("timestamp", timestamp);
		uploadJson.put("content", contents);
		// Construct the request
		String url = host + uploadPath;
		String postBody = uploadJson.toString();
		String sign = DigestUtils.md5Hex(("POST" + url + postBody + appMasterSecret).getBytes("utf8"));
		url = url + "?sign=" + sign;
		HttpPost post = new HttpPost(url);
		post.setHeader("User-Agent", USER_AGENT);
		StringEntity se = new StringEntity(postBody, "UTF-8");
		post.setEntity(se);
		// Send the post request and get the response
		CloseableHttpResponse response = httpClient.execute(post);
		System.out.println("Response Code : " + response.getStatusLine().getStatusCode());
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		System.out.println(result.toString());
		// Decode response string and get file_id from it
		JSONObject respJson = new JSONObject(result.toString());
		String ret = respJson.getString("ret");
		if (!ret.equals("SUCCESS")) {
			throw new Exception("Failed to upload file");
		}
		JSONObject data = respJson.getJSONObject("data");
		String fileId = data.getString("file_id");
		// Set file_id into rootJson using setPredefinedKeyValue
		
		return fileId;
	}

}
