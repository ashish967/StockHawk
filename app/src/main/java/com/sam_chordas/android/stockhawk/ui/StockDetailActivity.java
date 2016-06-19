package com.sam_chordas.android.stockhawk.ui;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.db.chart.model.LineSet;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.application.StockApplication;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StockDetailActivity extends AppCompatActivity {

    private static final String TAG = StockDetailActivity.class.getSimpleName();
    private LineChartView mLineChartView;
    LineSet mLineset;
    String mSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        mLineChartView= (LineChartView) findViewById(R.id.linechart);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getIntent().getStringExtra(QuoteColumns.SYMBOL));
        mSymbol=getIntent().getStringExtra(QuoteColumns.SYMBOL);

        if(mLineset==null){

            loadHistoricalData();

        }

    }

    static class HistoryFetcher extends AsyncTask<String,Integer,LineSet>{


        public interface HistoryFetcherListener {
            public void onLoadFinished(LineSet lineSet);
        }

        HistoryFetcherListener listener;
        HistoryFetcher(HistoryFetcherListener listener){
            this.listener=listener;
        }
        @Override
        protected LineSet doInBackground(String... params) {
            StringBuilder urlStringBuilder = new StringBuilder();
            try{
                // Base URL for the Yahoo query
                String mSymbol= params[0];
                Calendar calendar= Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH,-7);
                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd");

                String startDate= format.format(calendar.getTime());
                String endDate=format.format(Calendar.getInstance().getTime());

                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(Uri.encode("select * from yahoo.finance.historicaldata where symbol = \""+mSymbol+"\" and startDate = \""+startDate+"\" and endDate = \""+endDate+"\""));
                urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");
                String json= new StockTaskService(StockApplication.mContext).fetchData(urlStringBuilder.toString());

                Log.d(TAG,urlStringBuilder.toString());
                Log.d(TAG,json);

                JSONObject jsonObject= new JSONObject(json);

                LineSet mLineset= new LineSet();

                JSONArray jsonArray= jsonObject.getJSONObject("query").getJSONObject("results").getJSONArray("quote");

                for(int i=0;i<jsonArray.length();i++){
                    mLineset.addPoint(jsonArray.getJSONObject(i).getString("Date"), (float) jsonArray.getJSONObject(i).getDouble("High"));
                }

                return mLineset;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(LineSet o) {


            listener.onLoadFinished(o);
        }
    }
    private void loadHistoricalData() {


        HistoryFetcher fetcher= new HistoryFetcher(new HistoryFetcher.HistoryFetcherListener() {
            @Override
            public void onLoadFinished(LineSet success) {
                mLineset= success;
                if(mLineset!=null){
                    mLineChartView.addData(mLineset);
                    float max=0;
                    for(int i=0;i<mLineset.size();i++){

                        if(max<mLineset.getValue(i)){
                            max=mLineset.getValue(i);
                        }
                    }
                    if(max>20) {
                        mLineChartView.setStep((int) (max/ 20));
                    } mLineChartView.show();
                }
                else{
                    Snackbar.make(findViewById(R.id.linechart),R.string.no_internet_connection,Snackbar.LENGTH_INDEFINITE)
                            .setAction(getString(R.string.snackbar_action), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    loadHistoricalData();
                                }
                            })
                            .show();
                }

                findViewById(R.id.loader).setVisibility(View.GONE);

            }

        });

        fetcher.execute(mSymbol);
        findViewById(R.id.loader).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==android.R.id.home){

            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
