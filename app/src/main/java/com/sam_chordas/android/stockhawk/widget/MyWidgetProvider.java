package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

public class MyWidgetProvider extends AppWidgetProvider {

  public static final String TOAST_ACTION = "com.example.android.stackwidget.TOAST_ACTION";
  public static final String EXTRA_ITEM = "com.example.android.stackwidget.EXTRA_ITEM";

  @Override
  public void onDeleted(Context context, int[] appWidgetIds) {
    super.onDeleted(context, appWidgetIds);
  }

  @Override
  public void onDisabled(Context context) {
    super.onDisabled(context);
  }

  @Override
  public void onEnabled(Context context) {
    super.onEnabled(context);
  }

  @Override
  public void onReceive(Context context, Intent intent) {

    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
    if (intent.getAction().equals(TOAST_ACTION)) {
      int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
              AppWidgetManager.INVALID_APPWIDGET_ID);
      int viewIndex = intent.getIntExtra(EXTRA_ITEM, 0);
      Toast.makeText(context, "Touched view " + viewIndex, Toast.LENGTH_SHORT).show();
      Intent appIntent= new Intent(context, MyStocksActivity.class);
      appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
      context.startActivity(appIntent);
    }
    super.onReceive(context, intent);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                       int[] appWidgetIds) {

      for (int i = 0; i < appWidgetIds.length; ++i) {

          // Here we setup the intent which points to the StackViewService which will
          // provide the views for this collection.
          Intent intent = new Intent(context, MyWidgetService.class);
          intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
          // When intents are compared, the extras are ignored, so we need to embed the extras
          // into the data so that the extras will not be ignored.
          intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
          RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
          rv.setRemoteAdapter(R.id.lv_stocks, intent);

          // The empty view is displayed when the collection has no items. It should be a sibling
          // of the collection view.
          rv.setEmptyView(R.id.lv_stocks, R.id.update);

          // Here we setup the a pending intent template. Individuals items of a collection
          // cannot setup their own pending intents, instead, the collection as a whole can
          // setup a pending intent template, and the individual items can set a fillInIntent
          // to create unique before on an item to item basis.
          Intent toastIntent = new Intent(context, MyWidgetProvider.class);
          toastIntent.setAction(MyWidgetProvider.TOAST_ACTION);
          toastIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
          intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
          PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                  PendingIntent.FLAG_UPDATE_CURRENT);
          rv.setPendingIntentTemplate(R.id.lv_stocks, toastPendingIntent);

          appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
      }
      super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

} 