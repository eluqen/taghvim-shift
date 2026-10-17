package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
import com.eluqen.shiftcalendar.core.HijriDate;
import com.eluqen.shiftcalendar.core.PersianDate;
import com.eluqen.shiftcalendar.data.Prefs;
import com.eluqen.shiftcalendar.events.EventRepository;
import com.eluqen.shiftcalendar.hijri.HijriResolver;
public final class HubGeneral {
 private static final int SAVE=8911,RESTORE=8912;
 private static final String DATA="com.eluqen.shiftcalendar.data.",CORE="com.eluqen.shiftcalendar.core.";
 private HubGeneral(){}
 private static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
 private static TextView text(Activity a,String t,int s){
  TextView v=new TextView(a);v.setText(t);v.setTextSize(s);v.setTextColor(0xff233b54);
  v.setGravity(Gravity.RIGHT);v.setPadding(dp(a,8),dp(a,7),dp(a,8),dp(a,7));return v;
 }
 private static Button button(Activity a,String t,Runnable onClick){
  Button b=new Button(a);b.setText(t);b.setAllCaps(false);b.setTextColor(0xff17486d);b.setOnClickListener(v->onClick.run());return b;
 }
 private static LinearLayout card(Activity a,LinearLayout body,String heading){
  LinearLayout c=new LinearLayout(a);c.setOrientation(android.widget.LinearLayout.VERTICAL);c.setPadding(dp(a,12),dp(a,10),dp(a,12),dp(a,10));
  c.setBackgroundColor(0xffeaf0f5);c.addView(text(a,heading,17));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(a,11));body.addView(c,lp);return c;
 }
 private static android.content.SharedPreferences prefs(Activity a){return a.getSharedPreferences("eluqen_shift_calendar_preferences",0);}
 private static Object db(Activity a)throws Exception{return Class.forName(DATA+"AppDatabase").getMethod("get",Context.class).invoke(null,a);}
 private static Object now()throws Exception{return Class.forName(CORE+"PersianDate").getMethod("todayTehran").invoke(null);}
 private static Object pickDateObject(int y,int m,int d)throws Exception{return Class.forName(CORE+"PersianDate").getConstructor(int.class,int.class,int.class).newInstance(y,m,d);}
 public static void attach(Activity a,LinearLayout root){
  ScrollView sc=null;for(int i=0;i<root.getChildCount();i++)if(root.getChildAt(i) instanceof ScrollView){sc=(ScrollView)root.getChildAt(i);break;}
  if(sc==null||sc.getChildCount()==0||!(sc.getChildAt(0) instanceof LinearLayout))return;
  LinearLayout body=(LinearLayout)sc.getChildAt(0);int insert=-1;
  for(int i=body.getChildCount()-1;i>=0;i--){
   View v=body.getChildAt(i);if(!(v instanceof LinearLayout))continue;
   LinearLayout card=(LinearLayout)v;if(card.getChildCount()==0||!(card.getChildAt(0) instanceof TextView))continue;
   String name=((TextView)card.getChildAt(0)).getText().toString();
   if(name.contains("تنظیمات و پشتیبان")||name.contains("تنظیمات و پشتیبانی")){insert=i;body.removeViewAt(i);}
  }
  if(insert<0)insert=body.getChildCount();
  LinearLayout section=new LinearLayout(a);section.setOrientation(android.widget.LinearLayout.VERTICAL);
  body.addView(section,Math.min(insert,body.getChildCount()));
  LinearLayout home=card(a,section,"صفحه شروع برنامه");
  String[] labels={"تقویم ماهانه","همه امکانات","تقویم چهارشیفت","کارکرد","تنظیمات"};
  String[] values={"calendar","features","four_shift","cardex","settings"};
  Spinner spinner=new Spinner(a);spinner.setAdapter(FancyDialogs.spinner(a,labels));
  int selected=0;String saved=prefs(a).getString("default_screen","calendar");
  for(int i=0;i<values.length;i++)if(values[i].equals(saved)||("graphic".equals(saved)&&"settings".equals(values[i])))selected=i;
  spinner.setSelection(selected,false);home.addView(spinner,new LinearLayout.LayoutParams(-1,dp(a,52)));
  spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(android.widget.AdapterView<?> p){}
   public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){
    if(!values[i].equals(prefs(a).getString("default_screen","calendar")))prefs(a).edit().putString("default_screen",values[i]).apply();
   }
  });
  LinearLayout time=card(a,section,"تنظیم محاسبه کارکرد");
  Switch lunch=new Switch(a);lunch.setText("کسر بازه ۱۲ تا ۱۳ از استراحت و مرخصی");
  lunch.setTextColor(0xff233b54);lunch.setChecked(prefs(a).getBoolean("exclude_lunch_hour",false));
  lunch.setOnCheckedChangeListener((v,on)->prefs(a).edit().putBoolean("exclude_lunch_hour",on).apply());time.addView(lunch);
  LinearLayout dates=card(a,section,"تقویم و مناسبت‌ها");
  dates.addView(button(a,"تنظیم دستی تاریخ قمری",()->hijri(a)));
  dates.addView(button(a,"مناسبت‌های شخصی",()->personal(a)));
  LinearLayout backup=card(a,section,"پشتیبان‌گیری و بازیابی");
  backup.addView(text(a,"فایل پشتیبان حاوی الگوها، کارکرد، همکاران و مناسبت‌های شخصی است. بازیابی تنها بعد از تأیید انجام می‌شود.",12));
  backup.addView(button(a,"ساخت فایل پشتیبان",()->document(a,true)));
  backup.addView(button(a,"بازیابی فایل پشتیبان",()->document(a,false)));
 }
 private static void hijri(Activity a){
  HijriCorrectionDialog.show(a,null);
 }
 private static void personal(Activity a){
  a.startActivity(new Intent(a, PersonalEventsActivity.class));
 }
 private static void document(Activity a,boolean save){
  Intent i=new Intent(save?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT);
  i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(save?"application/json":"*/*");
  if(save)i.putExtra(Intent.EXTRA_TITLE,"ShiftCalendar-Backup.json");
  try{a.startActivityForResult(i,save?SAVE:RESTORE);}
  catch(Exception e){Toast.makeText(a,"مدیر فایل سازگار در دستگاه پیدا نشد",Toast.LENGTH_LONG).show();}
 }
 public static void openHijri(Activity a){hijri(a);}
 public static void openPersonalEvents(Activity a){personal(a);}
 public static void createBackup(Activity a){document(a,true);}
 public static void restoreBackup(Activity a){document(a,false);}
 public static boolean result(Activity a,int request,int response,Intent data){
  if(request!=SAVE&&request!=RESTORE)return false;
  if(response!=Activity.RESULT_OK||data==null||data.getData()==null)return true;
  final Uri uri=data.getData();
  Runnable work=()->new Thread(()->{try{
   Class<?> backup=Class.forName(DATA+"BackupService");
   if(request==SAVE)backup.getMethod("writeBackup",Context.class,Uri.class).invoke(null,a,uri);
   else backup.getMethod("restore",Context.class,Uri.class).invoke(null,a,uri);
   a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())Toast.makeText(a,request==SAVE?"پشتیبان ساخته شد":"بازیابی با موفقیت انجام شد",Toast.LENGTH_LONG).show();});
  }catch(Exception e){android.util.Log.e("ELUQEN_BACKUP","Operation failed",e);a.runOnUiThread(()->{if(!a.isFinishing()&&!a.isDestroyed())Toast.makeText(a,"عملیات پشتیبان‌گیری/بازیابی ناموفق بود",Toast.LENGTH_LONG).show();});}}).start();
  if(request==SAVE)work.run();
  else new AlertDialog.Builder(a).setTitle("تأیید بازیابی").setMessage("بازیابی، اطلاعات فعلی مانند الگوها، کارکرد، همکاران و مناسبت‌ها را با داده‌های فایل جایگزین می‌کند. مطمئن هستید؟")
   .setNegativeButton("انصراف",null).setPositiveButton("بازیابی", (dialog,which)->work.run()).show();
  return true;
 }
}
