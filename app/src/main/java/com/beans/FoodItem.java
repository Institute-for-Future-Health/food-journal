package com.beans;

class FoodItem{
    private FoodBean [] ItemList;

    public FoodItem(FoodBean[] itemList) {
        ItemList = itemList;
    }

    public FoodBean[] getItemList() {
        return ItemList;
    }

    public void setItemList(FoodBean[] itemList) {
        ItemList = itemList;
    }
}
