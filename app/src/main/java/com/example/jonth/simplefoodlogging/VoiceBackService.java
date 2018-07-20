package com.example.jonth.simplefoodlogging;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.vikramezhil.droidspeech.DroidSpeech;
import com.vikramezhil.droidspeech.OnDSListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class VoiceBackService extends Service implements OnDSListener{
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final static String TAG = "MyForegroundService";

    private final int TimeThreshold = 10;

    Random r;

    private DroidSpeech droidSpeech;
    private TextToSpeech speaker;
    AudioManager audioManager;              //Controling the audio play

    private boolean voiceDetected = false;  //Bool that indicates if any voice has been detected
    private boolean oralAlert = false;      //Bool that indicates if the oral alert has already been done or not

    private long startSpeakingTime = 0;
    private boolean startedCount = false;

    private String curResult = "";

    private String finishedSentence = "";

    private List<FoodEntry> foodList;
    private boolean listening = false;

    public VoiceBackService() {
        foodList = new ArrayList<FoodEntry>();
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(Message msg) {
            //promote to foreground and create persistent notification.
            //in Oreo we only have a few seconds to do this or the service is killed.
//            Notification notification = getNotification("MyService is running");
//            startForeground(msg.arg1, notification);  //not sure what the ID needs to be.
//            Log.d(TAG, "should be foreground now.");
            int times = 1, i;

            synchronized (this){
                toast("Start");
            }
            //loop that many times, sleeping for 5 seconds.
            for (i = 0; i < times; i++) {
                synchronized (this) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                }
                String info = i + " random ";

            }

//        return super.onStartCommand(intent, flags, startId);
//        return START_STICKY;
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1);  //notification will go away as well.
        }
    }
    final Handler mHandler = new Handler();

    // Helper for showing tests
    void toast(final CharSequence text) {
        mHandler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                Toast.makeText(VoiceBackService.this, text, Toast.LENGTH_SHORT).show();
                Vibrator v = (Vibrator) getSystemService(getApplication().VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                Log.v("Start", "service");
                speechInit();
            }
        });
    }

    @Override
    public void onDroidSpeechSupportedLanguages(String currentSpeechLanguage, List<String> supportedSpeechLanguages) {
        Log.v("Droid Lang", "Current speech language = " + currentSpeechLanguage);
    }

    @Override
    public void onDroidSpeechRmsChanged(float rmsChangedValue) {
        Log.v("RMS", rmsChangedValue + "");
    }

    @Override
    public void onDroidSpeechLiveResult(String liveSpeechResult) {
        Log.v("Live Result", "Live speech result = " + liveSpeechResult);
        if(!oralAlert){
            startSpeakingTime = System.currentTimeMillis();
            oralAlert = true;
        }
    }

    @Override
    public void onDroidSpeechFinalResult(String finalSpeechResult) {
        Log.v("Final Result", finalSpeechResult);
        int spentTime = millToSeconds(startSpeakingTime, System.currentTimeMillis());
        if(spentTime >= TimeThreshold){
            finishedSentence = curResult;
            Log.v("Finished Sentence", finishedSentence);
        }else {
            curResult += " " + finalSpeechResult;
        }
    }

    @Override
    public void onDroidSpeechClosedByUser() {
        Log.v("Event", "Closed");
    }

    @Override
    public void onDroidSpeechError(String errorMsg) {
        Log.v("Event", "Error");
        droidSpeech.closeDroidSpeechOperations();
        droidSpeech.startDroidSpeechRecognition();
    }

    private void speechInit(){
        audioManager = (AudioManager)getSystemService(getApplication().AUDIO_SERVICE);
        droidSpeech = new DroidSpeech(this, null);
        droidSpeech.setOnDroidSpeechListener(this);
        droidSpeech.setShowRecognitionProgressView(false);
        droidSpeech.setOfflineSpeechRecognition(true);
        droidSpeech.setOneStepResultVerify(false);
        droidSpeech.startDroidSpeechRecognition();
        Log.v("Event", "Start Listening");
    }


    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;//needed for stop.
        msg.setData(intent.getExtras());
        mServiceHandler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
    }

    @Override
    public void onDestroy() {
//        if(speaker != null){
//            speaker.stop();
//            speaker = null;
//            Log.v("Destroy","Destroy speaker");
//        }
//        Intent newservice = new Intent(this, VoiceBackService.class);
//        startService(newservice);
        Log.v("Destroy","Destroy service");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    // build a persistent notification and return it.
    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification getNotification(String message) {

        return new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)  //persistent notification!
                .setChannelId(MainActivity.id1)
                .setContentTitle("Service")   //Title message top row.
                .setContentText(message)  //message when looking at the notification, second row
                .build();  //finally build and return a Notification.
    }

    private void speakerInitialize(){
        speaker = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = speaker.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.v("TTS", "This Language is not supported");
                    }
                } else {
                    Log.v("TTS", "Initilization Failed!");

                }
            }
        });
    }

    private void speakText(String text){
        Bundle param = new Bundle();
        param.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 2.0f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.v("Speaker",text);

            speaker.speak(text, TextToSpeech.QUEUE_FLUSH, param, null);
        }else{
            speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
//        if(!oralAlert)
//            oralAlert = true;
    }

    private void waitTrigger(long waitTime){
        Handler handler = new Handler()
        {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void handleMessage(Message msg)
            {
                // TODO Auto-generated method stub
                super.handleMessage(msg);
            }
        };
        handler.sendEmptyMessageDelayed(0, waitTime);
    }

    private void audioMute(boolean mute){
        if(audioManager == null) audioManager = (AudioManager)getSystemService(getApplication().AUDIO_SERVICE);
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
    }

    private int millToSeconds(long start, long end){
        return (int)((end - start) / 1000);
    }

    private void countTime(){
        Thread t = new Thread() {
            public void run() {
                int sec = 0;
                while (true){
                    sec += 1;
                    try
                    {
                        sleep(1000);
                    }
                    catch (InterruptedException e)
                    {}
                    if(sec == 5 && curResult == "" && !oralAlert){
                        speakText("Hi, what are you eating, my friend!");
                    }
                    if(sec==40&&finishedSentence.length() == 0){
                        Log.v("Event", "Close the voice");
                        droidSpeech.closeDroidSpeechOperations();
                    }
                }

            }
        };
        t.run();
    }




}
