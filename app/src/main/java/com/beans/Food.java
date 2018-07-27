package com.beans;

public class Food {
    private String label;
    private int energy;
    private int protein;
    private int fat;
    private int carbon;

    public Food(String label, int energy, int protein, int fat, int carbon) {
        this.label = label;
        this.energy = energy;
        this.protein = protein;
        this.fat = fat;
        this.carbon = carbon;
    }

    public String getLabel() {
        return label;
    }

    public int getEnergy() {
        return energy;
    }

    public int getProtein() {
        return protein;
    }

    public int getFat() {
        return fat;
    }

    public int getCarbon() {
        return carbon;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public void setProtein(int protein) {
        this.protein = protein;
    }

    public void setFat(int fat) {
        this.fat = fat;
    }

    public void setCarbon(int carbon) {
        this.carbon = carbon;
    }
}
