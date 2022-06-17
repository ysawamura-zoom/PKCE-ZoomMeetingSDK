package com.sample.msdk;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import us.zoom.sdk.MeetingError;
import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import android.util.Log;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements Constants, MeetingServiceListener {

    public String TAG = "MainActivity";
    public SharedPreferences pref;
    public String USER_ID; // if this id and zak does not match it will lead to become a "guest" user
    public String DISPLAY_NAME;
    public String MEETING_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate");

        initializeSdk(this);

        Button btn = findViewById(R.id.button);
        EditText editTextNum = (EditText)findViewById(R.id.editTextNumber);
        btn.setOnClickListener( v -> {
            Log.d(TAG, "button clicked: " + editTextNum.getText().toString());
            MEETING_ID = editTextNum.getText().toString();

            pref = this.getSharedPreferences("msdk_settings", Context.MODE_PRIVATE);
            USER_ID = pref.getString("id", null);
            DISPLAY_NAME = pref.getString("display_name", null);
            final String access_token = pref.getString("access_token", null);
            ZoomSDK zoomSDK = ZoomSDK.getInstance();
            if(zoomSDK.isInitialized()) {
                registerMeetingServiceListener();
                getZakToken(access_token);
            }else{
                Log.e(TAG, "zoomSDK is not initialized");
            }
        });
    }

    public void initializeSdk(Context context) {
        Log.d(TAG, "initializeSdk");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        // TODO: For the purpose of this demo app, we are storing the credentials in the client app itself.
        // TODO: However, you should not use hard-coded values for your key/secret in your app in production.
        ZoomSDKInitParams params = new ZoomSDKInitParams();
        params.appKey = SDK_KEY;       // TODO: Retrieve your SDK key and enter it here
        params.appSecret = SDK_SECRET; // TODO: Retrieve your SDK secret and enter it here
        params.domain = "zoom.us";
        params.enableLog = true;
        // TODO: Add functionality to this listener (e.g. logs for debugging)
        ZoomSDKInitializeListener listener = new ZoomSDKInitializeListener() {
            /**
             * @param errorCode {@link us.zoom.sdk.ZoomError#ZOOM_ERROR_SUCCESS} if the SDK has been initialized successfully.
             */
            @Override
            public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
                Log.d(TAG, "onZoomSDKInitializeResult: " + errorCode);
                if(errorCode == 0){
                    Intent intent = new Intent(context, PKCEOAuthActivity.class);
                    startActivity(intent);
                }
            }
            @Override
            public void onZoomAuthIdentityExpired() {
                Log.e(TAG, "ZoomAuthIdentityExpired");
            }
        };
        zoomSDK.initialize(context, listener, params);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if(zoomSDK.isInitialized()) {
            MeetingService meetingService = zoomSDK.getMeetingService();
            meetingService.removeListener(this);
        }
    }

    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
        if(meetingStatus == meetingStatus.MEETING_STATUS_FAILED && errorCode == MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE) {
            Log.d(TAG, "MeetingStatus: MEETING_ERROR_CLIENT_INCOMPATIBLE");
        }
        if(meetingStatus == MeetingStatus.MEETING_STATUS_IDLE || meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
            //selectTab(TAB_WELCOME);
            Log.d(TAG, "MeetingStatus: MEETING_STATUS_IDLE or MEETING_STATUS_FAILED");
        }
    }

    @Override
    public void onMeetingParameterNotification(MeetingParameter meetingParameter) {

    }

    public void getZakToken(String ACCESS_TOKEN){
        Log.d(TAG, "getZakToken");

        String Url = "https://api.zoom.us/v2/users/me/zak";
        final String AuthCode = ACCESS_TOKEN;
        String AuthType = "Bearer";
        String RequestMethod = "GET";
        String ContentType = "application/json";

        HttpRequestClass httpRequest = new HttpRequestClass();
        httpRequest.setOnCallBack(new HttpRequestClass.CallBackTask(){

            @Override
            public void CallBack(String result) {
                super.CallBack(result);
                Log.d(TAG, "getZakToken HttpRequestClass: " + result);
                if (isJSONValid(result)){
                    try {
                        JSONObject jsonObject = new JSONObject(result);
                        String ZOOM_ACCESS_TOKEN = jsonObject.getString("token");
                        Log.d(TAG, "zak_token: " + ZOOM_ACCESS_TOKEN);
                        prepareZoomMeeting(ZOOM_ACCESS_TOKEN);
                    } catch (JSONException e) {
                        System.out.println("Error " + e.toString());
                    }
                }
            }
        });
        httpRequest.execute(Url, AuthType, AuthCode, RequestMethod, ContentType);
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

    private void prepareZoomMeeting(String ZOOM_ACCESS_TOKEN){
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if(!zoomSDK.isInitialized()) {
            Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
            return;
        }
        MeetingService meetingService = zoomSDK.getMeetingService();
        if(meetingService == null)
            return;
        if(meetingService.getMeetingStatus() == MeetingStatus.MEETING_STATUS_IDLE){
            startZoomMeeting(ZOOM_ACCESS_TOKEN);
        } else {
            meetingService.returnToMeeting(this);
        }
        overridePendingTransition(0, 0);
    }

    public void startZoomMeeting(String ZOOM_ACCESS_TOKEN) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if(!zoomSDK.isInitialized()) {
            Toast.makeText(this, "ZoomSDK has not been initialized successfully", Toast.LENGTH_LONG).show();
            return;
        }
        if(MEETING_ID == null) {
            Toast.makeText(this, "MEETING_ID in Constants can not be NULL", Toast.LENGTH_LONG).show();
            return;
        }
        MeetingService meetingService = zoomSDK.getMeetingService();
        StartMeetingOptions opts = new StartMeetingOptions();
        opts.no_driving_mode = false;
        opts.no_titlebar = false;
        opts.no_bottom_toolbar = false;
        opts.no_invite = false;
        //opts.no_meeting_end_message = false;

        StartMeetingParamsWithoutLogin params = new StartMeetingParamsWithoutLogin();
        params.zoomAccessToken = ZOOM_ACCESS_TOKEN;
        params.userId = USER_ID;
        params.meetingNo = MEETING_ID;
        params.displayName = DISPLAY_NAME;

        int ret = meetingService.startMeetingWithParams(this, params, opts);
        Log.d(TAG, "onClickBtnStartMeeting, ret=" + ret);
    }

    private void registerMeetingServiceListener() {
        Log.d(TAG, "registerMeetingServiceListener");
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        MeetingService meetingService = zoomSDK.getMeetingService();
        if(meetingService != null) {
            meetingService.addListener(this);
        }
    }
}
