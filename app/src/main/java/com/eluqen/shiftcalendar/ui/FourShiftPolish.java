package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class FourShiftPolish {
 private FourShiftPolish(){}
 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 private static List<?> events(Activity a,Object date){
  try{
   Class<?> e=Class.forName("com.eluqen.shiftcalendar.events.EventRepository");
   Object repo=e.getMethod("get",android.content.Context.class).invoke(null,a);
   return (List<?>)e.getMethod("forDate",Class.forName("com.eluqen.shiftcalendar.core.PersianDate")).invoke(repo,date);
  }catch(Exception ex){android.util.Log.e("ELUQEN_HOLIDAY","Events",ex);return Collections.emptyList();}
 }
 private static List<String> reasons(Activity a,Object date){
  List<String> names=new ArrayList<>();
  for(Object event:events(a,date))try{
   if(event.getClass().getField("holiday").getBoolean(event)){
    String title=(String)event.getClass().getField("title").get(event);
    if(title!=null&&!title.trim().isEmpty()&&!title.trim().equals("جمعه"))names.add(title.trim());
   }
  }catch(Exception ignored){}
  return names;
 }
 private static TextView dateLabel(LinearLayout row){
  if(row==null||row.getChildCount()==0)return null;
  android.view.View first=row.getChildAt(0);
  if(first instanceof TextView)return (TextView)first;
  if(first instanceof android.view.ViewGroup){
   android.view.ViewGroup group=(android.view.ViewGroup)first;
   for(int i=0;i<group.getChildCount();i++){
    android.view.View child=group.getChildAt(i);
    if(child instanceof TextView&&"four_date_text".equals(child.getTag()))return (TextView)child;
   }
  }
  return null;
 }
 public static void decorate(Activity a,Object date,LinearLayout row){
  TextView label=dateLabel(row);if(label==null)return;
  boolean friday=false;
  try{friday=(Integer)date.getClass().getMethod("dayOfWeek").invoke(date)==6;}catch(Exception ignored){}
  boolean official=!reasons(a,date).isEmpty();
  if(friday||official){
   label.setTextColor(Color.WHITE);label.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
   GradientDrawable bg=new GradientDrawable();bg.setColor(official?0xffc43e51:Ui.SECONDARY);
   bg.setCornerRadius(dp(a,9));label.setBackground(bg);
  }
  try{
   Object now=Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);
   if(now.equals(date)){
    // Today has its own warm amber language and never borrows the selected
    // date's theme-primary styling.
    GradientDrawable today=new GradientDrawable();
    today.setColor(official?0xffc43e51:friday?0xffffb300:0xffffe082);
    today.setCornerRadius(dp(a,9));today.setStroke(dp(a,2),0xffff8f00);
    label.setBackground(today);
    label.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
    label.setTextColor(official?Color.WHITE:0xff5d3b00);
    label.setText("● "+label.getText());
    GradientDrawable entire=new GradientDrawable();entire.setColor(0xfffff4cf);
    entire.setCornerRadius(dp(a,11));entire.setStroke(dp(a,4),0xffffa000);
    row.setBackground(entire);
    row.setPadding(dp(a,3),dp(a,2),dp(a,3),dp(a,2));
    row.setElevation(dp(a,3));
   }
  }catch(Exception ignored){}
 }
 public static void footer(Activity a,Object month,LinearLayout table){
  if(table==null)return;
  int days;
  try{days=(Integer)Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar").getMethod("daysInMonth",int.class,int.class).invoke(null,
    month.getClass().getField("year").getInt(month),month.getClass().getField("month").getInt(month));}
  catch(Exception e){return;}
  LinearLayout panel=new LinearLayout(a);panel.setOrientation(android.widget.LinearLayout.VERTICAL);panel.setPadding(dp(a,12),dp(a,11),dp(a,12),dp(a,12));
  GradientDrawable bg=new GradientDrawable();bg.setColor(mix(0xffc43e51,Ui.CARD,10));bg.setCornerRadius(dp(a,12));panel.setBackground(bg);
  TextView title=new TextView(a);title.setText("تعطیلات رسمی مناسبتی این ماه");
  title.setTextColor(0xff982d42);title.setTextSize(16);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);title.setGravity(Gravity.RIGHT);panel.addView(title);
  int count=0;
  for(int n=1;n<=days;n++)try{
   Object date=month.getClass().getConstructor(int.class,int.class,int.class).newInstance(
    month.getClass().getField("year").getInt(month),month.getClass().getField("month").getInt(month),n);
   List<String> reason=reasons(a,date);if(reason.isEmpty())continue;
   TextView item=new TextView(a);
   String number=(String)Class.forName("com.eluqen.shiftcalendar.core.PersianDigits").getMethod("of",Object.class).invoke(null,n);
   item.setText(number+" — "+android.text.TextUtils.join("، ",reason));item.setTextSize(13);
   item.setTextColor(0xff65404b);item.setGravity(Gravity.RIGHT);item.setPadding(0,dp(a,5),0,dp(a,5));
   panel.addView(item);count++;
  }catch(Exception error){android.util.Log.e("ELUQEN_HOLIDAY","List row",error);}
  if(count==0){
   TextView empty=new TextView(a);empty.setText("تعطیلی رسمی مناسبتی برای این ماه ثبت نشده است.");
   empty.setTextColor(0xff785d68);empty.setGravity(Gravity.RIGHT);empty.setTextSize(13);panel.addView(empty);
  }
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(a,12),0,dp(a,8));
  table.addView(panel,lp);
 }
 private static int mix(int foreground,int background,int foregroundPercent){
  int p=Math.max(0,Math.min(100,foregroundPercent)),q=100-p;
  return Color.rgb(
   (Color.red(foreground)*p+Color.red(background)*q)/100,
   (Color.green(foreground)*p+Color.green(background)*q)/100,
   (Color.blue(foreground)*p+Color.blue(background)*q)/100);
 }
}
