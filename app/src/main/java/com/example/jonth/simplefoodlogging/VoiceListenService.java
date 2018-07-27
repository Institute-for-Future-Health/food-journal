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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beans.Food;
import com.beans.FoodBean;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    Intent msgForMainActivity;

    private String partialResult = "";

    private FirebaseDatabase database;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        foodList = new ArrayList<FoodEntry>();

        //Init text razor for text analysis
        foodExtractor = new FoodExtractor();

        //Init sharedPreference for sotring data locally
        sharedpreferences = getApplicationContext().getSharedPreferences(Constants.preferenceToken, Context.MODE_PRIVATE);

        //Initialize the firebase database instance
        database = FirebaseDatabase.getInstance();


        //TODO do something useful
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                ((AudioManager) Objects.requireNonNull(
                        getSystemService(Context.AUDIO_SERVICE))).setStreamMute(AudioManager.STREAM_SYSTEM, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Initialize intent for broadcast sender
        msgForMainActivity = new Intent(STEP_UPDATE);
        manager = LocalBroadcastManager.getInstance(getApplicationContext());

        //Initialaize the voice recognizer
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
        if(!startListening) return;         //If user has not said "start", then don't do anything
//        if(startRecordTime == 0) startRecordTime = System.currentTimeMillis();

        int spentTime = millToSeconds(startTalkTime, System.currentTimeMillis());       //Check how much time since the start point
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
        if(!oralAlert){              //If user has not said anything in first 5 seconds, program alerts
            if(millToSeconds(startTalkTime,System.currentTimeMillis()) >= 5){
                oralAlert = true;
                speakText(Constants.vocalAlert, 2);
            }
        }
        if(gotFoodname == 1){            //If valid any food name has been caught, speak out to confirm
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

        //If the user has not said "start", don't do anything
        if(!startListening && !result.equals(Constants.startCommand)) return;

        //After user said "start"
        if(!startListening){
            startListening = true;
            speakText(Constants.startIndicate, 2);
            startTalkTime = System.currentTimeMillis();
        }

        //If user says "stop", then stop listening
        if(result.equals("stop")){
            Speech.getInstance().stopListening();
            startTalkTime = 0;
        }

        //The time duration after the previous sentence
        int spentTime = millToSeconds(lastResultTime, System.currentTimeMillis());
        if(spentTime < 5)
            return;


        Log.d("Result", result+"");
        if (!TextUtils.isEmpty(result)) {

            //Extract the valid food names
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            foodList = foodExtractor.handleQuery(result);
            String names = "";
            Log.v("Names", foodList.size()+"");
            if(foodList.size()>0){
                for(FoodEntry fn: foodList){
                    names += fn.getQuantity() + " " + fn.getName() + ",";
                    fd.getInfoFromDatabase(fn.getName(),fn.getQuantity());
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

        public void getInfoFromDatabase(String name, int quantity) {
            DatabaseReference databaseRef = database.getReference("food-collection");
            databaseRef.child(name).addListenerForSingleValueEvent(new ValueEventListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getValue() == null){
                        Log.v("FromDatabase", "Food not found, searching on API");
                        getInfoFromAPI(name, quantity);
                        return;
                    }


                    List<HashMap<String, Object>> foods = (List<HashMap<String, Object>>) dataSnapshot.getValue();

                    Log.v("FromDatabase", foods.toString());
                    Log.v("FromDatabase", foods.size() + "");
                    setMincal(Integer.MAX_VALUE);
                    setMaxcal(0);
                    for (int i = 0; i < foods.size(); i++) {
                        int cal = Math.toIntExact((long)foods.get(i).get("energy"));
//                        int fat = foods.get(i).getFat();
//                        int pro = foods.get(i).getProtein();
//                        int carb = foods.get(i).getCarbon();
                        if(cal != 0){
                            setMaxcal(Math.max(cal, getMaxcal()));
                            setMincal(Math.min(cal, getMincal()));
                        }

                    }

                    String energy = "";
                    if (mincal == maxcal) energy = mincal+"";
                    else energy = mincal + " to " + maxcal;


                    //Store data in local
                    SharedPreferences.Editor editor = sharedpreferences.edit();

                    JSONObject obj = new JSONObject();
                    try {
                        obj.put(Constants.nameTitle, name);
                        obj.put(Constants.energyTitle, energy);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    foodInfo.put(obj);
                    editor.putString("foodinfo", foodInfo.toString());
                    Log.v("Saved", foodInfo.toString());
                    editor.commit();

                    msgForMainActivity.putExtra(Constants.nameTitle, name);
                    msgForMainActivity.putExtra(Constants.energyTitle, energy);
                    manager.sendBroadcast(msgForMainActivity);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }

        public void getInfoFromAPI(String name, int quantity) {
            String paraFood = "ingr=" + name;
            String requestUrl = baseUrl + paraFood + parameter;

            AsyncHttpClient client = new AsyncHttpClient();

            client.get(requestUrl, null, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.v("Request", response + "");
                    try {
                        JSONArray hintsJson = response.getJSONArray("hints");
                        List<Food> foods = new ArrayList<Food>();
                        setMincal(Integer.MAX_VALUE);
                        setMaxcal(0);
                        for (int i = 0; i < hintsJson.length(); i++) {
                            JSONObject jobj = hintsJson.getJSONObject(i).getJSONObject("food");
                            int cal = 0;
                            int fat = 0;
                            int pro = 0;
                            int carb = 0;
                            if (!jobj.getJSONObject("nutrients").isNull("ENERC_KCAL")){
                                cal = (int) Math.round((double)jobj.getJSONObject("nutrients").get("ENERC_KCAL"));
                                Log.v("Fetched", cal + "");
                            }
                            if (!jobj.getJSONObject("nutrients").isNull("PROCNT"))
                                pro = (int) Math.round((double)jobj.getJSONObject("nutrients").get("PROCNT"));
                            if (!jobj.getJSONObject("nutrients").isNull("FAT"))
                                fat = (int) Math.round((double)jobj.getJSONObject("nutrients").get("FAT"));
                            if (!jobj.getJSONObject("nutrients").isNull("CHOCDF"))
                                carb = (int) Math.round((double)jobj.getJSONObject("nutrients").get("CHOCDF"));
//                            if(!hintsJson.getJSONObject(i).getJSONObject("food").getJSONObject("label").toString().equals(name.toUpperCase())) continue;
                            foods.add(new Food(jobj.get("label").toString(), cal,pro, fat, carb ));

                            if(cal!=0){
                                setMaxcal(Math.max(cal, getMaxcal()));
                                setMincal(Math.min(cal, getMincal()));
                            }
                        }

                        writeToDatabase(name, foods);

                        //Store data in local
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        String energy = "";
                        if (mincal == maxcal) energy = mincal+"";
                        else energy = mincal + " to " + maxcal;

                        JSONObject obj = new JSONObject();
                        obj.put(Constants.nameTitle, name);
                        obj.put(Constants.energyTitle, energy);
                        foodInfo.put(obj);
                        editor.putString("foodinfo", foodInfo.toString());
                        Log.v("Saved", foodInfo.toString());
                        editor.commit();

                        //Notify MainActivity to update the list
                        msgForMainActivity.putExtra(Constants.nameTitle, name);
                        msgForMainActivity.putExtra(Constants.energyTitle, energy);
                        manager.sendBroadcast(msgForMainActivity);

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

        private void writeToDatabase(String name, List<Food> foods){
            DatabaseReference databaseRef = database.getReference("food-collection");
            Map<String, Object> hopperUpdates = new HashMap<>();
            hopperUpdates.put(name, foods);

            databaseRef.updateChildren(hopperUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.v("Database", "Successfully added " + foods.size() + " pieces");
                }
            });

//            Log.v("From databse", myRef.child("FoodItem").toString());
//            myRef.child("FoodItem").setValue(foods).addOnSuccessListener(new OnSuccessListener<Void>() {
//                @Override
//                public void onSuccess(Void aVoid) {
//                    Log.v("Database", "Success" + foods.size());
//                }
//            });
            Log.v("Database", "list size:" + foods.size());
        }

    }




}
