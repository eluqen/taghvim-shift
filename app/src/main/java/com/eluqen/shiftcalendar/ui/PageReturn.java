package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class PageReturn {
 private static final WeakHashMap<Activity,Boolean> seen=new WeakHashMap<>();
 private PageReturn(){}
 private static Object today()throws Exception{return Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);}
 private static void put(Activity a,String field,Object value)throws Exception{
  Field f=a.getClass().getDeclaredField(field);f.setAccessible(true);f.set(a,value);
 }
 public static void reset(Activity a){
  boolean first=!seen.containsKey(a);seen.put(a,true);
  if(!first)return;
  if(a.getIntent()!=null&&a.getIntent().hasExtra("date"))return;
  try{
   String name=a.getClass().getSimpleName();Object now=today();
   if("MainActivity".equals(name)){
    put(a,"selected",now);put(a,"displayedMonth",now.getClass().getMethod("firstOfMonth").invoke(now));MonthStep.reset(a);
   }else if("CardexActivity".equals(name)){
    put(a,"selected",now);MonthStep.reset(a);
    Method m=a.getClass().getDeclaredMethod("updateSelectedButton");m.setAccessible(true);m.invoke(a);
   }else if("FourShiftActivity".equals(name)){
    put(a,"month",now.getClass().getMethod("firstOfMonth").invoke(now));FourShiftPage.clear(a);
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_RETURN","Today on page entry",e);}
 }
}
