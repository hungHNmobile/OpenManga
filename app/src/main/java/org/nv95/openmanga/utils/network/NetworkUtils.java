package org.nv95.openmanga.utils.network;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nv95.openmanga.content.RESTResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.CacheControl;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by nv95 on 29.11.16.
 */

public class NetworkUtils {

	private static final CacheControl CACHE_CONTROL_DEFAULT = new CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build();
	private static final Headers HEADERS_DEFAULT = new Headers.Builder().build();

	private static OkHttpClient sHttpClient = null;

	@NonNull
	private static OkHttpClient.Builder getClientBuilder() {
		return new OkHttpClient.Builder()
				.addInterceptor(CookieStore.getInstance())
				.addInterceptor(new CloudflareInterceptor());
	}

	public static void init(Context context, boolean useTor) {
		OkHttpClient.Builder builder = getClientBuilder();
		if (useTor && OrbotHelper.get(context).init()) {
			try {
				StrongOkHttpClientBuilder.forMaxSecurity(context)
						.applyTo(builder, new Intent()
								.putExtra(OrbotHelper.EXTRA_STATUS, "ON")); //TODO wtf
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		sHttpClient = builder.build();
	}

	@NonNull
	public static String getString(@NonNull String url) throws IOException {
		return getString(url, HEADERS_DEFAULT);
	}

	@NonNull
	public static String getString(@NonNull String url, @NonNull Headers headers) throws IOException {
		Request.Builder builder = new Request.Builder()
				.url(url)
				.headers(headers)
				.cacheControl(CACHE_CONTROL_DEFAULT)
				.get();
		Response response = null;
		try {
			response = sHttpClient.newCall(builder.build()).execute();
			ResponseBody body = response.body();
			if (body == null) {
				throw new IOException("ResponseBody is null");
			} else {
				return body.string();
			}
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@NonNull
	public static String postString(@NonNull String url, String... data) throws IOException {
		Request.Builder builder = new Request.Builder()
				.url(url)
				.cacheControl(CACHE_CONTROL_DEFAULT)
				.post(buildFormData(data));
		Response response = null;
		try {
			response = sHttpClient.newCall(builder.build()).execute();
			ResponseBody body = response.body();
			if (body == null) {
				throw new IOException("ResponseBody is null");
			} else {
				return body.string();
			}
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	@NonNull
	public static JSONObject postJSONObject(@NonNull String url, String... data) throws IOException, JSONException {
		return new JSONObject(postString(url, data));
	}

	@NonNull
	public static Document getDocument(@NonNull String url) throws IOException {
		return getDocument(url, HEADERS_DEFAULT);
	}

	@NonNull
	public static Document getDocument(@NonNull String url, @NonNull Headers headers) throws IOException {
		return Jsoup.parse(getString(url, headers), url);
	}

	@NonNull
	public static JSONObject getJSONObject(@NonNull String url) throws IOException, JSONException {
		return new JSONObject(getString(url));
	}


	@NonNull
	public static OkHttpClient getHttpClient() {
		return sHttpClient != null ? sHttpClient : getClientBuilder().build();
	}

	public static int getContentLength(Response response) {
		String header = response.header("content-length");
		if (header == null) {
			return -1;
		} else {
			try {
				return Integer.parseInt(header);
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return -1;
			}
		}
	}

	@Nullable
	public static String authorize(@NonNull String url, String... data) throws IOException {
		Request.Builder builder = new Request.Builder()
				.url(url)
				.cacheControl(CACHE_CONTROL_DEFAULT)
				.post(buildFormData(data));
		Response response = null;
		try {
			response = sHttpClient.newCall(builder.build()).execute();
			return response.header("set-cookie");
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

	private static RequestBody buildFormData(@NonNull String[] data) {
		final MultipartBody.Builder builder = new MultipartBody.Builder();
		builder.setType(MultipartBody.FORM);
		for (int i = 0; i < data.length; i = i + 2) {
			builder.addFormDataPart(data[i], data[i+1]);
		}
		return builder.build();
	}

	@NonNull
	public static RESTResponse restQuery(String url, @Nullable String token, String method, String... data) {
		Response response = null;
		try {
			Request.Builder builder = new Request.Builder()
					.url(url)
					.cacheControl(CACHE_CONTROL_DEFAULT)
					.method(method, buildFormData(data))
					.post(buildFormData(data));
			if (!android.text.TextUtils.isEmpty(token)) {
				builder.header("X-AuthToken", token);
			}
			response = sHttpClient.newCall(builder.build()).execute();
			ResponseBody body = response.body();
			if (body != null) {
				return new RESTResponse(new JSONObject(body.string()), response.code());
			} else {
				return new RESTResponse(new JSONObject(), response.code());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return RESTResponse.fromThrowable(e);
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}
}
