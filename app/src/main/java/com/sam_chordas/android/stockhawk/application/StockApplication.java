package com.sam_chordas.android.stockhawk.application;

import android.app.Application;
import android.content.Context;

/**
 * Created by ashish-novelroots on 18/6/16.
 */

public class StockApplication extends Application {


    public static Context mContext;
    @Override
    public void onCreate() {
        super.onCreate();
        mContext= getApplicationContext();
    }
}
