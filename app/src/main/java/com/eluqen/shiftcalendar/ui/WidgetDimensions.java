package com.eluqen.shiftcalendar.ui;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
public final class WidgetDimensions {
 private WidgetDimensions(){}
 public static void record(Context c,int id,Bundle options){
  if(options==null)return;
  int w=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,0);
  int h=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,0);
  int maxW=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);
  int maxH=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);
  if(w<=0)w=maxW;if(h<=0)h=maxH;
  if(w>0&&h>0){
   c.getSharedPreferences("eluqen_widget_dimensions",0).edit().putInt("width_"+id,w).putInt("height_"+id,h).apply();
   Log.i("ELUQEN_WIDGET_SIZE","Widget "+id+" size "+w+"x"+h+"dp; ranges "+maxW+"x"+maxH);
  }
 }
 public static void delete(Context c,int[] ids){
  if(ids==null)return;
  android.content.SharedPreferences.Editor e=c.getSharedPreferences("eluqen_widget_dimensions",0).edit();
  for(int id:ids){e.remove("width_"+id);e.remove("height_"+id);}e.apply();
 }
}
