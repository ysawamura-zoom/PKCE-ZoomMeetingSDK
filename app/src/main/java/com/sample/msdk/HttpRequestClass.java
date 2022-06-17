package com.sample.msdk;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestClass extends AsyncTask<String, Void, String> {

    private String TAG = "HttpRequestClass";
    private CallBackTask callbacktask;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "onPreExecute");
        callbacktask.CallBack("onPreExecute");
    }

    @Override
    protected String doInBackground(String... strings) {

        URL Url;
        String AuthType;
        String AuthCode;
        String RequestMethod;
        String ContentType;
        String server_response = null;
        Log.d(TAG, "doInBackground");

        HttpURLConnection urlConnection = null;

        try {
            Url = new URL(strings[0]);
            AuthType = strings[1];
            AuthCode = strings[2];
            RequestMethod = strings[3];
            ContentType = strings[4];

            Log.d(TAG,"URL: " + strings[0]);
            Log.d(TAG,"AuthType: " + AuthType);
            Log.d(TAG,"AuthCode: " + AuthCode);
            Log.d(TAG,"RequestMethod: " + RequestMethod);
            Log.d(TAG, "ContentType: " + ContentType);

            urlConnection = (HttpURLConnection) Url.openConnection();
            urlConnection.setRequestMethod(RequestMethod);
            //urlConnection.setRequestProperty("User-agent", "Mozilla/5.0");
            urlConnection.setRequestProperty("Authorization", AuthType + " " + AuthCode);
            urlConnection.setRequestProperty("Content-Type", ContentType);
            urlConnection.connect();

            int responseCode = urlConnection.getResponseCode();
            Log.d(TAG, "HttpURLConnection responseCode: " + responseCode);
            if(responseCode == HttpURLConnection.HTTP_OK){
                server_response = readStream(urlConnection.getInputStream());
                Log.d(TAG, "server_response: " + server_response);
                /*
                try {
                    JSONObject jsonObject = new JSONObject(server_response);
                    System.out.println("JSON Object: " + jsonObject);
                    String access_token = jsonObject.getString("access_token");
                    String refresh_token = jsonObject.getString("refresh_token");
                    Log.d(TAG, "access_token: " + access_token);
                    Log.d(TAG, "refresh_token: " + refresh_token);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("access_token", access_token);
                    editor.putString("refresh_token", refresh_token);
                    editor.commit();
                } catch (JSONException e) {
                    System.out.println("Error " + e.toString());
                }
                */
            }
        } catch (IOException e) {
            Log.d(TAG, "download error");
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                Log.d(TAG, "urlConnection.disconnect");
                urlConnection.disconnect();
            }
        }
        return server_response;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.d(TAG, "onPostExecute");
        callbacktask.CallBack("onPostExecute");
        callbacktask.CallBack(result);
    }

    public void setOnCallBack(CallBackTask _cbj) {
        Log.d(TAG, "setOnCallBack");
        callbacktask = _cbj;
    }

    public static class CallBackTask {
        private String TAG = "HttpRequestClass CallBackTask";
        public void CallBack(String result) {
            Log.d(TAG, "CallBackTask");
        }
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response.toString();
    }
}
