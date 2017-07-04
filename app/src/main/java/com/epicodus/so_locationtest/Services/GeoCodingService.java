package com.epicodus.so_locationtest.Services;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class GeoCodingService {
    public static void getCity(String latitude, String longitude, Callback callback) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://maps.google.com/maps/api/geocode/json?address=").newBuilder();
        urlBuilder.addQueryParameter("address", latitude + "," + longitude);
        String url = urlBuilder.build().toString();
        Log.d("finalURL", url);
        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
    }

    public static String processResults(String responseString) throws JSONException {
        String returnVal = "test";
        Log.d("successfulResponse", "got in0");
        JSONObject geoJSON = new JSONObject(responseString);
        Log.d("successfulResponse", "got in1");
        Log.d("geoJSON as string", geoJSON.toString());
        JSONObject resultsJSON = geoJSON.getJSONObject("results");
        Log.d("successfulResponse", "got in2");
        resultsJSON = resultsJSON.getJSONObject("address_components");
        Log.d("successfulResponse", "got in3");
        returnVal = resultsJSON.getString("long_name");
        Log.d("successfulResponse", "got in4");
        Log.d("inside", returnVal);
        Log.d("returnVal", returnVal);
        return returnVal;
    }
}
