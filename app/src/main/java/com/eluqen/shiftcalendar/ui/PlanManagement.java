package com.eluqen.shiftcalendar.ui;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;
import java.lang.reflect.*;
import java.util.*;

public final class PlanManagement {
 private static final String INTERNAL_GENERAL="جنرال";
 private static final class Def {
  final String company,team,pattern; final int y,m,d;
  Def(String c,String t,int y,int m,int d,String p){company=c;team=t;this.y=y;this.m=m;this.d=d;pattern=p;}
 }
 private static final Def[] FACTORY={
  new Def("پلی اکریل ایران","A",1398,1,5,"D,D,E,E,O,N,N,O"),
  new Def("پلی اکریل ایران","B",1398,1,1,"D,D,E,E,O,N,N,O"),
  new Def("پلی اکریل ایران","C",1398,1,7,"D,D,E,E,O,N,N,O"),
  new Def("پلی اکریل ایران","D",1398,1,3,"D,D,E,E,O,N,N,O"),
  new Def("فولاد مبارکه","A",1398,1,1,"D,N,O,O"),
  new Def("فولاد مبارکه","B",1398,1,3,"D,N,O,O"),
  new Def("فولاد مبارکه","C",1398,1,4,"D,N,O,O"),
  new Def("فولاد مبارکه","D",1398,1,2,"D,N,O,O"),
  new Def("ذوب آهن","A",1398,1,1,"D,D,E,E,O,N,N,O"),
  new Def("ذوب آهن","B",1398,1,3,"D,D,E,E,O,N,N,O"),
  new Def("ذوب آهن","C",1398,1,5,"D,D,E,E,O,N,N,O"),
  new Def("ذوب آهن","D",1398,1,7,"D,D,E,E,O,N,N,O"),
  // Legacy internal day-worker fallback is part of the factory DB but is not a
  // fourth preset company in the first-run chooser.
  new Def(INTERNAL_GENERAL,"روزکار",1398,1,1,"O,O,D,D,D,D,D")
 };
 private PlanManagement(){}

 private static Object db(Context c)throws Exception{
  return Class.forName("com.eluqen.shiftcalendar.data.AppDatabase").getMethod("get",Context.class).invoke(null,c);
 }
 private static SQLiteDatabase writable(Context c)throws Exception{
  return (SQLiteDatabase)db(c).getClass().getMethod("getWritableDatabase").invoke(db(c));
 }
 private static List<?> plans(Context c)throws Exception{
  return (List<?>)db(c).getClass().getMethod("plans").invoke(db(c));
 }
 private static long id(Object p)throws Exception{return p.getClass().getField("id").getLong(p);}
 private static String str(Object p,String f)throws Exception{return String.valueOf(p.getClass().getField(f).get(p));}
 private static long anchor(Object p)throws Exception{return p.getClass().getField("anchorJdn").getLong(p);}
 private static boolean active(Object p)throws Exception{return p.getClass().getField("active").getBoolean(p);}
 private static long jdn(Def d)throws Exception{
  return (Long)Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar").getMethod("toJdn",int.class,int.class,int.class).invoke(null,d.y,d.m,d.d);
 }
 private static String sig(String c,String t,long a,String p){return c+"\u0001"+t+"\u0001"+a+"\u0001"+p.replace(" ","");}
 private static String sig(Object p)throws Exception{return sig(str(p,"company"),str(p,"team"),anchor(p),str(p,"pattern"));}
 private static String sig(Def d)throws Exception{return sig(d.company,d.team,jdn(d),d.pattern);}

 public static boolean hasChanges(Context c){
  try{
   ArrayList<String> now=new ArrayList<>(),factory=new ArrayList<>();
   for(Object p:plans(c))now.add(sig(p));
   for(Def d:FACTORY)factory.add(sig(d));
   Collections.sort(now);Collections.sort(factory);
   return !now.equals(factory);
  }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Compare defaults",e);return true;}
 }
 public static List<String> companies(Context c){
  LinkedHashSet<String> out=new LinkedHashSet<>();
  try{
   for(Object p:plans(c)){
    String company=str(p,"company");
    if(!INTERNAL_GENERAL.equals(company))out.add(company);
   }
  }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Companies",e);}
  return new ArrayList<>(out);
 }
 public static boolean selectionValid(Context c){
  try{
   int count=0;
   for(Object p:plans(c))if(active(p))count++;
   return count==1;
  }catch(Exception e){return false;}
 }
 public static Object activePlan(Context c)throws Exception{
  for(Object p:plans(c))if(active(p))return p;
  return null;
 }
 public static String activeCompany(Context c){
  try{Object p=activePlan(c);return p==null?"":str(p,"company");}catch(Exception e){return "";}
 }
 public static String activeName(Context c){
  try{Object p=activePlan(c);return p==null?"":str(p,"company")+" / "+str(p,"team");}catch(Exception e){return "";}
 }
 private static void clearActive(SQLiteDatabase db){db.execSQL("UPDATE shift_plans SET active=0");}
 private static void invalidate(Activity a){
  try{Class.forName("com.eluqen.shiftcalendar.data.Prefs").getMethod("setOnboardingComplete",Context.class,boolean.class).invoke(null,a,false);}catch(Exception ignored){}
  try{Class.forName("com.eluqen.shiftcalendar.reminder.ReminderScheduler").getMethod("cancelAll",Context.class).invoke(null,a);}catch(Exception ignored){}
 }
 private static void requireSelection(Activity a){
  invalidate(a);
  FirstRunActivity.open(a);
 }
 public static void confirmDeleteActive(Activity a){
  try{
   Object target=CompanyPlans.shownPlan(a);
   if(target==null){Toast.makeText(a,"الگویی برای حذف انتخاب نشده است",Toast.LENGTH_SHORT).show();return;}
   Object active=activePlan(a);
   final long pid=id(target);final String targetCompany=str(target,"company");
   final String name=targetCompany+" / "+str(target,"team");
   final boolean removesSaved=active!=null&&id(active)==pid;
   String tail=removesSaved
    ?"این الگو انتخاب ذخیره‌شده برنامه است؛ پس از حذف باید یک الگوی دیگر را انتخاب و ذخیره کنید."
    :"این الگو فقط در حال نمایش است و انتخاب ذخیره‌شده فعلی شما تغییر نمی‌کند.";
   new AlertDialog.Builder(a)
    .setTitle("حذف الگوی کاری")
    .setMessage("الگوی «"+name+"» حذف شود؟\n\n"+tail+"\n\nاطلاعات کارکرد، یادداشت‌ها و تنظیمات یادآوری حذف نمی‌شوند.")
    .setPositiveButton("حذف",(d,w)->{
     try{
      SQLiteDatabase sql=writable(a);sql.beginTransaction();
      try{
       if(removesSaved)clearActive(sql);
       sql.delete("shift_plans","id=?",new String[]{String.valueOf(pid)});
       sql.setTransactionSuccessful();
      }finally{sql.endTransaction();}
      if(removesSaved)requireSelection(a);else CompanyPlans.afterDelete(a,targetCompany);
     }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Delete shown plan",e);Toast.makeText(a,"حذف الگو ناموفق بود",Toast.LENGTH_LONG).show();}
    }).setNegativeButton("انصراف",null).show();
  }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Confirm delete plan",e);}
 }

