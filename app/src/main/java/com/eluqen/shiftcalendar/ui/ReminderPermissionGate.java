package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Toast;
import java.lang.reflect.Field;

public final class ReminderPermissionGate {
 private static final int REQUEST=7101;
 private static final String PERMISSION="android.permission.POST_NOTIFICATIONS";
 private static final String PREFS="eluqen_shift_calendar_preferences";
 private static final String ENABLED="reminder_enabled";
 private static final String GATE="eluqen_reminder_permission_gate";
 private static final String PENDING="pending_enable";
 private ReminderPermissionGate(){}

 public static boolean notificationsAllowed(Context c){
  if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(PERMISSION)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return false;
  if(Build.VERSION.SDK_INT<24)return true;
  NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
  return nm!=null&&nm.areNotificationsEnabled();
 }

 private static void setUi(Activity a,boolean on){
  try{
   Field binding=a.getClass().getDeclaredField("binding");binding.setAccessible(true);
   Field enabled=a.getClass().getDeclaredField("enabled");enabled.setAccessible(true);
   binding.setBoolean(a,true);
   Object v=enabled.get(a);
   if(v instanceof CompoundButton)((CompoundButton)v).setChecked(on);
   binding.setBoolean(a,false);
  }catch(Exception e){android.util.Log.e("ELUQEN_REMINDER","Permission UI",e);}
 }

 private static void cancel(Context c){
  Context app=c.getApplicationContext();
  try{
   Class.forName("com.eluqen.shiftcalendar.reminder.ReminderScheduler")
    .getMethod("syncAsync",Context.class).invoke(null,app);
  }catch(Exception ignored){}
 }
 private static void refreshWidget(Context c){
  try{Class.forName("com.eluqen.shiftcalendar.ui.WidgetStudio").getMethod("refresh",Context.class).invoke(null,c);}catch(Exception ignored){}
 }
 private static void disable(Activity a){
  a.getSharedPreferences(PREFS,0).edit().putBoolean(ENABLED,false).apply();
  setUi(a,false);cancel(a);refreshWidget(a);
 }
 private static void enable(Activity a){
  a.getSharedPreferences(PREFS,0).edit().putBoolean(ENABLED,true).apply();
  a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,false).apply();
  setUi(a,true);refreshWidget(a);
 }

 private static void requestSystem(Activity a){
  a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,true).apply();
  a.requestPermissions(new String[]{PERMISSION},REQUEST);
 }

 private static void openNotificationSettings(Activity a){
  // Keep pending=true. If the user enables notifications in Android settings,
  // reconcile() will finish the activation automatically on return.
  a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,true).apply();
  try{
   Intent i;
   if(Build.VERSION.SDK_INT>=26){
    i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
      .putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName());
   }else{
    i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      android.net.Uri.parse("package:"+a.getPackageName()));
   }
   a.startActivity(i);
  }catch(Exception e){
   a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,false).apply();
   Toast.makeText(a,"صفحه تنظیمات اعلان در دسترس نیست",Toast.LENGTH_LONG).show();
  }
 }

 private static void permissionDialog(Activity a){
  final boolean canSystemAsk=Build.VERSION.SDK_INT>=33 &&
    (a.shouldShowRequestPermissionRationale(PERMISSION) ||
     !a.getSharedPreferences(GATE,0).getBoolean("asked_before",false));

  String positive=canSystemAsk?"ادامه و درخواست مجوز":"باز کردن تنظیمات اعلان";
  String message=canSystemAsk
    ?"برای فعال شدن یادآوری شیفت، اجازه ارسال اعلان لازم است. تا وقتی این مجوز صادر نشود، یادآوری خاموش می‌ماند."
    :"مجوز اعلان هنوز صادر نشده و اندروید پنجره درخواست را دوباره نمایش نمی‌دهد. اعلان‌های این برنامه را از تنظیمات اندروید مجاز کنید.";

  new AlertDialog.Builder(a)
   .setTitle("اجازه اعلان لازم است")
   .setMessage(message)
   .setPositiveButton(positive,(d,w)->{
     if(canSystemAsk){
      a.getSharedPreferences(GATE,0).edit().putBoolean("asked_before",true).apply();
      requestSystem(a);
     }else{
      openNotificationSettings(a);
     }
   })
   .setNegativeButton("انصراف",(d,w)->{
     a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,false).apply();
     disable(a);
   })
   .setOnCancelListener(d->{
     a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,false).apply();
     disable(a);
   })
   .show();
 }

 public static boolean allowEnable(Activity a){
  if(notificationsAllowed(a))return true;

  // Never leave the switch/pref ON while permission is missing.
  disable(a);

  if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(PERMISSION)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
   // Every tap produces a visible action. There is no silent second attempt.
   permissionDialog(a);
  }else{
   new AlertDialog.Builder(a)
    .setTitle("اعلان‌ها غیرفعال هستند")
    .setMessage("برای فعال شدن یادآوری شیفت، اعلان‌های برنامه باید در تنظیمات اندروید مجاز باشند.")
    .setPositiveButton("باز کردن تنظیمات اعلان",(d,w)->openNotificationSettings(a))
    .setNegativeButton("انصراف",null)
    .show();
  }
  return false;
 }

 public static void onResult(Activity a,int requestCode,int[] results){
  if(requestCode!=REQUEST)return;
  boolean pending=a.getSharedPreferences(GATE,0).getBoolean(PENDING,false);
  a.getSharedPreferences(GATE,0).edit().putBoolean(PENDING,false).apply();
  if(!pending)return;

  if(results!=null&&results.length>0&&results[0]==0&&notificationsAllowed(a)){
   enable(a);
  }else{
   disable(a);
  }
 }

 public static void reconcile(Activity a){
  SharedPreferences g=a.getSharedPreferences(GATE,0);
  boolean pending=g.getBoolean(PENDING,false);

  if(notificationsAllowed(a)){
   // Returning from Android notification settings after the user granted
   // access completes the enable action that the user initiated.
   if(pending){
    enable(a);
   }
   return;
  }

  // Permission can be temporarily unavailable after an OS/app update.
  // Never overwrite the user's previously persisted ON preference here.
  // The settings screen reports the missing permission and schedules again
  // once Android grants it; the user's choice must survive that transition.
  setUi(a,a.getSharedPreferences(PREFS,0).getBoolean(ENABLED,false));
 }
}
