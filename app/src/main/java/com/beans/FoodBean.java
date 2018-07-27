package com.beans;

public class FoodBean {
    String name;
    String energy;
    String protein;
    String fat;
    String carbon;

    public FoodBean(String name, String energy, String protein, String fat, String carbon) {
        this.name = name;
        this.energy = energy;
        this.protein = protein;
        this.fat = fat;
        this.carbon = carbon;
    }

    public String getName() {
        return name;
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

    public void setName(String name) {
        this.name = name;
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

}
