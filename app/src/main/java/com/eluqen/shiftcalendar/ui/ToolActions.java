package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class ToolActions {
 private static final String CORE="com.eluqen.shiftcalendar.core.",DATA="com.eluqen.shiftcalendar.data.";
 private static final int ICS=9281;
 private static final WeakHashMap<Activity,Object[]> pending=new WeakHashMap<>();
 private ToolActions(){}
 private static Class<?> c(String name)throws Exception{return Class.forName(name);}
 private static Object today()throws Exception{return c(CORE+"PersianDate").getMethod("todayTehran").invoke(null);}
 private static Object plus(Object date,long d)throws Exception{return date.getClass().getMethod("plusDays",long.class).invoke(date,d);}
 private static long jdn(Object date)throws Exception{return (Long)date.getClass().getMethod("toJdn").invoke(date);}
 private static Object plan(Activity a)throws Exception{return c(DATA+"AppDatabase").getMethod("get",Context.class).invoke(null,a).getClass().getMethod("activePlan").invoke(c(DATA+"AppDatabase").getMethod("get",Context.class).invoke(null,a));}
 private static String token(Activity a,Object plan,long day)throws Exception{
  String raw=(String)c(CORE+"ShiftEngine").getMethod("tokenFor",long.class,long.class,String.class)
   .invoke(null,day,plan.getClass().getField("anchorJdn").getLong(plan),plan.getClass().getField("pattern").get(plan));
  return GeneralWork.adjustJdn(a,day,plan,raw);
 }
 private static String title(Activity a,String code)throws Exception{
  return (String)c(DATA+"ShiftPreferences").getMethod("title",Context.class,String.class).invoke(null,a,code);
 }
 private static String printable(Object date)throws Exception{
  return (String)date.getClass().getMethod("displayLong").invoke(date);
 }
 public static void run(Activity a,String action){
  try{
   if(action.equals("calculate"))range(a,false);
   else if(action.equals("ics"))range(a,true);
   else if(action.equals("share"))share(a);
  }catch(Exception e){android.util.Log.e("ELUQEN_TOOL","Open failed",e);Toast.makeText(a,"این عملیات انجام نشد",Toast.LENGTH_LONG).show();}
 }
 private static void range(Activity a,boolean ics)throws Exception{
  Object[] dates={today(),plus(today(),ics?365:30)};
  LinearLayout form=new LinearLayout(a);form.setOrientation(android.widget.LinearLayout.VERTICAL);
  Button from=new Button(a),to=new Button(a);from.setAllCaps(false);to.setAllCaps(false);
  Runnable render=()->{try{from.setText("از: "+printable(dates[0]));to.setText("تا: "+printable(dates[1]));}catch(Exception ignored){}};
  from.setOnClickListener(v->DualDatePicker.show(a,dates[0],new DualDatePicker.Result(){public void selected(Object next){dates[0]=next;render.run();}}));
  to.setOnClickListener(v->DualDatePicker.show(a,dates[1],new DualDatePicker.Result(){public void selected(Object next){dates[1]=next;render.run();}}));
  form.addView(from);form.addView(to);render.run();
  new AlertDialog.Builder(a).setTitle(ics?"بازه خروجی ICS":"محاسبه شیفت")
   .setView(form).setNegativeButton("انصراف",null).setPositiveButton(ics?"ساخت فایل":"محاسبه",(dialog,which)->{
    try{if(ics)export(a,dates);else calculate(a,dates);}
    catch(Exception e){android.util.Log.e("ELUQEN_TOOL","Range error",e);Toast.makeText(a,"بازه معتبر نیست",Toast.LENGTH_LONG).show();}
   }).show();
 }
 private static void calculate(Activity a,Object[] days)throws Exception{
  Object plan=plan(a);long x=jdn(days[0]),y=jdn(days[1]);long start=Math.min(x,y),end=Math.max(x,y);
  if(end-start>36525){Toast.makeText(a,"حداکثر بازه ۱۰۰ سال است",Toast.LENGTH_SHORT).show();return;}
  Map<String,Integer> counts=new LinkedHashMap<>();
  for(long day=start;day<=end;day++){
   String type=title(a,token(a,plan,day));counts.put(type,counts.containsKey(type)?counts.get(type)+1:1);
  }
  StringBuilder report=new StringBuilder("تعداد روزهای بازه: "+(end-start+1)+"\n");
  for(Map.Entry<String,Integer> item:counts.entrySet())report.append(item.getKey()).append(": ").append(item.getValue()).append(" روز\n");
  new AlertDialog.Builder(a).setTitle("نتیجه محاسبه").setMessage(report.toString()).setPositiveButton("بستن",null).show();
 }
 private static void export(Activity a,Object[] dates)throws Exception{
  if(Math.abs(jdn(dates[0])-jdn(dates[1]))>3660){Toast.makeText(a,"حداکثر بازه فایل ۱۰ سال است",Toast.LENGTH_LONG).show();return;}
  pending.put(a,new Object[]{dates[0],dates[1]});
  Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
  intent.setType("text/calendar");intent.putExtra(Intent.EXTRA_TITLE,"ShiftCalendar.ics");
  a.startActivityForResult(intent,ICS);
 }
 private static void share(Activity a)throws Exception{
  Object now=today(),plan=plan(a);StringBuilder result=new StringBuilder("برنامه شیفت ۳۰ روز آینده\n\n");
  for(int i=0;i<30;i++){
   Object day=plus(now,i);result.append(printable(day)).append(" — ").append(title(a,token(a,plan,jdn(day)))).append("\n");
  }
  Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,result.toString());
  a.startActivity(Intent.createChooser(send,"اشتراک برنامه کاری"));
 }
 public static boolean result(Activity a,int request,int result,Intent data){
  if(request!=ICS)return false;
  Object[] dates=pending.remove(a);
  if(result!=Activity.RESULT_OK||data==null||data.getData()==null||dates==null)return true;
  Uri uri=data.getData();
  new Thread(()->{try{
   c(DATA+"BackupService").getMethod("writeIcs",Context.class,Uri.class,c(CORE+"PersianDate"),c(CORE+"PersianDate"))
    .invoke(null,a,uri,dates[0],dates[1]);
   a.runOnUiThread(()->Toast.makeText(a,"فایل ICS ذخیره شد",Toast.LENGTH_LONG).show());
  }catch(Exception e){android.util.Log.e("ELUQEN_TOOL","ICS write failed",e);a.runOnUiThread(()->Toast.makeText(a,"ساخت فایل ناموفق بود",Toast.LENGTH_LONG).show());}}).start();
  return true;
 }
}
