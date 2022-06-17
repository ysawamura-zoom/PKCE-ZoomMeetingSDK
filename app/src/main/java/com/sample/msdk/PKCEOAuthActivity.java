package com.sample.msdk;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.util.Log;
import android.net.Uri;
import android.content.Context;
import android.content.SharedPreferences;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class PKCEOAuthActivity extends Activity implements Constants{

    public String TAG = "PKCEOAuthActivity";
    //public WebView webView;
    public String url;
    public String code_verifier;
    public String code_challenge;
    public SharedPreferences pref;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webview);
        Log.d(TAG, "onCreate");
        pref = this.getSharedPreferences("msdk_settings", Context.MODE_PRIVATE);

        try {
            url = createAuthorizeURI();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        WebView webView = (WebView) findViewById(R.id.webView1);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                final Uri uri = Uri.parse(url);
                return handleUri(uri);
            }
            private boolean handleUri(final Uri uri) {
                Log.i(TAG, "Uri = " + uri);
                final String host = uri.getHost();
                final String scheme = uri.getScheme();
                if (!host.equals(REDIRECT_DOMAIN)) {
                    //Log.d(TAG, "host: " + host);
                    //Log.d(TAG, "scheme: " + scheme);
                    return false; // continue loading page mainly for Zoom Authentication screen
                } else {
                    //Log.d(TAG, "host: " + host);
                    //Log.d(TAG, "scheme: " + scheme);
                    String code = uri.getQueryParameter("code");
                    Log.d(TAG, "code: " + code);
                    getAccessToken(code);
                    return true; // Stop loading once getting Authentication_Code
                }
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.loadUrl(url);
    }

    public boolean isJSONValid(String strings) {
        try {
            new JSONObject(strings);
        } catch (JSONException ex) {
            try {
                new JSONArray(strings);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    public void getAccessToken(String code){
        Log.d(TAG, "getAccessToken");
        Log.d(TAG, "code_verifier: " + code_verifier);
        //String Url = "https://zoom.us/oauth/token?code="+ code + "&grant_type=authorization_code&redirect_uri=https%3A%2F%2Fplayground.zapto.org%3A8443%2F&code_verifier=" + code_verifier;
        String Url = "https://zoom.us/oauth/token?code="+ code + "&grant_type=authorization_code&redirect_uri=" + REDIRECT_URL_ENCODED + "&code_verifier=" + code_verifier;

        String clientid = CLIENT_ID;
        String clientsecret = CLIENT_SECRET;
        final String baseCode = clientid + ":" + clientsecret;
        final String AuthCode = Base64.encodeToString(baseCode.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);

        String AuthType = "Basic";
        String RequestMethod = "POST";
        String ContentType = "application/x-www-form-urlencoded";

        HttpRequestClass httpRequest = new HttpRequestClass();
        httpRequest.setOnCallBack(new HttpRequestClass.CallBackTask(){

            @Override
            public void CallBack(String result) {
                super.CallBack(result);
                Log.d(TAG, "getAccessToken HttpRequestClass: " + result);
                if (isJSONValid(result)){
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        String access_token = jsonObject.getString("access_token");
                        String refresh_token = jsonObject.getString("refresh_token");
                        Log.d(TAG, "access_token: " + access_token);
                        Log.d(TAG, "refresh_token: " + refresh_token);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("access_token", access_token);
                        editor.putString("refresh_token", refresh_token);
                        editor.commit();
                        getUserInfo(access_token);
                    } catch (JSONException e) {
                        System.out.println("Error " + e.toString());
                    }
                }
            }

        });

        httpRequest.execute(Url, AuthType, AuthCode, RequestMethod, ContentType);
    }

    public void getUserInfo(String access_token){
        Log.d(TAG, "getUserInfo");

        String Url = "https://api.zoom.us/v2/users/me";
        final String AuthCode = access_token;
        String AuthType = "Bearer";
        String RequestMethod = "GET";
        String ContentType = "application/json";

        HttpRequestClass httpRequest = new HttpRequestClass();
        httpRequest.setOnCallBack(new HttpRequestClass.CallBackTask(){

            @Override
            public void CallBack(String result) {
                super.CallBack(result);
                Log.d(TAG, "getUserInfo HttpRequestClass: " + result);
                if (isJSONValid(result)){
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        //System.out.println("JSON Object: " + jsonObject);
                        String id = jsonObject.getString("id");
                        String first_name = jsonObject.getString("first_name");
                        String last_name = jsonObject.getString("last_name");
                        String display_name = first_name + " " + last_name;
                        Log.d(TAG, "id: " + id);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("id", id);
                        editor.putString("display_name", display_name);
                        editor.commit();
                        finish();
                        //getZakToken(access_token);
                    } catch (JSONException e) {
                        System.out.println("Error " + e.toString());
                        //finish();
                    }
                }
            }
        });
        httpRequest.execute(Url, AuthType, AuthCode, RequestMethod, ContentType);
    }

    public final String createCodeVerifier() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] code = new byte[32];
        secureRandom.nextBytes(code);
        code_verifier = Base64.encodeToString(code, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return code_verifier;
    }

    public final String createCodeChallenge() throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] codeVerifierBytes = createCodeVerifier().getBytes("US-ASCII");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifierBytes);
        byte[] codeChallengeBytes = md.digest();
        code_challenge = Base64.encodeToString(codeChallengeBytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return code_challenge;
    }

    public String createAuthorizeURI() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        code_challenge = createCodeChallenge();
        Log.d(TAG, "code_challenge: " + code_challenge);
        String uriString = ADD_URL;
        //String uriString = "https://zoom.us/oauth/authorize?response_type=code&client_id=sqDfa1ScSRqMJAvYyeZmkQ&redirect_uri=https%3A%2F%2Fplayground.zapto.org%3A8443%2Fzoom%2Fsdkoauth%2F";
        Uri uri = Uri.parse(uriString)
                .buildUpon()
                .appendQueryParameter("code_challenge", code_challenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build();
        return uri.toString();
    }

}
