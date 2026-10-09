package com.eluqen.shiftcalendar.ui;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

public final class WidgetTypography {
 private static final String STUDIO="eluqen_widget_studio";
 private static final String PREFS="eluqen_shift_calendar_preferences";
 private static final String[] IDS={"widget_date","widget_weekday","widget_shift","widget_plan","widget_hours","widget_groups","widget_next","widget_reminder"};
 private static final String[] KEYS={"date","weekday","shift","plan","hours","groups","next","reminder"};
 private static final int[] DEF_SIZE={17,12,14,11,11,11,11,11};
 private WidgetTypography(){}

 private static int id(Context c,String name){return c.getResources().getIdentifier(name,"id",c.getPackageName());}
 private static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 private static int positive(int value,int fallback){return value>0?value:fallback;}
 private static float clamp(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
 private static boolean option(SharedPreferences p,String key,boolean def){return p.getBoolean(key,def);}
 private static float requested(SharedPreferences p,int i){return clamp(p.getInt("size_"+KEYS[i],DEF_SIZE[i]),8f,26f);}

 public static void apply(Context c,RemoteViews rv,int appWidgetId){
  try{
   Bundle o=AppWidgetManager.getInstance(c).getAppWidgetOptions(appWidgetId);
   SharedPreferences dims=c.getSharedPreferences("eluqen_widget_dimensions",0);
   int w=positive(o.getInt("appWidgetMinWidth",-1),dims.getInt("width_"+appWidgetId,110));
   int h=positive(o.getInt("appWidgetMinHeight",-1),dims.getInt("height_"+appWidgetId,110));
   w=Math.max(90,Math.min(480,w));h=Math.max(70,Math.min(320,h));

   SharedPreferences s=c.getSharedPreferences(STUDIO,0);
   SharedPreferences p=c.getSharedPreferences(PREFS,0);

   boolean[] visible={
    option(s,"date",true),
    option(s,"weekday",true),
    option(s,"shift",true),
    option(s,"plan",false),
    option(s,"hours",true),
    p.getBoolean("widget_show_groups",true),
    option(s,"next",true)&&p.getBoolean("widget_show_next",true),
    option(s,"reminder",true)&&p.getBoolean("reminder_enabled",false)
   };

   float[] req=new float[8];
   float total=0f;
   for(int i=0;i<8;i++){
    req[i]=requested(s,i);
    if(visible[i])total+=req[i]*1.13f;
   }

   DisplayMetrics dm=c.getResources().getDisplayMetrics();
   float fontScale=dm.scaledDensity/Math.max(.1f,dm.density);
   float available=Math.max(54f,h-6f);
   float fit=Math.min(1f,available/Math.max(1f,total*fontScale));

   // Respect the user's requested sp size. Only shrink proportionally when the
   // chosen combination physically cannot fit the selected fixed widget.
   for(int i=0;i<8;i++){
    float actual=req[i]*fit;
    float floor=i==0?9.5f:i==2?9.0f:7.5f;
    actual=Math.max(floor,actual);

    // Horizontal safety caps per information type.
    float denom=(i==0?5.0f:i==1?8.0f:i==2?7.0f:(i==5||i==7)?10.0f:9.0f);
    float widthCap=clamp((w-8f)/denom,floor,26f);
    actual=Math.min(actual,widthCap);

    rv.setTextViewTextSize(id(c,IDS[i]),TypedValue.COMPLEX_UNIT_SP,actual);
   }

   float dateActual=Math.min(req[0]*fit,clamp((w-18f)/5.0f,9.5f,26f));
   rv.setTextViewTextSize(id(c,"widget_settings"),TypedValue.COMPLEX_UNIT_SP,clamp(dateActual+1.5f,14f,24f));
   rv.setViewPadding(id(c,"widget_content"),dp(c,3),dp(c,2),dp(c,3),dp(c,2));
  }catch(Exception e){android.util.Log.e("ELUQEN_WIDGET","Typography custom",e);}
 }
}
