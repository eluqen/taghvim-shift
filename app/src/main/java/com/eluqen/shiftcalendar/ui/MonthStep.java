package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import android.util.Log;
import java.lang.reflect.*;
import java.util.WeakHashMap;
public final class MonthStep {
 private static final class State {int desiredDay;String last;}
 private static final WeakHashMap<Activity,State> states=new WeakHashMap<>();
 private MonthStep(){}
 public static void reset(Activity activity){states.remove(activity);}
 public static Object[] snapshot(Activity activity){State s=states.get(activity);return s==null?null:new Object[]{s.desiredDay,s.last};}
 public static void restore(Activity activity,Object[] saved){
  if(saved==null){states.remove(activity);return;}
  State s=new State();s.desiredDay=(Integer)saved[0];s.last=(String)saved[1];states.put(activity,s);
 }
 public static Object next(Activity activity,Object current,int delta){
  if(current==null||delta==0)return current;
  try{
   String key=current.toString();
   State state=states.get(activity);
   if(state==null||!key.equals(state.last)){
    state=new State();state.desiredDay=current.getClass().getField("day").getInt(current);states.put(activity,state);
   }
   Object target=current.getClass().getMethod("plusMonths",int.class).invoke(current,delta);
   int year=target.getClass().getField("year").getInt(target);
   int month=target.getClass().getField("month").getInt(target);
   Class<?> jalali=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar");
   int days=(Integer)jalali.getMethod("daysInMonth",int.class,int.class).invoke(null,year,month);
   Object chosen=current.getClass().getConstructor(int.class,int.class,int.class)
    .newInstance(year,month,Math.min(state.desiredDay,days));
   state.last=chosen.toString();
   return chosen;
  }catch(Exception error){Log.e("ELUQEN_MONTH","Month navigation failed",error);return current;}
 }
}
