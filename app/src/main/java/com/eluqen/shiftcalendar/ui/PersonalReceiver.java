package com.eluqen.shiftcalendar.ui;
import android.content.*;
public final class PersonalReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(i!=null&&PersonalAlarm.ACTION.equals(i.getAction()))PersonalAlarm.fire(c,i.getIntExtra("id",-1));
 }
}