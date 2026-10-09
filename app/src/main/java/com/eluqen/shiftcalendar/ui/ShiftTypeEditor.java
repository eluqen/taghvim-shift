package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class ShiftTypeEditor {
 public interface Changed{void changed();}
 private interface IntChanged{void value(int v);}
 private static final String STATE="eluqen_shift_extra", TYPES="eluqen_shift_calendar_shift_types";
 private static final String[] BASE={"D","E","N","O"};
 private static final int[] COLORS={
  0xffd8942f,0xffc9537a,0xff5367c7,0xff177e79,0xff7e57c2,0xff00897b,
  0xffe65e4f,0xff3f51b5,0xff2e7d32,0xff607080,0xff795548,0xff00796b
 };
 private static final String[] COLOR_NAMES={"طلایی","صورتی","آبی","سبزآبی","بنفش","فیروزه‌ای","قرمز","نیلی","سبز","خاکستری","قهوه‌ای","سبز تیره"};
 private ShiftTypeEditor(){}
 private static int dp(Context c,int n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
 private static SharedPreferences state(Context c){return c.getSharedPreferences(STATE,0);}
 private static Object profile(Context c,String code)throws Exception{
  return Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences").getMethod("get",Context.class,String.class).invoke(null,c,code);
 }
 private static String str(Object p,String f)throws Exception{return String.valueOf(p.getClass().getField(f).get(p));}
 private static int integer(Object p,String f)throws Exception{return p.getClass().getField(f).getInt(p);}
 private static boolean bool(Object p,String f)throws Exception{return p.getClass().getField(f).getBoolean(p);}
 public static String title(Context c,String code){
  try{return str(profile(c,code),"title");}catch(Exception e){
   if("D".equals(code))return "روزکار";if("E".equals(code))return "عصرکار";if("N".equals(code))return "شبکار";if("O".equals(code))return "استراحت";return code;
  }
 }
 public static int color(Context c,String code){try{return integer(profile(c,code),"color");}catch(Exception e){return 0xff607080;}}
 private static boolean explicitCustom(Context c,String code){return code.startsWith("C")&&state(c).getBoolean(code,false);}
 public static List<String> onboardingCodes(Context c){
  ArrayList<String> out=new ArrayList<>(Arrays.asList(BASE));
  for(int i=1;i<=10;i++){String code="C"+i;if(explicitCustom(c,code))out.add(code);}
  return out;
 }
 private static Set<String> dbUsed(Context c){
  HashSet<String> out=new HashSet<>();
  try{
   Class<?> dbClass=Class.forName("com.eluqen.shiftcalendar.data.AppDatabase");
   Object db=dbClass.getMethod("get",Context.class).invoke(null,c);
   for(Object plan:(List<?>)dbClass.getMethod("plans").invoke(db)){
    String pattern=String.valueOf(plan.getClass().getField("pattern").get(plan));
    for(String x:pattern.split("[,،;؛|\\s]+"))if(!x.trim().isEmpty())out.add(x.trim().toUpperCase(Locale.US));
   }
  }catch(Exception ignored){}
  return out;
 }
 private static String freeCode(Context c){
  Set<String> used=dbUsed(c);SharedPreferences values=c.getSharedPreferences(TYPES,0);
  for(int i=1;i<=10;i++){
   String code="C"+i;
   if(!used.contains(code)&&!state(c).getBoolean(code,false)&&!values.contains(code+".title"))return code;
  }
  return null;
 }
 private static void save(Context c,String code,String title,String shortName,int start,int end,int color,boolean working,boolean reminder)throws Exception{
  Class<?> prefs=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences");
  Class<?> pc=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences$Profile");
  Constructor<?> ctor=pc.getConstructor(String.class,String.class,String.class,int.class,int.class,int.class,boolean.class,boolean.class);
  Object p=ctor.newInstance(code,title,shortName,start,end,color,working,working&&reminder);
  prefs.getMethod("save",Context.class,pc).invoke(null,c,p);
  if(code.startsWith("C"))state(c).edit().putBoolean(code,true).commit();
 }
 private static Button btn(Activity a,String t){
  Button b=new Button(a);b.setAllCaps(false);b.setText(t);b.setTextSize(13);b.setTextColor(0xff23465e);b.setMinHeight(0);return b;
 }
 private static EditText input(Activity a,String h){
  EditText e=new EditText(a);e.setHint(h);e.setSingleLine(true);e.setTextSize(14);e.setGravity(Gravity.RIGHT);e.setTextDirection(View.TEXT_DIRECTION_RTL);return e;
 }
 private static void chooseTime(Activity a,int current,IntChanged cb){
  int value=current<0?480:current;
  new TimePickerDialog(a,(v,h,m)->cb.value(h*60+m),value/60,value%60,true).show();
 }
 private static String time(int minute){
  if(minute<0)return "انتخاب نشده";
  return String.format(Locale.US,"%02d:%02d",minute/60,minute%60);
 }
 private static void chooseColor(Activity a,int current,IntChanged cb){
  int checked=0;for(int i=0;i<COLORS.length;i++)if(COLORS[i]==current)checked=i;
  final int[] selected={checked};
  new AlertDialog.Builder(a).setTitle("انتخاب رنگ").setSingleChoiceItems(FancyDialogs.single(a,COLOR_NAMES),checked,(d,w)->selected[0]=w)
   .setPositiveButton("تأیید",(d,w)->cb.value(COLORS[selected[0]])).setNegativeButton("انصراف",null).show();
 }
 private static void resync(Activity a){
  try{Class.forName("com.eluqen.shiftcalendar.widget.ShiftWidgetProvider").getMethod("updateAll",Context.class).invoke(null,a);}catch(Exception ignored){}
 }
 public static void manage(Activity a,Changed changed){
  final AlertDialog[] host={null};
  Changed done=()->{if(host[0]!=null)host[0].dismiss();if(changed!=null)changed.changed();};
  LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(a,12),dp(a,6),dp(a,12),dp(a,10));
  TextView help=new TextView(a);help.setText("نوع‌های اصلی را می‌توانید ویرایش کنید و هر نوع سفارشی موردنیازتان را هم با نام، ساعت، رنگ و وضعیت کاری خودش بسازید.");
  help.setTextSize(13);help.setTextColor(0xff647686);help.setGravity(Gravity.RIGHT);box.addView(help);
  for(String code:onboardingCodes(a)){
   Button b=btn(a,"ویرایش «"+title(a,code)+"»");b.setOnClickListener(v->edit(a,code,false,done));
   box.addView(b,new LinearLayout.LayoutParams(-1,dp(a,46)));
  }
  Button add=btn(a,"＋ افزودن نوع شیفت جدید");add.setTextColor(0xff17665c);add.setOnClickListener(v->{
   String code=freeCode(a);if(code==null){Toast.makeText(a,"ظرفیت نوع‌های سفارشی تکمیل شده است",Toast.LENGTH_SHORT).show();return;}
   edit(a,code,true,done);
  });box.addView(add,new LinearLayout.LayoutParams(-1,dp(a,48)));
  ScrollView scroll=new ScrollView(a);scroll.addView(box);
  host[0]=new AlertDialog.Builder(a).setTitle("مدیریت نوع‌های شیفت").setView(scroll).setPositiveButton("بستن",null).create();host[0].show();
 }
 private static void edit(Activity a,String code,boolean fresh,Changed changed){
  try{
   Object old=profile(a,code);
   String oldTitle=fresh?"":str(old,"title"),oldShort=fresh?"":str(old,"shortTitle");
   int[] start={fresh?480:integer(old,"startMinute")},end={fresh?960:integer(old,"endMinute")},color={fresh?0xff607080:integer(old,"color")};
   boolean base=!code.startsWith("C"),fixedOff="O".equals(code);
   boolean[] working={fresh?true:bool(old,"working")};
   LinearLayout form=new LinearLayout(a);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(a,14),dp(a,7),dp(a,14),dp(a,9));
   EditText title=input(a,"عنوان کامل؛ مثلاً کشیک یا شیفت ۱۲ ساعته");title.setText(oldTitle);
   EditText shortName=input(a,"عنوان کوتاه، حداکثر ۳ نویسه");shortName.setText(oldShort);
   form.addView(title);form.addView(shortName);
   CheckBox work=new CheckBox(a);work.setText("این نوع، شیفت کاری است");work.setChecked(fixedOff?false:working[0]);
   if(base)work.setEnabled(false);form.addView(work);
   Button from=btn(a,"شروع: "+time(start[0])),to=btn(a,"پایان: "+time(end[0]));
   form.addView(from);form.addView(to);
   from.setOnClickListener(v->chooseTime(a,start[0],x->{start[0]=x;from.setText("شروع: "+time(x));}));
   to.setOnClickListener(v->chooseTime(a,end[0],x->{end[0]=x;to.setText("پایان: "+time(x));}));
   Button paint=btn(a,"رنگ نوع شیفت");paint.setBackgroundColor(color[0]);paint.setTextColor(Color.WHITE);form.addView(paint);
   paint.setOnClickListener(v->chooseColor(a,color[0],x->{color[0]=x;paint.setBackgroundColor(x);paint.setTextColor(Color.WHITE);}));
   CheckBox reminder=new CheckBox(a);reminder.setText("برای این نوع شیفت یادآوری فعال باشد");reminder.setChecked(fresh||bool(old,"reminder"));form.addView(reminder);
   Runnable enable=()->{boolean on=work.isChecked()&&!fixedOff;from.setEnabled(on);to.setEnabled(on);reminder.setEnabled(on);if(!on)reminder.setChecked(false);};
   work.setOnCheckedChangeListener((v,on)->{working[0]=on;enable.run();});enable.run();
   ScrollView sc=new ScrollView(a);sc.addView(form);
   AlertDialog dialog=new AlertDialog.Builder(a).setTitle(fresh?"افزودن نوع شیفت":"ویرایش "+oldTitle).setView(sc)
    .setPositiveButton("ذخیره",null).setNegativeButton("انصراف",null).create();
   if(code.startsWith("C")&&!fresh)dialog.setButton(DialogInterface.BUTTON_NEUTRAL,"حذف", (DialogInterface.OnClickListener)null);
   dialog.setOnShowListener(x->{
    if(code.startsWith("C")&&!fresh){
     dialog.setButton(DialogInterface.BUTTON_NEUTRAL,"حذف",(d,w)->{});
     dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v->{
      if(dbUsed(a).contains(code)){Toast.makeText(a,"این نوع در یک الگوی ذخیره‌شده استفاده می‌شود و فعلاً قابل حذف نیست",Toast.LENGTH_LONG).show();return;}
      SharedPreferences.Editor e=a.getSharedPreferences(TYPES,0).edit();
      for(String k:new String[]{"title","short","start","end","color","working","reminder"})e.remove(code+"."+k);e.commit();
      state(a).edit().putBoolean(code,false).commit();dialog.dismiss();if(changed!=null)changed.changed();resync(a);
     });
    }
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{
     String n=title.getText().toString().trim(),sh=shortName.getText().toString().trim();
     if(n.isEmpty()||sh.isEmpty()||sh.length()>3){Toast.makeText(a,"عنوان کامل و عنوان کوتاه را درست وارد کنید",Toast.LENGTH_LONG).show();return;}
     boolean isWorking=!fixedOff&&work.isChecked();
     if(isWorking&&(start[0]<0||end[0]<0)){Toast.makeText(a,"ساعت شروع و پایان را مشخص کنید",Toast.LENGTH_LONG).show();return;}
     try{save(a,code,n,sh,isWorking?start[0]:-1,isWorking?end[0]:-1,color[0],isWorking,reminder.isChecked());
      dialog.dismiss();if(changed!=null)changed.changed();resync(a);
     }catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Save type",e);Toast.makeText(a,"ذخیره نوع شیفت ناموفق بود",Toast.LENGTH_LONG).show();}
    });
   });
   dialog.show();
  }catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Edit type",e);Toast.makeText(a,"امکان ویرایش نوع شیفت نیست",Toast.LENGTH_LONG).show();}
 }
}
