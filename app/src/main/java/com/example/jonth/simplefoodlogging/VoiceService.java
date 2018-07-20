package com.example.jonth.simplefoodlogging;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class VoiceService extends Service implements RecognitionListener {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final static String TAG = "MyForegroundService";

    private final int TimeThreshold = 10;

    Random r;
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    private TextToSpeech speaker;
    AudioManager audioManager;              //Controling the audio play

    private boolean voiceDetected = false;  //Bool that indicates if any voice has been detected
    private boolean oralAlert = false;      //Bool that indicates if the oral alert has already been done or not

    private int spokingTime = 0;
    private long startSpeakingTime = 0;
    private boolean startedCount = false;

    private String curResult = "";


    public VoiceService() {

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
                Toast.makeText(VoiceService.this, text, Toast.LENGTH_SHORT).show();
                Vibrator v = (Vibrator) getSystemService(getApplication().VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                Log.v("Start", "service");
                initializeSpeechRecog();
                StartListening();
            }
        });
    }


    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new VoiceService.ServiceHandler(mServiceLooper);
    }

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
//        Intent number5 = new Intent(getBaseContext(), MyForeGroundService.class);
//        number5.putExtra("times", 5);
//        startService(number5);

        if(speech != null){
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech = null;
            Log.v("Destroy","Destroy Speech");

        }
        if(speaker != null){
            speaker.stop();
            speaker = null;
            Log.v("Destroy","Destroy speaker");
        }
        Intent newservice = new Intent(this, VoiceService.class);
        startService(newservice);
        Log.v("Destroy","Destroy service");
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }

    // build a persistent notification and return it.
    public Notification getNotification(String message) {

        return new Notification.Builder(getApplicationContext())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)  //persistent notification!
                .setContentTitle("Service")   //Title message top row.
                .setContentText(message)  //message when looking at the notification, second row
                .build();  //finally build and return a Notification.
    }


    public void StartListening() {
        //if setting.SpeechEnable
        Log.v("listen", "Start listening");
        speech.startListening(recognizerIntent);
    }


    private void initializeSpeechRecog(){
        audioManager = (AudioManager)getSystemService(getApplication().AUDIO_SERVICE);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        if(!oralAlert) audioMute(true);         //Here is for muting the beep of voice recognizer
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                /*
        Minimum time to listen in millis. Here 5 seconds
         */
//        recognizerIntent.putExtra("android.speech.extra.DICTATION_MODE", true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Log", "onBeginningOfSpeech");
        if(!startedCount)
            startSpeakingTime =  System.currentTimeMillis();
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d("Log", "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Log", "onEndOfSpeech");
    }



    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.d("Log", "FAILED " + errorMessage);
        StartListening();
    }

    @Override
    public void onEvent(int arg0, Bundle arg1) {
        Log.d("Log", "onEvent");
    }

    @Override
    public void onPartialResults(Bundle arg0) {
        Log.d("Log", "onPartialResults");
        audioMute(false);
        ArrayList<String> matches = arg0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        /* To get all close matchs
        for (String result : matches)
        {
            text += result + "\n";
        }
        */
        text += matches.get(0); //  Remove this line while uncommenting above codes
        Log.v("OnPartialResult", text);
    }

    @Override
    public void onReadyForSpeech(Bundle arg0) {
        Log.d("Log", "onReadyForSpeech");

    }

    @Override
    public void onResults(Bundle results) {
        audioMute(false);
        ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        int spentTime = millToSeconds(startSpeakingTime, System.currentTimeMillis());
        if(spentTime < TimeThreshold){
            curResult += data.get(0).toString();
        }else{
            Log.v("Finished Sentence", curResult);
            curResult = "";
        }
        voiceDetected = true;
        Log.v("Current Sentence", curResult);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.d("Log", "onRmsChanged: " + rmsdB);
//        progressBar.setProgress((int) rmsdB);

    }

    public void turnOf(){
        speech.stopListening();
        speech.destroy();
    }

    public String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                turnOf();
//                StartListening();
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
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
//                speakText("Hello");
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

        if(!oralAlert)
            oralAlert = true;
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
                Vibrator v = (Vibrator) getSystemService(getApplication().VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                Log.v("Start", "service");
                StartListening();
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




}
