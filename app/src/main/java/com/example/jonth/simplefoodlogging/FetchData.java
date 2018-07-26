package com.example.jonth.simplefoodlogging;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import java.util.List;

public class FetchData {
    public String baseUrl = "https://api.edamam.com/api/food-database/parser?";
    public String parameter = "&app_id="+ Constants.edamaId +"&app_key=" + Constants.edamamKey;
    public int maxcal = 0;
    public int mincal = Integer.MAX_VALUE;
    SharedPreferences sharedpreferences;

    public FetchData() {
    }

    public int getMaxcal() {
        return maxcal;
    }

    public int getMincal() {
        return mincal;
    }

    public void setMaxcal(int maxcal) {
        this.maxcal = maxcal;
    }

    public void setMincal(int mincal) {
        this.mincal = mincal;
    }

    public void getInfo(String name) {
        String paraFood = "ingr=" + name;
        String requestUrl = baseUrl + paraFood + parameter;

        AsyncHttpClient client = new AsyncHttpClient();

        client.get(requestUrl, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.v("Request", response + "");
                try {
                    JSONArray hintsJson = response.getJSONArray("hints");
                    for (int i = 0; i < hintsJson.length(); i++) {
                        int cal = (int) Math.round((double) hintsJson.getJSONObject(i).getJSONObject("food").getJSONObject("nutrients").get("ENERC_KCAL"));
                        setMaxcal(Math.max(cal, getMaxcal()));
                        setMincal(Math.min(cal, getMincal()));
                    }


                    return;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Log.v("Request", "Request failure");
            }
        });
    }

//    public class AsyncTaskRunner extends AsyncTask<String, String, String> {
//
//        private String resp;
//
//        @Override
//        protected String doInBackground(String... params) {
//            String name = params[0];
//            getData(name);
//            return resp;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            // execution of result of Long time consuming operation
//        }
//
//
//        @Override
//        protected void onPreExecute() {
//
//        }
//
//
//        @Override
//        protected void onProgressUpdate(String... text) {
//
//        }
//
//        public String getData(String name) {
//
//        }
//    }


}
