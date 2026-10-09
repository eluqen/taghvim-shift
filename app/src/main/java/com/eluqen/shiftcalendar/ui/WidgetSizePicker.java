package com.eluqen.shiftcalendar.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Build;
import android.widget.Toast;
import android.app.Activity;

public final class WidgetSizePicker {
 private WidgetSizePicker(){}

 private static String className(String preset){
  return "com.eluqen.shiftcalendar.widget.ShiftWidgetProviderLarge";
 }

 public static void pin(Activity a,String preset){
  try{
   if(Build.VERSION.SDK_INT<26){
    Toast.makeText(a,"این لانچر افزودن مستقیم ویجت را پشتیبانی نمی‌کند",Toast.LENGTH_LONG).show();
    return;
   }
   AppWidgetManager m=AppWidgetManager.getInstance(a);
   if(!m.isRequestPinAppWidgetSupported()){
    Toast.makeText(a,"از فهرست ویجت‌های صفحه اصلی، ویجت تقویم شیفت را اضافه کنید",Toast.LENGTH_LONG).show();
    return;
   }
   boolean ok=m.requestPinAppWidget(new ComponentName(a.getPackageName(),className(preset)),null,null);
   if(!ok)Toast.makeText(a,"لانچر درخواست افزودن ویجت را نپذیرفت",Toast.LENGTH_LONG).show();
  }catch(Exception e){
   android.util.Log.e("ELUQEN_WIDGET","Pin preset",e);
   Toast.makeText(a,"افزودن ویجت با این اندازه انجام نشد",Toast.LENGTH_LONG).show();
  }
 }
}
