package com.eluqen.shiftcalendar.ui;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.util.Locale;

public final class WidgetStudio {
 private static final String FILE="eluqen_widget_studio";
 private static final int[] TEXT={0xffffffff,0xffffffff,0xffffffff,0xffffffff,0xffffffff,0xff162735,0xff573b02,0xffffffff,0xffffffff,0xff17333c};
 private static final String[] KEYS={"weekday","plan","next","reminder","hours"};
 private static final boolean[] DEFAULTS={true,false,true,true,true};
 private WidgetStudio(){}

 private static SharedPreferences pref(Context c){return c.getSharedPreferences(FILE,0);}
 private static int id(Context c,String name){return c.getResources().getIdentifier(name,"id",c.getPackageName());}
 private static boolean option(Context c,int i){return pref(c).getBoolean(KEYS[i],DEFAULTS[i]);}
 private static int color(SharedPreferences p,String key,int fallback){return p.getInt("color_"+key,fallback);}

 public static boolean refresh(Context c){
  try{
   Class.forName("com.eluqen.shiftcalendar.widget.ShiftWidgetProvider").getMethod("updateAll",Context.class).invoke(null,c.getApplicationContext());
   return true;
  }catch(Exception e){android.util.Log.e("ELUQEN_Widget","Refresh failed",e);if(c instanceof Activity)android.widget.Toast.makeText(c,"به‌روزرسانی ویجت انجام نشد",android.widget.Toast.LENGTH_LONG).show();return false;}
 }

 private static int countOne(Context c,String cls){
  try{
   return AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c.getPackageName(),cls)).length;
  }catch(Throwable t){
   android.util.Log.w("ELUQEN_Widget","Widget count unavailable for "+cls,t);
   return 0;
  }
 }
 public static int count(Context c){
  return countOne(c,"com.eluqen.shiftcalendar.widget.ShiftWidgetProviderLarge");
 }
 public static void captureOptions(Context c,int appWidgetId,Bundle o){
  if(o==null)return;int w=o.getInt("appWidgetMinWidth",-1),h=o.getInt("appWidgetMinHeight",-1);if(w<=0||h<=0)return;
  c.getSharedPreferences("eluqen_widget_dimensions",0).edit().putInt("width_"+appWidgetId,w).putInt("height_"+appWidgetId,h).commit();
 }

 public static void apply(Context c,RemoteViews r,int appWidgetId){
  SharedPreferences p=pref(c);
  int pal=Math.max(0,Math.min(9,p.getInt("palette",0)));
  int oldMain=p.getInt("text_custom",TEXT[pal]);
  int oldSecondary=p.getInt("secondary_custom",TEXT[pal]);

  r.setTextColor(id(c,"widget_date"),color(p,"date",oldMain));
  r.setTextColor(id(c,"widget_weekday"),color(p,"weekday",oldSecondary));
  r.setTextColor(id(c,"widget_shift"),color(p,"shift",oldMain));
  r.setTextColor(id(c,"widget_plan"),color(p,"plan",oldSecondary));
  r.setTextColor(id(c,"widget_hours"),color(p,"hours",oldSecondary));
  r.setTextColor(id(c,"widget_groups"),color(p,"groups",oldSecondary));
  r.setTextColor(id(c,"widget_next"),color(p,"next",oldSecondary));
  r.setTextColor(id(c,"widget_reminder"),color(p,"reminder",oldSecondary));
  r.setTextColor(id(c,"widget_settings"),color(p,"date",oldMain));

  if(!p.getBoolean("date",true))r.setViewVisibility(id(c,"widget_date"),View.GONE);
  if(!option(c,0))r.setViewVisibility(id(c,"widget_weekday"),View.GONE);
  if(!p.getBoolean("shift",true))r.setViewVisibility(id(c,"widget_shift"),View.GONE);
  if(!option(c,1))r.setViewVisibility(id(c,"widget_plan"),View.GONE);
  if(!option(c,2))r.setViewVisibility(id(c,"widget_next"),View.GONE);
  if(!option(c,4))r.setViewVisibility(id(c,"widget_hours"),View.GONE);

  SharedPreferences q=c.getSharedPreferences("eluqen_shift_calendar_preferences",0);
  if(!q.getBoolean("widget_show_groups",true))r.setViewVisibility(id(c,"widget_groups"),View.GONE);

  if(option(c,3)&&q.getBoolean("reminder_enabled",false)){
   r.setTextViewText(id(c,"widget_reminder"),"◉ یادآوری: "+q.getInt("reminder_lead_minutes",30)+" دقیقه");
   r.setViewVisibility(id(c,"widget_reminder"),View.VISIBLE);
  }else r.setViewVisibility(id(c,"widget_reminder"),View.GONE);

  Intent intent=new Intent();intent.setClassName(c,"com.eluqen.shiftcalendar.ui.WidgetSettingsActivity");
  intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,appWidgetId);
  r.setOnClickPendingIntent(id(c,"widget_settings"),PendingIntent.getActivity(c,appWidgetId+1000,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
 }

 public static void hours(Context c,RemoteViews r,String code){
  SharedPreferences s=c.getSharedPreferences("eluqen_shift_calendar_shift_types",0);
  int start=code.equals("D")?420:code.equals("E")?900:code.equals("N")?1380:code.equals("C1")?480:code.equals("C2")?960:480;
  int end=code.equals("D")?900:code.equals("E")?1380:code.equals("N")?420:code.equals("C1")?960:code.equals("C2")?0:960;
  start=s.getInt(code+".start",start);end=s.getInt(code+".end",end);
  int key=id(c,"widget_hours");
  if(start<0||end<0||!s.getBoolean(code+".working",!code.equals("O"))){r.setViewVisibility(key,View.GONE);return;}
  r.setTextViewText(key,String.format(Locale.US,"ساعت: %02d:%02d تا %02d:%02d",start/60,start%60,end/60,end%60));
  if(option(c,4))r.setViewVisibility(key,View.VISIBLE);
 }
}
