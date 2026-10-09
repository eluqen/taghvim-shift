package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import java.util.WeakHashMap;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
public final class SoundPicker {
 public interface Callback {void selected(String choice);}
 private static final int FILE=9231,RING=9232;
 private static final WeakHashMap<Activity,Callback> pending=new WeakHashMap<>();
 private static final WeakHashMap<Activity,Ringtone> preview=new WeakHashMap<>();
 private static final WeakHashMap<Activity,Runnable> previewStops=new WeakHashMap<>();
 private static final Handler main=new Handler(Looper.getMainLooper());
 private SoundPicker(){}

 public static void stopPreview(Activity a){
  Runnable stop=previewStops.remove(a);
  if(stop!=null)main.removeCallbacks(stop);
  Ringtone ring=preview.remove(a);
  if(ring!=null)try{if(ring.isPlaying())ring.stop();}catch(Exception ignored){}
 }
 private static void playPreview(Activity a,String choice){
  stopPreview(a);
  Uri target=uri(choice);
  if(target==null){
   Toast.makeText(a,"این گزینه بی‌صدا است",Toast.LENGTH_SHORT).show();
   return;
  }
  try{
   Ringtone ring=RingtoneManager.getRingtone(a,target);
   if(ring==null)throw new IllegalStateException("ringtone unavailable");
   if(Build.VERSION.SDK_INT>=28)ring.setLooping(false);
   preview.put(a,ring);
   ring.play();
   Runnable stop=()->stopPreview(a);
   previewStops.put(a,stop);
   main.postDelayed(stop,8000);
  }catch(Exception e){
   stopPreview(a);
   Toast.makeText(a,"فایل صوتی در دسترس نیست",Toast.LENGTH_SHORT).show();
  }
 }
 public static Uri uri(String code){
  if("default_alarm".equals(code))return Settings.System.DEFAULT_ALARM_ALERT_URI;
  if("silent".equals(code))return null;
  if(code==null||code.length()==0||"default_notification".equals(code))return Settings.System.DEFAULT_NOTIFICATION_URI;
  try{return Uri.parse(code);}catch(Exception e){return Settings.System.DEFAULT_NOTIFICATION_URI;}
 }
 public static String title(String choice){
  if("silent".equals(choice))return "بدون صدا";
  if("default_alarm".equals(choice))return "زنگ پیش‌فرض هشدار گوشی";
  if(choice==null||choice.isEmpty()||"default_notification".equals(choice))return "صدای پیش‌فرض اعلان";
  return "آهنگ انتخابی گوشی / فایل شخصی";
 }
 public static void open(Activity a,String current,Callback selected){
  String[] options={"صدای پیش‌فرض اعلان","زنگ پیش‌فرض هشدار","بی‌صدا","انتخاب از آهنگ‌ها و زنگ‌های گوشی","انتخاب فایل صوتی از حافظه","پخش نمونه انتخاب فعلی","توقف پخش نمونه"};
  new AlertDialog.Builder(a).setTitle("آهنگ یادآوری").setAdapter(FancyDialogs.list(a,options),(dialog,which)->{
   if(which<3){
    stopPreview(a);
    selected.selected(which==0?"default_notification":which==1?"default_alarm":"silent");
    return;
   }
   if(which==5){playPreview(a,current);return;}
   if(which==6){stopPreview(a);return;}
   pending.put(a,selected);
   try{
    if(which==3){
     Intent i=new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
     i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALL);
     i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true);
     i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,true);
     i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,uri(current));
     a.startActivityForResult(i,RING);
    }else{
     Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*");
     i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
     a.startActivityForResult(i,FILE);
    }
   }catch(Exception e){
    pending.remove(a);
    Toast.makeText(a,"انتخاب‌کننده صوتی در دستگاه در دسترس نیست",Toast.LENGTH_LONG).show();
   }
  }).setNegativeButton("انصراف",null).show();
 }
 public static boolean result(Activity a,int request,int response,Intent data){
  if(request!=FILE&&request!=RING)return false;
  stopPreview(a);
  Callback listener=pending.remove(a);if(response!=Activity.RESULT_OK||data==null||listener==null)return true;
  Uri result=null;
  if(request==FILE){
   result=data.getData();
   if(result!=null)try{
    a.getContentResolver().takePersistableUriPermission(result,Intent.FLAG_GRANT_READ_URI_PERMISSION);
   }catch(SecurityException ex){Toast.makeText(a,"دسترسی دائمی به فایل داده نشد",Toast.LENGTH_LONG).show();return true;}
  }else result=(Uri)data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
  listener.selected(result==null?"silent":result.toString());return true;
 }
}

