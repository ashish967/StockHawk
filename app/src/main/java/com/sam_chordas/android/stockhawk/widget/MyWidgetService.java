package com.sam_chordas.android.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Binder;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ashish-novelroots on 19/6/16.
 */

public class MyWidgetService extends RemoteViewsService {




    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new MyWidgetRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    class MyWidgetRemoteViewsFactory implements RemoteViewsFactory {

        private List<WidgetItem> mWidgetItems = new ArrayList<WidgetItem>();
        private Context mContext;
        private int mAppWidgetId;

        public MyWidgetRemoteViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        @Override
        public void onCreate() {

            Cursor cursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{  "Distinct " +QuoteColumns.SYMBOL,QuoteColumns._ID, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, null,
                    null, null);

            DatabaseUtils.dumpCursor(cursor);
            if(cursor!=null) {
                cursor.moveToFirst();
                for (int i = 0; i < cursor.getCount(); i++){
                    WidgetItem item = new WidgetItem();
                    item.setSymbol(cursor.getString(cursor.getColumnIndex("symbol")));
                    item.setIsup(cursor.getInt(cursor.getColumnIndex("is_up")));
                    item.setBidprice(cursor.getString(cursor.getColumnIndex("bid_price")));
                    item.setChange(cursor.getString(cursor.getColumnIndex("percent_change")));
                    item.setPercentage_change(cursor.getString(cursor.getColumnIndex("change")));
                    mWidgetItems.add(item);
                    cursor.moveToNext();
                }
            }
        }

        @Override
        public void onDataSetChanged() {
            final long identityToken = Binder.clearCallingIdentity();
            mWidgetItems.clear();
            onCreate();
            Binder.restoreCallingIdentity(identityToken);

        }

        @Override
        public void onDestroy() {
            mWidgetItems.clear();

        }

        @Override
        public int getCount() {
            return mWidgetItems.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            // position will always range from 0 to getCount() - 1.

            // We construct a remote views item based on our widget item xml file, and set the
            // text based on the position.
            RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
            rv.setTextViewText(R.id.stock_symbol, mWidgetItems.get(position).getSymbol());
            rv.setTextViewText(R.id.bid_price, mWidgetItems.get(position).getBidprice());
            rv.setTextViewText(R.id.change, mWidgetItems.get(position).getChange());

            if(mWidgetItems.get(position).getIsup()==1) {
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_green);
            }
            else{
                rv.setInt(R.id.change, "setBackgroundResource", R.drawable.percent_change_pill_red);
            }
            // Next, we set a fill-intent which will be used to fill-in the pending intent template
            // which is set on the collection view in StackWidgetProvider.
            Bundle extras = new Bundle();
            extras.putInt(MyWidgetProvider.EXTRA_ITEM, position);
            Intent fillInIntent = new Intent();
            fillInIntent.putExtras(extras);
            rv.setOnClickFillInIntent(R.id.list_item, fillInIntent);

            // You can do heaving lifting in here, synchronously. For example, if you need to
            // process an image, fetch something from the network, etc., it is ok to do it here,
            // synchronously. A loading view will show up in lieu of the actual contents in the
            // interim.


            // Return the remote views object.
            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
