package com.example.jonth.simplefoodlogging;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.sac.speech.GoogleVoiceTypingDisabledException;
import com.sac.speech.Speech;
import com.sac.speech.SpeechDelegate;
import com.sac.speech.SpeechRecognitionNotAvailable;
import com.tbruyelle.rxpermissions.RxPermissions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

public class VoiceListenService extends Service implements SpeechDelegate, Speech.stopDueToDelay  {


    public static SpeechDelegate delegate;
    private boolean startedTalk = false;
    private long startTalkTime = 0;
    private long startRecordTime = 0;
    private String curResult = "";
    private boolean oralAlert = false;
    private String finishedSentence = "";
    private boolean speakerunmute = false;

    private int gotFoodname = 0;
    private String foodnames = "";

    private List<FoodEntry> foodList;
    private FoodExtractor foodExtractor;

    long lastResultTime = 0;

    private boolean startListening = false;
    private boolean stopListening = false;

    private JSONArray foodInfo = new JSONArray();

    private FetchData fd = new FetchData();
    SharedPreferences sharedpreferences;

    public static String STEP_UPDATE = "1";

    public LocalBroadcastManager manager;
    Intent i;

    private String partialResult = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        foodList = new ArrayList<FoodEntry>();
        foodExtractor = new FoodExtractor();
        sharedpreferences = getApplicationContext().getSharedPreferences(Constants.preferenceToken, Context.MODE_PRIVATE);

