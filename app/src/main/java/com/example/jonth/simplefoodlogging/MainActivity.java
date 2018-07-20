package com.example.jonth.simplefoodlogging;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.services.speech.v1beta1.Speech;
import com.google.api.services.speech.v1beta1.SpeechRequestInitializer;
import com.google.api.services.speech.v1beta1.model.RecognitionAudio;
import com.google.api.services.speech.v1beta1.model.RecognitionConfig;
import com.google.api.services.speech.v1beta1.model.SpeechRecognitionResult;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeRequest;
import com.google.api.services.speech.v1beta1.model.SyncRecognizeResponse;
import com.textrazor.AnalysisException;
import com.textrazor.NetworkException;
import com.textrazor.TextRazor;
import com.textrazor.annotations.AnalyzedText;
import com.textrazor.annotations.Entity;
import com.textrazor.annotations.Response;
import com.textrazor.annotations.Word;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static String id1 = "test_channel_01";
    private static final int MY_PERMISSIONS_RECORD_AUDIO = 1;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    private FoodExtractor foodExtractor;
    private FloatingActionButton record;
    private TextRazor client;
    public enum MealType {
        BREAKFAST, LUNCH, DINNER, SNACK, NONE
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        record = findViewById(R.id.fab);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Initialize TextRazor
        client = new TextRazor(BuildConfig.ApiKey);
        client.addExtractor("words");
        client.addExtractor("entities");

        foodExtractor = new FoodExtractor();
        String hq1 = "I ate 2 bowls of chicken tikka masala and 3 coca colas for dinner.";
        String hq2 = "five apples";
        List<FoodEntry> foodEntries = foodExtractor.handleQuery(hq1);

        displayMeals(foodEntries);
//        handleQuery("I ate 2 bowls of chicken tikka masala and 3 coca colas for dinner.");

//        handleQuery("I had some veggie for lunch today.");

//        handleQuery("I had some beef for breakfast");
//        handleQuery("I ate 2 pizzas and an egg for breakfast.");
        createchannel();
        requestRecordAudioPermission();
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent number5 = new Intent(getBaseContext(), VoiceListenService.class);
                number5.putExtra("times", 5);
                createchannel();
                startService(number5);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    protected void handleQuery(String query) {
        Response response = analyzeQuery(query);

//        Log.v("Response", response.getCleanedText());
        List<Entity> foods = findFoods(response);
        List<Word> quantities = findQuantities(response);


        FoodExtractor.MealType mealType = findMealType(response);
        List<FoodEntry> foodEntries = createFoodEntries(foods, quantities, mealType);

        displayMeals(foodEntries);


    }

    protected void displayMeals(List<FoodEntry> foodEntries) {
        LinearLayout breakfastLayout = (LinearLayout) findViewById(R.id.breakfast_linear_layout_view);
        LinearLayout lunchLayout = (LinearLayout) findViewById(R.id.lunch_linear_layout_view);
        LinearLayout dinnerLayout = (LinearLayout) findViewById(R.id.dinner_linear_layout_view);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (FoodEntry item : foodEntries) {
            View view;
            LinearLayout linearLayout;
            if (item.getMealType() == FoodExtractor.MealType.BREAKFAST) {
                view  = inflater.inflate(R.layout.food_entry_view, breakfastLayout, false);
                linearLayout = breakfastLayout;
            } else if (item.getMealType() == FoodExtractor.MealType.LUNCH) {
                view  = inflater.inflate(R.layout.food_entry_view, lunchLayout, false);
                linearLayout = lunchLayout;
            } else {
                view  = inflater.inflate(R.layout.food_entry_view, dinnerLayout, false);
                linearLayout = dinnerLayout;
            }

            TextView foodname = (TextView) view.findViewById(R.id.food_name_entry);
            TextView foodQuantity = (TextView) view.findViewById(R.id.food_quantity);

            foodname.setText(item.getName());
            foodQuantity.setText(Integer.toString(item.getQuantity()) + " servings");
            // set item content in view
            linearLayout.addView(view);
        }
    }

    protected Response analyzeQuery(String query) {
        try {
            AnalyzedText response = client.analyze(query);
            return response.getResponse();
        } catch (NetworkException e) {
            e.printStackTrace();
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected List<FoodEntry> createFoodEntries(List<Entity> foods, List<Word> quantities, FoodExtractor.MealType mealType)
    {
        List<FoodEntry> foodEntries = new ArrayList<FoodEntry>();
        for(Entity foodEntity: foods) {
            FoodEntry food = new FoodEntry();
            food.setMealType(mealType);
            food.setTime(new DateTime());
            food.setQuantity(1);
            food.setName(foodEntity.getEntityId().toString());
            foodEntries.add(food);
            Log.v("Analyzed names", foodEntity.getEntityId().toString());
        }


        if(quantities != null) {
            int currentSmallestQuantityDistance = Integer.MAX_VALUE;
            Entity closestFood = null;
            for(Word quantity: quantities) {
                for(Entity foodEntity: foods) {
                    if (quantity.getStartingPos() < foodEntity.getStartingPos() && foodEntity.getStartingPos() - quantity.getStartingPos() < currentSmallestQuantityDistance) {
                        currentSmallestQuantityDistance = foodEntity.getStartingPos() - quantity.getPosition();
                        closestFood = foodEntity;
                    }
                }

                for(FoodEntry food: foodEntries) {
                    if(closestFood.getEntityId().equals(food.getName())) {
                        food.setQuantity(Integer.parseInt(quantity.getToken()));
                    }
                }
            }
        }

        return foodEntries;
    }

    protected ArrayList<Entity> findFoods(Response response) {
        List<Entity> keywords = response.getEntities();

        for(Entity en: keywords){
            Log.v("logentity", logEntity(en));
        }

        ArrayList<Entity> foods = new ArrayList<Entity>();

        for(Entity keyword: keywords){
            List<String> itemTypes = keyword.getFreebaseTypes();

            Log.v("Food&Type", keyword.getEntityId()+":"+itemTypes);
            if(stringContainsItemFromList("/food/food", itemTypes) && keyword.getConfidenceScore() > 1){
//                Log.v("Match Food", keyword.getEntityId());
                foods.add(keyword);
            }
        }
        removeDuplicates(foods);
        return foods;
    }

    protected static List<Word> findQuantities(Response response) {
        List<Word> words = response.getWords();
        List<Word> quantities = new ArrayList<Word>();

        for(Word word: words){
            if(word.getPartOfSpeech().contains("CD")){
                quantities.add(word);
            }
        }

        return quantities;
    }

    protected FoodExtractor.MealType findMealType(Response response) {
        List<Entity> keywords = response.getEntities();

        // Find meal keyword
        for (Entity keyword : keywords) {
            List<String> itemTypes = keyword.getFreebaseTypes();

            if (stringContainsItemFromList("/dining/cuisine", itemTypes) || stringContainsItemFromList("/travel/accommodation_feature", itemTypes)) {
                try{
                    return FoodExtractor.MealType.valueOf(keyword.getEntityId().toUpperCase());
                }catch(IllegalArgumentException ex){
                    continue;
                }
            }
        }

        // Find the closest time if there was no keyword
        return findClosestMealTime(response);
    }

    protected FoodExtractor.MealType findClosestMealTime(Response response) {
        DateTime dt = new DateTime();
        int hour = dt.getHourOfDay();

        if(hour >= 6 && hour < 11){
            return FoodExtractor.MealType.BREAKFAST;
        }else if(hour >= 11 && hour < 14){
            return FoodExtractor.MealType.LUNCH;
        }else if(hour >= 17 && hour <= 21) {
            return FoodExtractor.MealType.DINNER;
        }else{
            return FoodExtractor.MealType.SNACK;
        }
    }

    protected void removeDuplicates(List<Entity> foods) {
        List<Entity> duplicates = new ArrayList<Entity>();
        Set<Integer> uniqueWordPositions = new HashSet<Integer>();
        sortByPhraseLength(foods);

        for (Entity food: foods) {
            for (Word word : food.getMatchingWords()) {
                if (uniqueWordPositions.contains(word.getPosition())) {
                    duplicates.add(food);
                    break;
                } else {
                    uniqueWordPositions.add(word.getPosition());
                }
            }
        }
        foods.removeAll(duplicates);
    }

    public void sortByPhraseLength(List<Entity> foods) {
        Collections.sort(foods, new Comparator<Entity>(){
            public int compare(Entity o1, Entity o2){
                if(o1.getMatchingWords().size() == o2.getMatchingWords().size())
                    return 0;

                return o1.getMatchingWords().size() > o2.getMatchingWords().size() ? -1 : 1;
            }
        });
    }

    public static boolean stringContainsItemFromList(String inputStr, List<String> items) {
        if(items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (inputStr.contains(items.get(i))) {
//                    Log.v("Contain",items.get(i));
                    return true;
                }
            }
        }

        return false;
    }


    private String logEntity(Entity entity){
        String text = "";
        if(entity.getData()!=null)
            text += "data:" + entity.getData() + "\n";
        if(entity.getEntityId()!=null)
            text += "enid:" + entity.getEntityId() + "\n";
        if(entity.getFreebaseTypes()!=null)
            text += "freetype:" + entity.getFreebaseTypes() + "\n";
        if(entity.getMatchedText()!=null)
            text += "matchedtext:" + entity.getMatchedText() + "\n";
        return text;
    }

    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }


    private void createchannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Log.v("Notification","Start Notification");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel mChannel = new NotificationChannel("channel 1",
                    getString(R.string.channel_name),  //name of the channel
                    NotificationManager.IMPORTANCE_LOW);   //importance level
            //important level: default is is high on the phone.  high is urgent on the phone.  low is medium, so none is low?
            // Configure the notification channel.
            mChannel.setDescription(getString(R.string.channel_description));
            mChannel.enableLights(true);
            // Sets the notification light color for notifications posted to this channel, if the device supports this feature.
            mChannel.setShowBadge(true);
            nm.createNotificationChannel(mChannel);
        }
    }
}
