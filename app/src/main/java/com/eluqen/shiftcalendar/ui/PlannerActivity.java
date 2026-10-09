package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;
import java.lang.reflect.*;
public final class PlannerActivity extends Activity {
 private LinearLayout list;
 private String mode="all";
 private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(15));return d;}
 private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Ui.TEXT);v.setGravity(Gravity.RIGHT);v.setPadding(dp(6),dp(6),dp(6),dp(6));return v;}
 private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Ui.SECONDARY);b.setMinHeight(0);b.setBackground(bg(Ui.SURFACE_ALT));return b;}
 private Button accentButton(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(bg(Ui.SECONDARY));return b;}
 private String stamp(long at){
  if(at<=0)return "بدون موعد";
  return new SimpleDateFormat("yyyy/MM/dd  HH:mm",Locale.US).format(new Date(at));
 }
 @Override public void onCreate(Bundle state){
  super.onCreate(state);AppTheme.apply(this);
  mode=getIntent().getStringExtra("mode");if(mode==null)mode="all";
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
   requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},911);
  build();
 }
 @Override protected void onResume(){super.onResume();PersonalAlarm.reschedule(this);render();BackDock.refresh(this);ScrollTopReset.top(this);}
 @Override public void onBackPressed(){BackDock.handleBack(this);}
 private void build(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(android.widget.LinearLayout.VERTICAL);root.setBackgroundColor(Ui.SURFACE);
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(android.widget.LinearLayout.VERTICAL);hero.setPadding(dp(14),dp(12),dp(14),dp(12));hero.setBackground(bg(Ui.SURFACE_ALT));
  TextView title=label(mode.equals("todo")?"فهرست کارها":mode.equals("event")?"یادآوری رخدادها":"برنامه‌ریزی شخصی",22);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setTextColor(Ui.PRIMARY);hero.addView(title);
  TextView note=label(mode.equals("event")?"موعدهای شخصی با تاریخ، ساعت و اعلان مستقل":"برنامه‌ریزی شخصی",13);note.setTextColor(Ui.MUTED);hero.addView(note);root.addView(hero);
  LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button todo=button("＋ کار جدید"),event=accentButton("＋ یادآوری جدید");
  actions.addView(todo,new LinearLayout.LayoutParams(0,dp(48),1));
  actions.addView(event,new LinearLayout.LayoutParams(0,dp(52),1));
  todo.setOnClickListener(v->edit(null,false));event.setOnClickListener(v->edit(null,true));
  todo.setVisibility(mode.equals("event")?View.GONE:View.VISIBLE);
  event.setVisibility(mode.equals("todo")?View.GONE:View.VISIBLE);
  LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2);alp.setMargins(dp(12),dp(8),dp(12),dp(6));root.addView(actions,alp);
  ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(android.widget.LinearLayout.VERTICAL);list.setPadding(dp(11),dp(5),dp(11),dp(22));
  scroll.addView(list);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);BackDock.install(this,root);
 }
 private void render(){
  if(list==null)return;list.removeAllViews();JSONArray values=PersonalStore.all(this);
  ArrayList<JSONObject> upcoming=new ArrayList<>(),done=new ArrayList<>();
  for(int i=0;i<values.length();i++){JSONObject x=values.optJSONObject(i);if(x==null)continue;
   if(!mode.equals("all")&&!mode.equals(x.optString("kind","todo")))continue;
   (x.optBoolean("done")?done:upcoming).add(x); }
  Collections.sort(upcoming,(a,b)->{
   long x=a.optLong("when",0),y=b.optLong("when",0);
   return Long.compare(x==0?Long.MAX_VALUE:x,y==0?Long.MAX_VALUE:y);
  });
  TextView status=label((mode.equals("event")?"یادآوری‌های فعال: ":"فعال: ")+upcoming.size()+"   •   انجام‌شده: "+done.size(),15);status.setTypeface(Typeface.DEFAULT,Typeface.BOLD);status.setTextColor(Ui.SECONDARY);status.setBackground(bg(Ui.SURFACE_ALT));list.addView(status);
  if(upcoming.isEmpty()){String msg;if(!done.isEmpty())msg=mode.equals("event")?"همه یادآوری‌های ثبت‌شده انجام شده‌اند.":"همه موارد ثبت‌شده انجام شده‌اند.";else msg=mode.equals("event")?"برای افزودن اولین یادآوری، دکمه «＋ یادآوری جدید» را بزنید.":"موردی برای نمایش نیست.";TextView empty=label(msg,14);empty.setBackground(bg(0xffeef5f8));list.addView(empty);}
  for(JSONObject x:upcoming)row(x,false);
  if(!done.isEmpty()){TextView h=label("انجام‌شده‌ها",17);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setTextColor(Ui.MUTED);h.setBackground(bg(Ui.SURFACE_ALT));list.addView(h);}
  for(JSONObject x:done)row(x,true);
 }
 private void row(JSONObject item,boolean done){
  int id=item.optInt("id");boolean todo="todo".equals(item.optString("kind","todo"));
  LinearLayout card=new LinearLayout(this);card.setOrientation(android.widget.LinearLayout.VERTICAL);
  card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackground(bg(done?Ui.SURFACE_ALT:Ui.CARD));
  TextView title=label(item.optString("title"),17);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  card.addView(title);
  boolean hasDate=item.optBoolean("has_date",item.optLong("when")>0),hasTime=item.optBoolean("has_time",item.optLong("when")>0);
  String whenText=!hasDate?"تاریخ تعیین نشده":new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date(item.optLong("when")))+(hasTime?"  ·  "+new SimpleDateFormat("HH:mm",Locale.US).format(new Date(item.optLong("when"))):"  ·  بدون ساعت");
  TextView summary=label((todo?"کار":"یادآوری رخداد")+"  •  "+whenText+(item.optBoolean("alert")?"  •  "+SoundPicker.title(item.optString("sound","default_notification")):"  •  بدون اعلان"),12);
  if(item.optLong("when")>0&&item.optLong("when")<System.currentTimeMillis()&&!done)summary.setTextColor(0xffb43c43);
  card.addView(summary);
  if(!item.optString("note").trim().isEmpty())card.addView(label(item.optString("note"),13));
  LinearLayout actions=new LinearLayout(this);actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
  Button edit=button("ویرایش");actions.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));edit.setOnClickListener(v->edit(item,!todo));
  Button finish=button(done?"بازگردانی":"انجام شد");actions.addView(finish,new LinearLayout.LayoutParams(0,dp(42),1));
  finish.setOnClickListener(v->{PersonalStore.setDone(this,id,!done);render();});
  Button remove=button("حذف");actions.addView(remove,new LinearLayout.LayoutParams(0,dp(42),1));
  remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف مورد").setMessage("«"+item.optString("title")+"» حذف شود؟")
   .setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->{PersonalStore.remove(this,id);render();}).show());
  card.addView(actions);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,dp(9));list.addView(card,lp);
 }
 private Object persianAt(long epoch)throws Exception{
  Calendar g=Calendar.getInstance();g.setTimeInMillis(epoch);
  return Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar")
   .getMethod("fromGregorian",int.class,int.class,int.class)
   .invoke(null,g.get(Calendar.YEAR),g.get(Calendar.MONTH)+1,g.get(Calendar.DAY_OF_MONTH));
 }
 private void chooseDate(long[] due,boolean[] hasDate,boolean[] hasTime,Runnable redraw){
  try{Object current=persianAt(hasDate[0]&&due[0]>0?due[0]:System.currentTimeMillis());DualDatePicker.show(this,current,(DualDatePicker.Result)(chosen->{
   try{Class<?> pd=Class.forName("com.eluqen.shiftcalendar.core.PersianDate");Object greg=Class.forName("com.eluqen.shiftcalendar.core.JalaliCalendar").getMethod("toGregorian",pd).invoke(null,chosen);
    int y=greg.getClass().getField("year").getInt(greg),m=greg.getClass().getField("month").getInt(greg),d=greg.getClass().getField("day").getInt(greg);Calendar c=Calendar.getInstance();c.setTimeInMillis(due[0]>0?due[0]:System.currentTimeMillis());
    c.set(y,m-1,d);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);if(!hasTime[0]){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);}due[0]=c.getTimeInMillis();hasDate[0]=true;redraw.run();
   }catch(Exception ex){android.util.Log.e("ELUQEN_PERSONAL","Date",ex);}}));
  }catch(Exception ex){android.util.Log.e("ELUQEN_PERSONAL","Picker",ex);Toast.makeText(this,"انتخاب تاریخ ناموفق بود",Toast.LENGTH_SHORT).show();}
 }
 private void edit(JSONObject original,boolean event){
  final long[] due={original==null?0:original.optLong("when")};final boolean[] hasDate={original!=null&&original.optBoolean("has_date",original.optLong("when")>0)},hasTime={original!=null&&original.optBoolean("has_time",original.optLong("when")>0)};
  final String[] sound={original==null?"default_notification":original.optString("sound","default_notification")};LinearLayout form=new LinearLayout(this);form.setOrientation(android.widget.LinearLayout.VERTICAL);form.setPadding(dp(16),0,dp(16),0);
  EditText name=new EditText(this);name.setSingleLine(true);name.setHint("عنوان رخداد، مثل موعد چک");name.setText(original==null?"":original.optString("title"));form.addView(name);
  EditText note=new EditText(this);note.setHint("توضیحات (اختیاری)");note.setText(original==null?"":original.optString("note"));form.addView(note);
  TextView date=label("",14);form.addView(date);Button choose=button("انتخاب / تغییر تاریخ");form.addView(choose);Button time=button("انتخاب / تغییر ساعت");form.addView(time);
  LinearLayout clears=new LinearLayout(this);clears.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);Button clearDate=button("حذف تاریخ"),clearTime=button("حذف ساعت");clears.addView(clearDate,new LinearLayout.LayoutParams(0,dp(44),1));clears.addView(clearTime,new LinearLayout.LayoutParams(0,dp(44),1));form.addView(clears);
  CheckBox alert=new CheckBox(this);alert.setText("اعلان و زنگ در تاریخ و ساعت تعیین‌شده");alert.setChecked(original==null?event:original.optBoolean("alert"));form.addView(alert);Button tone=button("");form.addView(tone);
  Runnable redraw=()->{String ds=hasDate[0]?new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date(due[0])):"انتخاب نشده";String ts=hasTime[0]?new SimpleDateFormat("HH:mm",Locale.US).format(new Date(due[0])):"انتخاب نشده";date.setText("تاریخ: "+ds+"     |     ساعت: "+ts);tone.setText("آهنگ: "+SoundPicker.title(sound[0]));tone.setEnabled(alert.isChecked());};
  redraw.run();choose.setOnClickListener(v->chooseDate(due,hasDate,hasTime,redraw));time.setOnClickListener(v->{Calendar c=Calendar.getInstance();c.setTimeInMillis(due[0]>0?due[0]:System.currentTimeMillis());new TimePickerDialog(this,(picker,h,min)->{Calendar x=Calendar.getInstance();x.setTimeInMillis(due[0]>0?due[0]:System.currentTimeMillis());x.set(Calendar.HOUR_OF_DAY,h);x.set(Calendar.MINUTE,min);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);due[0]=x.getTimeInMillis();hasTime[0]=true;redraw.run();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),true).show();});
  clearDate.setOnClickListener(v->{hasDate[0]=false;redraw.run();});clearTime.setOnClickListener(v->{hasTime[0]=false;if(due[0]>0){Calendar c=Calendar.getInstance();c.setTimeInMillis(due[0]);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);due[0]=c.getTimeInMillis();}redraw.run();});alert.setOnCheckedChangeListener((v,value)->redraw.run());tone.setOnClickListener(v->SoundPicker.open(this,sound[0],chosen->{sound[0]=chosen;redraw.run();}));
  ScrollView sc=new ScrollView(this);sc.addView(form);AlertDialog dlg=new AlertDialog.Builder(this).setTitle("یادآوری رخداد").setView(sc).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
  dlg.setOnShowListener(d->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()){name.setError("عنوان را وارد کنید");return;}if(alert.isChecked()&&(!hasDate[0]||!hasTime[0])){Toast.makeText(this,"برای اعلان، تاریخ و ساعت را جداگانه تعیین کنید",Toast.LENGTH_LONG).show();return;}try{JSONObject value=original==null?new JSONObject():new JSONObject(original.toString());value.put("title",name.getText().toString().trim());value.put("note",note.getText().toString().trim());value.put("kind","event");value.put("when",due[0]);value.put("has_date",hasDate[0]);value.put("has_time",hasTime[0]);value.put("alert",alert.isChecked());value.put("sound",sound[0]);value.put("fired",false);PersonalStore.save(this,value);dlg.dismiss();render();if(alert.isChecked()&&due[0]>System.currentTimeMillis())permissionHint();}catch(Exception ex){Toast.makeText(this,"ثبت انجام نشد: "+ex.getMessage(),Toast.LENGTH_LONG).show();}}));dlg.show();
 }
 private void permissionHint(){
  if(Build.VERSION.SDK_INT>=31){
   AlarmManager am=(AlarmManager)getSystemService(Context.ALARM_SERVICE);
   if(am!=null&&!am.canScheduleExactAlarms())
    new AlertDialog.Builder(this).setTitle("مجوز یادآوری دقیق")
     .setMessage("موعد ثبت شد، اما بدون مجوز زمان‌بندی دقیق ممکن است اعلان با تأخیر برسد. مجوز را در تنظیمات اندروید فعال کنید.")
     .setNegativeButton("بعداً",null).setPositiveButton("تنظیمات",(d,w)->{
      try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(android.net.Uri.parse("package:"+getPackageName())));}
      catch(Exception e){Toast.makeText(this,"مجوز را از تنظیمات برنامه فعال کنید",Toast.LENGTH_LONG).show();}
     }).show();
  }
 }
 @Override protected void onActivityResult(int request,int result,Intent data){
  if(!SoundPicker.result(this,request,result,data))super.onActivityResult(request,result,data);
 }
}
