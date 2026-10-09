package com.eluqen.shiftcalendar.ui;
import android.app.Activity;
import java.lang.reflect.*;
public final class CardexOpen {
 private CardexOpen(){}
 public static void four(Activity a,Object date){
  try{
   Class<?> editor=Class.forName("com.eluqen.shiftcalendar.ui.CardexEditor");
   Class<?> d=Class.forName("com.eluqen.shiftcalendar.core.PersianDate");
   Class<?> listener=Class.forName("com.eluqen.shiftcalendar.ui.CardexEditor$Listener");
   Object callback=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(proxy,method,args)->{
    if("onSaved".equals(method.getName()))try{Method r=a.getClass().getDeclaredMethod("render");r.setAccessible(true);r.invoke(a);}catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","FourShift refresh",e);}
    return null;
   });
   editor.getMethod("show",Activity.class,d,listener).invoke(null,a,date,callback);
  }catch(Exception e){android.util.Log.e("ELUQEN_CARDEX","Open FourShift editor",e);}
 }
}
