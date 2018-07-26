package com.example.jonth.simplefoodlogging.com.futurehealth.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.jonth.simplefoodlogging.FoodBean;
import com.example.jonth.simplefoodlogging.FoodEntry;
import com.example.jonth.simplefoodlogging.R;

import java.util.List;

public class FoodAdapter extends BaseAdapter {
    private Context context;
    private List<FoodBean> foodlist;
    public FoodAdapter(Context context, List<FoodBean> foodlist) {
        super();
        this.context = context;
        this.foodlist = foodlist;
    }

    @Override
    public int getCount() {
        return foodlist.size();
    }

    @Override
    public Object getItem(int position) {
        return foodlist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RelativeLayout relaytivelayout;
        if(convertView==null){
            relaytivelayout=(RelativeLayout) View.inflate(context, R.layout.food_entry_view, null);
        }
        else{
            relaytivelayout=(RelativeLayout) convertView;
        }
        FoodBean curEntry = (FoodBean)getItem(position);
        TextView fdname = relaytivelayout.findViewById(R.id.food_name_entry);
        TextView fdqty = relaytivelayout.findViewById(R.id.food_quantity);
        TextView fdinfo = relaytivelayout.findViewById(R.id.info);
        fdname.setText(curEntry.getName());
        fdinfo.setText(curEntry.getEnergy() + " cal");
//        fdqty.setText(curEntry.getQuantity()+"");

        return relaytivelayout;
    }
}
