package com.eluqen.shiftcalendar.ui;
import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.eluqen.shiftcalendar.data.Prefs;

public final class GeneralSettingsActivity extends BaseActivity {
 static final String EXTRA_RESET_SCROLL = "eluqen_reset_settings_scroll";
 private static final String STATE_SCROLL_Y = "settings_scroll_y";
 private SettingsFeaturesUi.Row reminderRow;
 private ScrollView settingsScroll;
 private boolean resetScrollOnResume;
 private String renderedThemeKey;
 private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private android.content.SharedPreferences prefs(){return getSharedPreferences("eluqen_shift_calendar_preferences",0);}

 private GradientDrawable rounded(int color,float radius){
  GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;
 }
 private TextView text(String s,float size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.RIGHT);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
 }
 private void open(String name){
  startActivity(new Intent().setClassName(this,"com.eluqen.shiftcalendar.ui."+name));
 }
 private LinearLayout section(LinearLayout body,String title){
  return SettingsFeaturesUi.section(this,body,title);
 }
 private void divider(LinearLayout box){SettingsFeaturesUi.divider(this,box);}
 private void action(LinearLayout box,String title,String detail,String target){
  action(box,title,detail,()->open(target));
 }
 private void action(LinearLayout box,String title,String detail,Runnable run){
  SettingsFeaturesUi.row(this,box,title,detail,SettingsFeaturesUi.kind(title),run);
 }
 private void reminderAction(LinearLayout box){
  reminderRow=SettingsFeaturesUi.row(this,box,"یادآوری شروع شیفت",
    "زمان اعلان، مجوزها و صدای هشدار",1,()->open("ReminderSettingsActivity"));
  refreshReminderIndicator();
 }
 private void refreshReminderIndicator(){
  if(reminderRow!=null)reminderRow.active(Prefs.reminderEnabled(this));
 }
 private void addShiftSection(LinearLayout body){
  LinearLayout box=section(body,"شیفت و برنامه کاری");
  action(box,"الگوهای کاری","انتخاب، ساخت، ویرایش و حذف الگوی کاری","SettingsActivity");divider(box);
  action(box,"مشخصات شیفت‌ها","عنوان، ساعت و رنگ شیفت‌های اصلی و سفارشی","ShiftTypesActivity");divider(box);
  reminderAction(box);divider(box);
  action(box,"همکاران","دفترچه همکاران و ارتباط سریع","ColleaguesActivity");
 }
 private void addDisplaySection(LinearLayout body){
  LinearLayout box=section(body,"نمایش و شروع برنامه");
  LinearLayout start=new LinearLayout(this);start.setOrientation(LinearLayout.VERTICAL);start.setPadding(dp(14),dp(10),dp(14),dp(8));
  start.addView(text("صفحه شروع برنامه",16,Ui.TEXT,true));
  Spinner spinner=new Spinner(this);
  String[] labels={"تقویم ماهانه","همه امکانات","تقویم چهارشیفت","کارکرد","تنظیمات"};
  String[] values={"calendar","features","four_shift","cardex","settings"};
  spinner.setAdapter(FancyDialogs.spinner(this,labels));
  String saved=prefs().getString("default_screen","calendar");int selected=0;
  for(int i=0;i<values.length;i++)if(values[i].equals(saved)||("graphic".equals(saved)&&"settings".equals(values[i])))selected=i;
  spinner.setSelection(selected,false);
  spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
   public void onNothingSelected(android.widget.AdapterView<?> p){}
   public void onItemSelected(android.widget.AdapterView<?> p,View v,int i,long id){
    if(!values[i].equals(prefs().getString("default_screen","calendar")))prefs().edit().putString("default_screen",values[i]).apply();
   }});
  start.addView(spinner,new LinearLayout.LayoutParams(-1,dp(52)));box.addView(start);divider(box);
  action(box,"تم برنامه","تغییر هماهنگ رنگ رابط با حفظ خوانایی","ThemeSettingsActivity");divider(box);
  action(box,"ویجت صفحه اصلی","انتخاب رنگ و افزودن ویجت","WidgetSettingsActivity");
 }
 private void addCalendarSection(LinearLayout body){
  LinearLayout box=section(body,"تقویم و مناسبت‌ها");
  action(box,"تنظیم دستی تاریخ قمری","پیش‌نمایش اصلاح تاریخ؛ تغییر فقط با ذخیره اعمال می‌شود",()->HubGeneral.openHijri(this));divider(box);
  action(box,"مناسبت‌های شخصی","افزودن، حذف و علامت‌گذاری روز تعطیل",()->HubGeneral.openPersonalEvents(this));
 }
 private void addWorkSection(LinearLayout body){
  LinearLayout box=section(body,"محاسبه کارکرد");
  Switch lunch=new Switch(this);lunch.setText("کسر بازه ۱۲ تا ۱۳ از استراحت و مرخصی");
  lunch.setTextSize(14);lunch.setMinHeight(dp(56));lunch.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{Ui.SECONDARY,Ui.MUTED}));lunch.setTextColor(Ui.TEXT);lunch.setPadding(dp(14),dp(8),dp(14),dp(8));
  lunch.setChecked(prefs().getBoolean("exclude_lunch_hour",false));
  lunch.setOnCheckedChangeListener((v,on)->prefs().edit().putBoolean("exclude_lunch_hour",on).apply());
  box.addView(lunch,new LinearLayout.LayoutParams(-1,-2));
 }
 private void addDataSection(LinearLayout body){
  LinearLayout box=section(body,"داده و پشتیبان‌گیری");
  action(box,"ساخت فایل پشتیبان","ذخیره اطلاعات برنامه در یک فایل",()->HubGeneral.createBackup(this));divider(box);
  action(box,"بازیابی فایل پشتیبان","بازیابی اطلاعات از فایل پس از تأیید نهایی",()->HubGeneral.restoreBackup(this));
 }
 private void addUpdateSupportSection(LinearLayout body){
  LinearLayout box=section(body,"به‌روزرسانی و پشتیبانی");
  action(box,"مرکز به‌روزرسانی","نسخه برنامه، منبع دریافت و بانک مناسبت‌ها","UpdateCenterActivity");divider(box);
  action(box,"درباره برنامه","نسخه، حریم خصوصی و راه‌های ارتباطی ELUQEN","AboutActivity");divider(box);
  action(box,"ارتباط با توسعه‌دهنده","دیدگاه‌ها و پیشنهادهای خود را برای بهبود نسخه‌های آینده با ما در میان بگذارید.",()->feedback());
 }
 private void feedback(){
  Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:dev@eluqen.com"));
  i.putExtra(Intent.EXTRA_SUBJECT,"ELUQEN Taghvim Shift — Feedback");
  try{startActivity(i);}catch(Exception e){Toast.makeText(this,"برنامه ایمیل در گوشی پیدا نشد",Toast.LENGTH_LONG).show();}
 }

 @Override protected void onCreate(Bundle state){
  super.onCreate(state);
  resetScrollOnResume = state == null;
  buildSettingsUi(state == null ? 0 : state.getInt(STATE_SCROLL_Y,0));
 }
 private void buildSettingsUi(int restoreY){
  // Repaint the existing settings activity after a theme change, not the activity stack.
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(Ui.SURFACE);
  ScrollView scroll=new ScrollView(this);settingsScroll=scroll;scroll.setFillViewport(false);scroll.setClipToPadding(false);
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),0,dp(16),dp(28));
  scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  addShiftSection(body);addDisplaySection(body);addCalendarSection(body);
  addWorkSection(body);addDataSection(body);addUpdateSupportSection(body);
  setContentView(root);
  renderedThemeKey=AppTheme.currentKey(this);
  if(restoreY>0)scroll.post(()->scroll.scrollTo(0,restoreY));
 }
 @Override protected void onNewIntent(Intent intent){
  super.onNewIntent(intent);setIntent(intent);
  if(intent!=null&&intent.getBooleanExtra(EXTRA_RESET_SCROLL,false))resetScrollOnResume=true;
 }
 @Override protected void onSaveInstanceState(Bundle outState){
  if(settingsScroll!=null)outState.putInt(STATE_SCROLL_Y,settingsScroll.getScrollY());
  super.onSaveInstanceState(outState);
 }
 @Override protected void onResume(){
  super.onResume();
  if(SettingsThemeRefresh.needsRepaint(renderedThemeKey,AppTheme.currentKey(this))){
   int previousScroll=settingsScroll==null?0:settingsScroll.getScrollY();
   buildSettingsUi(previousScroll);
  }
  refreshReminderIndicator();
  if(resetScrollOnResume){ScrollTopReset.top(this);resetScrollOnResume=false;}
 }
 @Override protected void onActivityResult(int request,int response,Intent data){
  if(HubGeneral.result(this,request,response,data))return;
  super.onActivityResult(request,response,data);
 }
}
