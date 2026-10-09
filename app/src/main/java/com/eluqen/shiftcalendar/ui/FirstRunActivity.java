package com.eluqen.shiftcalendar.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;

public final class FirstRunActivity extends Activity {
 private static final String[] PRESET_COMPANIES={"پلی اکریل ایران","فولاد مبارکه","ذوب آهن"};
 private LinearLayout body;
 private int page=0;

 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int fill,int stroke,int radius){
  GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));g.setStroke(dp(1),stroke);return g;
 }
 private TextView text(String value,float size,int color,boolean bold){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.RIGHT);
  if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 private Button button(String title,boolean primary){
  Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  b.setTextColor(primary?0xffffffff:0xff245c68);b.setBackground(bg(primary?0xff17665c:0xffedf5f4,primary?0xff17665c:0xff9cc8c0,13));
  b.setMinHeight(0);return b;
 }
 private LinearLayout.LayoutParams lp(int top){
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(top),0,0);return p;
 }
 private EditText input(String hint){
  EditText e=new EditText(this);e.setHint(hint);e.setTextSize(14);e.setTextColor(0xff21384e);e.setHintTextColor(0xff82909c);
  e.setGravity(Gravity.RIGHT);e.setTextDirection(View.TEXT_DIRECTION_RTL);e.setSingleLine(true);
  e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setBackground(bg(0xffffffff,0xffc8d7df,10));return e;
 }
 private void clear(){body.removeAllViews();}
 private void heading(String title,String sub){
  TextView h=text(title,22,0xff173b52,true);body.addView(h,lp(0));
  TextView s=text(sub,13.5f,0xff607383,false);s.setLineSpacing(dp(3),1.05f);body.addView(s,lp(7));
 }
 private Object db() throws Exception{
  return Class.forName("com.eluqen.shiftcalendar.data.AppDatabase").getMethod("get",android.content.Context.class).invoke(null,this);
 }
 private List<?> plans() throws Exception{return (List<?>)db().getClass().getMethod("plans").invoke(db());}
 private static String company(Object p)throws Exception{return String.valueOf(p.getClass().getField("company").get(p));}
 private static String team(Object p)throws Exception{return String.valueOf(p.getClass().getField("team").get(p));}
 private static long id(Object p)throws Exception{return p.getClass().getField("id").getLong(p);}
 private void selectPlan(Object plan){
  try{
   Object d=db();d.getClass().getMethod("setActivePlan",long.class).invoke(d,id(plan));complete();
  }catch(Exception e){toast("خطا در ذخیره انتخاب");android.util.Log.e("ELUQEN_FIRST","Select preset",e);}
 }
 private void complete(){
  try{
   Class.forName("com.eluqen.shiftcalendar.data.Prefs").getMethod("setOnboardingComplete",android.content.Context.class,boolean.class).invoke(null,this,true);
   try{Class.forName("com.eluqen.shiftcalendar.widget.ShiftWidgetProvider").getMethod("updateAll",android.content.Context.class).invoke(null,this);}catch(Exception ignored){}
   try{
    boolean enabled=(Boolean)Class.forName("com.eluqen.shiftcalendar.data.Prefs").getMethod("reminderEnabled",android.content.Context.class).invoke(null,this);
    if(enabled)new Thread(()->{try{Class.forName("com.eluqen.shiftcalendar.reminder.ReminderScheduler").getMethod("sync",android.content.Context.class).invoke(null,this);}catch(Exception ignored){}},"eluqen-first-run-reminder").start();
   }catch(Exception ignored){}
   Intent i=new Intent();i.setClassName(this,"com.eluqen.shiftcalendar.ui.LaunchActivity");
   startActivity(i);finish();
  }catch(Exception e){toast("خطا در تکمیل راه‌اندازی");android.util.Log.e("ELUQEN_FIRST","Complete",e);}
 }
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 public static void open(Activity a){
  Intent i=new Intent();i.setClassName(a,"com.eluqen.shiftcalendar.ui.FirstRunActivity");a.startActivity(i);a.finish();
 }

 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(0xfff5f8fa);
  TextView brand=text("ELUQEN",18,0xffffffff,true);brand.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
  brand.setPadding(dp(18),0,dp(18),0);brand.setBackgroundColor(0xff174b59);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(58)));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(20),dp(18),dp(28));scroll.addView(body);
  setContentView(root);showCompanies();
 }
 private void showCompanies(){
  page=0;clear();
  heading("راه‌اندازی تقویم شیفت","برای اینکه تقویم و محاسبات از همان ابتدا درست باشند، ابتدا محل کار خود را انتخاب کنید. این انتخاب فقط پس از انتخاب دقیق الگوی کاری ذخیره می‌شود.");
  List<String> companies=PlanManagement.companies(this);
  TextView label=text(companies.isEmpty()?"هیچ محل کاری تعریف نشده است":"شرکت‌ها و محل‌های کاری موجود",14,0xff536a7b,true);body.addView(label,lp(22));
  for(String c:companies){
   Button b=button(c,true);b.setOnClickListener(v->showPresetPlans(c));body.addView(b,lp(8));
  }
  TextView other=text("اگر محل کار شما در فهرست نیست، می‌توانید شرکت، بیمارستان، مجموعه خصوصی یا هر محل کار دیگری را همراه با الگوی مخصوص خودتان تعریف کنید.",13,0xff637685,false);
  other.setLineSpacing(dp(2),1.04f);body.addView(other,lp(20));
  Button custom=button("محل کار دیگر / سفارشی‌سازی",false);custom.setOnClickListener(v->showCustom());body.addView(custom,lp(9));
  if(PlanManagement.hasChanges(this)){
   TextView resetInfo=text("الگوهای کاری نسبت به حالت اولیه تغییر کرده‌اند. در صورت نیاز می‌توانید فقط بخش شرکت‌ها و الگوها را به پیش‌فرض برگردانید؛ اطلاعات کارکرد، یادداشت‌ها و تنظیمات یادآوری حذف نمی‌شوند.",12.5f,0xff7b6549,false);
   resetInfo.setLineSpacing(dp(2),1.04f);body.addView(resetInfo,lp(18));
   Button reset=button("بازگردانی الگوهای پیش‌فرض",false);reset.setOnClickListener(v->PlanManagement.confirmReset(this,()->showCompanies()));body.addView(reset,lp(8));
  }
 }
 private void showPresetPlans(String c){
  page=1;clear();
  heading(c,"گروه یا الگوی کاری خود را انتخاب کنید. بعد از انتخاب، برنامه مستقیماً با همین الگو راه‌اندازی می‌شود.");
  try{
   int found=0;
   for(Object p:plans())if(c.equals(company(p))){
    String t=team(p);Button b=button("گروه / الگو: "+t,true);b.setOnClickListener(v->selectPlan(p));body.addView(b,lp(8));found++;
   }
   if(found==0)body.addView(text("برای این شرکت الگویی پیدا نشد.",14,0xff9a3f3f,false),lp(15));
  }catch(Exception e){android.util.Log.e("ELUQEN_FIRST","Preset plans",e);body.addView(text("خطا در خواندن الگوهای شرکت",14,0xff9a3f3f,false),lp(15));}
  Button back=button("بازگشت به انتخاب شرکت",false);back.setOnClickListener(v->showCompanies());body.addView(back,lp(18));
 }

 private Object today(){
  try{return Class.forName("com.eluqen.shiftcalendar.core.PersianDate").getMethod("todayTehran").invoke(null);}
  catch(Exception e){return null;}
 }
 private void chooseDate(Object[] state,Button target){
  try{
   Class<?> date=Class.forName("com.eluqen.shiftcalendar.core.PersianDate");
   Class<?> listener=Class.forName("com.eluqen.shiftcalendar.ui.PersianDatePicker$Listener");
   Object proxy=Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},(p,m,args)->{
    if("onPicked".equals(m.getName())&&args!=null&&args.length>0){
     state[0]=args[0];target.setText("شروع الگوی چرخشی: "+fa(String.valueOf(args[0])));
    }
    return null;
   });
   Class.forName("com.eluqen.shiftcalendar.ui.PersianDatePicker").getMethod("show",Activity.class,date,listener).invoke(null,this,state[0],proxy);
  }catch(Exception e){android.util.Log.e("ELUQEN_FIRST","Date picker",e);toast("امکان باز کردن انتخابگر تاریخ نیست");}
 }
 private void renderSequence(ArrayList<String> raw,TextView sequence){
  if(raw.isEmpty()){sequence.setText("الگوی چرخشی هنوز ساخته نشده است.");return;}
  ArrayList<String> names=new ArrayList<>();
  for(int i=0;i<raw.size();i++)names.add((i+1)+"‌ـ "+ShiftTypeEditor.title(this,raw.get(i)));
  sequence.setText(fa(android.text.TextUtils.join("   ←   ",names)));
 }
 private void renderPalette(LinearLayout palette,ArrayList<String> raw,TextView sequence){
  palette.removeAllViews();
  List<String> codes=ShiftTypeEditor.onboardingCodes(this);
  for(Iterator<String> it=raw.iterator();it.hasNext();)if(!codes.contains(it.next()))it.remove();
  renderSequence(raw,sequence);
  TextView label=text("نوع شیفت را برای افزودن به روز بعدی لمس کنید:",12.5f,0xff617586,true);palette.addView(label);
  LinearLayout row=null;int pos=0;
  for(String code:codes){
   if(pos%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);palette.addView(row);}
   Button b=button(ShiftTypeEditor.title(this,code),false);b.setTextSize(12.5f);
   int color=ShiftTypeEditor.color(this,code);GradientDrawable g=new GradientDrawable();g.setColor(0xfff5f8fa);g.setCornerRadius(dp(11));g.setStroke(dp(2),color);b.setBackground(g);b.setTextColor(color);
   b.setOnClickListener(v->{if(raw.size()<120){raw.add(code);renderSequence(raw,sequence);}else toast("الگو نمی‌تواند بیش از ۱۲۰ روز باشد");});
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(2),dp(5),dp(2),0);row.addView(b,p);pos++;
  }
  if(pos%2==1&&row!=null){Space sp=new Space(this);row.addView(sp,new LinearLayout.LayoutParams(0,dp(44),1));}
 }
 private void showCustom(){
  page=2;clear();
  heading("تعریف محل کار و الگوی شخصی","نام محل کار را وارد کنید و مشخص کنید روزکار معمولی هستید یا برنامه شیفتی / چرخشی دارید.");
  final EditText company=input("نام شرکت، بیمارستان یا محل کار");body.addView(company,lp(18));

  LinearLayout mode=new LinearLayout(this);mode.setOrientation(LinearLayout.HORIZONTAL);mode.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button day=button("روزکار معمولی",true),cycle=button("شیفتی / چرخشی",false);
  LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,dp(46),1);half.setMargins(dp(3),0,dp(3),0);mode.addView(day,half);mode.addView(cycle,half);body.addView(mode,lp(12));

  final EditText team=input("نام گروه یا الگو؛ مثلاً A یا پرستاری");team.setText("A");body.addView(team,lp(12));
  final Object[] anchor={today()};
  final Button anchorButton=button("انتخاب تاریخ شروع الگوی چرخشی",false);
  if(anchor[0]!=null)anchorButton.setText("شروع الگوی چرخشی: "+fa(String.valueOf(anchor[0])));
  anchorButton.setOnClickListener(v->chooseDate(anchor,anchorButton));body.addView(anchorButton,lp(8));

  final LinearLayout builder=new LinearLayout(this);builder.setOrientation(LinearLayout.VERTICAL);builder.setPadding(dp(8),dp(9),dp(8),dp(10));builder.setBackground(bg(0xfff9fbfc,0xffd5e1e8,11));body.addView(builder,lp(10));
  final ArrayList<String> raw=new ArrayList<>();
  final TextView sequence=text("الگوی چرخشی هنوز ساخته نشده است.",13,0xff465e70,false);sequence.setLineSpacing(dp(2),1.05f);builder.addView(sequence);
  final LinearLayout palette=new LinearLayout(this);palette.setOrientation(LinearLayout.VERTICAL);builder.addView(palette,lp(5));
  renderPalette(palette,raw,sequence);

  Button manage=button("تعریف و ویرایش نوع‌های شیفت",false);manage.setOnClickListener(v->
   ShiftTypeEditor.manage(this,()->{renderPalette(palette,raw,sequence);})
  );builder.addView(manage,lp(8));

  LinearLayout edit=new LinearLayout(this);edit.setOrientation(LinearLayout.HORIZONTAL);edit.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button undo=button("حذف آخرین روز",false),clear=button("پاک‌کردن الگو",false);
  undo.setOnClickListener(v->{if(!raw.isEmpty())raw.remove(raw.size()-1);renderSequence(raw,sequence);});
  clear.setOnClickListener(v->{raw.clear();renderSequence(raw,sequence);});
  LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(0,dp(42),1);ep.setMargins(dp(2),dp(5),dp(2),0);edit.addView(undo,ep);edit.addView(clear,ep);builder.addView(edit);

  final boolean[] dayMode={true};
  Runnable refreshMode=()->{
   boolean d=dayMode[0];
   day.setTextColor(d?0xffffffff:0xff245c68);day.setBackground(bg(d?0xff17665c:0xffedf5f4,d?0xff17665c:0xff9cc8c0,13));
   cycle.setTextColor(!d?0xffffffff:0xff245c68);cycle.setBackground(bg(!d?0xff17665c:0xffedf5f4,!d?0xff17665c:0xff9cc8c0,13));
   team.setVisibility(d?View.GONE:View.VISIBLE);anchorButton.setVisibility(d?View.GONE:View.VISIBLE);builder.setVisibility(d?View.GONE:View.VISIBLE);
  };
  day.setOnClickListener(v->{dayMode[0]=true;refreshMode.run();});
  cycle.setOnClickListener(v->{dayMode[0]=false;refreshMode.run();});refreshMode.run();

  TextView note=text("بعداً از «تنظیمات ← مدیریت شیفت ← مدیریت الگوهای کاری» می‌توانید برای همین محل کار هر تعداد الگوی دیگر بسازید و نوع‌های شیفت، ساعت‌ها، رنگ‌ها و الگوها را ویرایش کنید.",12.5f,0xff667886,false);
  note.setLineSpacing(dp(2),1.05f);body.addView(note,lp(14));

  Button save=button("ذخیره و ورود به برنامه",true);save.setOnClickListener(v->{
   String c=company.getText().toString().trim();
   if(c.isEmpty()){toast("نام محل کار را وارد کنید");return;}
   if(dayMode[0])saveCustom(c,"روزکار","D",today(),true);
   else{
    String t=team.getText().toString().trim();if(t.isEmpty()){toast("نام گروه یا الگو را وارد کنید");return;}
    if(anchor[0]==null){toast("تاریخ شروع الگوی چرخشی را انتخاب کنید");return;}
    if(raw.isEmpty()){toast("الگوی شیفت چرخشی را بسازید");return;}
    saveCustom(c,t,android.text.TextUtils.join(",",raw),anchor[0],false);
   }
  });body.addView(save,lp(16));
  Button back=button("بازگشت",false);back.setOnClickListener(v->showCompanies());body.addView(back,lp(8));
 }
 private void saveCustom(String company,String team,String pattern,Object anchor,boolean dayWorker){
  try{
   if(anchor==null)anchor=today();
   long jdn=(Long)anchor.getClass().getMethod("toJdn").invoke(anchor);
   Class<?> engine=Class.forName("com.eluqen.shiftcalendar.core.ShiftEngine");
   Object parsed=engine.getMethod("parsePattern",String.class).invoke(null,pattern);
   int n=java.lang.reflect.Array.getLength(parsed);
   if(n<1||n>120){toast("الگوی چرخشی باید بین ۱ تا ۱۲۰ روز باشد");return;}
   Class<?> pc=Class.forName("com.eluqen.shiftcalendar.data.ShiftPlan");Object p=pc.getConstructor().newInstance();
   pc.getField("company").set(p,company);pc.getField("team").set(p,team);pc.getField("anchorJdn").setLong(p,jdn);pc.getField("pattern").set(p,pattern);pc.getField("active").setBoolean(p,true);
   Object d=db();long pid=(Long)d.getClass().getMethod("savePlan",pc).invoke(d,p);d.getClass().getMethod("setActivePlan",long.class).invoke(d,pid);
   complete();
  }catch(Exception e){toast("ذخیره الگوی کاری ناموفق بود");android.util.Log.e("ELUQEN_FIRST","Save custom",e);}
 }
 private static String fa(String s){
  if(s==null)return "";StringBuilder b=new StringBuilder();
  for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c>='0'&&c<='9')c=(char)('۰'+c-'0');b.append(c);}return b.toString();
 }
 @Override public void onBackPressed(){
  if(page==0){super.onBackPressed();return;}showCompanies();
 }
}
