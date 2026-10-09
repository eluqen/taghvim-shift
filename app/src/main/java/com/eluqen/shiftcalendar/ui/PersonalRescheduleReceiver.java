package com.eluqen.shiftcalendar.ui;
import android.content.*;
public final class PersonalRescheduleReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(i==null)return;
  String action=i.getAction();
  if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_TIME_CHANGED.equals(action)||
   Intent.ACTION_TIMEZONE_CHANGED.equals(action)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)||
   "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action)){
   final PendingResult done=goAsync();
   new Thread(()->{try{PersonalAlarm.reschedule(c.getApplicationContext());}finally{done.finish();}},"eluqen-personal-reschedule").start();
  }
 }
}