        //TODO do something useful
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ((AudioManager) Objects.requireNonNull(
                        getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        i = new Intent(STEP_UPDATE);
        manager = LocalBroadcastManager.getInstance(getApplicationContext());

        Speech.init(this);
        delegate = this;
        Speech.getInstance().setListener(this);

        if (Speech.getInstance().isListening()) {
            Speech.getInstance().stopListening();
            if(!speakerunmute)
                muteBeepSoundOfRecorder(true);
        } else {
            System.setProperty("rx.unsafe-disable", "True");
            RxPermissions.getInstance(this).request(Manifest.permission.RECORD_AUDIO).subscribe(granted -> {
                if (granted) { // Always true pre-M
                    try {
                        Speech.getInstance().stopTextToSpeech();
                        Speech.getInstance().startListening(null, this);
                    } catch (SpeechRecognitionNotAvailable exc) {
                        //showSpeechNotSupportedDialog();

                    } catch (GoogleVoiceTypingDisabledException exc) {
                        //showEnableGoogleVoiceTyping();
                    }
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                }
            });
            if(!speakerunmute)
                muteBeepSoundOfRecorder(true);
        }
        return Service.START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSpecifiedCommandPronounced(String event) {
        Log.v("Specified", event);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ((AudioManager) Objects.requireNonNull(
                        getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Speech.getInstance().isListening()) {
            if(!speakerunmute)
                muteBeepSoundOfRecorder(true);
            Speech.getInstance().stopListening();
        } else {
            RxPermissions.getInstance(this).request(Manifest.permission.RECORD_AUDIO).subscribe(granted -> {
                if (granted) { // Always true pre-M
                    try {
                        Speech.getInstance().stopTextToSpeech();
                        Speech.getInstance().startListening(null, this);
                    } catch (SpeechRecognitionNotAvailable exc) {
                        //showSpeechNotSupportedDialog();

                    } catch (GoogleVoiceTypingDisabledException exc) {
                        //showEnableGoogleVoiceTyping();
                    }
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                }
            });
            if(!speakerunmute)
                muteBeepSoundOfRecorder(true);
        }
    }

    @Override
    public void onStartOfSpeech() {
        Log.v("Event","Speech Started");
        if(!startedTalk&&startListening){
            startedTalk = true;
            startTalkTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onSpeechRmsChanged(float value) {
        if(!startListening) return;
//        if(startRecordTime == 0) startRecordTime = System.currentTimeMillis();
        int spentTime = millToSeconds(startTalkTime, System.currentTimeMillis());
        if(spentTime >= Constants.CycleThreshold && partialResult == ""){
            Log.v("StopSign", spentTime + "");
            Speech.getInstance().stopListening();
            startListening = false;
            startTalkTime = 0;
        }
        if(value > 7&&!oralAlert){
            Toast.makeText(getApplicationContext(), "Voice Detected", Toast.LENGTH_SHORT).show();
            oralAlert = true;
        }
        if(!oralAlert){
            if(millToSeconds(startTalkTime,System.currentTimeMillis()) >= 5){
                oralAlert = true;
                speakText(Constants.vocalAlert, 2);
            }
        }
        if(gotFoodname == 1){
            Log.v("Event", "foodname detected");
            gotFoodname = 0;
            speakText("I got you, you had" + foodnames, 3);
            foodnames = "";
        }else if(gotFoodname == 2){
            Log.v("Event", "Not understand");
            gotFoodname = 0;
            speakText("Sorry, I did not catch any food name.", 3);
        }
    }

    @Override
    public void onSpeechPartialResults(List<String> results) {

        for (String partial : results) {
            partialResult += partial;
//            Log.v("Result", partial+"");
        }

    }

    @Override
    public void onSpeechResult(String result) {
        if(!startListening && !result.equals(Constants.startCommand)) return;
        if(!startListening){
            startListening = true;
            speakText(Constants.startIndicate, 2);
            startTalkTime = System.currentTimeMillis();
        }
        if(result.equals("stop")){
            Speech.getInstance().stopListening();
            startTalkTime = 0;
        }
        int spentTime = millToSeconds(lastResultTime, System.currentTimeMillis());
        if(spentTime < 5)
            return;
//        curResult += result;
//        if(spentTime >= 30){
//            finishedSentence = curResult;
//            curResult = "";
//        }

        Log.d("Result", result+"");
        if (!TextUtils.isEmpty(result)) {
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            foodList = foodExtractor.handleQuery(result);
            String names = "";
            Log.v("Names", foodList.size()+"");
            if(foodList.size()>0){
                for(FoodEntry fn: foodList){
                    names += fn.getQuantity() + " " + fn.getName() + ",";
                    fd.getInfo(fn.getName(), fn.getQuantity());
                }
                Log.v("Names", names);
            }
            if(names.length()>0){
                gotFoodname = 1;
                foodnames = names;
                lastResultTime = System.currentTimeMillis();
            }
            if(names == ""){
                Log.v("Names", "Nothing");
                gotFoodname = 2;
            }
            partialResult = "";

        }
    }

    /**
     * Function to remove the beep sound of voice recognizer.
     */
    private void muteBeepSoundOfRecorder(boolean control) {
        AudioManager amanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (amanager != null) {
            amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, control);
            amanager.setStreamMute(AudioManager.STREAM_RING, true);
            amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        //Restarting the service if it is removed.
        PendingIntent service =
                PendingIntent.getService(getApplicationContext(), new Random().nextInt(),
                        new Intent(getApplicationContext(), VoiceListenService.class), PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, service);
        super.onTaskRemoved(rootIntent);
    }

    private void speakText(String text, int duration){
        Bundle param = new Bundle();
        param.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 2.0f);
        Log.v("Speaker", text);
        Speech.getInstance().stopListening();
        muteBeepSoundOfRecorder(false);
        speakerunmute = true;
        Speech.getInstance().say(text);

        //CountDown some seconds to let the speak finish its speaking
        new CountDownTimer(duration*1000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                muteBeepSoundOfRecorder(true);
                Toast.makeText(getApplicationContext(),"start again", Toast.LENGTH_SHORT).show();
                RxPermissions.getInstance(getApplicationContext()).request(Manifest.permission.RECORD_AUDIO).subscribe(granted -> {
                    if (granted) { // Always true pre-M
                        try {
                            Speech.getInstance().stopTextToSpeech();
                            Speech.getInstance().startListening(null, VoiceListenService.this);
                        } catch (SpeechRecognitionNotAvailable exc) {
                            //showSpeechNotSupportedDialog();
                        } catch (GoogleVoiceTypingDisabledException exc) {
                            //showEnableGoogleVoiceTyping();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.permission_required, Toast.LENGTH_LONG).show();
                    }
                });
                speakerunmute = false;
            }
        }.start();
    }

    private int millToSeconds(long start, long end){
        return (int)((end - start) / 1000);
    }


    public class FetchData {
        public String baseUrl = "https://api.edamam.com/api/food-database/parser?";
        public String parameter = "&app_id="+ Constants.edamaId +"&app_key=" + Constants.edamamKey;
        public int maxcal = 0;
        public int mincal = Integer.MAX_VALUE;

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

        public void getInfo(String name, int quantity) {
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
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        String energy = "";
                        if (mincal == maxcal) energy = mincal+"";
                        else energy = mincal + " to " + maxcal;

                        JSONObject obj = new JSONObject();
                        obj.put(Constants.nameTitle, name);
                        obj.put(Constants.energyTitle, energy);

                        i.putExtra(Constants.nameTitle, name);
                        i.putExtra(Constants.energyTitle, energy);
                        manager.sendBroadcast(i);

                        foodInfo.put(obj);
                        editor.putString("foodinfo", foodInfo.toString());
                        Log.v("Saved", foodInfo.toString());
                        editor.commit();
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

    }


}