 public static void confirmDeleteCompany(Activity a){
  final String targetCompany=CompanyPlans.selectedCompany(a);
  if(targetCompany.isEmpty()){Toast.makeText(a,"محل کاری برای حذف انتخاب نشده است",Toast.LENGTH_SHORT).show();return;}
  final String savedCompany=activeCompany(a);
  final boolean removesSaved=targetCompany.equals(savedCompany);
  int count=0;try{for(Object p:plans(a))if(targetCompany.equals(str(p,"company")))count++;}catch(Exception ignored){}
  final int total=count;
  String tail=removesSaved
   ?"این محل کار شامل انتخاب ذخیره‌شده فعلی شماست؛ پس از حذف باید دوباره یک شرکت و الگوی کاری را انتخاب کنید."
   :"انتخاب ذخیره‌شده فعلی شما در محل کار دیگری است و با این حذف تغییر نمی‌کند.";
  new AlertDialog.Builder(a)
   .setTitle("حذف محل کار")
   .setMessage("«"+targetCompany+"» و تمام "+total+" الگوی زیرمجموعه آن حذف شوند؟\n\n"+tail+"\n\nاین کار فقط شرکت/محل کار و الگوهای کاری آن را حذف می‌کند. کارکرد، یادداشت‌ها، مناسبت‌ها، همکاران و تنظیمات یادآوری حذف نمی‌شوند.")
   .setPositiveButton("حذف کامل",(d,w)->{
    try{
     SQLiteDatabase sql=writable(a);sql.beginTransaction();
     try{
      if(removesSaved)clearActive(sql);
      sql.delete("shift_plans","company=?",new String[]{targetCompany});
      sql.setTransactionSuccessful();
     }finally{sql.endTransaction();}
     if(removesSaved)requireSelection(a);else CompanyPlans.afterDelete(a,null);
    }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Delete company",e);Toast.makeText(a,"حذف محل کار ناموفق بود",Toast.LENGTH_LONG).show();}
   }).setNegativeButton("انصراف",null).show();
 }
 private static void insert(SQLiteDatabase sql,Def d)throws Exception{
  android.content.ContentValues v=new android.content.ContentValues();
  v.put("company",d.company);v.put("team",d.team);v.put("anchor_jdn",jdn(d));v.put("pattern",d.pattern);v.put("active",0);
  sql.insertOrThrow("shift_plans",null,v);
 }
 public static void reset(Activity a,Runnable after){
  try{
   SQLiteDatabase sql=writable(a);sql.beginTransaction();
   try{
    sql.delete("shift_plans",null,null);
    for(Def d:FACTORY)insert(sql,d);
    clearActive(sql);
    sql.setTransactionSuccessful();
   }finally{sql.endTransaction();}
   invalidate(a);
   if(after!=null)after.run();else FirstRunActivity.open(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_PLAN","Reset defaults",e);Toast.makeText(a,"بازگردانی الگوهای پیش‌فرض ناموفق بود",Toast.LENGTH_LONG).show();}
 }
 public static void confirmReset(Activity a,Runnable after){
  new AlertDialog.Builder(a)
   .setTitle("بازگردانی تنظیمات پیش‌فرض الگوها")
   .setMessage("تمام شرکت‌ها/محل‌های کاری و الگوهای سفارشی حذف می‌شوند و الگوهای پیش‌فرض کارخانه دوباره ساخته می‌شوند. هر تغییری که در الگوهای سه شرکت پیش‌فرض داده‌اید نیز به حالت اولیه برمی‌گردد.\n\nاطلاعات ثبت‌شده شما حذف نمی‌شود: کارکرد، یادداشت‌ها، مناسبت‌ها، همکاران و تنظیمات یادآوری دست‌نخورده می‌مانند. فقط بخش شرکت‌ها و الگوهای کاری بازنشانی می‌شود.\n\nپس از بازنشانی باید دوباره شرکت و الگوی فعال خود را انتخاب کنید. ادامه می‌دهید؟")
   .setPositiveButton("بله، بازنشانی شود",(d,w)->reset(a,after))
   .setNegativeButton("انصراف",null).show();
 }
}
