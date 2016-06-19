package com.sam_chordas.android.stockhawk.service;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.application.StockApplication;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.widget.MyWidgetProvider;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService{
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STOCK_STATUS_OK,STOCK_STATUS_SERVER_DOWN,STOCK_STATUS_SERVER_INVALID,STOCK_STATUS_UNKNOWN})
    public @interface StockStatus{};
    public static final int STOCK_STATUS_OK=0;
    public static final int STOCK_STATUS_SERVER_DOWN=1;
    public static final int STOCK_STATUS_SERVER_INVALID=4;
    public static final int STOCK_STATUS_UNKNOWN=3;

    public StockTaskService(){}

    public StockTaskService(Context context){
        mContext = context;
    }
    public String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params){
        Cursor initQueryCursor;
        if (mContext == null){
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try{
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals("init") || params.getTag().equals("periodic")){
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
                    null, null);
            if (Utils.isFirstRequest(StockApplication.mContext)){
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null){
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();

                if(initQueryCursor.getCount()==0){

                    return GcmNetworkManager.RESULT_SUCCESS; // Since no stock in the list returning nothing.
                }
                for (int i = 0; i < initQueryCursor.getCount(); i++){
                    mStoredSymbols.append("\""+
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))+"\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")){
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\""+stockInput+"\")", "UTF-8"));
            } catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null){
            urlString = urlStringBuilder.toString();
            try{
                Log.d(LOG_TAG,"Url "+urlString);
                getResponse = fetchData(urlString);
                setStockStatus(StockApplication.mContext,STOCK_STATUS_UNKNOWN);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate){
                        contentValues.put(QuoteColumns.ISCURRENT, 0);
                        mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                                null, null);
                    }

                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                    mContext.getContentResolver().delete(QuoteProvider.Quotes.CONTENT_URI,QuoteColumns.ISCURRENT+"=?",new String[]{"0"});

                    setStockStatus(StockApplication.mContext,STOCK_STATUS_OK);
                    Utils.setFirstRequest(StockApplication.mContext);
                    ComponentName thisWidget = new ComponentName(StockApplication.mContext,
                            MyWidgetProvider.class);
                    AppWidgetManager manager=AppWidgetManager.getInstance(StockApplication.mContext);
                    int[] allWidgetIds = manager.getAppWidgetIds(thisWidget);
                    for (int widgetId:allWidgetIds) {
                        manager.notifyAppWidgetViewDataChanged(widgetId,R.id.lv_stocks);
                    }

                }catch (Exception e){
                    setStockStatus(StockApplication.mContext,STOCK_STATUS_SERVER_INVALID);
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                    e.printStackTrace();
                }
            } catch (IOException e){
                setStockStatus(StockApplication.mContext,STOCK_STATUS_SERVER_DOWN);
                        e.printStackTrace();
            }
        }

        return result;
    }

    public  static  void setStockStatus(Context c, @StockTaskService.StockStatus int stockstatus){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor spe = sp.edit();
        spe.putInt(c.getString(R.string.pref_stock_status_key), stockstatus);
        spe.commit();
    }
}
