package com.example.jonth.simplefoodlogging;
import org.joda.time.DateTime;

public class FoodEntry
{
    private String name;
    private DateTime time;
    private float duration;
    private int quantity;
    private String nutrition;
    private FoodExtractor.MealType mealType;
    private String emotion;

    private String energy;
    private String protein;
    private String fat;
    private String carbon;


    public FoodEntry(String name, DateTime time, int quantity, FoodExtractor.MealType mealType, String emotion, String energy, String protein, String fat, String carbon) {
        this.name = name;
        this.time = time;
        this.quantity = quantity;
        this.mealType = mealType;
        this.emotion = emotion;

        this.energy = energy;
        this.protein = protein;
        this.fat = fat;
        this.carbon = carbon;

    }

    public void setEnergy(String energy) {
        this.energy = energy;
    }

    public void setProtein(String protein) {
        this.protein = protein;
    }

    public void setFat(String fat) {
        this.fat = fat;
    }

    public void setCarbon(String carbon) {
        this.carbon = carbon;
    }

    public String getEnergy() {
        return energy;
    }

    public String getProtein() {
        return protein;
    }

    public String getFat() {
        return fat;
    }

    public String getCarbon() {
        return carbon;
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
