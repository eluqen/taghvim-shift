package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public final class ShiftExtras {
 private static final String STORE="eluqen_shift_extra",TYPES="eluqen_shift_calendar_shift_types";
 private static final String[] BASIC={"D","E","N","O"};
 private ShiftExtras(){}
 private static SharedPreferences state(Context c){return c.getSharedPreferences(STORE,0);}
 private static int dp(Context c,int v){return (int)(v*c.getResources().getDisplayMetrics().density+.5);}
 private static Object prefs(String method,Context c,String code) throws Exception{
  Class<?> cls=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences");
  return cls.getMethod(method,Context.class,String.class).invoke(null,c,code);
 }
 private static String str(Object obj,String field) throws Exception{return (String)obj.getClass().getField(field).get(obj);}
 private static int integer(Object obj,String field) throws Exception{return obj.getClass().getField(field).getInt(obj);}
 private static boolean bool(Object obj,String field) throws Exception{return obj.getClass().getField(field).getBoolean(obj);}
 private static Set<String> used(Context c){
  Set<String> set=new HashSet<>();
  try{
   Class<?> cls=Class.forName("com.eluqen.shiftcalendar.data.AppDatabase");
   Object db=cls.getMethod("get",Context.class).invoke(null,c);
   set.addAll((Set<String>)cls.getMethod("cardexShiftCodes").invoke(db));
   for(Object plan:(List<?>)cls.getMethod("plans").invoke(db)){
    String pat=(String)plan.getClass().getField("pattern").get(plan);
    for(String part:pat.split("[,،;؛|\\s]+"))set.add(part.trim().toUpperCase(java.util.Locale.US));
   }
  }catch(Exception e){set.addAll(Arrays.asList("D","E","N","O","C1","C2"));}
  return set;
 }
 public static String[] visibleCodes(Context c){
  SharedPreferences flags=state(c),values=c.getSharedPreferences(TYPES,0);
  Set<String> used=used(c);ArrayList<String> all=new ArrayList<>();
  for(String b:BASIC)if(!flags.getBoolean("hidden_"+b,false)||used.contains(b))all.add(b);
  for(int i=1;i<=12;i++){
   String code="C"+i;
   if(flags.getBoolean(code,false)||used.contains(code)||values.contains(code+".title"))all.add(code);
  }
  return all.toArray(new String[0]);
 }
 public static void restoreBase(Context c){
  SharedPreferences.Editor edit=state(c).edit();
  for(String code:BASIC)edit.remove("hidden_"+code);
  edit.commit();
 }
 public static void enableImported(Context c,String code){
  if(code.matches("C(?:[1-9]|1[0-2])"))state(c).edit().putBoolean(code,true).commit();
 }
 private static int countCustom(Context c){
  int count=0;for(String s:visibleCodes(c))if(s.startsWith("C"))count++;return count;
 }
 private static void save(Context c,String code,String title,String shortName,int start,int end,int color,boolean working,boolean reminder) throws Exception{
  Class<?> cls=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences");
  Class<?> item=Class.forName("com.eluqen.shiftcalendar.data.ShiftPreferences$Profile");
  Constructor<?> ctor=item.getConstructor(String.class,String.class,String.class,int.class,int.class,int.class,boolean.class,boolean.class);
  Object profile=ctor.newInstance(code,title,shortName,start,end,color,working,working&&reminder);
  cls.getMethod("save",Context.class,item).invoke(null,c,profile);
 }
 private static void reschedule(Context c){
  WidgetStudio.refresh(c);
  new Thread(()->{
   try{Class.forName("com.eluqen.shiftcalendar.reminder.ReminderScheduler").getMethod("sync",Context.class).invoke(null,c.getApplicationContext());}
   catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Reminder sync failed",e);}
  },"eluqen-shift-resync").start();
 }
 private static void redraw(Activity a,String newCode){
  try{
   Method method=a.getClass().getDeclaredMethod("render");method.setAccessible(true);method.invoke(a);
   Field f=a.getClass().getDeclaredField("list");f.setAccessible(true);
   LinearLayout list=(LinearLayout)f.get(a);ScrollView sc=(ScrollView)list.getParent();
   int index=Arrays.asList(visibleCodes(a)).indexOf(newCode)+1;
   View card=index>0&&index<list.getChildCount()?list.getChildAt(index):list;
   sc.post(()->sc.scrollTo(0,Math.max(0,card.getTop()-dp(a,10))));
  }catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Render failed",e);a.recreate();}
 }
 private static TextView caption(Activity a,String title){
  TextView t=new TextView(a);t.setText(title);t.setTextSize(14);t.setTextColor(Ui.TEXT);
  t.setPadding(dp(a,4),dp(a,9),dp(a,4),dp(a,6));return t;
 }
 private static EditText input(Activity a,String hint){
  EditText e=new EditText(a);e.setSingleLine(true);e.setTextSize(15);e.setHint(hint);
  e.setTextColor(Ui.TEXT);return e;
 }
 private static Button button(Activity a,String title){
  Button b=new Button(a);b.setText(title);b.setAllCaps(false);b.setTextSize(13);
  b.setTextColor(Ui.TEXT);return b;
 }
 private static void time(Activity a,Button button,int[] state,int defaultHour,String label){
  new TimePickerDialog(a,(picker,h,m)->{state[0]=h*60+m;button.setText(label+String.format(java.util.Locale.US,"%02d:%02d",h,m));},
   state[0]>=0?state[0]/60:defaultHour,state[0]>=0?state[0]%60:0,true).show();
 }
 private static void add(Activity a){
  if(countCustom(a)>=10){Toast.makeText(a,"حداکثر ۱۰ نوع شیفت سفارشی",Toast.LENGTH_SHORT).show();return;}
  String code=null;Set<String> used=used(a);
  for(int i=1;i<=10;i++){
   String key="C"+i;if(!used.contains(key)&&!Arrays.asList(visibleCodes(a)).contains(key)){code=key;break;}
  }
  if(code==null){Toast.makeText(a,"جای خالی برای شیفت جدید وجود ندارد",Toast.LENGTH_SHORT).show();return;}
  final String newCode=code;
  LinearLayout form=new LinearLayout(a);form.setOrientation(android.widget.LinearLayout.VERTICAL);form.setPadding(dp(a,14),dp(a,7),dp(a,14),dp(a,9));
  EditText title=input(a,"عنوان کامل (ضروری)"),shortName=input(a,"عنوان کوتاه، حداکثر ۳ نویسه");
  form.addView(title);form.addView(shortName);
  CheckBox working=new CheckBox(a);working.setText("این نوع، شیفت کاری است");form.addView(working);
  int[] start={-1},end={-1},color={0};
  Button from=button(a,"ساعت شروع: انتخاب نشده"),to=button(a,"ساعت پایان: انتخاب نشده");
  form.addView(from);form.addView(to);
  from.setOnClickListener(v->time(a,from,start,8,"شروع: "));to.setOnClickListener(v->time(a,to,end,16,"پایان: "));
  Button paint=button(a,"انتخاب رنگ شیفت");form.addView(paint);
  paint.setOnClickListener(v->ShiftVisuals.choose(a,ShiftVisuals.COLORS,color[0],chosen->{color[0]=chosen;ShiftVisuals.selection(paint,chosen);}));
  CheckBox remind=new CheckBox(a);remind.setText("برای این نوع شیفت یادآوری ساخته شود");form.addView(remind);
  working.setOnCheckedChangeListener((v,on)->{from.setEnabled(on);to.setEnabled(on);remind.setEnabled(on);if(!on)remind.setChecked(false);});
  from.setEnabled(false);to.setEnabled(false);remind.setEnabled(false);
  ScrollView scroll=new ScrollView(a);scroll.addView(form);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("افزودن شیفت سفارشی")
   .setView(scroll).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
  dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(btn->{
   String name=title.getText().toString().trim(),shortLabel=shortName.getText().toString().trim();
   if(name.isEmpty()||shortLabel.isEmpty()||color[0]==0){
    Toast.makeText(a,"عنوان، نام کوتاه و رنگ باید انتخاب شوند",Toast.LENGTH_LONG).show();return;
   }
   if(name.length()>24){title.setError("عنوان کامل حداکثر ۲۴ نویسه");title.requestFocus();return;}
   if(shortLabel.length()>3){shortName.setError("عنوان کوتاه حداکثر ۳ نویسه");shortName.requestFocus();return;}
   if(working.isChecked()&&(start[0]<0||end[0]<0)){
    Toast.makeText(a,"ساعت شروع و پایان شیفت را مشخص کنید",Toast.LENGTH_LONG).show();return;
   }
   try{
    save(a,newCode,name,shortLabel,working.isChecked()?start[0]:-1,working.isChecked()?end[0]:-1,color[0],working.isChecked(),remind.isChecked());
    state(a).edit().putBoolean(newCode,true).commit();
    dialog.dismiss();redraw(a,newCode);reschedule(a);
    Toast.makeText(a,"شیفت جدید ایجاد شد",Toast.LENGTH_SHORT).show();
   }catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Create failed",e);Toast.makeText(a,"ذخیره شیفت ناموفق بود",Toast.LENGTH_LONG).show();}
  }));
  dialog.show();
 }
 private static void remove(Activity a,String code,LinearLayout list,View card){
  if(used(a).contains(code)){
   new AlertDialog.Builder(a).setTitle("این شیفت در برنامه یا کارکرد استفاده می‌شود")
    .setMessage("برای جلوگیری از تغییر ناخواسته تقویم، ابتدا این نوع را در چرخه‌های کاری و تعویض شیفت یا اضافه‌کاری ثبت‌شده جایگزین کنید.")
    .setPositiveButton("متوجه شدم",null).show();return;
  }
  new AlertDialog.Builder(a).setTitle("حذف "+code+"؟")
   .setMessage("این نوع از فهرست حذف شود؟ این عملیات روی شیفت‌های سایر الگوها اثری ندارد.")
   .setNegativeButton("انصراف",null).setPositiveButton("حذف",(dialog,which)->{
    SharedPreferences.Editor e=state(a).edit();
    if(code.startsWith("C")){
     e.putBoolean(code,false);
     SharedPreferences.Editor values=a.getSharedPreferences(TYPES,0).edit();
     for(String key:new String[]{"title","short","start","end","color","working","reminder"})values.remove(code+"."+key);
     values.commit();
    }else e.putBoolean("hidden_"+code,true);
    e.commit();list.removeView(card);reschedule(a);
   }).show();
 }
 public static void decorate(Activity a,LinearLayout list){
  String[] visible=visibleCodes(a);
  for(int i=0;i<visible.length;i++){
   int index=i+1;if(index>=list.getChildCount())break;
   View card=list.getChildAt(index);if(!(card instanceof LinearLayout))continue;
   String code=visible[i];LinearLayout row=new LinearLayout(a);
   row.setGravity(Gravity.LEFT);row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
   Button trash=button(a,"🗑");trash.setContentDescription("حذف "+code);
   trash.setTextSize(21);trash.setTextColor(0xffb7354d);
   row.addView(trash,new LinearLayout.LayoutParams(dp(a,48),dp(a,42)));
   ((LinearLayout)card).addView(row,0);
   trash.setOnClickListener(v->remove(a,code,list,card));
  }
  Button plus=button(a,"＋ افزودن شیفت سفارشی");
  plus.setTextColor(Color.WHITE);GradientDrawable g=new GradientDrawable();
  g.setColor(Ui.PRIMARY);g.setCornerRadius(dp(a,15));plus.setBackground(g);
  plus.setOnClickListener(v->add(a));list.addView(plus,new LinearLayout.LayoutParams(-1,dp(a,52)));
  boolean hidden=false;for(String code:BASIC)hidden|=state(a).getBoolean("hidden_"+code,false);
  if(hidden){
   Button restore=button(a,"بازگردانی انواع اصلی حذف‌شده");
   restore.setOnClickListener(v->{restoreBase(a);redraw(a,"D");});
   list.addView(restore,new LinearLayout.LayoutParams(-1,dp(a,48)));
  }
 }
 public static void patternButtons(Activity a,LinearLayout dialog,EditText field){
  String[] choices=visibleCodes(a);LinearLayout line=null;int pos=0;
  for(String code:choices){
   if(!code.startsWith("C"))continue;
   if(pos%3==0){line=new LinearLayout(a);dialog.addView(line);}
   String label=code;
   try{Object p=prefs("get",a,code);label=str(p,"title");}catch(Exception ignored){}
   Button b=button(a,label);b.setContentDescription("افزودن "+label+" به الگو");line.addView(b,new LinearLayout.LayoutParams(0,dp(a,46),1));
   b.setOnClickListener(v->{String old=field.getText().toString().trim();field.setText(old.isEmpty()?code:old+", "+code);field.setSelection(field.length());});
   pos++;
  }
 }
 public static void reminderEditor(Activity a){
  String[] codes=visibleCodes(a),labels=new String[codes.length];
  if(codes.length==0){Toast.makeText(a,"ابتدا یک نوع شیفت تعریف کنید",Toast.LENGTH_SHORT).show();return;}
  for(int i=0;i<codes.length;i++){
   try{Object p=prefs("get",a,codes[i]);labels[i]=str(p,"title")+" ("+codes[i]+")";}
   catch(Exception e){labels[i]=codes[i];}
  }
  new AlertDialog.Builder(a).setTitle("ساعت و یادآوری کدام شیفت؟").setAdapter(FancyDialogs.list(a,labels),(d,index)->editReminder(a,codes[index]))
   .setNegativeButton("انصراف",null).show();
 }
 private static void editReminder(Activity a,String code){
  try{
   Object profile=prefs("get",a,code);
   String name=str(profile,"title"),shortName=str(profile,"shortTitle");int color=integer(profile,"color");
   boolean working=bool(profile,"working");int[] start={integer(profile,"startMinute")},end={integer(profile,"endMinute")};
   LinearLayout form=new LinearLayout(a);form.setPadding(dp(a,14),dp(a,9),dp(a,14),dp(a,9));form.setOrientation(android.widget.LinearLayout.VERTICAL);
   form.addView(caption(a,name));Button from=button(a,"شروع: "+(start[0]<0?"مشخص نشده":format(start[0])));
   Button to=button(a,"پایان: "+(end[0]<0?"مشخص نشده":format(end[0])));
   from.setEnabled(working);to.setEnabled(working);form.addView(from);form.addView(to);
   from.setOnClickListener(v->time(a,from,start,8,"شروع: "));to.setOnClickListener(v->time(a,to,end,16,"پایان: "));
   CheckBox remind=new CheckBox(a);remind.setText("یادآوری این نوع شیفت فعال باشد");
   remind.setChecked(bool(profile,"reminder"));remind.setEnabled(working);form.addView(remind);
   new AlertDialog.Builder(a).setTitle("زمان و اعلان "+name).setView(form)
    .setNegativeButton("انصراف",null).setPositiveButton("ذخیره",(dialog,which)->{
     try{save(a,code,name,shortName,start[0],end[0],color,working,remind.isChecked());reschedule(a);}
     catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Reminder edit failed",e);Toast.makeText(a,"خطا در ذخیره",Toast.LENGTH_LONG).show();}
    }).show();
  }catch(Exception e){android.util.Log.e("ELUQEN_SHIFT","Editor failed",e);Toast.makeText(a,"امکان بارگذاری تنظیمات شیفت نیست",Toast.LENGTH_LONG).show();}
 }
 private static String format(int minute){return String.format(java.util.Locale.US,"%02d:%02d",minute/60,minute%60);}
}
