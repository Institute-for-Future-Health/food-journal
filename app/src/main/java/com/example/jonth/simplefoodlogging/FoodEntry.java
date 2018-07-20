package com.example.jonth.simplefoodlogging;
import org.joda.time.DateTime;

public class FoodEntry
{
    //    logType: foodIntake
//    Time:
//    contents:
//    {
//        time: start
//        name:
//        duration:
//        nutrition:
//        quantity:
//        mealType:
//        emotion:
//        metadata: {
//            img:
//            audio:
//            recipe:
//            restaurant:
//        }
//    }

    private String name;
    private DateTime time;
    private float duration;
    private int quantity;
    private String nutrition;
    private FoodExtractor.MealType mealType;
    private String emotion;

    public FoodEntry(String name, DateTime time, int quantity, FoodExtractor.MealType mealType, String emotion) {
        this.name = name;
        this.time = time;
        this.quantity = quantity;
        this.mealType = mealType;
        this.emotion = emotion;
    }

    public FoodEntry() {
        this.name = "";
        this.time = new DateTime();
        this.quantity = 1;
        this.mealType = FoodExtractor.MealType.NONE;
        this.emotion = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public FoodExtractor.MealType getMealType() {
        return mealType;
    }

    public void setMealType(FoodExtractor.MealType mealType) {
        this.mealType = mealType;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
}
