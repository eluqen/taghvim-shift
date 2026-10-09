package com.eluqen.shiftcalendar.ui;

import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class ShiftOccurrence {
 private ShiftOccurrence(){}

 private static String field(Object o,String name){
  try{Field f=o.getClass().getField(name);Object v=f.get(o);return v==null?"":String.valueOf(v);}
  catch(Exception e){return "";}
 }
 private static int intField(Object o,String name){
  try{return o.getClass().getField(name).getInt(o);}catch(Exception e){return -1;}
 }
 private static boolean presetCompany(Object plan){
  String c=field(plan,"company");
  return "پلی اکریل ایران".equals(c)||"فولاد مبارکه".equals(c)||"ذوب آهن".equals(c);
 }
 private static String code(Object profile){return field(profile,"code");}
 public static boolean previousEvening(Object plan,Object profile){
  return plan!=null&&profile!=null&&presetCompany(plan)&&"N".equals(code(profile));
 }
 public static boolean previousEveningCode(Object plan,String code){
  return plan!=null&&presetCompany(plan)&&"N".equals(code);
 }
 public static Object startDate(Object plan,Object nominal,Object profile){
  if(nominal==null)return null;
  if(!previousEvening(plan,profile))return nominal;
  try{return nominal.getClass().getMethod("plusDays",long.class).invoke(nominal,-1L);}
  catch(Exception e){android.util.Log.e("ELUQEN_SHIFT_TIME","Night date",e);return nominal;}
 }
 public static long startEpoch(Context context,Object plan,Object nominal,Object profile,TimeZone zone){
  try{
   Object date=startDate(plan,nominal,profile);
   Class<?> pd=Class.forName("com.eluqen.shiftcalendar.core.PersianDate");
   Class<?> jc=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar");
   Object gd=jc.getMethod("toGregorian",pd).invoke(null,date);
   Class<?> gc=gd.getClass();
   int minute=intField(profile,"startMinute");
   Calendar cal=Calendar.getInstance(zone,Locale.US);
   cal.clear();
   cal.set(gc.getField("year").getInt(gd),gc.getField("month").getInt(gd)-1,gc.getField("day").getInt(gd),minute/60,minute%60,0);
   return cal.getTimeInMillis();
  }catch(Exception e){
   android.util.Log.e("ELUQEN_SHIFT_TIME","Start epoch",e);
   return Long.MIN_VALUE/4;
  }
 }
 public static String displayDate(Object plan,Object nominal,Object profile){
  Object d=startDate(plan,nominal,profile);
  if(d==null)return "";
  try{return String.valueOf(d.getClass().getMethod("displayLong").invoke(d));}
  catch(Exception e){return String.valueOf(d);}
 }
 public static String nextPrefix(Object plan,String code){
  return previousEveningCode(plan,code)?"امشب: ":"فردا: ";
 }
}
